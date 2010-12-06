package agarnet.variables.atoms;

abstract class AbstractVar implements ObjectVar {
  protected final String name;
  protected final String desc;
  protected boolean isset = false;
  
  public AbstractVar (String name, String desc) {
    this.name = name;
    this.desc = desc;
  }
  
  @Override
  public String getDesc () {
    return desc;
  }
  
  @Override
  public String getName () {
    return name;
  }
  
  public String toString () {
    return name;
  }
  
  public boolean isSet () {
    return isset;
  }
}
