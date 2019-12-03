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

import agarnet.variables.atoms.BooleanVar;
import gnu.getopt.Getopt;

public class DebugConfigOption extends ConfigurableOption {
  public DebugConfigOption (String longOption, char shortOption,
      String argDesc, String help, int longoptHasArg,
      ConfigOptionSet subopts) {
    super (longOption, shortOption, argDesc, help, longoptHasArg, subopts);
  }
  
  @Override
  public ConfigurableOption parse (String args) {
    if (args != null)
      super.parse_subopts (this.subopts, args);
    return this;
  }

  @Override
  public void getopt (Getopt g) {
    BooleanVar db = ((BooleanVar) subopts.get ("debug"));
    db.set (true);
    parse (g.getOptarg ());
  }
}
