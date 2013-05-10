package kcoresim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

class neighbour_msg  {
  final long srcid;
  final long gen;
  final int kbound;
  
  neighbour_msg (long srcid, long gen, int kbound) {
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

  public byte [] serialise () throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream ();
    DataOutputStream dos = new DataOutputStream (bos);
    
    dos.writeLong (srcid);
    dos.writeLong (gen);
    dos.writeInt (kbound);
    
    return bos.toByteArray ();
  }
  
  public static neighbour_msg deserialise (byte [] b) throws IOException {
    DataInputStream dis = new DataInputStream (new ByteArrayInputStream (b));
    
    long srcid = dis.readLong ();
    long gen = dis.readLong ();
    int kbound = dis.readInt ();
    
    return new neighbour_msg (srcid, gen, kbound);
  }
}