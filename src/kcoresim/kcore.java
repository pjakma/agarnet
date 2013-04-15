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
    genupdated = true;
    
    if (gen <= generation)
      return;
    
    generation = gen;
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
    
    debug.printf ("%d: link_update...\n", selfId);
    
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
          /* no generation update is required for removes */
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
    debug.printf ("%d: %d\n", selfId, kbound);
    
    for (Long neigh : connected)
      send (neigh, get_default_new_nmsg ());
    
    genupdated = false;
  }
  
  int calc_kbound () {
    int kbound = connected.size ();
    int [] seen = new int [connected.size () + 1];
    int highestseen = 0;
        
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
      
      debug.printf ("%d: seen: %s\n", selfId, sbseen);
    }
    
    for (int i = highestseen, tot = 0; i >= 0; i--) {
      kbound = i;
      
      tot += seen[i];
      
      if (tot < i)
        continue;
      
      break;
    }
    
    debug.printf ("%d: result %d\n", selfId, kbound);
    
    return kbound;
  }
  
  private boolean kbound () {
    int origkbound = kbound;
    
    debug.printf ("%d: start, orig %d\n", selfId, origkbound);
    kbound = calc_kbound ();
    debug.printf ("%d: got %d\n", selfId, kbound);
    
    return (origkbound != kbound);
  }
  
  @Override
  public void up(Long src, byte[] data) {
    debug.printf ("%s: receive msg from %s\n", selfId, src);
    neighbour_msg m = null;
    
    try {
      m = marshall.deserialise (m, data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, "%d: Unhandled message!\n", selfId);
      return;
    }
        
    if (m.srcid != src.longValue ()) {
      debug.printf (debug.levels.ERROR,
                    "src %d doesn't match packet Id %d!\n",
                    src, m.srcid);
      return;
    }
    
    if (neighbours.get (m.srcid) == null) {
      debug.printf (debug.levels.ERROR, "%d: Unknown neighour %d!\n", selfId, src);
    }
    
    stats_inc (stat.recvd);
    
    generation_check (m);
    
    neighbours.put (src, m);
    
    debug.printf ("%s: Received %s from %d\n", selfId, m, src);
    
    if (kbound () || genupdated)
      broadcast (kbound);
  }
  
  public String toString () {
    StringBuilder sb = new StringBuilder ();

    sb.append (super.toString ());
    sb.append ("\nkcore: kb/gen/deg: (" + this.kbound);
    sb.append ("," + this.generation);
    sb.append ("," + this.connected.size ());
    sb.append ("\nconnected:\n" + this.connected);
    sb.append ("\nneighbours:");
    for (Long neigh : neighbours.keySet ()) {
      neighbour_msg m = neighbours.get (neigh);
      if (m.kbound >= this.kbound) {
        simhost remote = (simhost) sim.id2node (m.srcid );
        sb.append (String.format ("\n  msg from %d (degree %d): ",
                   m.srcid, remote.degree ()));
        sb.append (String.format ("got/actual (%d,%d) (%d,%d)%s",
                                  m.gen, m.kbound,
                                  remote.kgen (), remote.kbound (),
                                  (m.gen != remote.kgen ()
                                   || m.kbound != remote.kbound ()) ? "!" : ""
                                  ));
      }
    }
    sb.append ('\n');
    return sb.toString ();
  }
}
