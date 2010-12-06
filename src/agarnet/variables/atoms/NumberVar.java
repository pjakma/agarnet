package agarnet.variables.atoms;

public interface NumberVar extends ObjectVar {
  
  public NumberVar set (Number val);
  
  public NumberVar set (String s);
  
  public Number get ();
  
  public String getName ();
  
  public String getDesc ();
  
}