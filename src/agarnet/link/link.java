/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2013 Paul Jakma
 * Copyright (c) Facebook, Inc. and its affiliates
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
package agarnet.link;

import agarnet.framework.resetable;

import java.util.*;

/** 
 * A duplex link. Basically an administrative wrapper around a pair of simplex links.
 * @author paul
 */
public class link<I> implements resetable {
  final unilink<I> ul1, ul2;
  private final List<I> host_list;
  
  public link (I id1, I id2) {
    this.ul1 = new unilink<> (id1);
    this.ul2 = new unilink<> (id2);
    host_list = Arrays.asList(id1, id2);
  }

  public link (I id1, I id2, int bandwidth, int latency) {
    this.ul1 = new unilink<> (id1, bandwidth, latency);
    this.ul2 = new unilink<> (id2, bandwidth, latency);
    host_list = Arrays.asList(id1, id2);
  }

  public link (I id1, I id2, int bandwidth, int latency, int capacity) {
    this.ul1 = new unilink<> (id1, bandwidth, latency, capacity);
    this.ul2 = new unilink<> (id2, bandwidth, latency, capacity);
    host_list = Arrays.asList(id1, id2);
  }
  
  public link (unilink<I> l1, unilink<I> l2) {
    this.ul1 = l1;
    this.ul2 = l2;
    host_list = Arrays.asList(l1.id, l2.id);
  }
  
  public unilink<I> get (I id) {
    if (ul1.id.equals (id))
      return ul1;
    if (ul2.id.equals (id))
      return ul2;
    return null;
  }

  public I id1 () { 
    return ul1.id;
  }
  public I id2 () { 
    return ul2.id;
  }

  public void reset () {
    ul1.reset ();
    ul2.reset ();
  }
  public int size () {
    return ul1.buffered + ul2.buffered;
  }
  
  public String toString () {
    return "link(" + ul1 + ", " + ul2 + ")";
  }
}
