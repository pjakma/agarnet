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

public class FloatVar extends AbstractVar implements NumberVar {
  private float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
  private float value;
  
  public FloatVar (String name, String desc) {
    super (name, desc);
  }
  public FloatVar (String name, String desc, float min, float max) {
    super (name, desc);
    this.max = max;
    this.min = min;
    
    if (max < min)
      throw new IllegalArgumentException (name + ": max must be >= min");
  }
  
  public FloatVar set (Number val) {
    if (val.floatValue () > max)
      throw new IllegalArgumentException (name + " must be <= " + max);
    if (val.floatValue () < min)
      throw new IllegalArgumentException (name +
                                          " must be >= " + min);
    value = val.floatValue ();
    isset = true;
    return this;
  }
  
  public FloatVar set (String s) {
    float v;
    try {
      v = new Float (s).floatValue ();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException (name +
                                          " requires numeric argument");
    }
    set (v);
    return this;
  }
  
  public Float get () {
    return value;
  }
  
  public String toString () {
    return super.toString () + ": " + value;
  }
}
