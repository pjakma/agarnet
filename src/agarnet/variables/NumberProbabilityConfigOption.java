/* This file is part of 'Subopt'
 *
 * Copyright (C) 2010, 2011 Paul Jakma
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
package agarnet.variables;

import agarnet.variables.atoms.NumberProbVar;

/* ConfigurableOption wrapper around NumeberProb */
public class NumberProbabilityConfigOption extends ConfigurableOption {
  final NumberProbVar val;
  
  public NumberProbabilityConfigOption (String longOption,
                                        char shortOption,
                                        String argDesc, String help,
                                        int longoptHasArg) {
    super (longOption, shortOption, argDesc, help, longoptHasArg);
    val = new NumberProbVar (longOption, help);
  }
  
  public NumberProbabilityConfigOption set (float num) {
    val.set (num);
    return this;
  }
  
  /**
   * Parse the argument
   * @param arg
   */
  public NumberProbabilityConfigOption parse (String arg)
                               throws IllegalArgumentException {
    val.set (arg);
    return this;
  }
  
  public float get () {
    return val.get ();
  }
  
  public String toString () {
    return val.toString ();
  }
}
