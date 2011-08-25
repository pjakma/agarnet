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
  protected abstract H get_host (Long id);
  
  /* map Graph node objects to stable, persistent IDs that protocols can use */
  private class idmap<N> {
    Map<Long,N> id2simh = new HashMap<Long,N> ();
    Map<N,Long> simh2id = new HashMap<N,Long> ();
    
    synchronized public Long get (N n) {
      if (n == null)
        throw new AssertionError ("idmap: node must not be null!");
      
      return simh2id.get (n);
    }
    
    synchronized public void put (Long l, N n) {
      if (n == null)
        throw new AssertionError ("idmap put: node must not be null!");
      if (l == null)
        throw new AssertionError ("idmap put: Long key must not be null!");
      
      if (id2simh.get (l) != null)
        throw new AssertionError ("idmap put: Long key already exists!");
      if (simh2id.get (n) != null)
        throw new AssertionError ("idmap put: Node already registered!");
      
      if (id2simh.put (l, n) != null)
        throw new AssertionError ("id already exists, impossible, wtf?" + l);
      
      simh2id.put (n, l);
      
      return;
    }
    synchronized public N get (Long l) {
      return id2simh.get (l);
    }
    
    synchronized public String toString () {
      StringBuilder sb = new StringBuilder ();
      for (N s : simh2id.keySet ()) {
        sb.append (s);
        sb.append (" -> ");
        sb.append (get (s));
        sb.append (" (back?: ");
        sb.append (id2simh.containsKey (get(s)));
        sb.append (")\n");
      }
      return sb.toString ();
    }
  }
  
  public void new_node (Long id, H s) {
    idmap.put (id, s);
  }
  
  public Long node2id (H s) {
    return idmap.get (s);
  }
  
  public H id2node (Long l) {
    return idmap.get (l);
  }
  
  public H id2node (String sl) {
    long l;
    H n;
    
    try {
      l = Long.parseLong (sl);
    } catch (NumberFormatException e) {
      debug.println ("Invalid host identifier: " + sl);
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
  
  /**
   * Setup code for each run. The default implementation is to 
   * reset every node and edge in the network.
   */
  protected void run_setup (int run) {
    /* reset the network, not needed first time around, but no harm
     * in exercising the code anyway
     */
    for (H p : network) {
      p.reset ();
      for (Edge<H,link<H>> e : network.edges (p))
        e.label ().reset ();
    }
  }
  
  final public void main_loop () {    
    doing_network_setup = false;
    int runs = get_runs ();
    
    initial_setup ();
    
    for (int i = 0; i < runs; i++) {
      System.out.println ("# starting run " + i);
      
      run_setup (i);
      
      rewire ();
      
      describe_begin ();
      
      long start = System.currentTimeMillis ();
      run ();
      long fin = System.currentTimeMillis ();
      
      System.out.printf ("Runtime: %.3f seconds\n", (float)(fin - start) / 1000);
      describe_end ();
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
  abstract protected void initial_setup ();
  
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
      debug.println ("ticking links");
      
      Iterable<H> nib = this.get_random_tick () ? network.random_node_iterable ()
                                                : network;
      
      for (H p : nib)
        for (Edge<H,link<H>> e : network.edges (p)) {
          debug.printf ("tick link %s\n", e);
          e.label ().get (p).tick ();
        }
      
      /* now tick the nodes and deliver any messages to them */
      debug.println ("ticking nodes");
      for (H p : nib) {
        p.tick ();
        
        for (Edge<H,link<H>> e : network.edges (p)) {          
          /* dequeue from the link to the connected peer */
          byte [] data;
          
          if (e.label ().size () > 0)
            setChanged ();
          
          debug.printf (" check link: %s\n", e);
          
          while ((data = e.label ().get (p).poll ()) != null) {
            debug.printf ("Dequeue on link %s\n", e);  
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
    
    debug.printf ("tx: %s %s -> %s\n", data, from, to);
    
    if (edge == null) {
      debug.printf ("tx called for non-existent edge %s -> %s!",
                    from, to);
      return false;
    }
    sim_stats.messages_sent++;
    
    return edge.label ().get (idmap.get (from)). offer (data);
  }
  
  final public void update (Observable o, Object arg) {
    debug.printf ("update, in setup: %s, object: %s\n",
                  doing_network_setup, ((H) null));
    if (doing_network_setup)
      return;
    
    /* we have to notify all hosts, so long as we don't have an in-sim
     * routing protocol
     */
    //if (arg != null)
    //  ((N) arg).link_update ();
    //else
      for (H h : network)
        h.link_update ();
  }
}
