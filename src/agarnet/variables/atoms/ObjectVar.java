package agarnet.variables.atoms;

public interface ObjectVar {
    
  ObjectVar set (String s);
  boolean isSet ();
  
  String getName ();
  
  String getDesc ();
  
}