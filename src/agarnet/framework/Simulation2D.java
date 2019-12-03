/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2013 Paul Jakma
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

import java.awt.Dimension;
import java.io.Serializable;

import org.nongnu.multigraph.Graph;

import agarnet.link.link;
import agarnet.protocols.host.PositionableHost;

/**
 * Simulation where hosts are positioned within a finite 2D space.
 * @author paul
 * @param <I> The Id type used to identify hosts nodes
 * @param <H> The type used for Hosts in the Simulation
 */
public abstract class Simulation2D<I extends Serializable,
                                   H extends PositionableHost<I,H>>
                extends Simulation<I,H> {
  public final Dimension model_size;

  public Simulation2D (Graph<H,link<H>> g, Dimension space) {
    super (g);
    this.model_size = space;
  }
}
