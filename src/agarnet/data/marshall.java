/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2013 Paul Jakma
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
package agarnet.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.*;
/**
 * Helper to marshall data to/from byte form
 * @author paul
 *
 */
public class marshall {
  
  /* Helper to deserialise a byte stream into a Java object,
   * using Java serialisation.
   */
  @SuppressWarnings ("unchecked")
  public static <T> T deserialise (T obj, byte [] bytes)
                      throws IOException, ClassNotFoundException {
    ObjectInputStream ois;
    
    ois = new ObjectInputStream (new ByteArrayInputStream (bytes));
    T bd = (T) ois.readObject ();
    ois.close ();
    return bd;
  }
  /* Helper to serialise a Java object to a byte stream ,
   * using Java serialisation.
   */
  public static <T> byte [] serialise (T obj) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream ();
    ObjectOutputStream oos;
    oos = new ObjectOutputStream (bos);
    oos.writeObject (obj);
    bos.close ();
    return bos.toByteArray ();
  }
  
  /* Helpers to serialise core types via a direct serialisation via
   * Data{In,Out}putStream.
   *
   * Direct Data*Stream serialisation tends to be a lot more efficient than
   * Object*Stream serdes.  These helpers are to facilitate DS based
   * serialisation of core types, which can not be simply extended to
   * implement serialisable.
   */
  public static <I> boolean serialisable_by_datastream (I obj) {
    return (obj instanceof String
            || obj instanceof Long
            || obj instanceof Integer);
  }
  
  public static void serialise (DataOutputStream dos, String s) 
                     throws IOException {
    dos.writeUTF (s);
  }
  public static void serialise (DataOutputStream dos, Long l) 
                      throws IOException {
    dos.writeLong (l);
  }
  public static void serialise (DataOutputStream dos, Integer i)
                     throws IOException {
    dos.writeInt (i);
  }
  public static <I> void serialise (DataOutputStream dos, I obj) 
                     throws IOException {
    if (obj instanceof String)
      serialise (dos, (String) obj);
    else if (obj instanceof Long)
      serialise (dos, (Long) obj);
    else if (obj instanceof Integer)
      serialise (dos, (Integer) obj);
    else
      throw new IOException("serialise: Unknown type!");
  }
  
  @SuppressWarnings ("unchecked")
  public static <T> T deserialise (T obj, DataInputStream dis) 
                      throws IOException {
    if (obj instanceof String)
      return (T) dis.readUTF ();
    if (obj instanceof Long)
      return (T) new Long (dis.readLong ());
    if (obj instanceof Integer)
      return (T) new Integer (dis.readInt ());

    throw new IOException("deserialise_dis: Unknown type!");
  }
  
}
