/* This file is part of 'agarnet'
 *
 * Copyright (C) 201, 2011, 2013, 2015 Paul Jakma
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
package agarnet.protocols.host;

import org.nongnu.multigraph.layout.AbstractPositionableNode;

import agarnet.framework.Simulation;
import agarnet.protocols.protocol;
import agarnet.protocols.protocol_stats;
import java.io.Serializable;

public class PositionableHost<I extends Serializable,N>
       extends AbstractPositionableNode
       implements protocol<I> {
  protected host<I,N> host;
  private boolean movable = true;
  protected Simulation<I,N> sim;
  
  protected PositionableHost () {}
  public PositionableHost (Simulation<I,N> sim,
                           boolean movable,
                           protocol<I> [] protocols) {
    this.host = new host<> (sim, protocols);
    this.movable = movable;
    this.sim = sim;
  }
  
  public void reset () {
    host.reset ();
  }
  public long stat_get (protocol_stats.stat s) {
    return host.stat_get (s);
  }
  public long stat_get (int ordinal) {
    return host.stat_get (ordinal);
  }
  public void stats_reset () {
    host.stats_reset ();
  }
  public void stats_reset (protocol_stats.stat s) {
    host.stats_reset (s);
  }
  public void tick () {
    host.tick ();
  }
  public PositionableHost<I,N> setId (I id) {
    host.setId (id);
    return this;
  }
  public I getId () {
    return host.getId ();
  }
  
  public void down (I dst, byte [] data) {
    host.down (dst, data);
  }
  
  public float getSize () {
    return host.stat_get (protocol_stats.stat.stored);
  }

  public void setSize (float arg0) {}
  
  @Override
  public boolean isMovable () {
    return movable;
  }
  
  public String toString () {
    return host.toString ();
  }

  public void up (I src, byte [] data) {
    host.up (src, data);
  }

  @Override
  public void insert (protocol<I> above,
                      protocol<I> below) {
    host.insert (above, below);
  }
  
  @Override
  @Deprecated
  public void link_update () {
    host.link_update ();
  }
  
  @Override
  public void link_add (I id) {
   host.link_add (id);
  }
  @Override
  public void link_remove (I id) {
   host.link_remove (id);
  }
  
  @Override
  public boolean hasChanged () {
    return host.hasChanged ();
  }
  
}
