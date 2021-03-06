/* This file is part of 'Subopt'
 *
 * Copyright (C) 2010, 2013 Paul Jakma
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

import agarnet.variables.atoms.IntVar;
import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

/**
 * An integer configuration variable.
 * @author paul
 *
 */
public class IntConfigOption extends ConfigurableOption {
  final IntVar val;
  
  /**
   *
   * Integer configuration variable
   * 
   * @param longOption the long option name for the variable
   * @param shortOption A single character short option for the variable
   * @param argDesc   A concise, short functional description of the argument
   * @param help        A more detailed, general help string
   * @param longoptHasArg This is taken to be REQUIRED_ARGUMENT, the actual
   *                      value supplied will be ignored.
   */
  public IntConfigOption (String longOption, char shortOption,
                       String argDesc, String help, int longoptHasArg) {
    super (longOption, shortOption, argDesc, help, longoptHasArg);
    
    val = new IntVar (longOption, help);
  }
  
  /**
   * Integer configuration variable, with the specified min and max range.
   * 
   * @param longOption the long option name for the variable
   * @param shortOption A single character short option for the variable
   * @param shortDesc   A concise, short functional description
   * @param help        A more detailed help string
   * @param longoptHasArg This is taken to be REQUIRED_ARGUMENT, the actual
   *                      value supplied will be ignored.
   * @param min Minimum range for the integer
   * @param max Maximum range for the integer
   */
  public IntConfigOption (String longOption, char shortOption,
                       String shortDesc, String help,
                       int longoptHasArg,
                       int min, int max) {
    super (longOption, shortOption, shortDesc, help,
           LongOpt.REQUIRED_ARGUMENT);
    
    val = new IntVar (longOption, help, min, max);
  }
  
  /**
   * Parse the argument
   * @param arg
   */
  public IntConfigOption parse (String arg) throws IllegalArgumentException {
    val.set (arg);
    return this;
  }
  
  public int get () {
    return val.get ();
  }
  public IntConfigOption set (int val) {
    this.val.set (val);
    return this;
  }
  
  public String toString () {
    return val.toString ();
  }
}
