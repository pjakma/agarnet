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
package agarnet.protocols.host;

import agarnet.framework.Coloured;
import agarnet.framework.Simulation;
import agarnet.protocols.protocol;

import java.awt.Color;
import java.io.Serializable;

public abstract class AnimatableHost<I extends Serializable,N>
                extends PositionableHost<I,N>
                implements Coloured {
  protected AnimatableHost () {}
  public AnimatableHost (Simulation<I,N> sim,
                           boolean movable, 
                           protocol<I> [] protocols) {
    super (sim, movable, protocols);
  }
  
  private Color colour = new Color (255, 255, 255);
  public Color colour () {
    /* Color appears to be immutable, so this should always be fine */
    return colour;
  }
  public void colour (Color c) {
    colour = c;
  }
}
