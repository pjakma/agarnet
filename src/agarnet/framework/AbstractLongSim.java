package agarnet.framework;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;

import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.SimpleGraph;
import org.nongnu.multigraph.debug;

import agarnet.link.link;
import agarnet.protocols.host.PositionableHost;

/**
 * An abstract simulation, implementing a framework for a simulation using Long
 * identifiers and PositionableHosts. Hooks are provided for concrete implementations
 * to update the state of nodes at appropriate points.
 * @author paul
 *
 * @param <T>
 * @param <H>
 */
public abstract class AbstractLongSim<H extends PositionableHost<Long,H>> 
				extends Simulation<Long,H>
				implements Observer {
  /* so we can ignore observer events initially */
  protected boolean doing_network_setup = true;
  protected idmap<H> idmap = new idmap<H> ();
  protected sim_stats sim_stats = new sim_stats ();
  
  protected class sim_stats {
    private long messages_sent = 0;
    public long get_messages_sent () { return messages_sent; }
    private long ticks;
    public long get_ticks () { return ticks; }
  }

  /**
   * Hook for concrete implementations, to allow Host object
   * to be retrieved, and possibly created, for any given Id.
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
  
  public AbstractLongSim (Dimension d) {
    super (new SimpleGraph<H,link<H>> (), d);
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
  
  final public void main_loop () {
    debug.println ("initial setup");
    network.plugObservable ();
    initial_setup ();
    
    doing_network_setup = false;
    
    for (H p : network)
      p.link_update ();
    
    int runs = get_runs ();
    
    /* runs == 0 is a special-case meaning:
     * "setup the graph and describe it, but don't run the
     * "protocol"
     */
    for (int i = 0; runs == 0 || i < runs; i++) {
      System.out.println ("\n# starting run " + i);
      
      network.plugObservable ();
      run_setup (i);
      setChanged ();
      notifyObservers ();
      network.unplugObservable ();

      describe_begin ();
      
      long start = System.currentTimeMillis ();
      if (runs > 0)
        run ();
      long fin = System.currentTimeMillis ();
      
      System.out.printf ("Runtime: %.3f seconds\n", (float)(fin - start) / 1000);
      describe_end ();
      
      if (runs == 0)
        runs--;
    }
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
  abstract protected void describe_begin ();
  abstract protected void describe_end ();
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
  final void run () {
    sim_stats.ticks = 0;
    sim_stats.messages_sent = 0;
    recent_activity ra_link = new recent_activity (get_period (), true);
    recent_activity ra_notconverged 
      = new recent_activity (get_period (), false);
    
    while (ra_link.was_there (true) || ra_notconverged.was_there (false)) {
      /* Links have to be ticked over separately from nodes, otherwise
       * a message might get across multiple nodes and links in just one tick. 
       */
      Iterable<H> netiter
        = this.get_random_tick () ? network.random_node_iterable ()
                                  : network;
      
      for (H p : netiter)
        for (Edge<H,link<H>> e : network.edges (p)) {
          e.label ().get (p).tick ();
        }
      
      /* now tick the nodes and deliver any messages to them */
      for (H p : netiter) {
        p.tick ();
        
        for (Edge<H,link<H>> e : network.edges (p)) {          
          /* dequeue from the link to the connected peer */
          byte [] data;
          
          if (e.label ().size () > 0)
            setChanged ();
          
          while ((data = e.label ().get (p).poll ()) != null) {
            e.to ().up (idmap.get (p), data);
          }
        }
        
        if (p.hasChanged ())
          setChanged ();
      }
      
      perturb ();
      
      ra_link.set (this.hasChanged ());
      ra_notconverged.set (this.has_converged ());
      
      debug.printf ("was there? ra_link %s, ra_notconv %s\n",
                    ra_link.was_there (true),
                    ra_notconverged.was_there (false));
      
      notifyObservers ();
      
      sim_stats.ticks++;
      
      if (get_sleep () == 0)
        continue;
      
      try {
        Thread.sleep (get_sleep ());
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
  protected boolean has_converged () { return false; }
  
  @Override
  final public Set<Long> connected (Long node) {
    Set<Long> ids = new HashSet<Long> ();
    for (H s : network.successors (idmap.get (node)))
        ids.add (idmap.get (s));
    return ids;
  }
  
  @Override
  final public boolean tx (Long from, Long to, byte [] data) {
    Edge<H, link<H>> edge = network.edge (idmap.get (from), idmap.get (to));
    
    if (edge == null) {
      debug.printf (debug.levels.ERROR, "tx called for non-existent edge %s -> %s!",
                    from, to);
      return false;
    }
    sim_stats.messages_sent++;
    
    return edge.label ().get (idmap.get (from)). offer (data);
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
