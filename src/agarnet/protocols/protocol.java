/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2013, 2016 Paul Jakma
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
package agarnet.protocols;

import agarnet.framework.resetable;
import agarnet.framework.tickable;

/**
 * Generic interface for a stackable protocol:
 * {@code
 * +---------------+
 * |p1 = Protocol1 |
 * +---------------+
 *    ^       |
 *    |       |
 * p1.up () p2.down () 
 *    |       |
 *    |       v
 * +---------------+
 * |p2 = protocol2 |
 * +---------------+
 * }
 *
 * Data is passed as opaque byte streams (as in real network stacks), and
 * each layer encaps or decaps its meta-data as data goes down/up the stack,
 * just like a real network stack.
 *
 * The 'host' class provides a specialised 'protocol' implementation to
 * contain a protocol stack, provide some cross-protocol stack services
 * (e.g.  passing on notifications), and interact with the simulation
 * framework.
 * 
 */
public interface protocol<N> 
        extends resetable, tickable, protocol_stats {
  void up (N src, byte [] data);
  void down (N dst, byte [] data);
  /**
   * Globally unique identifier for the host the protocol instance is
   * associated with, handed up by the lower protocol. The protocol
   * may remap the ID in some fashion. The protocol is responsible for
   * then handing up an Id to a protocol above it.
   * 
   * Note that when protocols in a stack remap IDs to different types, 
   * co-ordinating the type checks is beyond the abilities of Java generics,
   * e.g. generic types have the same signature due to erasure.
   * 
   * Coping with this problem is an implementation issue, e.g. by using
   * a single type that contains multiple spaces. 
   */
  protocol<N> setId (N id);
  N getId ();
  /**
   * Insert an instance of a protocol into a stack.
   * @param above
   * @param below
   */
  void insert (protocol<N> above, protocol<N> below);

  /**
   * Notify a protocol of probable change in link status. I.e. one or more
   * hosts have been connected or disconnected from this host.
   * @deprecated
   */
  @Deprecated
  void link_update ();
  
  /**
   * Notify a protocol of added link to a neighbouring host.
   */
  default void link_add (N n) {
    link_update ();
  }

  /**
   * Notify a protocol of removal of a link to a neighbouring host.
   */
  default void link_remove (N n) {
    link_update ();
  }
  
  /**
   * In the spirit of the Observable interface, however we do not want
   * to burden protocols generally with the full overhead of Observable. A
   * container host object can provide such functionality, on behalf of all
   * its constituent protocols. 
   */
  boolean hasChanged ();
}
