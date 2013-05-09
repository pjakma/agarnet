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
    return String.format ("gate(tid/r/f/w: %d/%d/%s)", 
                          Thread.currentThread ().getId (),
                          waiting,
                          trace ());
  }
}