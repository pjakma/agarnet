/* This file is part of 'agarnet'
 *
 * Copyright (C) 2013, 2015 Paul Jakma
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
package agarnet.protocols;

import agarnet.protocols.protocol;

import java.io.Serializable;

import org.nongnu.multigraph.debug;

public class protocol_id_map<A extends Serializable, B extends Serializable>
       extends AbstractProtocol<A> {
  static public interface remapper<A, B> {
    B toB (A a);
    A toA (B b);
  }

  final private protocol<B> inner;
  final private remapper<A,B> remap;  
  
  public protocol_id_map (protocol<B> innerproto, remapper<A,B> remapper) {
    this.inner = innerproto;
    this.remap = remapper;
  }
  
  public void reset () {
    super.reset ();
    inner.reset ();
  }

  @Override
  public void up (A src, byte [] data) {
    inner.up (remap.toB (src), data);
  }
  
  @Override
  public void down (A dst, byte [] data) {
    inner.down (remap.toB (dst), data);
  }

  @Override
  public protocol<A> setId (A id) {
    inner.setId (remap.toB (id));
    return this;
  }
  
  @Override
  public A getId () {
    return remap.toA (inner.getId ());
  }
 
  @Override 
  public void link_add (A n) {
    inner.link_add (remap.toB (n));
  }
  @Override
  public void link_remove (A n) {
    inner.link_remove (remap.toB (n));
  }

  @Override
  public void insert (protocol<A> above, protocol<A> below) {
    remapper<B,A> inverse = new remapper<B,A> () {
      public B toA (A a) { return remap.toB (a); }
      public A toB (B b) { return remap.toA (b); }
    };
    inner.insert (
      above != null ? new protocol_id_map<B, A> (above, inverse) : null,
      below != null ? new protocol_id_map<B, A> (below, inverse) : null
    );
  }
  public void tick () {
    inner.tick ();
  }
}
