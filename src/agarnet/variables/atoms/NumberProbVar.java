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

/* A number which represents either a probability or a positive integer value */
public class NumberProbVar extends FloatVar implements NumberVar {

  public NumberProbVar (String name, String desc) {
    super (name, desc);
  }
  /**
   * parse a <number|probability> string.
   * @param s A string representing an integer number, or a probability.
   * @return A float value, which represents a probability if 0 <= x < 1 or
   *         or x ends in with a % and 0 < x <= 100, an integer number otherwise.
   */
  private static float number_probability (String s)
                       throws NumberFormatException {
    boolean pc = false;
    float num;
    int i;
    
    if ((i = s.indexOf ('%')) >= 0) {
      if (i != s.length () - 1)
        throw new NumberFormatException ("% is only valid as last character");
      
      s = s.replace ('%', '\0');
      pc = true;
    }
    
    num = new Float (s).floatValue ();
    
    if (pc) {
      if (num > 100)
        throw new NumberFormatException ("% probability must be <= 100");
      
      num /= 100;
    }
    
    return num;
  }
  @Override
  public FloatVar set (Number val) {
    float num = val.floatValue ();
    
    if (num >= 1 && Math.floor (num) != num)
      throw new NumberFormatException ("number must be an integer value");
    else if (num < 0)
      throw new NumberFormatException ("number must be a probability or a " +
                                       "positive integer");
    return super.set (num);
  }
  @Override
  public FloatVar set (String s) {
    return super.set (number_probability (s));
  }
  @Override
  public String toString () {
    return super.toString ();
  }
  
}
