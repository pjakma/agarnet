package agarnet.variables;

import gnu.getopt.LongOpt;

public class StringConfigOption extends ConfigurableOption {
  String val = null;
  
  public StringConfigOption (String longOption, char shortOption,
                           String help) {
    super (longOption, shortOption, "", help, LongOpt.NO_ARGUMENT);
  }

  public String get () {
    return val;
  }

  public StringConfigOption set (String val) {
    this.val = val;
    return this;
  }
  
  public String toString () {
    return super.toString () + ": " + val;
  }

  @Override
  public ConfigurableOption parse (String arg) {
    val = arg;
    return this;
  }
}
