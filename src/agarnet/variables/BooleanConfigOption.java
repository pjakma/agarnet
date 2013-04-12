package agarnet.variables;

import gnu.getopt.LongOpt;

public class BooleanConfigOption extends ConfigurableOption {
  boolean val = false;
  
  public BooleanConfigOption (String longOption, char shortOption,
                           String help) {
    super (longOption, shortOption, "", help, LongOpt.NO_ARGUMENT);
  }

  public boolean get () {
    return val;
  }

  public BooleanConfigOption set (boolean val) {
    this.val = val;
    return this;
  }
  
  public String toString () {
    return lopt.getName () + ": " + val;
  }

  @Override
  public ConfigurableOption parse (String arg) {
    return this;
  }
}
