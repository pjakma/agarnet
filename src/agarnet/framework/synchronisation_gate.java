/* This file is part of 'agarnet'
 *
 * Copyright (C) 2013, 2014 Paul Jakma
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

import org.nongnu.multigraph.debug;

public class synchronisation_gate {
  private final int num;
  private int waiting;
  private int finished;
  
  private Map<Long,StringBuilder> traces;
  private int tracenums = 0;
  private String space = ".  .";
  synchronisation_gate (int num) {
    this.num = num;
    traces = new HashMap<Long,StringBuilder> ();
  }
  
  private void trace (String s) {
    if (!debug.applies ())
      return;
    
    Long id = Thread.currentThread ().getId ();
    StringBuilder trace = traces.get (id);
    if (trace == null) {
      trace = new StringBuilder ();
      for (int i = 0; i < tracenums; i++)
        trace.append (space);
      traces.put (id, trace);
    }
    
    trace.append (String.format ("%4s", s));
    tracenums++;
    
    for (StringBuilder tr : traces.values ()) {
      if (tr != trace)
        tr.append (space);
    }
  }
  private String trace () {
    StringBuilder s = traces.get (Thread.currentThread ().getId ());
    return (s != null) ? s.toString () : "";
  }
  
  synchronized void wait_ready () {
    waiting++;
    while (waiting + finished < num)
      try {
        trace ("wr");
        debug.printf ("before: %s\n", this);
        this.wait ();
      } catch (InterruptedException ex) {}
    
    if (waiting == num)
      this.notifyAll ();
    
    waiting--;
    finished++;
    
    if (waiting == 0)
      finished = 0;
    
    trace ("wre");
    debug.printf ("exit: %s\n", this);
  }
  
  synchronized public String toString () {
    return String.format ("gate(tid/o/r/w: %d/%d/%d/%s)", 
                          Thread.currentThread ().getId (),
                          this.hashCode (),
                          waiting,
                          trace ());
  }
}