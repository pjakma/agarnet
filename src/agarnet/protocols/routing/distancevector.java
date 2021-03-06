/* This file is part of 'agarnet'
 *
 * Copyright (C) 2019, 2020 Paul Jakma
 * Copyright (c) Facebook, Inc. and its affiliates
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
import java.util.function.BiConsumer;
import java.io.Serializable;
import java.io.IOException;
import java.io.PrintStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;


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
  Set<I> connected = new HashSet<> ();
  
  static public enum msgtype { 
    UPDATE(1), WITHDRAW(2);
    
    public final int value;
    
    msgtype (int value) {
      this.value = value;
    }
    
    public static msgtype fromInt (int value) {
      switch (value) {
        case 1: return UPDATE;
        case 2: return WITHDRAW;
        default: throw new IllegalArgumentException("unknown value " + value);
      }
    }
  };
  
  public static class message<I> 
                implements Serializable, agarnet.serialisable {
    private static final long serialVersionUID = -5208759084907769495L;
    private static boolean direct_serialisation = false;
    
    final msgtype type;
    final I destination;
    final int cost;

    message (msgtype type, I destination, int cost) {
      this.type = type;
      this.destination = destination;
      this.cost = cost;
      
      if (marshall.serialisable_by_datastream (destination)) {
        direct_serialisation = true;
      }
    }

    @Override
    public byte [] serialise () throws IOException {
      if (direct_serialisation) {
        /* marshall knows how to do a data-stream based serialisation of
         * the I type being used for destination, great - more efficient!
         * use that.
         */
        ByteArrayOutputStream bos = new ByteArrayOutputStream ();
        DataOutputStream dos = new DataOutputStream (bos);
        
        dos.writeInt (type.value);
        dos.writeInt (cost);
        marshall.serialise (dos, destination);
        
        return bos.toByteArray ();
      } else {
        /* Otherwise has to be via Java Object Serialisation, more overhead */
        return marshall.serialise (this);
      }
    }
    
    public static <I> message<I> deserialise (I obj, byte [] bytes)
                                 throws IOException, ClassNotFoundException {
      if (direct_serialisation) {
        /* great, more efficient Data*Stream case */
        ByteArrayInputStream bis = new ByteArrayInputStream (bytes);
        DataInputStream dis = new DataInputStream (bis);
        msgtype type = msgtype.fromInt (dis.readInt ());
        int cost = dis.readInt ();
        I destination = marshall.deserialise (obj, dis);
        
        return new message<I> (type, destination, cost);
      } else {
        /* Otherwise via Java Object Serialisation */
        return marshall.deserialise ((message<I>) null, bytes);
      }
    }
    
    @Override
    public String toString () {
      return "msg(" + type + ", " + destination + ", " + cost + ")";
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
      //return this.cost == o.cost && this.nexthop.equals (o.nexthop); 
      return this.nexthop.equals (o.nexthop);
    }
    public int hashCode () {
      return Objects.hash(cost, nexthop.hashCode ());
    }
    public String toString () {
      return "vec(" + nexthop + ", " + cost + ", " 
             + (best ? "*" : "-")
             + ")";
    }
  }

  /* Adj-RIB In/Out: Tracking the received vector from each neighbour,
   * and the last message type sent to the neighbour
   */
  class adjrib {
    class entry {
      vector in = null;
      msgtype out = null;
      
      entry () {}
      entry (vector in, msgtype out) {
        this.in = in;
        this.out = out;
      }
    }
    
    private Map<I,entry> nexthop2adjrib = new HashMap<> ();
    
    void forEach(BiConsumer<? super I,? super entry> action) {
      nexthop2adjrib.forEach (action);
    }
    
    adjrib () {}
    
    entry get_entry (I neighbour) {
      return nexthop2adjrib.computeIfAbsent (neighbour,
        k -> new entry ()
      );
    }
    
    vector in (I neighbour) {
      return get_entry (neighbour).in;
    }
    void put_in (I neighbour, vector in) {
      get_entry (neighbour).in = in;
    }
    
    msgtype out (I neighbour) {
      return get_entry (neighbour).out;
    }
    void put_out (I neighbour, msgtype msg) {
      get_entry (neighbour).out = msg;
    }    
  }
  
  /* Routes for a specific destination */
  class route_entries implements Iterable<vector> {
    final I dest;
    int bestcost = Integer.MAX_VALUE;
    boolean needs_update = false;

    /* vector does not have equal consistent with compareTo, so
     * TreeSet can not be used 
     */
    Set<vector> bestpaths = new HashSet<> ();
    PriorityQueue<vector> restpaths = new PriorityQueue<> ();
    adjrib adjrib = new adjrib ();
    
    route_entries (I dest) {
      this.dest = dest;
    }

    Collection<vector> bestpaths () {
      if (bestpaths != null && bestpaths.size () > 0)
        //return Collections.unmodifiableCollection (bestpaths);
        return bestpaths;
      return Collections.emptySet ();
    }
    
    int bestcost () {
      return bestcost;
      /*if (bestpaths.size () > 0) {
        for (vector v : bestpaths)
          return v.cost;
      }
      return Integer.MAX_VALUE;*/
    }
    
    boolean send_updates () {
      if (!needs_update)
	return false;
      
      /* The best set and/or best cost has changed for this route:
       * - Withdraw to any neighbour that has transitioned into best-set
       * - Update to any neighbour that has transitioned out of best-set
       * - Update to to all neighbous not in best set if cost has changed. 
       *
       * This method uses the 'best' flag on the vec to help determine state
       * changes of a vec and this method is responsible for updating that
       * flag.
       */
      needs_update = false;
      int bestcost = bestcost ();
      debug.printf("%s: %s cost %d bp: %s, rp: %s\n",
                   selfId, dest, bestcost, bestpaths, restpaths);

      connected.forEach ((neighbour) -> {
        if (dest.equals (neighbour))
          return;
        
        adjrib.entry adj = adjrib.get_entry (neighbour);
        vector vec = adj.in;
         
        if (vec != null) {
          /* Withdraw to any neighbour that has transitioned into best-set */
          if (vec.cost == bestcost && !vec.best) {
            debug.printf("%s: %s withdraw to %s\n", selfId, dest, neighbour);
            if (adj.out != null && adj.out == msgtype.UPDATE)
              send (neighbour, 
                    new message<> (msgtype.WITHDRAW, dest, -1));
            adj.out = msgtype.WITHDRAW;
            vec.best = true;
          }
          
          /* Update to any neighbour that has transitioned out of best-set */
          if (vec.cost > bestcost && vec.best) {
            /* split horizon */
            if (vec.nexthop != neighbour) {
              debug.printf("%s: out of best set, update %s to %s, %d\n", 
                           selfId, dest, neighbour, bestcost());
              adj.out = msgtype.UPDATE;
              send (neighbour, 
                    new message<> (msgtype.UPDATE, dest, bestcost + 1));
            }
            vec.best = false;
          }
        } else {
          debug.printf("%s: no vec for %s from %s, cost %d, update/withdraw\n",
                       selfId, dest, neighbour, bestcost);
          if (bestcost < Integer.MAX_VALUE) {
            adj.out = msgtype.UPDATE;
            send (neighbour, 
                  new message<> (msgtype.UPDATE, dest, bestcost + 1));
          } else {
            if (adj.out != null
                && adj.out == msgtype.UPDATE) {
              adj.out = msgtype.WITHDRAW;
              send (neighbour,
                    new message<> (msgtype.WITHDRAW, dest, -1));
            } else {
              debug.printf("%s: suppressed withdraw\n", selfId);
            }
          }
        }
      });
      return true;
    }
    
    boolean update (I nexthop, int cost) {
      needs_update = true;
      debug.printf("%s: %s -> %s, %d\n", selfId, dest, nexthop, cost);
      vector vec = adjrib.in (nexthop);
      int prevbestcost = bestcost ();
      
      /* get a vector for this update */
      if (vec == null) {
        vec = new vector (nexthop, cost);
        //nexthop2vector.put (nexthop, vec);
        adjrib.put_in (nexthop, vec);
      } else {
        vec.cost = cost;
      }
      
      /* Check if there was a state change, and handle */
      
      /* not best before or now, ensure in rest-paths and done */
      if (vec.cost > prevbestcost && !vec.best) {
        restpaths.add (vec);
        return (needs_update = false);
      }
      
      /* already in best, and no-change, spurious update */
      if (vec.cost == prevbestcost && vec.best) {
        assert bestpaths.contains (vec);
        return (needs_update = false);
      }
      
      /* addition to best path, make sure in bestpaths */
      if (vec.cost == prevbestcost)
        bestpaths.add (vec);
      
      /* best path, and invalidating prior bestpaths */
      if (vec.cost < prevbestcost) {
        restpaths.addAll (bestpaths);
        bestpaths.clear ();
        bestpaths.add (vec);
      }
      bestcost = vec.cost;
      return true;
    }
    
    
    void remove (I nexthop) {
      needs_update = true;
      int prevbestcost = bestcost ();
      adjrib.entry adj = adjrib.get_entry (nexthop);
      vector vec = adj.in;
      if (vec == null) return;
      
      adj.in = null;
      
      assert vec.cost >= prevbestcost;
      
      if (vec.cost > prevbestcost) {
        boolean ret = restpaths.remove (vec);
        assert ret : restpaths;
        needs_update = false;
        return;
      }
      
      boolean ret = bestpaths.remove (vec);
      assert ret : bestpaths;
      
      if (bestpaths.size () > 0) {
        needs_update = false;
        return;
      }
      
      int newbestcost = Integer.MAX_VALUE;
      needs_update = true;
      
      while ((vec = restpaths.peek ()) != null
             && vec.cost <= newbestcost) {
        restpaths.poll ();
        bestpaths.add (vec);
        assert     newbestcost == Integer.MAX_VALUE 
                || vec.cost == newbestcost;
        newbestcost = vec.cost;
      }
      bestcost = newbestcost;
    }

    public String toString () {
      StringBuilder sb = new StringBuilder ();
      sb.append (selfId + " -> " + dest + ":\n");
      
      restpaths.forEach ((vec) -> 
        sb.append ("  " + vec + "\n")
      );
      bestpaths.forEach ((vec) ->
        sb.append ("  bp: " + vec + "\n")
      );
      return sb.toString ();
    }
    
    public Iterator<vector> iterator () {
      return new Iterator<vector> () {
        private Iterator<vector> bpit = bestpaths.iterator ();
        private Iterator<vector> rpit = restpaths.iterator ();
        public boolean hasNext () {
          return bpit.hasNext () || rpit.hasNext ();
        }
        public vector next () {
          if (bpit.hasNext ())
            return bpit.next ();
          if (rpit.hasNext ())
            return rpit.next ();
          throw new NoSuchElementException ("bestpaths and restpaths empty!");
        }
      };
    }
  }

  class rib {
    Map<I,route_entries> routes = new HashMap<> ();
    
    void update (I dest, I nexthop, int cost) {
      route_entries entries = routes.computeIfAbsent (
        dest,
        k -> new route_entries (k)
      );
      entries.update (nexthop, cost);
    }
    
    void scan () {
      rib.routes.forEach ((dest, routes) -> {
        if (routes.needs_update)
          setChanged ();
        routes.send_updates ();
      });
    }
    
    void remove (I dest, I nexthop) {
      route_entries entries = routes.get (dest);
      if (entries != null) /* this shouldn't happen really */
        entries.remove (nexthop);
    }
    
    /* Remove all of a neighbours vectors */
    void remove (I nexthop) {
      routes.forEach ((dest, entries) -> {
        remove (dest, nexthop);
      });
    }
    
    Collection<vector> bestpaths (I dest) {
      route_entries entries = routes.get (dest);
      if (entries == null)
        return Collections.emptyList ();
      return entries.bestpaths ();
    }
    
    public void dump_table (PrintStream out) {
      routes.forEach ((dest, routes) -> {
        out.println (routes);
      });
    }
  }

  rib rib = new rib ();

  @Override
  public void tick () {
    super.tick ();
    rib.scan ();
  }
  
  public void up (I linksrc, byte [] data) {
    message<I> msg = null;
    try {
      msg = message.deserialise (linksrc, data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, 
                    "%s: Unhandled message from %s, %s!\n",
                    selfId, linksrc, e.getMessage ());
      return;
    }
    debug.printf ("%s: from %s: %s\n", selfId, linksrc, msg);
    switch (msg.type) {
      case UPDATE:
        rib.update (msg.destination, linksrc, msg.cost);
        break;
      case WITHDRAW:
        rib.remove (msg.destination, linksrc);
        break;
    }
  }
  
  public void link_add (I neighbour) {
    debug.printf("%s: %s\n", selfId, neighbour);
    super.link_add (neighbour);
    connected.add (neighbour);
    
    send (neighbour, 
          new message<> (msgtype.UPDATE, selfId, 1));
    
    rib.routes.forEach ((dest,routes) -> {
      if (dest.equals(neighbour))
        return;
      
      routes.bestpaths ().forEach ((vec) -> {
        assert vec.nexthop != neighbour;
        if (vec.nexthop.equals (neighbour))
          return;
        
        debug.printf ("%s: send update for %s to %s\n",
                      selfId, dest, neighbour);
        routes.adjrib.put_out (neighbour, msgtype.UPDATE);
        send (neighbour, 
              new message<> (msgtype.UPDATE, dest, vec.cost + 1)); 
      });
    });
  }

  public void link_remove (I neighbour) {
    debug.printf("%s: neighbour %s\n", selfId, neighbour);
    super.link_remove (neighbour);
    connected.remove (neighbour);
    rib.remove (neighbour);
  }

  @Deprecated
  public void link_update (I neighbour) {
    debug.printf("%s: %s\n", selfId, neighbour);
    super.link_update ();
  }

  public void dump_table (PrintStream out) {
    rib.dump_table (out);
  }
}
