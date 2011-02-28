/* Copyright (C) 2009 Paul Jakma
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
package agarnet;


/**
 * Analogous to the GNU C library getsubopt() function. This class parses
 * a list of comma-separated suboptions, where the list of valid suboptions 
 * is given by a list of tokens. Each suboption optionally can specify
 * a value argument, introduced with a =. E.g.:
 * 
 *  suboption1,suboption2=value1,suboption3=value2,suboption4
 * 
 * The original GNU getsubopt() code described the function as:
 * 
 * "Parse comma separated suboption from *OPTIONP and match against
 * strings in TOKENS.  If found return index and set *VALUEP to
 * optional value introduced by an equal sign.  If the suboption is
 * not part of TOKENS return in *VALUEP beginning of unknown
 * suboption.  On exit *OPTIONP is set to the beginning of the next
 * token or at the terminating NUL character."
 * 
 * This class tries to follow it in spirit. The original full optionp
 * string and tokens are specified to the constructor, rather than given as
 * pointers. The get() function sets public optionp and valuep values in the
 * manner as specified for the C getsubopt() function. It also returns -1 when
 * there is no valid option, however for valid options it returns a count
 * of which suboption this is (rather than an index into a pointer). 
 * 
 * @author Paul Jakma
 *
 */
public class Subopt {
  private String [] options;
  private final String [] tokens;
  private int curopt = 0;
  
  public String optionp;
  public String valuep;
  
  /**
   * Instantiate a Subopt parser for the given optionp String and list of valid
   * tokens.
   * @param optionp A comma delimited string of sub-option strings. Each 
   *                sub-option
   *                string may either be in form 'suboption' or 'suboption=value'.
   * @param tokens  A list of valid suboptions to recognise.
   **/
  public Subopt (String optionp, String [] tokens) {
    this.options = optionp.split (",");
    this.tokens = tokens;
  }
  
  /**
   * Find the next sub-option string
   * @return If a valid suboption is found, then returns its index in the token
   *         array, with optionp set to the suboption string and valuep set 
   *         to the optional value string, if present.
   *         
   *         If a suboption was found, but not recognised as valid, then -1 is
   *         returned with valuep set to the unrecognised suboption.
   *         
   *         If there are no further suboptions, -1 is returned, and optionp
   *         and valuep are set to null.
   */
  public int get () {
    optionp = valuep = null;
    String [] s;
    
    /* No more subopts, return null optionp and -1 */
    if (curopt >= options.length)
      return -1;
    
    s = options[curopt++].split ("=", 2);
    
    if (s.length == 0)
      return -1;
    
    /* If the suboption is not part of TOKENS return in VALUEP the unknown
     * suboption
     */
    valuep = s[0];
    
    for (int i = 0; i < tokens.length; i++) {
      if (!tokens[i].equals (s[0]))
        continue;
      /* If found return index and set VALUEP to
       * optional value introduced by an equal sign.
       */
      optionp = s[0];
      valuep = (s.length > 1 ? s[1] : null);
      
      return i;
    }
    return -1;
  }
}
