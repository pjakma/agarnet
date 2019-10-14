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