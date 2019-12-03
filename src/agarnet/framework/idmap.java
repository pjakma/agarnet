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

import java.util.HashMap;
import java.util.Map;

/**
 * map Graph node objects to stable, persistent IDs that protocols can use
 * @author paul
 * @params I The I-type to use for identifiers of nodes.
 * @params N The N-type to use for the node objects.
 */

public class idmap<I, N> {
  Map<I,N> id2simh = new HashMap<I,N> ();
  Map<N,I> simh2id = new HashMap<N,I> ();

  public I getId (N n) {
    if (n == null)
      throw new AssertionError ("idmap: node must not be null!");

    return simh2id.get (n);
  }

  public void put (I l, N n) {
    if (n == null)
      throw new AssertionError ("idmap put: node must not be null!");
    if (l == null)
      throw new AssertionError ("idmap put: Id key must not be null!");

    if (id2simh.get (l) != null)
      throw new AssertionError ("idmap put: I key already exists!");
    if (simh2id.get (n) != null)
      throw new AssertionError ("idmap put: Node already registered!");

    if (id2simh.put (l, n) != null)
      throw new AssertionError ("id already exists, impossible, wtf?" + l);

    simh2id.put (n, l);

    return;
  }
  public N getNode (I l) {
    return id2simh.get (l);
  }

  @Override
  public String toString () {
    StringBuilder sb = new StringBuilder ();
    for (N s : simh2id.keySet ()) {
      sb.append (s);
      sb.append (" -> ");
      sb.append (getId (s));
      sb.append (" (back?: ");
      sb.append (id2simh.containsKey (getId(s)));
      sb.append (")\n");
    }
    return sb.toString ();
  }
}