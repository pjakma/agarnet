package agarnet.variables;

import java.awt.Dimension;

public class DimensionConfigOption extends ConfigurableOption {
  Dimension d = new Dimension ();
  
  public DimensionConfigOption (String longOption, char shortOption,
                                  String shortDesc, String help,
                                  int longoptHasArg) {
    super (longOption, shortOption, shortDesc, help, longoptHasArg);
  }
  
  public Dimension get () {
    return d;
  }
  public DimensionConfigOption set (Dimension d) {
    this.d = d;
    return this;
  }
  
  public DimensionConfigOption parse (String arg)
    throws IllegalArgumentException {
    String [] subargs  = arg.split ("[xX]");
    
    if (subargs.length != 2)
      throw new IllegalArgumentException (
            lopt.getName () + "requires <integer>x<integer> as arguments");
    try {
      int width, height;
      width = new Integer (subargs[0]).intValue ();
      height = new Integer (subargs[1]).intValue ();
      d.setSize (width, height);
      
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException (lopt.getName () + 
                                "requires <integer>x<integer> as arguments");
    }
    return this;
  }
  
  public String toString () {
    return super.toString () + ": " + d;
  }
}
