/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2013 Paul Jakma
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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

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
}
