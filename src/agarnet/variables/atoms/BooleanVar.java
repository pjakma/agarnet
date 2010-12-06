package agarnet.variables.atoms;

/* Variable with no associated data - just a name */
public class BooleanVar extends AbstractVar { 
  boolean val;
  
  public BooleanVar (String name, String desc) {
    super (name, desc);
  }

  public boolean get () { return val; }

  @Override
  public BooleanVar set (String s) {
    set (Boolean.parseBoolean (s));
    return this;
  }
  
  public BooleanVar set (boolean b) {
    val = b;
    isset = true;
    return this;
  }
  
  public String toString () {
    return super.toString () + ": " + val;
  }
}