/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2013, 2014 Paul Jakma
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
package agarnet.framework;

import java.util.Observable;
import java.util.Set;
import org.nongnu.multigraph.*;

import agarnet.link.link;

/**
 * This class represents the Simulation to hosts within the Simulation. It is
 * the interface though which hosts interact with the underlying Simulation.
 *
 * @author paul
 * @param <I> The Id type used to identify the hosts/nodes in the simulation
 * @param <H> The type used for the objects of Hosts in the Simulation
 */
public abstract class Simulation<I,H> extends Observable {
  /* Abstract class because Observable is a class, not an interface.
   * TODO: Perhaps switch to the new Flow interface.
   */
  public final Graph<H,link<H>> network;
  public Simulation (Graph<H,link<H>> g) {
    this.network = g;
  }

  /**
   * @param The node to query the simulation about
   * @return The set of nodes which are connected to the given node.
   */
  abstract public Set<I> connected (I node);

  /**
  * Whether the nodes are connected.
   * @param a Node to query the simulation about
   * @param b Node to query the simulation about
   * @return Whether the nodes are connected.
   */
  abstract public boolean connected (I a, I b);
	
  /**
   * Transmit the packet from one node to the other, I.e. link-layer. 
   * This should therefore only be called from the bottom of the host's
   * protocol stack, usually.
   * @param from Id of the sending node
   * @param to  Id of the node to send to
   * @param data Opaque data
   */
  abstract public boolean tx (I from, I to, byte [] data);
  
  /**
   * Map from the H-typed Host object from which the network Graph is made
   * to the I-typed IDs the Simulation generally uses for nodes.
   * 
   * Using these methods is a layering violation and generally discouraged. 
   * However there may be certain, very special cases where this is
   * justified.
   *
   * @param node The node object to retrieve the I typed Id for
   * @return The Id of the given node
   */
  abstract public I node2id(H node);
  /**
   * retrieve the appropriate Host for the given ID
   * @param node the node Id to lookup
   * @return the Host object for the given Id
   */
  abstract public H id2node (I node);
}
