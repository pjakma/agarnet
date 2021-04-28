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

import java.lang.reflect.Constructor;

import agarnet.variables.atoms.NumberVar;

/* ConfigurableOption wrapper around NumberVar */
public class NumberConfigOption<T extends NumberVar>
       extends ConfigurableOption {
  final NumberVar val;
  
  public NumberConfigOption (Class<T> nvclass, 
                             String longOption,
                             char shortOption,
                             String argDesc, String help,
                             int longoptHasArg) {
    super (longOption, shortOption, argDesc, help, longoptHasArg);
    Constructor<T> c = null;
    NumberVar tmpvar = null;
    
    try {
      c = nvclass.getConstructor (String.class, String.class);
    } catch (Exception e) {
      System.err.println ("Error getting constructor: ");
      System.err.println (e.toString ());
      System.exit (1);
    }
    
    try {
      tmpvar = c.newInstance (longOption, help);
    } catch (Exception e) {
      System.err.println ("Error getting newinstance: ");
      System.err.println (e.toString ());
      System.err.println (e.getCause ());
      for (StackTraceElement se : e.getStackTrace ())
        System.err.println (se);
      System.out.println (c);
      System.exit (1);
    }
    // val will always be initialised to other than null.
    // workaround compiler not understanding what System.exit means.
    val = tmpvar;
  }
  
  public NumberConfigOption set (float num) {
    val.set (num);
    return this;
  }
  
  /**
   * Parse the argument
   * @param arg
   */
  public NumberConfigOption parse (String arg)
                               throws IllegalArgumentException {
    val.set (arg);
    return this;
  }
  
  public float get () {
    return val.get ().floatValue ();
  }
  
  public String toString () {
    return val.toString ();
  }
}
