package agarnet.variables.atoms;

public class FloatVar extends AbstractVar implements NumberVar {
  private float min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
  private float value;
  
  public FloatVar (String name, String desc) {
    super (name, desc);
  }
  public FloatVar (String name, String desc, float min, float max) {
    super (name, desc);
    this.max = max;
    this.min = min;
    
    if (max < min)
      throw new IllegalArgumentException (name + ": max must be >= min");
  }
  
  public FloatVar set (Number val) {
    if (val.floatValue () > max)
      throw new IllegalArgumentException (name + " must be <= " + max);
    if (val.floatValue () < min)
      throw new IllegalArgumentException (name +
                                          " must be >= " + min);
    value = val.floatValue ();
    isset = true;
    return this;
  }
  
  public FloatVar set (String s) {
    float v;
    try {
      v = new Float (s).floatValue ();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException (name +
                                          " requires numeric argument");
    }
    set (v);
    return this;
  }
  
  public Float get () {
    return value;
  }
  
  public String toString () {
    return super.toString () + ": " + value;
  }
}
