/* This file is part of 'Subopt'
 *
 * Copyright (C) 2010 Paul Jakma
 *
 * Subopt is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * Subopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with Subopt.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.variables.atoms;

import org.nongnu.multigraph.layout.Vector2D;

public class VectorVar extends AbstractVar {
  private Vector2D val;
  
  public VectorVar (String name, String desc) {
    super (name, desc);
  }
  
  public VectorVar set (String s) {
    String [] results = s.split ("x");
    double x,y;
    
    if (results.length != 2)
      throw new IllegalArgumentException ("Vector must in the form XxY");
    
    x = new Double (results[0]).doubleValue ();
    y = new Double (results[1]).doubleValue ();
    
    set (new Vector2D (x,y));
    return this;
  }
  public VectorVar set (Vector2D val) {
    this.val = val;
    isset = true;
    return this;
  }
  public Vector2D get () { return val; }
  public String toString () {
    return super.toString () + ": " + val;
  }
}