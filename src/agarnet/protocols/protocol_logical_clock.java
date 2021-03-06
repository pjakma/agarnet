/* This file is part of 'agarnet'
 *
 * Copyright (C) 2013, 2015 Paul Jakma
 *
 * agarnet is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * agarnet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with agarnet.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.protocols;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;

import org.nongnu.multigraph.debug;

public class protocol_logical_clock<N extends Serializable> extends AbstractProtocol<N> {
  enum prev_dir {
    NONE, DOWN, UP,
  };
  prev_dir prev = prev_dir.NONE;
  
  private long time = 0;
  public long time () {
    return time;
  }

  public void reset () {
    super.reset ();
    prev = prev_dir.NONE;
    time = 0;
  }
  static public class logical_clock_msg implements agarnet.serialisable {
    static final long serialVersionUID = 6220316327321966622L;
    final byte [] data;
    final long tstamp;
    
    public logical_clock_msg (long timestamp, final byte [] data) {
      this.data = data;
      this.tstamp = timestamp;
    }
    
    public byte [] serialise () throws IOException {
      ByteArrayOutputStream bos = new ByteArrayOutputStream ();
      DataOutputStream dos = new DataOutputStream (bos);
      
      dos.writeLong (tstamp);
      if (data == null || data.length == 0) {
        dos.writeInt (0);
      } else {
        dos.writeInt (data.length);
        dos.write (data);
      }
      
      return bos.toByteArray ();
    }
    
    public static logical_clock_msg deserialise (byte [] b) throws IOException {
      DataInputStream dis = new DataInputStream (new ByteArrayInputStream (b));
      
      long tstamp = dis.readLong ();
      int len = dis.readInt ();
      byte [] data = null;
      
      if (len > 0) {
        data = new byte[len];
        if (dis.read (data) != data.length)
          throw new IOException ("Data length mismatch!");
      }
      
      return new logical_clock_msg (tstamp, data);
    }
  }

  @Override
  public void up (N src, byte [] data) {
    logical_clock_msg msg;
    
    try {
      msg = logical_clock_msg.deserialise (data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, "Error decoding message! %s\n",
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
