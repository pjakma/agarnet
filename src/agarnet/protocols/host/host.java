/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2011, 2013, 2014, 2016, 2019 Paul Jakma
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
package agarnet.protocols.host;

import java.util.LinkedList;
import java.util.Queue;

import org.nongnu.multigraph.debug;

import agarnet.framework.Simulation;
import agarnet.protocols.AbstractProtocol;
import agarnet.protocols.protocol;
import agarnet.protocols.protocol_stats;
import agarnet.protocols.protocol_stats.stat;
import java.io.Serializable;

/**
 * Class to represent a host. A host is the (slim) interface between a protocol
 * stack and the main network simulator.
 * @author paul
 *
 * @param <N> The type the simulation uses to represent hosts in
 *            the graph. I.e,, the class/interface used for hosts.
 *
 * @param <I> The type of the persistent, stable identifiers of hosts in the
 *            graph - this ID must be capable of being reliably serialised and
 *            deserialised to the same value.
 *            I.e., the address used for hosts.
 */
public class host<I extends Serializable,N> extends AbstractProtocol<I> {
  protected protocol<I> [] protocols;
  Simulation<I,N> sim;
  
  /* the amount of cpu power left in this tick */;
  private int cpu;
  /* limit the number of messages which may be processed per tick
   * -1 for no limit.
   */
  private static final int cpu_per_tick = -1;
  
  private class recvd_msg {
    final I src;
    final byte [] data;
    
    recvd_msg (I src, byte [] data) {
      this.src = src;
      this.data = data;
    }
  }
  
  Queue<recvd_msg> inputbuf = new LinkedList<> ();
  
  public host (Simulation<I,N> sim, protocol<I> [] protocols) {
    this.sim = sim;
    this.protocols = protocols;
    cpu = cpu_per_tick;
  }
  
  public void stack_protocols () {
    protocol<I> lower = this;
    protocol<I> higher;
    
    if (protocols.length == 0)
      return;
    
    for (int i = 0; i < protocols.length; i++) {
      higher = (i < protocols.length - 1) ? protocols[i + 1] : null;
      protocols[i].insert (higher, lower);
      lower = protocols[i];
    }
    above = protocols[0];
  }
  
  @Override
  public host<I,N> setId (I id) {
    selfId = id;
    stack_protocols ();
    super.setId (id);
    
    return this;
  }
  
  @Override
  public void down (I dst, byte [] data) {
    if (cpu > 0)
      cpu--;
    
    //debug.printf ("host %s: send down msg to %s\n", selfId, dst);
    
    sim.tx (selfId, dst, data);
    stats_inc (stat.sent);
    _check_ids ();
  }
  
  @Override
  public void up (I src, byte [] data) {
    /* processing must occur on the tick */
    inputbuf.add (new recvd_msg (src, data));
    _check_ids ();
  }
  
  /* this does the actual up work, without regard to CPU usage */
  void _do_up (recvd_msg msg) {
    if (cpu > 0)
      cpu--;
    
    if (selfId == null)
      throw new IllegalStateException ("Id Must be set before use");
    
    if (protocols.length == 0)
      return;
    
    //debug.printf ("host %s: send up msg from %s\n", selfId, msg.src);
    
    above.up (msg.src, msg.data);
    stats_inc (protocol_stats.stat.recvd);
    setChanged ();
  }
  
  public void insert (I self, protocol<I> a, protocol<I> b) {
    throw new UnsupportedOperationException ("Host can not be stacked");
  }

  @Override
  public long stat_get (stat s) {
    int ord = s.ordinal ();
    if (s.equals (stat.stored)) {
      long stored = 0;
      for (int i = 0; i < protocols.length; i++)
        stored += protocols[i].stat_get (ord);
      return stored;
    }
    return super.stat_get (s);
  }
  
  @Override
  public String toString () {
    return "(" + selfId + ")";
  }
  
  @Override
  public void reset () {
    I id = selfId;
    
    for (protocol<I> p : protocols)
       p.reset ();
    super.reset ();
    cpu = cpu_per_tick;
    
    setId (id);
    _check_ids ();
  }

  @Override
  public void tick () {
    ticks++;
    cpu = cpu_per_tick;
    clearChanged ();
    
    _check_ids ();
    
    while ((cpu < 0 || cpu > 0) && !inputbuf.isEmpty ())
      _do_up (inputbuf.poll ());
    
    if (!inputbuf.isEmpty ())
      setChanged ();

    for (int i = 0; i < protocols.length; i++) {
      protocols[i].tick ();
      if (protocols[i].hasChanged ())
        setChanged ();
    }
    
    debug.printf ("host %s: tick %d\n", selfId, ticks);
  }
  
  private void _check_ids () {
    /* disabled for now */
    if (true)
      return;
    if (protocols == null)
      return;
    for (int i = 0; i < protocols.length; i++)
      if (!protocols[i].getId ().equals (selfId))
        throw new AssertionError ("Ids don't match!");
  }
  
  @Override
  @Deprecated
  public void link_update () {
    for (int i = 0; i < protocols.length; i++)
      protocols[i].link_update ();
  }

  @Override
  public void link_add (I id) {
    debug.println(selfId + ": for " + id);
    for (int i = 0; i < protocols.length; i++)
      protocols[i].link_add (id);
    setChanged ();
  }

  @Override
  public void link_remove (I id) {
    debug.println(selfId + ": for " + id);
    for (int i = 0; i < protocols.length; i++)
      protocols[i].link_remove (id);
    setChanged ();
  }
  
  @SuppressWarnings ({"unchecked","rawtypes"})
  public protocol<I> [] protocols () {
    protocol<I> [] newp = new protocol [protocols.length];
    System.arraycopy (protocols, 0, newp, 0, protocols.length);
    return newp;
  }
}
