package agarnet.variables.atoms;

public class StringVar extends AbstractVar {
  private String val;
  
  public StringVar (String name, String desc) {
    super (name, desc);
  }
  
  public StringVar set (String val) {
    this.val = val;
    isset = true;
    return this;
  }
  public String get () { return val; }
  public String toString () {
    return super.toString () + ": " + val;
  }
}