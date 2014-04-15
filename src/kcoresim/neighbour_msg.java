package kcoresim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class neighbour_msg  {
  enum msg_type {
    KBOUND,
    DEGREE;
    private final static msg_type types[] = values ();
    static msg_type to_msg_type (int ordinal) { return types[ordinal]; }
  };
  final long srcid;
  final msg_type type;
  final long gen;

  /* KBOUND, value is kbound
   * DEGREE, value is degree
   */
  final int value;

  static neighbour_msg new_kbound_msg (long srcid, long gen, int value) {
    return new neighbour_msg (srcid, msg_type.KBOUND, gen, value);
  }
  static neighbour_msg new_degree_msg (long srcid, long gen, int value) {
    return new neighbour_msg (srcid, msg_type.DEGREE, gen, value);
  }

  neighbour_msg (long srcid, msg_type type, long gen, int value) {
    this.srcid = srcid;
    this.type = type;
    this.gen = gen;
    this.value = value;
  }

  public String toString () {
    String s = "";
    switch (this.type) {
      case KBOUND:
        s = "(neighbour_msg(K): " + srcid + ","
                             + gen + ","
                             + value + ")";
        break;
      case DEGREE:
        s = "(neighbour_msg(D): " + srcid + "," + value + ")";
        break;
    }
    return s;
  }

  public byte [] serialise () throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream ();
    DataOutputStream dos = new DataOutputStream (bos);
    
    dos.writeLong (srcid);
    dos.writeInt (type.ordinal ());
    dos.writeLong (gen);
    dos.writeInt (value);

    return bos.toByteArray ();
  }
  
  public static neighbour_msg deserialise (byte [] b) throws IOException {
    DataInputStream dis = new DataInputStream (new ByteArrayInputStream (b));
    
    long srcid = dis.readLong ();
    msg_type type = msg_type.to_msg_type (dis.readInt ());
    long gen = dis.readLong ();
    int value = dis.readInt ();
    
    return new neighbour_msg (srcid, type, gen, value);
  }
}