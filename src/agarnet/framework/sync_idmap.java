/* This file is part of 'agarnet'
 *
 * Copyright (C) 2013 Paul Jakma
 *
 * agarnet is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * agarnet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with agarnet.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.framework;

/**
 * Synchronised, thread-safe version of idmap.
 * @author paul
 */
public class sync_idmap<I, N> extends idmap<I, N> {
  private idmap<I, N> idmap = new idmap<> ();

  @Override
  public synchronized I getId (N n) {
    return idmap.getId (n);
  }

  @Override
  public synchronized void put (I l, N n) {
    idmap.put (l, n);
  }
  @Override
  public synchronized N getNode (I l) {
    return idmap.getNode (l);
  }

  @Override
  public synchronized String toString () {
    return idmap.toString ();
  }
}
