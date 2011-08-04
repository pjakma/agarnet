package kcoresim;

import java.io.Serializable;

class neighbour_msg implements Serializable {
  private static final long serialVersionUID = 1L;
  final Long srcid;
  final Long gen;
  final int kbound;
  
  neighbour_msg (Long srcid, Long gen, int kbound) {
    this.srcid = srcid;
    this.gen = gen;
    this.kbound = kbound;
  }
  
  public String toString () {
    String s = "(neighbour_msg: " + srcid + ","
                                  + gen + ","
                                  + kbound + ")";
    return s;
  }
}