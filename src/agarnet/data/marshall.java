package agarnet.data;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Helper to marshall data to/from byte form, using Java serialisation
 * @author paul
 *
 */
public class marshall {
  
  @SuppressWarnings ("unchecked")
  public static <T> T deserialise (T obj, byte [] bytes)
                  throws IOException, ClassNotFoundException {
    ObjectInputStream ois;
    
    ois = new ObjectInputStream (new ByteArrayInputStream (bytes));
    T bd = (T) ois.readObject ();
    ois.close ();
    return bd;
  }
  public static <T> byte [] serialise (T obj) throws IOException {
    ByteArrayOutputStream bos = new ByteArrayOutputStream ();
    ObjectOutputStream oos;
    oos = new ObjectOutputStream (bos);
    oos.writeObject (obj);
    bos.close ();
    return bos.toByteArray ();
  }
}
