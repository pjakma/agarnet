package kcoresim;

import java.io.IOException;

import kcoresim.neighbour_msg.msg_type;

import org.nongnu.multigraph.debug;

import agarnet.framework.Simulation;

public final class opt_kcore<N> extends kcore<N> {
  public opt_kcore (Simulation<Long, N> sim) {
    super (sim);
  }

  public String kcore_type () {
    return "opt";
  }
  
  protected boolean generation_check (neighbour_msg m) {
    if (m.gen <= generation)
      return false;

    if (m.gen > generation && m.value < kbound)
      return false;

    generation_update (m.gen);
    return true;
  }

  @Override
  protected void edge_added (Long neighbour, int nneighbours) {
    /* send initialisation message of our degree, so each side can
     * figure out whether the other matters to it, before actually
     * updating it's generation.
     */
    send_init (neighbour, nneighbours);
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
  
  protected int calc_kbound () {
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
    
    if (m.type == msg_type.KBOUND
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
}
