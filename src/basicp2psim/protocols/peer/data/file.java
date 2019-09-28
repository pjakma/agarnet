package basicp2psim.protocols.peer.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.nongnu.multigraph.debug;

public class file implements Iterable<block_descriptor>, agarnet.serialisable {
  private static final long serialVersionUID = 2571143375373576236L;
  public final String name;
  List<block_descriptor> blocks;
  
  public file (String name) {
    this.name = name;
  }
  
  public file (String name, byte [] data) {
    block_descriptor bd;
    
    this.name = name;
    
    if (data == null || data.length == 0)
      return;
    
    blocks = new LinkedList<block_descriptor> ();
    
    blocks.add ((bd = new block_descriptor (new file (name))));
    bd.set (new data_block (bd, data));
  }

  @Override
  public Iterator<block_descriptor> iterator () {
    return blocks.iterator ();
  }
  
  public String toString () {
    int len = 0;
    
    for (block_descriptor bd : blocks)
      len += bd.d.size ();
    
    return "[file: " + name + ", len: " + len + "]";
  }
  
  /*
   * We want files to be equal if they have the same name, even after
   * serialisation changes the instance.
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals (Object obj) {
    return (obj instanceof file &&
            name.equals (((file) obj).name));
  }

  @Override
  public int hashCode () {
    return name.hashCode ();
  }
  
  /* agarnet.serialisable */
  public byte [] serialise () throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream ();
    DataOutputStream dos = new DataOutputStream (bos);
    
    dos.writeUTF (name);
    int datalen = 0;
    for (block_descriptor bd: blocks) {
      if (datalen < (Integer.MAX_VALUE - bd.d.data.length))
        datalen += bd.d.data.length;
      else
        break;
    }
    dos.writeInt (datalen);
    for (block_descriptor bd: blocks) {
      if (datalen > 0) {
        dos.write (bd.d.data);
        datalen -= bd.d.data.length;
      } else
        break;
    }
    debug.printf ("%s, writing %d bytes (%d left)\n",
                  this, dos.size(), datalen);
    return bos.toByteArray ();
  }
  
  public static file deserialise (byte [] b) throws IOException {
    DataInputStream dis = new DataInputStream (new ByteArrayInputStream (b));
    
    String name = dis.readUTF ();
    int datalen = dis.readInt ();
    byte [] data = null;
    
    debug.printf ("name %s, datalen %d\n", name, datalen);
    
    if (datalen > 0) {
      data = new byte[datalen];
      if (dis.read (data) != data.length)
        throw new IOException ("Data length mismatch!");
    }
    return new file (name, data);
  }
}
