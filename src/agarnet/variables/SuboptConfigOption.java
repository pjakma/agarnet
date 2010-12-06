package agarnet.variables;

import agarnet.Subopt;

/* Config option with multiple branches of sub-options, e.g.:
 *  
 * --option (branch1[,<one or more branch1 subopts>]|branch2[,...][|..])
 **/
public class SuboptConfigOption extends ConfigurableOption {
  protected String primary;
  
  public SuboptConfigOption (String longOption,
                             char shortOption,
                             String argDesc,
                             String help,
                             int longoptHasArg,
                             ConfigOptionSet subopts) {
    super (longOption, shortOption, argDesc, help, longoptHasArg, subopts);
  }
  
  protected Subopt _parse (String args) {
    Subopt subopt = new Subopt (args, subopts.keys ());
    
    int ret = subopt.get ();
    primary = subopt.optionp;
    
    if (ret >= 0) {
      if (subopts.num_subopts (subopt.optionp) == 1
          && subopt.valuep != null)
        throw new IllegalArgumentException (
            subopt.valuep + " does not take further sub-options");
      
      if (subopts.branch_keys ().contains (subopt.optionp))
        parse_subopts (subopts, subopt.optionp, args);
      else
        parse_subopts (subopts, args);
    }
    return subopt;
  }
  
  public SuboptConfigOption parse (String args) {
    _parse (args);
    return this;
  }
  
  public String get () {
    return primary;
  }
  
  public String toString () {
    StringBuilder sb = new StringBuilder ();
    
    sb.append (super.toString () + ": " + primary + "\n");
    if (subopts.num_subopts (primary) > 0)
      for (String subkey : subopts.subopt_keys (primary))
        if (subopts.get (primary, subkey).isSet ())
          sb.append ("  " + subopts.get (primary, subkey) + "\n");
    for (String key : subopts.subopt_keys ())
      if (subopts.get (key).isSet ())
        sb.append ("  " + subopts.get (key) + "\n");
    return sb.toString ();
  }
}
