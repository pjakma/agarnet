package basicp2psim.protocols.peer.data;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class file implements Iterable<block_descriptor>, Serializable {
  private static final long serialVersionUID = 2571143375373576236L;
  public final String name;
  List<block_descriptor> blocks;
  
  public file (String name) {
    this.name = name;
  }
  
  public file (String name, byte [] data) {
    block_descriptor bd;
    
    this.name = name;
    
    if (data.length == 0)
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
}
