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

abstract class AbstractVar implements ObjectVar {
  protected final String name;
  protected final String desc;
  protected boolean isset = false;
  
  public AbstractVar (String name, String desc) {
    this.name = name;
    this.desc = desc;
  }
  
  @Override
  public String getDesc () {
    return desc;
  }
  
  @Override
  public String getName () {
    return name;
  }
  
  public String toString () {
    return name;
  }
  
  public boolean isSet () {
    return isset;
  }
}
