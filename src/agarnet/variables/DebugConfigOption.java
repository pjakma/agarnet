package agarnet.variables;

import agarnet.variables.atoms.BooleanVar;
import gnu.getopt.Getopt;

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

  @Override
  public void getopt (Getopt g) {
    BooleanVar db = ((BooleanVar) subopts.get ("debug"));
    db.set (true);
    parse (g.getOptarg ());
  }
}
