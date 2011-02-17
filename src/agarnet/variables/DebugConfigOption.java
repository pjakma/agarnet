package agarnet.variables;

public class DebugConfigOption extends ConfigurableOption {
  public DebugConfigOption (String longOption, char shortOption,
      String argDesc, String help, int longoptHasArg,
      ConfigOptionSet subopts) {
    super (longOption, shortOption, argDesc, help, longoptHasArg, subopts);
  }
  
  @Override
  public ConfigurableOption parse (String args) {
    if (args != null)
      super.parse_subopts (this.subopts, args);
    return this;
  }
}
