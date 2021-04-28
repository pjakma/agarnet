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

/* A number which represents a probability */
public class ProbVar extends NumberProbVar implements NumberVar {

  public ProbVar (String name, String desc) {
    super (name, desc);
  }

  @Override
  public FloatVar set (Number val) {
    float num = val.floatValue ();
    
    if (num < 0 || num > 1)
      throw new NumberFormatException (
        "number must be a probability, percentage or between 0 and 1 inclusive"
      );
    
    return super.set (num);
  }
  @Override
  public FloatVar set (String s) {
    return set (number_probability (s));
  }
}
