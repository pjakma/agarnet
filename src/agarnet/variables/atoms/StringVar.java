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

public class StringVar extends AbstractVar {
  private String val;
  
  public StringVar (String name, String desc) {
    super (name, desc);
  }
  
  public StringVar set (String val) {
    this.val = val;
    isset = true;
    return this;
  }
  public String get () { return val; }
  public String toString () {
    return super.toString () + ": " + val;
  }
}
