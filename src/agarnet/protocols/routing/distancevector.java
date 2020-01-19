/* This file is part of 'agarnet'
 *
 * Copyright (C) 2019 Paul Jakma
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
  static public class message<I> 
                implements Serializable {
    private static final long serialVersionUID = -5208759084907769495L;
    public enum type { UPDATE, WITHDRAW };
    
    final type type;
    final I destination;
    final int cost;
    
    message (type type, I destination, int cost) {
      this.type = type;
      this.destination = destination;
      this.cost = cost;
    }
  }

  static public class vector<I> implements Comparable<vector<I>> {
    final I nexthop;
    final int cost;
    
    vector (I nexthop, int cost) {
      this.nexthop = nexthop;
      this.cost = cost;
    }
    
    public int compareTo (vector<I> other) {
      return this.cost - other.cost;
    }
    public boolean equals (Object obj) {
      if (obj == this) return true;
      if (obj == null) return false;
      if (!(obj instanceof vector<?>)) return false;
      
      vector<?> o = (vector) obj;
      return this.cost == o.cost && this.nexthop.equals (o.nexthop); 
    }
    public int hashCode () {
      return Objects.hash(cost, nexthop.hashCode ());
    }
  }
  
  /* Routes for a specific destination */
  static class route_entries<I> {
    SortedSet<vector<I>> vectors = new TreeSet<> ();
    LinkedList<vector<I>> bestpaths = new LinkedList<> ();
    Map<I,vector<I>> nexthop2vector = new HashMap<> ();
    
    Collection<vector<I>> bestpaths () {
      return Collections.unmodifiableCollection (bestpaths);
    }
    
    boolean update_bestpaths () {
      bestpaths.clear ();
      int cost = -1;
      for (vector<I> v : vectors) {
        if (bestpaths.size () == 0) {
          cost = v.cost;
        }
        if (v.cost > cost)
          break;
        
        bestpaths.add (v);
      }
      return true;
    }
    
    boolean update (I nexthop, int cost) {
      vector<I> vec = nexthop2vector.computeIfAbsent (
        nexthop, 
        k -> new vector<I> (k, cost)
      );
      
      vectors.remove (vec);
      vectors.add (vec);
      
      if (bestpaths.size () == 0 || bestpaths.peekFirst().cost > cost)
        return update_bestpaths ();
      
      return false;
    }
    
    
    boolean remove (I nexthop) {
      vector<I> vec = nexthop2vector.remove (nexthop);
      if (vec == null) return false;
      
      int cost = vec.cost;
      
      assert (vectors.remove (vec));
      
      if (cost == bestpaths.peek ().cost)
        return update_bestpaths ();
      return false;
    }
  }
  
  static class rib<I> {
    Map<I,route_entries<I>> rib = new HashMap<> ();
    
    boolean update (I dest, I nexthop, int cost) {
      route_entries<I> entries = rib.computeIfAbsent (
        dest,
        k -> new route_entries<I> ()
      );
      return entries.update (nexthop, cost);
    }
    
    boolean remove (I dest, I nexthop, int cost) {
      route_entries<I> entries = rib.get (dest);
      if (entries == null)
        return false;
      return entries.remove (nexthop);
    }
    
    Collection<vector<I>> bestpaths (I dest) {
      route_entries<I> entries = rib.get (dest);
      if (entries == null)
        return Collections.emptyList ();
      return entries.bestpaths ();
    }
  }
  
  rib<I> rib = new rib<> ();
  
  public void up (I linksrc, byte [] data) {
    message<I> msg = null;
    try {
      msg = marshall.deserialise (msg, data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, "Unhandled message from %s, %s!\n",
                    linksrc, e.getMessage ());
      return;
    }
    
    boolean change;
    switch (msg.type) {
      case UPDATE:
        change = rib.update (msg.destination, linksrc, msg.cost);
        break;
      case WITHDRAW:
        change = rib.remove (msg.destination, linksrc, msg.cost);
        break;
    }
  }
}
