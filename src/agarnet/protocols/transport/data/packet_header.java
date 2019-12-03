/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010 Paul Jakma
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
package agarnet.protocols.transport.data;

import java.io.Serializable;

public class packet_header<N> implements Serializable {
  private static final long serialVersionUID = 5341648201992790681L;
  private final N src;
  /* null indicates all hosts */
  private final N dst;
  private final type t; 
  private int ttl = 64;

  public enum type { 
    file,
  }
  
  public packet_header (N src, N dst, type type) {
    if (src == null)
      throw new IllegalArgumentException ("Src must not be null");
    
    this.src = src;
    this.dst = dst;
    this.t = type;
  }
  
  public N getSrc () {
    return src;
  }

  public N getDst () {
    return dst;
  }

  public type getType () {
    return t;
  }
  
  public int getTTL () {
    return ttl;
  }

  public void setTTL (int ttl) {
    if (ttl < 0)
      throw new AssertionError ("ttl can not be negative!");
    
    this.ttl = ttl;
  }

  public void decTTL () {
    ttl--;
    
    ttl = (ttl >= 0) ? ttl : 0;
  }
  
  public String toString () {
    return "[packet: " + src + " -> "+ dst +", ttl " + ttl + "]";
  }
}
