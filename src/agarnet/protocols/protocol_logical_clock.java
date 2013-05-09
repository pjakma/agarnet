package agarnet.protocols;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;

import org.nongnu.multigraph.debug;

import agarnet.data.marshall;

public class protocol_logical_clock<N extends Serializable> extends AbstractProtocol<N> {
  enum prev_dir {
    NONE, DOWN, UP,
  };
  prev_dir prev = prev_dir.NONE;
  
  private long time = 0;
  public long time () {
    return time;
  }
  
  static public class logical_clock_msg implements Serializable, Externalizable {
    byte [] data = null;
    long tstamp;
    //private static final long serialVersionUID = -1L;
    
    public logical_clock_msg () { super (); }
    public logical_clock_msg (long timestamp, final byte [] data) {
      this.data = data;
      this.tstamp = timestamp;
    }
    
    @Override
    public void writeExternal (ObjectOutput out) throws IOException {
      out.writeLong (tstamp);
      if (data == null || data.length == 0) {
        out.writeInt (0);
        return;
      }
      
      out.writeInt (data.length);
      out.write (data, 0, data.length);
    }

    @Override
    public void readExternal (ObjectInput in) throws IOException,
        ClassNotFoundException {
      tstamp = in.readLong ();
      int len = in.readInt ();
      if (len > 0) {
        data = new byte[len];
        if (in.read (data, 0, len) != len)
          throw new IOException ("Unable to read data!");
      }
    }
  }

  @Override
  public void up (N src, byte [] data) {
    logical_clock_msg msg = null;
    
    try {
      msg = marshall.deserialise (msg, data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, "Unhandled message! %s\n",
                                         e.getMessage ());
      return;
    }
    
    if (msg.tstamp > time)
      time = msg.tstamp;
    
    prev = prev_dir.UP;
    above.up (src, msg.data);
  }
  
  @Override
  public void down (N dst, byte [] data) {
    if (prev != prev_dir.DOWN)
      time++;
    prev = prev_dir.DOWN;
    
    send (dst, new logical_clock_msg (time, data));
  }
}
