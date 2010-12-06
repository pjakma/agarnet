package agarnet.variables.atoms;

/* A number which represents either a probability or a positive integer value */
public class NumberProbVar extends FloatVar implements NumberVar {

  public NumberProbVar (String name, String desc) {
    super (name, desc);
  }
  /**
   * parse a <number|probability> string.
   * @param s A string representing an integer number, or a probability.
   * @return A float value, which represents a probability if 0 <= x < 1 or
   *         or x ends in with a % and 0 < x <= 100, an integer number otherwise.
   */
  private static float number_probability (String s)
                       throws NumberFormatException {
    boolean pc = false;
    float num;
    int i;
    
    if ((i = s.indexOf ('%')) >= 0) {
      if (i != s.length () - 1)
        throw new NumberFormatException ("% is only valid as last character");
      
      s = s.replace ('%', '\0');
      pc = true;
    }
    
    num = new Float (s).floatValue ();
    
    if (pc) {
      if (num > 100)
        throw new NumberFormatException ("% probability must be <= 100");
      
      num /= 100;
    }
    
    return num;
  }
  @Override
  public FloatVar set (Number val) {
    float num = val.floatValue ();
    
    if (num >= 1 && Math.floor (num) != num)
      throw new NumberFormatException ("number must be an integer value");
    else if (num < 0)
      throw new NumberFormatException ("number must be a probability or a " +
                                       "positive integer");
    return super.set (num);
  }
  @Override
  public FloatVar set (String s) {
    return super.set (number_probability (s));
  }
  @Override
  public String toString () {
    return super.toString ();
  }
  
}
