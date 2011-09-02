package kcoresim;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.nongnu.multigraph.debug;

import agarnet.data.marshall;
import agarnet.framework.Simulation;
import agarnet.protocols.AbstractProtocol;

public class kcore<N> extends AbstractProtocol<Long> {
  /* for connected hosts */
  Simulation<Long,N> sim;
  /* currently connected hosts */
  Set<Long> connected = new HashSet<Long> ();
  int kbound;
  /* The generation of the algorithm, and
   * the ID of the gen setter.
   */
  long generation;
  boolean genupdated;
  
  /* Last received message from neighbour */
  Map<Long,neighbour_msg> neighbours = new HashMap<Long,neighbour_msg>();
  
  public kcore (Simulation<Long, N> sim) {
    super ();
    this.sim = sim;
    this.reset ();
  }
  
  private neighbour_msg get_default_new_nmsg () {
    return new neighbour_msg (selfId, generation, kbound);
  }
  private neighbour_msg get_default_new_nmsg (int kbound) {
    return new neighbour_msg (selfId, generation, kbound);
  }
  
  private void generation_update () {
    if (genupdated)
      return;
    
    generation++;
    genupdated = true;
  }
  private void generation_update (long gen) {
    if (gen < generation)
      return;
    
    generation = gen;
    genupdated = true;
  }
  
  boolean generation_check (neighbour_msg m) {
    neighbour_msg prevm;
    
    if (m.gen > generation
        || ((prevm = neighbours.get (m.srcid)) != null
            && prevm.gen < m.gen)) {
      generation_update (m.gen);
      return true;
    }
    return false;
  }
  
  @Override
  public void reset () {
    super.reset ();
    kbound = 0;
    neighbours.clear ();
    connected.clear ();
    generation_update (1L);
  }
  
  @Override
  public long stat_get (int ordinal) {
    if (ordinal == stat.stored.ordinal ())
      return kbound;
    return super.stat_get (ordinal);
  }
  
  @Override
  public void link_update () {
    Set<Long> newc = sim.connected (this.selfId);
    
    debug.println ("link_update...");
    
    if (newc != null) {
      /* Find new neighbours */
      for (Long neigh : newc) {
        if (!connected.contains (neigh)) {
          /* add default message, with new degree, for new neighbour */
          generation_update ();
          neighbours.put (neigh, get_default_new_nmsg (newc.size ()));
        }
      }
      /* Find removed neighbours */
      Iterator<Long> it = connected.iterator ();
      while (it.hasNext ()) {
        Long neigh = it.next ();
        if (!newc.contains (neigh)) {
          it.remove ();
          neighbours.remove (neigh);
        }
      }
    } else
      neighbours.clear ();
    
    connected = newc;
    
    if (kbound () || genupdated)
      broadcast (kbound);
  }
  
  private void broadcast (int kbound) {
    debug.printf ("%d\n", kbound);
    
    for (Long neigh : connected)
      send (neigh, get_default_new_nmsg ());
    
    genupdated = false;
  }
  
  int calc_kbound () {
    int kbound = connected.size ();
    int [] seen = new int [connected.size () + 1];
    int highestseen = 0;
    
    debug.printf ("neighb vals: %s\n", neighbours.values ());
    
    for (neighbour_msg nmsg : neighbours.values ()) {
      int neigh_bound = (nmsg.gen == generation) ? nmsg.kbound
                                                 : connected.size ();
      
      int i = Math.min (neigh_bound, kbound);
      seen[i]++;
      highestseen = Math.max (highestseen, i);
    }
    
    if (debug.applies ()) {
      StringBuilder sbseen = new StringBuilder ();
      
      for (int i = 0; i < seen.length; i++)
        sbseen.append (" " + seen[i]);
      
      debug.printf ("seen: %s\n", sbseen);
    }
    
    for (int i = highestseen; i >= 0; i--) {
      int tot = 0;
      
      kbound = i;
      
      for (int j = i; j < seen.length; j++)
        tot += seen[j];
      
      debug.printf ("i %d, tot %d\n", i, tot);
      
      if (tot < i)
        continue;
      
      break;
    }
    
    debug.printf ("result %d\n", kbound);
    
    return kbound;
  }
  
  private boolean kbound () {
    int origkbound = kbound;
    
    debug.printf ("start, orig %d\n", origkbound);
    kbound = calc_kbound ();
    debug.printf ("got %d\n", kbound);
    
    return (origkbound != kbound);
  }
  
  @Override
  public void up(Long src, byte[] data) {
    debug.printf ("peer %s: receive msg from %s\n", this, src);
    neighbour_msg m = null;
    
    try {
      m = marshall.deserialise (m, data);
    } catch (Exception e) {
      debug.println (debug.levels.ERROR, "Unhandled message!");
      return;
    }
        
    if (m.srcid != src.longValue ()) {
      debug.printf (debug.levels.ERROR,
                    "src %d doesn't match packet Id %d!\n",
                    src, m.srcid);
      return;
    }
    
    stats_inc (stat.recvd);
    neighbours.put (src, m);
    
    generation_check (m);
    
    debug.printf ("Received %s from %d\n", m, src);
    
    if (kbound () || genupdated)
      broadcast (kbound);
  }
}
