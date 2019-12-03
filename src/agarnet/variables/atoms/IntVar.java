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

public class IntVar extends AbstractVar implements NumberVar {
  private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
  private int value;
  
  public IntVar (String name, String desc) {
    super (name, desc);
  }
  public IntVar (String name, String desc, int min, int max) {
    super (name, desc);
    this.max = max;
    this.min = min;
    
    if (max < min)
      throw new IllegalArgumentException (name + ": max must be >= min");
  }
  
  /* (non-Javadoc)
   * @see basic_model.variables.NumberVar#set(int)
   */
  public IntVar set (Number val) {
    if (val.intValue () > max)
      throw new IllegalArgumentException (getName() + " must be <= " + max);
    if (val.intValue () < min)
      throw new IllegalArgumentException (getName() +
                                          " must be >= " + min);
    value = val.intValue ();
    isset = true;
    return this;
  }
  
  /* (non-Javadoc)
   * @see basic_model.variables.NumberVar#set(java.lang.String)
   */
  public IntVar set (String s) {
    int v;
    try {
      v = new Integer (s).intValue ();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException (getName() +
                                          " requires integer as argument");
    }
    set (v);
    return this;
  }
  
  /* (non-Javadoc)
   * @see basic_model.variables.NumberVar#get()
   */
  public Integer get () {
    return value;
  }
  
  public String toString () {
    return super.toString () + ": " + value;
  }
}
