/* This file is part of 'agarnet'
 *
 * Copyright (C) 2019, 2020 Paul Jakma
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
package agarnet.protocols.routing;

import java.util.*;
import java.io.Serializable;
import java.io.IOException;

import org.nongnu.multigraph.debug;

import agarnet.protocols.AbstractProtocol;
import agarnet.data.marshall;

/**
 * A simple Distance-Vector routing protocol.
 *
 * Basic Distance-Vector routing protocol, to route the I-typed identifiers
 * (addresses) in the simulation.  The protocol builds and maintains a table
 * to map each of the I-typed destinations in the simulation, to an I-typed
 * nexthop.
 * 
 * @author paul
 *
 * @param <I> The type of the Identifiers used to address nodes in the
 * simulation.
 */
public class distancevector<I extends Serializable> 
             extends AbstractProtocol<I> {
  static public enum msgtype { UPDATE, WITHDRAW };
  static public class message<I> 
                implements Serializable, agarnet.serialisable {
    private static final long serialVersionUID = -5208759084907769495L;
    
    final msgtype type;
    final I destination;
    final int cost;
    
    message (msgtype type, I destination, int cost) {
      this.type = type;
      this.destination = destination;
      this.cost = cost;
    }
    
    @Override
    public byte [] serialise () throws IOException {
      return marshall.serialise (this);
    }
  }

  public class vector implements Comparable<vector> {
    final I nexthop;
    int cost;
    /* Whether this vector is in the best-path set */
    boolean best = false;
    
    vector (I nexthop, int cost) {
      this.nexthop = nexthop;
      this.cost = cost;
    }
    
    public int compareTo (vector other) {
      return this.cost - other.cost;
    }
    public boolean equals (Object obj) {
      if (obj == this) return true;
      if (obj == null) return false;
      if (!(obj instanceof agarnet.protocols.routing.distancevector<?>.vector))
        return false;
      
      @SuppressWarnings("unchecked")
      vector o = (vector) obj;
      return this.cost == o.cost && this.nexthop.equals (o.nexthop); 
    }
    public int hashCode () {
      return Objects.hash(cost, nexthop.hashCode ());
    }
  }
  
  /* Routes for a specific destination */
  class route_entries {
    final I dest;
    SortedSet<vector> bestpaths = null;
    SortedSet<vector> vectors = new TreeSet<> ();
    Map<I,vector> nexthop2vector = new HashMap<> ();
    
    route_entries (I dest) {
      this.dest = dest;
    }
    
    Collection<vector> bestpaths () {
      if (bestpaths != null)
        return Collections.unmodifiableCollection (bestpaths);
      return Collections.emptySet ();
    }
    
    int bestpathcost () {
      if (bestpaths != null)
        return bestpaths.first ().cost;
      return Integer.MAX_VALUE;
    }
    
    void update_bestpaths () {
      if (vectors.size () == 0) {
        bestpaths = null;
        return;
      }
      
      int leastcost = vectors.first ().cost;
      for (vector vec : vectors) {
        if (vec.cost == leastcost)
          continue;
        bestpaths = vectors.headSet (vec);
        break;
      }
      return;
    }
    
    boolean update (I nexthop, int cost) {
      vector vec = nexthop2vector.get (nexthop);
      int prevleastcost = bestpathcost ();
      
      /* get a vector for this update, and make sure it's not in the vectors
       * p-queue, one way or another */
      if (vec == null) {
        vec = new vector (nexthop, cost);        
      } else {
        vectors.remove (vec);
        vec.cost = cost;
      }
      
      /* Add the updated vector to the priority-queue */
      vectors.add (vec);
      update_bestpaths ();
      
      /* Check if there was a change, and handle */
      if (vec.cost > prevleastcost)
        return false;
      
      /* spurious / no-change update */
      if (vec.cost == prevleastcost && vec.best)
        return false;
      
      /* best and/or cost has changed:
       * - Withdraw to any nexthop that has transitioned into best-set
       * - Update to nodes not in best set. 
       */
      for (vector other : vectors) {
        if (other.cost <= vec.cost) {
          /* Send with-draw to any neighbours that have transitioned into
           * best-set
           */
          if (!other.best)
            send (other.nexthop, 
                  new message<I> (msgtype.WITHDRAW, dest, -1));
          other.best = true;
          continue;
        }
        
        /* update the rest, non-best */
        send (other.nexthop,
              new message<I> (msgtype.UPDATE, dest, vec.cost));
        other.best = false;
      }
      
      return true;
    }
    
    
    void remove (I nexthop) {
      vector vec = nexthop2vector.remove (nexthop);
      if (vec == null) return;
      
      int cost = vec.cost;
      
      assert (vectors.remove (vec));
      
      update_bestpaths ();
    }
  }
  
  class rib {
    Map<I,route_entries> rib = new HashMap<> ();
    
    void update (I dest, I nexthop, int cost) {
      route_entries entries = rib.computeIfAbsent (
        dest,
        k -> new route_entries (k)
      );
      entries.update (nexthop, cost);
    }
    
    void remove (I dest, I nexthop) {
      route_entries entries = rib.get (dest);
      if (entries != null) /* this shouldn't happen really */
        entries.remove (nexthop);
    }
    
    Collection<vector> bestpaths (I dest) {
      route_entries entries = rib.get (dest);
      if (entries == null)
        return Collections.emptyList ();
      return entries.bestpaths ();
    }
  }
  
  rib rib = new rib ();
  
  public void up (I linksrc, byte [] data) {
    message<I> msg = null;
    try {
      msg = marshall.deserialise (msg, data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, "Unhandled message from %s, %s!\n",
                    linksrc, e.getMessage ());
      return;
    }
    
    switch (msg.type) {
      case UPDATE:
        rib.update (msg.destination, linksrc, msg.cost);
        break;
      case WITHDRAW:
        rib.remove (msg.destination, linksrc);
        break;
    }
  }
}
