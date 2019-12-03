/* This file is part of 'Subopt'
 *
 * Copyright (C) 2010, 2011, 2013, 2018 Paul Jakma
 *
 * Subopt is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * Subopt is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with Subopt.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.variables;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;
import agarnet.Subopt;
import agarnet.variables.atoms.*;
/* Support for a Config option with 0 or more sub-options, e.g.:
 * --option ([sub-option1][,sub-option2][,...])
 * The details of which are down to the concrete implementations.
 */
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
  
  /**
   * Handle command line argument parsing with the given...
   * 
   * @param name
   * @param g
   */
  public static int getopts (String name, String [] args,
                             List<ConfigurableOption> confvars) {
    int c;
    LinkedList<LongOpt> lopts = new LinkedList<LongOpt> ();
    Map<Integer, ConfigurableOption> char2opt = new HashMap<> ();
    
    for (ConfigurableOption cv : confvars) {
      lopts.add (cv.lopt);
      char2opt.put (cv.lopt.getVal (), cv);
    }
    
    Getopt g = new Getopt(name, args, 
                          ConfigurableOption.get_optstring (confvars),
                          lopts.toArray (new LongOpt [0]));
    while ((c = g.getopt ()) != -1) {
      if (c == '?')
        return g.getOptopt ();
      
      ConfigurableOption cv = char2opt.get (c);
      
      if (cv == null)
        return -1;
      
      cv.getopt (g);
    }
    return 0;
  }
  
  public void getopt (Getopt g) {
    parse (g.getOptarg ());
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
    
    if (subopt.valuep != null)
      throw new IllegalArgumentException ("Unknown suboption: " 
                                          + subopt.valuep);
  }
  
  public String toString () {
    StringBuilder sb = new StringBuilder ();
    
    /* special-case: a booleanvar subopt, with same name as 
     * as the option itself, is assumed to stand for the
     * option.
     */
    if (subopts != null && subopts.get (lopt.getName ()) != null)
      sb.append (subopts.get (lopt.getName ()));
    else
      sb.append (lopt.getName () + ":");

    if (subopts == null)
      return sb.toString ();

    for (String key : subopts.subopt_keys ())
      if (subopts.get (key).isSet ())
        sb.append ("\n  " + subopts.get (key));
    return sb.toString ();
  }
  public abstract ConfigurableOption parse (String arg);
}
