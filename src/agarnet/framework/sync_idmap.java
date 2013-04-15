/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package agarnet.framework;

/**
 *
 * @author paul
 */
public class sync_idmap<N> extends idmap<N> {
  private idmap<N> idmap = new idmap<N> ();

  @Override
  public synchronized Long get (N n) {
    return idmap.get (n);
  }

  @Override
  public synchronized void put (Long l, N n) {
    idmap.put (l, n);
  }
  @Override
  public synchronized N get (Long l) {
    return idmap.get (l);
  }

  @Override
  public synchronized String toString () {
    return idmap.toString ();
  }
}
