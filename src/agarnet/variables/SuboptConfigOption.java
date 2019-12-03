/* This file is part of 'Subopt'
 *
 * Copyright (C) 2010, 2011, 2013 Paul Jakma
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

import agarnet.Subopt;

/* Config option with multiple branches of sub-options, e.g.:
 *  
 * --option (branch1[,<one or more branch1 subopts>]|branch2[,...][|..])
 **/
public class SuboptConfigOption extends ConfigurableOption {
  protected String primary;
  
  public SuboptConfigOption (String longOption,
                             char shortOption,
                             String argDesc,
                             String help,
                             int longoptHasArg,
                             ConfigOptionSet subopts) {
    super (longOption, shortOption, argDesc, help, longoptHasArg, subopts);
  }
  
  protected Subopt _parse (String args) {
    Subopt subopt = new Subopt (args, subopts.keys ());
    
    int ret = subopt.get ();
    primary = subopt.optionp;
    
    if (ret < 0)
      throw new IllegalArgumentException ("Unknown suboption: "
                                          + subopt.valuep);
    if (ret >= 0) {
      if (subopts.branch_keys ().contains (subopt.optionp))
        parse_subopts (subopts, subopt.optionp, args);
      else
        parse_subopts (subopts, args);
    }
    return subopt;
  }
  
  public SuboptConfigOption parse (String args) {
    if (args != null)
      _parse (args);
    return this;
  }
  
  public String get () {
    return primary;
  }
  
  public String toString () {
    StringBuilder sb = new StringBuilder ();
    
    sb.append (lopt.getName () + ": " + primary);
    if (subopts.num_subopts (primary) > 0)
      for (String subkey : subopts.subopt_keys (primary))
        if (subopts.get (primary, subkey).isSet ())
          sb.append ("\n  " + subopts.get (primary, subkey));
    for (String key : subopts.subopt_keys ())
      if (subopts.get (key).isSet ())
        sb.append ("\n  " + subopts.get (key));
    return sb.toString ();
  }
}
