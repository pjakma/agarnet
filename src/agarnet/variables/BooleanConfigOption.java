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

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

public class BooleanConfigOption extends ConfigurableOption {
  boolean val = false;
  
  public BooleanConfigOption (String longOption, char shortOption,
                           String help) {
    super (longOption, shortOption, "", help, LongOpt.NO_ARGUMENT);
  }

  public boolean get () {
    return val;
  }

  public BooleanConfigOption set (boolean val) {
    this.val = val;
    return this;
  }
  
  public String toString () {
    return lopt.getName () + ": " + val;
  }

  @Override
  public ConfigurableOption parse (String arg) {
    return this;
  }

  @Override
  public void getopt (Getopt g) {
    set (true);
  }
}
