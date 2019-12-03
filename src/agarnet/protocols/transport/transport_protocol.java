/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2011, 2013 Paul Jakma
 *
 * agarnet is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * agarnet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with agarnet.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.protocols.transport;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.nongnu.multigraph.Graph;
import org.nongnu.multigraph.ShortestPathFirst;
import org.nongnu.multigraph.debug;

import agarnet.data.marshall;
import agarnet.framework.Simulation;
import agarnet.protocols.AbstractProtocol;
import agarnet.protocols.transport.data.message;
import agarnet.protocols.transport.data.packet_header;

public class transport_protocol<I,N,L>
       extends AbstractProtocol<I> {
  ShortestPathFirst<N,L> spf;
  Simulation<I,N> sim;
  
  public transport_protocol (Simulation<I,N> sim, Graph<N,L> network) {
    spf = new ShortestPathFirst<N,L> (network);
    this.sim = sim;
  }
  
  public void up (I linksrc, byte [] data) {
    message<I> msg = null;
    I src, dst;
    
    debug.printf ("transport %s: rcv from link %s\n", selfId, linksrc);
    
    try {
      msg = marshall.deserialise (msg, data);
    } catch (Exception e) {
      debug.printf ("transport: error demarshalling msg from %s\n", linksrc);
      e.printStackTrace();
      return;
    }
    this.stats_inc (stat.recvd);
    src = msg.getHeader ().getSrc ();
    dst = msg.getHeader ().getDst ();
    
    /* should it be forwarded ? */
    if ((dst == null || !dst.equals (selfId))
        && msg.getHeader ().getTTL () > 0) {
      debug.printf ("transport %s: forward message %s from %s (%s) to %s\n",
                    selfId, msg.getHeader (), src, linksrc, dst);
      msg.getHeader ().decTTL ();
      output (src, dst, data);
    }
    
    /* should it be received locally? */
    if (dst == null || dst.equals (selfId)) {
      debug.printf ("transport %s: send up msg from %s (%s)\n", selfId,
                    src, linksrc);
      above.up (src, msg.getData ());
    }
  }
  
  @Override
  public void down (I dst, byte [] data) {
    /* Add a header, map the destination host to the (set) of connected
     * hosts and job done.
     */
    packet_header<I> ph = new packet_header<I> (selfId, dst,
                                                packet_header.type.file);
    message<I> msg = new message<I> (ph, data);
    byte [] output;
    
    try {
      output = marshall.serialise (msg);
    } catch (IOException e) {
      debug.println ("Weird, unable to serialise message!: " + e.getMessage ());
      e.printStackTrace();
      return;
    }
    
    
    debug.printf ("transport %s: send %s to %s\n",
                  selfId, ph, dst);
    output (selfId, dst, output);
  }
  
  void output (I linksrc, I dst, byte [] data) {
    Set<I> nhops = dst2nexthop (dst);
    
    if (nhops != null)
      for (I linkdst : nhops)
        if (!linkdst.equals (linksrc)) {
          debug.printf ("transport %s: output msg to %s (%s)\n",
                        selfId, dst, linkdst);
          stats_inc (stat.sent);
          below.down (linkdst, data);
        }
  }
  
  /* Routing lookup, mapping arbitrary dst to one or more directly connected,
   * next-hop node IDs
   */
  private Set<I> dst2nexthop (I dst) {
    N nh;
    Set<I> nhops = new HashSet<I> ();
    
    /* null is the 'allhosts' address for our primitive multicast */
    if (dst == null) {
      debug.println ("dst2nexthop: multicast, returning connected set");
      return sim.connected (selfId);
    }
    
    if ((nh = spf.nexthop (sim.id2node (dst))) != null)
      nhops.add (sim.node2id (nh));
    else
      nhops = null;
    
    debug.printf ("dst2nexthop: %s -> %s\n", dst, nhops);
    
    return nhops;
  }
  
  private void _update_spf () {
    debug.levels origlevel = debug.level ();
    debug.level (debug.levels.ERROR);
    
    if (selfId != null)
      spf.run (sim.id2node (selfId));
    
    debug.level (origlevel);
  }
  
  @Override
  public void link_update () {
    //debug.printf ("transport: update %s\n", this);
    _update_spf ();
  }
}
