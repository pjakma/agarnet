/* This file is part of 'agarnet'
 *
 * Copyright (c) Facebook, Inc. and its affiliates
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

package basicp2psim.protocols.peer;

import java.util.Arrays;

import org.fusesource.hawtjni.runtime.*;
import static org.fusesource.hawtjni.runtime.ArgFlag.*;
import static org.fusesource.hawtjni.runtime.MethodFlag.*;

import org.nongnu.multigraph.debug;

import agarnet.protocols.protocol;
import agarnet.protocols.AbstractProtocol;

import basicp2psim.protocols.peer.data.file;

@JniClass
public class flakey extends AbstractProtocol<Long> {
 private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
 public static String bytesToHex(byte[] bytes) {
     char[] hexChars = new char[bytes.length * 3];
     for (int j = 0; j < bytes.length; j++) {
         int v = bytes[j] & 0xFF;
         hexChars[j * 3] = ' ';
         hexChars[j * 3 + 1] = HEX_ARRAY[v >>> 4];
         hexChars[j * 3 + 2] = HEX_ARRAY[v & 0x0F];
         //System.out.printf ("%2d: %3d %c%c\n", j, v, hexChars[j * 3 + 1], hexChars[j * 3 + 2]);
     }
     return new String(hexChars);
 }

    
  private static final Library LIBRARY = new Library("agarnet", 
                                                     flakey.class);
  private static Callback debug_cb;
  static { 
    LIBRARY.load();
    debug_cb = new Callback (flakey.class, "debug_from_native", 1);
  }
  
  /* HawtJNI Callback is extremely limited on arguments.
   * can only take long arguments.
   * 
   * So use the Callback as a notification method, to then
   * call to a better typed call into native.
   *
   * TODO: Find a better solution, e.g.  just use hand-crafted JNI to
   * produce exactly the callback required, if needs be.  FBJNI maybe has
   * something better.
   */
  static public long debug_from_native (long instance) {
    byte [] msg = new byte[200];
    native_get_debug_msg (instance, msg, msg.length);
    debug.println(new String(msg));
    return 0;
  }

  /* Be nice to gather together the various (byte[], length) parameters
   * below into this struct.
   *
   * However, HawtJNI does not know how to properly copy references to arrays
   * back and forth from native. It uses sizeof(field) for the length, so only
   * supports embedded, fixed size arrays.
   *
   * Be nice to teach HawtJNI how to use an additional 'length' field to deal
   * with array references in structs.
   *
   * Otherwise... hand-coded JNI, or a better JNI generator.
   */
  @JniClass(flags={ClassFlag.STRUCT})
  public static class native_buffer {
    @JniField byte [] data;
    @JniField(cast="size_t") long len;
    @JniField(cast="size_t") long capacity;

    public native_buffer (byte [] data, long capacity) {
      this.data = data;
      this.len = data.length;
      this.capacity = capacity;
    }    
    public native_buffer (byte [] data) {
      this (data, data.length);
    }
  };
 
  /* rename the internal java methods to 'native_...' so it is clear
   * elsewhere where we're calling the native statics */
  @JniMethod(cast="void *", accessor="flakeyproto_new",
             flags={MethodFlag.POINTER_RETURN}) 
  public static final native long native_new (float probability_percent);
  
  @JniMethod(accessor="flakeyproto_down")
  public static native void native_down(
    @JniArg(cast="const void *", flags={ArgFlag.CRITICAL}) long self,
    long to,
    @JniArg(cast="uint8_t *", flags={CRITICAL, NO_OUT}) byte [] buf,
    @JniArg(cast="size_t", flags={CRITICAL, NO_OUT}) long len
  );
  
  @JniMethod(accessor="flakeyproto_up")
  public static native void native_up(
    @JniArg(cast="const void *", flags={ArgFlag.CRITICAL}) long self,
    long from,
    @JniArg(cast="uint8_t *", flags={CRITICAL, NO_OUT}) byte [] buf,
    @JniArg(cast="size_t", flags={CRITICAL, NO_OUT}) long len
  );

  @JniMethod(accessor="flakeyproto_flood")
  public static native void native_flood(
    @JniArg(cast="const void *", flags={ArgFlag.CRITICAL}) long self,
    long from,
    @JniArg(cast="uint8_t *", flags={CRITICAL, NO_OUT}) byte [] buf,
    @JniArg(cast="size_t", flags={CRITICAL, NO_OUT}) long len
  );

  @JniMethod(cast="void *", accessor="flakeyproto_link_add") 
  public static final native long native_link_add (
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long self,
    long neighbour
  );

  @JniMethod(accessor="flakeyproto_link_remove")
  public static final native void native_link_remove (
    @JniArg(cast="const void *", flags={ArgFlag.CRITICAL}) long self,
    long neighbour
  );

  @JniMethod(accessor="flakeyproto_setid")
  public static final native void native_setid (
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long self,
    long id
  );
  

  @JniMethod(accessor="flakeyproto_set_debug")
  public static final native void native_set_debug (
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long self,
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long debug_cb
  );

  @JniMethod(accessor="flakeyproto_get_debug_msg")
  public static final native void native_get_debug_msg (
    @JniArg(cast="void *", flags={CRITICAL}) long self,
    @JniArg(cast="char *", flags={CRITICAL}) byte[] msg,
    @JniArg(cast="size_t") long length
  );

  @JniMethod(accessor="flakeyproto_set_send_notify")
  public static final native void native_set_send_notify (
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long self,
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long send_cb
  );

  @JniMethod(cast="size_t", accessor="flakeyproto_send_out")
  public static final native long native_send_out (
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long self,
    @JniArg(cast="void *", flags={ArgFlag.CRITICAL}) long msgp,
    @JniArg(cast="uint8_t *", flags={CRITICAL}) byte[] msg,
    @JniArg(cast="size_t") long length
  );

  
  private long native_instance;
  private int num_to_seed;
  
  public flakey (float flakeyprob, int num_to_seed) {
    native_instance = native_new (flakeyprob);
    native_set_debug (native_instance, debug_cb.getAddress ());
    this.num_to_seed = num_to_seed;
  }
  
  public flakey (float flakeyprob) {
    this (flakeyprob, 0);
  }
  
  public void up (Long src, byte [] data) {
    file f = null;
    debug.println(selfId + ": java native up, from " 
                       + src 
                       + " len " + data.length
                       + " " + data[0]);
    try {
      f = file.deserialise (data);
    } catch (Exception e) {
      debug.println ("Unhandled message: " + e);
      return;
    }
    debug.printf ("peer %s: got file %s\n", this, f);
    //debug.printf ("peer %s: byte array: %s\n", this, bytesToHex (data));

    native_buffer buf = new native_buffer (data);

    Callback send_cb = new Callback (this, "send_ready", 3);
    native_set_send_notify (native_instance, send_cb.getAddress ());
    native_up (native_instance, src, data, data.length);
    
    try {
      f = file.deserialise (buf.data);
      debug.printf ("peer %s: after buf data file %s\n", this, f);
    } catch (Exception e) {
      debug.println ("Unhandled message: " + e);
      return;
    }
    
    send_cb.dispose ();
  }

  
  //@JniClass(flags={ClassFlag.STRUCT})
  //public static class flakeyproto_msg {
  //  @JniField(cast="size_t") long len;
  //  byte [] data;
  //};
  
  @JniField(flags={FieldFlag.CONSTANT})
  public static int flakeyproto_msg_maxlen;
  
  /* The notification call back that the native instance has something
   * to send.
   *
   * XXX: Crufty dance with notify callback calling back to ntaive, because
   * Callback method is limited in argument types to long.
   * TODO: Find a better solution, e.g. hand-crafted JNI if needs be.
   */
  public long send_ready (long instance, long to, long msgp) {
    native_buffer msg = new native_buffer (new byte [1000]);
    
    debug.printf ("%d: to %d %d\n", selfId, to, msg.len);
    
    /* HACK: use native code to convert the msgp to byte array */
    native_send_out (instance, msgp, msg.data, msg.len);
    
    try {
      file f = file.deserialise (msg.data);
      debug.printf ("peer %s: send out file %s\n", this, f);
    } catch (Exception e) {
      debug.println ("Unhandled message: " + e);
    }

    send (to, msg.data);

    return 0;
  }
  
  public void down (Long dst, byte [] data) {
    debug.println(selfId + ": java native down");
    native_down (native_instance, dst, data, data.length);
  }
  
  private void seed_file () {
    file f = new file (selfId + "/" + num_to_seed, 
                       "blah blah blah".getBytes ());
    debug.printf ("seed %s: seeding %s\n", selfId, f);
    try {
      byte [] data = f.serialise ().clone();
      Callback send_cb = new Callback (this, "send_ready", 3);
      native_set_send_notify (native_instance, send_cb.getAddress ());
      native_flood (native_instance, selfId, data, data.length);
      send_cb.dispose ();
    } catch (Exception e) {
      debug.println ("Unhandled message: " + e);
    }
  }


  @Override
  public void tick () {
    debug.printf ("flakey %s tick %d\n", selfId, ticks);
    
    if (num_to_seed > 0) {
      seed_file ();
      num_to_seed--;
    } 
    super.tick ();
  }
  
  @Override  
  public void link_add (Long neighbour) {
   debug.println(selfId + ": framework native link_add");
   native_instance = native_link_add (native_instance, neighbour);
  }
  
  @Override
  public void link_remove (Long neighbour) {
   debug.println(selfId + ":framework native link_remove");
   native_link_remove (native_instance, neighbour);
  }
  
  public flakey setId (Long id) {
    super.setId (id);
    native_setid (native_instance, id);
    return this;
  }
}
