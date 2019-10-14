package agarnet.framework;

/**
 * Synchronised, thread-safe version of idmap.
 * @author paul
 */
public class sync_idmap<I, N> extends idmap<I, N> {
  private idmap<I, N> idmap = new idmap<> ();

  @Override
  public synchronized I getId (N n) {
    return idmap.getId (n);
  }

  @Override
  public synchronized void put (I l, N n) {
    idmap.put (l, n);
  }
  @Override
  public synchronized N getNode (I l) {
    return idmap.getNode (l);
  }

  @Override
  public synchronized String toString () {
    return idmap.toString ();
  }
}
