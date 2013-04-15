/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package agarnet.framework;

import java.util.HashMap;
import java.util.Map;

/**
 * map Graph node objects to stable, persistent IDs that protocols can use
 * @author paul
 */

public class idmap<N> {
  Map<Long,N> id2simh = new HashMap<Long,N> ();
  Map<N,Long> simh2id = new HashMap<N,Long> ();

  public Long get (N n) {
    if (n == null)
      throw new AssertionError ("idmap: node must not be null!");

    return simh2id.get (n);
  }

  public void put (Long l, N n) {
    if (n == null)
      throw new AssertionError ("idmap put: node must not be null!");
    if (l == null)
      throw new AssertionError ("idmap put: Long key must not be null!");

    if (id2simh.get (l) != null)
      throw new AssertionError ("idmap put: Long key already exists!");
    if (simh2id.get (n) != null)
      throw new AssertionError ("idmap put: Node already registered!");

    if (id2simh.put (l, n) != null)
      throw new AssertionError ("id already exists, impossible, wtf?" + l);

    simh2id.put (n, l);

    return;
  }
  public N get (Long l) {
    return id2simh.get (l);
  }

  @Override
  public String toString () {
    StringBuilder sb = new StringBuilder ();
    for (N s : simh2id.keySet ()) {
      sb.append (s);
      sb.append (" -> ");
      sb.append (get (s));
      sb.append (" (back?: ");
      sb.append (id2simh.containsKey (get(s)));
      sb.append (")\n");
    }
    return sb.toString ();
  }
}