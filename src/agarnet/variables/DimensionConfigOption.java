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

import java.awt.Dimension;

public class DimensionConfigOption extends ConfigurableOption {
  Dimension d = new Dimension ();
  
  public DimensionConfigOption (String longOption, char shortOption,
                                  String shortDesc, String help,
                                  int longoptHasArg) {
    super (longOption, shortOption, shortDesc, help, longoptHasArg);
  }
  
  public Dimension get () {
    return d;
  }
  public DimensionConfigOption set (Dimension d) {
    this.d = d;
    return this;
  }
  
  public DimensionConfigOption parse (String arg)
    throws IllegalArgumentException {
    String [] subargs  = arg.split ("[xX]");
    
    if (subargs.length != 2)
      throw new IllegalArgumentException (
            lopt.getName () + "requires <integer>x<integer> as arguments");
    try {
      int width, height;
      width = new Integer (subargs[0]).intValue ();
      height = new Integer (subargs[1]).intValue ();
      d.setSize (width, height);
      
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException (lopt.getName () + 
                                "requires <integer>x<integer> as arguments");
    }
    return this;
  }
  
  public String toString () {
    return lopt.getName () + ": " + d;
  }
}
