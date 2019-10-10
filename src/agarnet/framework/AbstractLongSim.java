package agarnet.framework;

import java.awt.Dimension;
import java.util.HashSet;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.nongnu.multigraph.Graph;
import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.SimpleGraph;
import org.nongnu.multigraph.debug;
import org.nongnu.multigraph.PartitionGraph;
import org.nongnu.multigraph.PartitionGraph.PartitionCallbacks;

import agarnet.link.link;
import agarnet.link.unilink;
import agarnet.protocols.host.PositionableHost;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;


/**
 * An abstract simulation, implementing a framework for a simulation of H
 * hosts identified by I ids.  Hooks are provided for concrete
 * implementations to update the state of nodes at appropriate points.
 *
 * @author paul
 *
 * @param <T>
 * @param <H>
 */
public abstract class AbstractLongSim<H extends PositionableHost<Long,H>>
				extends Simulation2D<Long,H>
				implements Observer {
  /* so we can ignore observer events initially */
  protected boolean doing_network_setup = true;
  protected idmap<H> idmap = new sync_idmap<> ();
  protected sim_stats sim_stats = new sim_stats ();
  private PartitionGraph<H,link<H>> partition_graph;
  
  protected class sim_stats {
    /* node Tx path is multi-threaded now */
    private AtomicLong messages_sent = new AtomicLong ();
    public long get_messages_sent () { return messages_sent.get (); }
    private long ticks;
    public long get_ticks () { return ticks; }
  }

  /**
   * Hook for concrete implementations, to allow Host object
   * to be retrieved, and possibly created, for any given Id.
   *
   * XXX: Should be factored out.
   *
   * @param Long id of the host
   * @return Existing or new H host object for the given Id.
   */
  protected abstract H get_host (Long id);
  
  public void new_node (Long id, H s) {
    idmap.put (id, s);
  }
  
  @Override
  public Long node2id (H s) {
    return idmap.get (s);
  }
  
  @Override
  public H id2node (Long l) {
    return idmap.get (l);
  }
  
  public H id2node (String sl) {
    long l;
    H n;
    
    try {
      l = Long.parseLong (sl);
    } catch (NumberFormatException e) {
      debug.println (debug.levels.WARNING, "Invalid host identifier: " + sl);
      return null;
    }
    
    if ((n = id2node (l)) != null)
      return n;
    
    return get_host (l);
  }
  
  static private class order_partition<N,E> implements PartitionCallbacks<N,E> {
    private int count = 0;
    private int num = Math.max (Runtime.getRuntime ().availableProcessors () - 2, 1);
    private Map<N,Integer> map = new HashMap<> ();

    @Override
    public Graph<N,E> create_graph () {
      return new SimpleGraph<> ();
    }

    @Override
    public int num_partitions () {
      return num;
    }

    @Override
    public int node2partition (N n) {
      if (map.containsKey (n))
        return map.get (n);
      int partition = count;
      count = (count + 1) % num;
      map.put (n, partition);
      return partition;
    }
  }
  public AbstractLongSim (Dimension d) {
    super (new PartitionGraph<H,link<H>> (new order_partition<H,link<H>> ()), d);
    partition_graph = (PartitionGraph<H, link<H>>) network;
    network.addObserver (this);
  }
  
  final protected void reset_network () {
    for (H p : network) {
      p.reset ();
      for (Edge<H,link<H>> e : network.edges (p))
        e.label ().reset ();
    }
  }
  
  /**
   * Setup code for each run. The default implementation is to 
   * rewire the graph, reset every node and edge in the network.
   */
  protected void run_setup (int run) {
    reset_network ();
    rewire ();
    rewire_update_hosts ();
  }
  
  /**
   * Hook for initial setup of the hosts, prior to any runs.
   * Default implementation simply calls the add_initial_hosts hook.
   */
  protected void initial_setup () {
    add_initial_hosts ();
  }

  

  private class partition_runner extends Thread {
    Set<H> partition;
    final synchronisation_gate nodeg, edgeg, doneg;
    final int pid;

    partition_runner (int id, Set<H> partition,
                      synchronisation_gate nodeg,
                      synchronisation_gate edgeg,
                      synchronisation_gate doneg) {
      this.nodeg = nodeg;
      this.edgeg = edgeg;
      this.doneg = doneg;
      this.partition = partition;
      this.pid = id;
    }
    @Override
    public String toString () {
      return String.format ("partition(id: %d, tid: %d, size: %d %s / %s / %s)",
                            pid, Thread.currentThread ().getId (),
                            partition.size (), nodeg, edgeg, doneg);
    }
    @Override
    public void run () {
      
      while (true) {
        nodeg.wait_ready ();
        for (final H n : partition) {
          n.tick ();
          debug.printf ("%s: tick node %s\n", this, n);
        }
        
        edgeg.wait_ready ();
        for (final H n : partition) {
          debug.printf ("%s: ticking edges of %s\n", this, n);
          tick_edges_of (n);
        }
        doneg.wait_ready ();
      }
    }
  }

  List<partition_runner> partition_runners = new LinkedList<> ();
  synchronisation_gate node_gate;
  synchronisation_gate edge_gate;
  synchronisation_gate done_gate;
  
  final public void main_loop () {
    debug.println ("initial setup");
    network.plugObservable ();

    node_gate = new synchronisation_gate (partition_graph.partitions () + 1);
    edge_gate = new synchronisation_gate (partition_graph.partitions () + 1);
    done_gate = new synchronisation_gate (partition_graph.partitions () + 1);
    for (int i = 0; i < partition_graph.partitions (); i++) {
      Set<H> partition = partition_graph.partition (i);
      partition_runner pr = new partition_runner (i, partition,
                                                  node_gate,
                                                  edge_gate,
                                                  done_gate);
      partition_runners.add (pr);
      pr.start ();
      debug.println ("started partition " + pr);
    }

    initial_setup ();
    
    doing_network_setup = false;
    
    /* runs == 0 is a special-case meaning:
     * "setup the graph and describe it, but don't run the
     * "protocol"
     */
    for (int i = 0; (i == 0 && get_runs () == 0) || i <= get_runs (); i++) {
      System.out.println ("\n# starting run " + i + " / " + get_runs ());
      
      network.plugObservable ();
      sim_stats.ticks = 0;
      sim_stats.messages_sent.set (0);
      run_setup (i);
      setChanged ();
      notifyObservers ();
      network.unplugObservable ();

      describe_begin (i);
      
      long start = System.currentTimeMillis ();
      if (get_runs () > 0)
        run ();
      long fin = System.currentTimeMillis ();
      
      System.out.printf ("Runtime: %.3f seconds\n", (float)(fin - start) / 1000);
      describe_end (i);
    }

    for (final partition_runner pr : partition_runners)
      pr.stop ();
  }
  
  private static class recent_activity {
    boolean [] ac;
    int head = 0;
    
    recent_activity (int extent, boolean initial) {
      ac = new boolean[extent];
      for (int i = 0; i < extent; i++)
        ac[i] = initial;
    }
    
    void set (boolean state) {
      ac[head] = state;
      head = (head + 1) % ac.length;
    }
    
    boolean was_there (boolean kind) {
      for (int i = 0; i < ac.length; i++)
        if (ac[i] == kind)
          return true;
      return false;
    }
  }
  
  abstract protected int get_runs ();
  abstract protected void describe_begin (int run);
  abstract protected void describe_end (int run);
  abstract protected void rewire ();
  abstract protected boolean get_random_tick ();
  abstract protected void add_initial_hosts ();
  
  /**
   * Retrieve the period for the simulation: a granularity of time
   * in simulation ticks within which the simulation is certain to
   * have made progress, if there is any progress to be made.
   * 
   * In particular, this period is used to judge when the simulation
   * no longer has made progress. So long as as any link sees activity
   * AND the simulation has not converged in the last 'period' ticks
   * of the simulation, it will continue to run. Otherwise, the
   * simulation is judged to be quiescent and will terminate.
   * @return simulation time ticks over which to consider activity
   */ 
  abstract protected int get_period ();
  /**
   * A period of real time, in milliseconds, to sleep between each iteration
   * of the simulation's discrete time ticks. This is useful if the simulation's
   * results are being animated.
   * @return sleep time in milliseconds
   */
  abstract protected int get_sleep ();
  /**
   * Callback in which to perturb the state of the simulation, e.g. to
   * move hosts around and/or delete/add links between hosts. Must
   * setChanged() accordingly.
   */
  abstract protected void perturb ();
  
  /* the main loop of the simulation */
  protected void run () {
    recent_activity ra_link = new recent_activity (get_period (), true);
    recent_activity ra_notconverged 
      = new recent_activity (get_period (), false);
    int sleep = get_sleep ();

    debug.printf ("gate states:\nnode_gate %s\nedge_gate %s\ndone_gate %s\n",
                  node_gate, edge_gate, done_gate);
    while (ra_link.was_there (true) || ra_notconverged.was_there (false)) {
      /* Links have to be ticked over separately from nodes, otherwise
       * a message might get across multiple nodes and links in just one tick. 
       */
      //Iterable<H> netiter
      //  = this.get_random_tick () ? network.random_node_iterable ()
      //                            : network;
      
      debug.println ("wait to queue nodes");
      node_gate.wait_ready ();
      
      debug.println ("wait to queue links");
      edge_gate.wait_ready ();
      
      debug.println ("wait till links are done");
      done_gate.wait_ready ();
      debug.println ("links done");
      
      perturb ();
      
      ra_link.set (this.hasChanged ());
      ra_notconverged.set (this.has_converged ());
      
      debug.printf ("was there? ra_link %s, ra_notconv %s\n",
                    ra_link.was_there (true),
                    ra_notconverged.was_there (false));
      
      notifyObservers ();
      
      sim_stats.ticks++;
      
      if (sleep == 0)
        continue;

      if (sleep > 0)
        try {
          Thread.sleep (sleep);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
    }
  }
  
  /***
   * Indicate whether or not the simulation has converged. I.e. has ceased to
   * make progress. Concrete implementations may wish to override this and
   * use more specific knowledge of the simulation and types of hosts to
   * find cases where a simulation has converged.
   * @return
   */
  protected boolean has_converged () {
    boolean converged = true;
    for (H h : network)
      if (h.hasChanged ()) {
        this.setChanged ();
        converged = false;
        break;
      }
    return converged;
  }
  
  @Override
  final public Set<Long> connected (Long node) {
    Set<Long> ids = new HashSet<> ();
    for (H s : network.successors (idmap.get (node)))
        ids.add (idmap.get (s));
    return ids;
  }
  
  private void tick_edges_of (H h) {
    for (Edge<H,link<H>> e : network.edges (h)) {
      debug.printf ("%s: edge %s\n", h, e);
      byte [] data;

      unilink<H> ul = e.label ().get (h);
      
      if (ul.size () > 0) {
        setChanged ();
        debug.printf ("%s: changed sim, before tick\n", h);
      }
      
      ul.tick ();
      
      /* dequeue from the link */
      while ((data = ul.poll ()) != null) {
        //debug.out.printf ("dequeue from %s up to %s, edge: %s\n", e.to (), h, e);
        h.up (idmap.get (e.to ()), data);
      }
      
      if (ul.size () > 0) {
        setChanged ();
        debug.printf ("%s: changed sim, after tick\n", h);
      }
    }
  }
  
  @Override
  final public boolean tx (Long from, Long to, byte [] data) {
    H hfrom = idmap.get (from);
    H hto = idmap.get (to);
    Edge<H, link<H>> edge = network.edge (hfrom, hto);
    
    debug.printf ("tx %d to %d, edge: %s\n", from, to, edge);
    
    if (edge == null) {
      debug.printf (debug.levels.ERROR, "tx called for non-existent edge %s -> %s!",
                    from, to);
      return false;
    }

    setChanged ();
    sim_stats.messages_sent.getAndIncrement ();
    
    return edge.label ().get (hto). offer (data);
  }
  
  @Override
  final public void update (Observable o, Object arg) {
    debug.printf ("update, in setup: %s, object: %s\n",
                  doing_network_setup, ((H) null));
    if (doing_network_setup)
      return;
    
    setChanged ();
    notifyObservers ();
    
    /* we have to notify all hosts, so long as we don't have an in-sim
     * routing protocol
     */
    //if (arg != null)
    //  ((N) arg).link_update ();
    //else
      for (H h : network)
        h.link_update ();
  }
  
  protected void rewire_update_hosts () {
    for (H p : network)
      rewire_update_host (p);
    setChanged ();
  }
  
  /**
   * Hook, called after network has been rewired, to update a host.
   * Default implementation updates the mass of nodes with their degree.
   */
  protected void rewire_update_host (H host) {
    /* set the mass according to the degree, useful for things like
     * Force-Directed Layout.
     */
    host.setMass (network.nodal_outdegree (host));
  }
}
