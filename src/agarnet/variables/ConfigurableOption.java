package agarnet.variables;
import java.util.List;

import gnu.getopt.LongOpt;
import agarnet.Subopt;
import agarnet.variables.atoms.*;

public abstract class ConfigurableOption {
  public final String help;
  public final String arg_desc;
  public final LongOpt lopt;
  public ConfigOptionSet subopts;
  
  private void _sanity_check_vars (String long_option, char short_option,
                                   String short_desc, String help,
                                   int longopt_has_arg) {
    if (long_option == null)
      throw new IllegalArgumentException ("long_option must not be null");
    if (short_option == '\0')
      throw new IllegalArgumentException ("short_option must not be null");
    if (short_desc == null)
      throw new IllegalArgumentException ("short_desc must not be null");
    if (help == null)
      throw new IllegalArgumentException ("help must not be null");
    
    switch (longopt_has_arg) {
      case LongOpt.NO_ARGUMENT:
      case LongOpt.OPTIONAL_ARGUMENT:
      case LongOpt.REQUIRED_ARGUMENT:
        break;
      default:
        throw new IllegalArgumentException ("has_arg must be a valid Longopt" +
        		                                " value"); 
    }
  }
  
  public ConfigurableOption (String long_option,
                             char short_option,
                             String arg_desc,
                             String help,
                             int longopt_has_arg,
                             ConfigOptionSet subopts) {
    _sanity_check_vars (long_option, short_option, arg_desc, help,
                        longopt_has_arg);
    
    this.subopts = subopts;
    this.arg_desc = arg_desc;
    this.help = help;
    lopt = new LongOpt (long_option, longopt_has_arg, null, short_option);
  }
  
  public ConfigurableOption (String long_option,
                             char short_option,
                             String arg_desc,
                             String help,
                             int longopt_has_arg) {
    _sanity_check_vars (long_option, short_option, arg_desc, help,
                        longopt_has_arg);
    
    this.subopts = null;
    this.arg_desc = arg_desc;
    this.help = help;
    lopt = new LongOpt (long_option, longopt_has_arg, null, short_option);
  }
  
  /**
   * Return an option string suitable GNU Getopt
   * @param cvars A List of ConfigureVariable objects, for which to
   *              construct the getopt() option string.
   * @return An option description string suitable for GNU getopt.
   */
  public static String get_optstring (List<ConfigurableOption> cvars) {
    StringBuilder sb = new StringBuilder ();
    
    for (ConfigurableOption cv : cvars) {
      sb.append ((char) cv.lopt.getVal ());
      
      switch (cv.lopt.getHasArg ()) {
        case LongOpt.OPTIONAL_ARGUMENT:
          sb.append ("::");
          break;
        case LongOpt.REQUIRED_ARGUMENT:
          sb.append (':');
          break;
      }
    }
    return sb.toString ();
  }
  /**
   * Return an option string suitable GNU Getopt
   * @param cvars An array of ConfigureVariable objects, for which to
   *              construct the getopt() option string.
   * @return An option description string suitable for GNU getopt.
   */
  public static String get_optstring (ConfigurableOption [] cvars) {
    /* This should remain identical to the List version.
     * They can't be consolidated without needless copying,
     * it seems.
     */
    StringBuilder sb = new StringBuilder ();
    
    for (ConfigurableOption cv : cvars) {
      sb.append ((char) cv.lopt.getVal ());
      
      switch (cv.lopt.getHasArg ()) {
        case LongOpt.OPTIONAL_ARGUMENT:
          sb.append ("::");
          break;
        case LongOpt.REQUIRED_ARGUMENT:
          sb.append (':');
          break;
      }
    }
    return sb.toString ();
  }
  
  protected void parse_subopts (ConfigOptionSet subopts,
                                String args) {
    parse_subopts (subopts, null, args);
  }
  protected void parse_subopts (ConfigOptionSet subopts,
                                String branch,
                                String args) {
    Subopt subopt;
    
    subopt = (branch != null) ? new Subopt (args, subopts.keys (branch))
                              : new Subopt (args, subopts.keys ());
    
    while (subopt.get () >= 0) {
      ObjectVar obv;
      
      obv = (branch != null) ? subopts.get (branch, subopt.optionp)
                            : subopts.get (subopt.optionp);
      
      /* Boolean is the only one we should special case like this,
       * because it's the only one that need not take an argument. 
       */
      if (obv instanceof BooleanVar) {
        if (subopt.valuep != null)
          obv.set (subopt.valuep);
        else {
          ((BooleanVar) obv).set (true);
        }
        
        continue;
      }
      
      if (subopt.valuep == null)
        throw new IllegalArgumentException (obv.getName () + 
                                            " requires argument");
      
      obv.set (subopt.valuep);
    }
  }
  
  public String toString () {
    return lopt.getName ();
  }
  public abstract ConfigurableOption parse (String arg);
}
