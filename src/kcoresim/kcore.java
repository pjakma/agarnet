package kcoresim;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.nongnu.multigraph.debug;

import agarnet.framework.Simulation;
import agarnet.protocols.AbstractProtocol;

public final class kcore<N> extends AbstractProtocol<Long> {
  /* for connected hosts */
  Simulation<Long,N> sim;
  /* currently connected hosts */
  Set<Long> connected = new HashSet<> ();
  int kbound;
  /* The generation of the algorithm, and
   * the ID of the gen setter.
   */
  long generation;
  boolean genupdated;
  
  /* Last received kbound message from neighbour */
  Map<Long,neighbour_msg> neighbours = new HashMap<>();
  
  public kcore (Simulation<Long, N> sim) {
    super ();
    this.sim = sim;
    this.reset ();
  }
  
  private neighbour_msg get_default_new_nmsg () {
    return neighbour_msg.new_kbound_msg (selfId, generation, kbound);
  }
  private neighbour_msg get_default_new_nmsg (int kbound) {
    return neighbour_msg.new_kbound_msg (selfId, generation, kbound);
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
    if (m.gen <= generation)
      return false;

    if (m.gen > generation && m.value < kbound)
      return false;

    generation_update (m.gen);
    return true;
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
          /* send initialisation message of our degree, so each side can
           * figure out whether the other matters to it, before actually
           * updating it's generation.
           */
          send_init (neigh, newc.size ());
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

  private void send_init (Long neigh, int degree) {
    debug.printf ("%d: send DEGREE to neigh %d degree %d\n", selfId, neigh, degree);
    
    try {
      neighbour_msg m
        = neighbour_msg.new_degree_msg (selfId, generation, degree);
      send (neigh, m.serialise ());
    } catch (IOException e) {
      debug.println (debug.levels.ERROR,
                     "Unable to send message to" + neigh + "! " +
                     e.getMessage ());
    }
  }
  
  private void broadcast (int kbound) {
    debug.printf ("%d: %d\n", selfId, kbound);
    neighbour_msg msg = get_default_new_nmsg ();
    byte [] bmsg;
    
    try {
      bmsg = msg.serialise ();
    } catch (IOException e) {
      debug.println (debug.levels.ERROR,
                     "Unable to serialise message!: " + e.getMessage ());
      return;
    }

    for (Long neigh : connected)
      send (neigh, bmsg);
    
    genupdated = false;
  }
  
  int calc_kbound () {
    int newkbound = connected.size ();
    int [] seen = new int [connected.size () + 1];
    int highest = 0;

    for (neighbour_msg nmsg : neighbours.values ()) {
      int neigh_bound = (nmsg.gen >= generation) ? nmsg.value
                                                 : newkbound;
      if (nmsg.gen > generation)
        assert (nmsg.value < this.kbound);

      int i = Math.min (neigh_bound, newkbound);
      highest = Math.max (i, highest);
      seen[i]++;
    }
    
    if (debug.applies ()) {
      StringBuilder sbseen = new StringBuilder ();
      
      for (int i = 0; i < seen.length; i++)
        sbseen.append (" ").append (seen[i]);
      
      debug.printf ("%d: seen: %s\n", selfId, sbseen);
    }

    newkbound = highest;
    int tot = seen[newkbound];
    while (newkbound > tot) {
      newkbound--;
      tot += seen[newkbound];
    }
        
    debug.printf ("%d: result %d\n", selfId, newkbound);
    this.setChanged ();
    return newkbound;
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
    neighbour_msg m;
    
    try {
      m = neighbour_msg.deserialise (data);
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
    
    if (m.type == m.type.KBOUND
        && neighbours.get (m.srcid) == null) {
      debug.printf (debug.levels.ERROR,
                    "%d: Unknown neighbour %d!\n", selfId, src);
    }
    
    stats_inc (stat.recvd);
    
    debug.printf ("%s: Received %s from %d\n", selfId, m, src);

    switch (m.type) {
      case DEGREE:
        /* initialisation message */
        if (m.value >= kbound)
          generation_update (Math.max (this.generation + 1, m.gen));
        neighbours.put (src, get_default_new_nmsg (m.value));
        if (m.value < kbound)
          return;
        break;
      case KBOUND:
        neighbours.put (src, m);
        generation_check (m);
        break;
    }

    if (kbound () || genupdated)
      broadcast (kbound);
  }
  
  public String toString () {
    StringBuilder sb = new StringBuilder ();

    sb.append (super.toString ());
    sb.append ("\nkcore: kb/gen/deg: (" + this.kbound);
    sb.append ("," + this.generation);
    sb.append ("," + this.connected.size ());
    sb.append ("\ncalc_kbound(): " + this.calc_kbound ());
    sb.append ("\nconnected:\n" + this.connected);
    sb.append ("\nneighbours:");
    for (Long neigh : neighbours.keySet ()) {
      neighbour_msg m = neighbours.get (neigh);
      if (m.value >= this.kbound) {
        simhost remote = (simhost) sim.id2node (m.srcid );
        sb.append (String.format ("\n  msg from %d (degree %d): ",
                   m.srcid, remote.degree ()));
        sb.append (String.format ("got/actual (%d,%d) (%d,%d)%s",
                                  m.gen, m.value,
                                  remote.kgen (), remote.kbound (),
                                  (m.gen != remote.kgen ()
                                   || m.value != remote.kbound ()) ? "!" : ""
                                  ));
      }
    }
    sb.append ('\n');
    return sb.toString ();
  }
}
