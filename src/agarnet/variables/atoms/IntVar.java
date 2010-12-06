package agarnet.variables.atoms;

public class IntVar extends AbstractVar implements NumberVar {
  private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;
  private int value;
  
  public IntVar (String name, String desc) {
    super (name, desc);
  }
  public IntVar (String name, String desc, int min, int max) {
    super (name, desc);
    this.max = max;
    this.min = min;
    
    if (max < min)
      throw new IllegalArgumentException (name + ": max must be >= min");
  }
  
  /* (non-Javadoc)
   * @see basic_model.variables.NumberVar#set(int)
   */
  public IntVar set (Number val) {
    if (val.intValue () > max)
      throw new IllegalArgumentException (getName() + " must be <= " + max);
    if (val.intValue () < min)
      throw new IllegalArgumentException (getName() +
                                          " must be >= " + min);
    value = val.intValue ();
    isset = true;
    return this;
  }
  
  /* (non-Javadoc)
   * @see basic_model.variables.NumberVar#set(java.lang.String)
   */
  public IntVar set (String s) {
    int v;
    try {
      v = new Integer (s).intValue ();
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException (getName() +
                                          " requires integer as argument");
    }
    set (v);
    return this;
  }
  
  /* (non-Javadoc)
   * @see basic_model.variables.NumberVar#get()
   */
  public Integer get () {
    return value;
  }
  
  public String toString () {
    return super.toString () + ": " + value;
  }
}
