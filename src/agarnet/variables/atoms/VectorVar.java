package agarnet.variables.atoms;

import org.nongnu.multigraph.layout.Vector2D;

public class VectorVar extends AbstractVar {
  private Vector2D val;
  
  public VectorVar (String name, String desc) {
    super (name, desc);
  }
  
  public VectorVar set (String s) {
    String [] results = s.split ("x");
    double x,y;
    
    if (results.length != 2)
      throw new IllegalArgumentException ("Vector must in the form XxY");
    
    x = new Double (results[0]).doubleValue ();
    y = new Double (results[1]).doubleValue ();
    
    set (new Vector2D (x,y));
    return this;
  }
  public VectorVar set (Vector2D val) {
    this.val = val;
    isset = true;
    return this;
  }
  public Vector2D get () { return val; }
  public String toString () {
    return super.toString () + ": " + val;
  }
}