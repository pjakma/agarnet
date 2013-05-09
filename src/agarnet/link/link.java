package agarnet.link;

import agarnet.framework.resetable;

/** 
 * A duplex link. Basically an administrative wrapper around a pair of simplex links.
 * @author paul
 */
public class link<I> implements resetable {
  final unilink<I> ul1, ul2;
  
  public link (unilink<I> l1, unilink<I> l2) {
    this.ul1 = l1;
    this.ul2 = l2;
  }
  
  public unilink<I> get (I id) {
    if (ul1.id.equals (id))
      return ul1;
    if (ul2.id.equals (id))
      return ul2;
    return null;
  }

  public void reset () {
    ul1.reset ();
    ul2.reset ();
  }
  public int size () {
    return ul1.buffered + ul2.buffered;
  }
  
  public String toString () {
    return "link(" + ul1 + ", " + ul2 + ")";
  }
}
