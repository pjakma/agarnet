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

public class message<N> implements Serializable {
  private static final long serialVersionUID = -6459204811746335463L;
  private final packet_header<N> header;
  private final byte [] data;
  
  public message (packet_header<N> hdr, byte [] data) {
    if (hdr == null)
      throw new IllegalArgumentException ("header must not be null");
    this.header = hdr;
    this.data = data;
  }
  
  public packet_header<N> getHeader () {
    return header;
  }
  public byte [] getData () {
    return data;
  }
}
