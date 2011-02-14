package agarnet.variables;

import org.nongnu.multigraph.layout.Layout;

/* Parameters for layout algorithms.
 * 
 * ForceLayout is the only one that really takes anything
 */
public class LayoutConfigOption extends SuboptConfigOption {  
  public LayoutConfigOption (String longOption, char shortOption,
                             String argDesc, String help,
                             int longoptHasArg,
                             ConfigOptionSet subopts) {
    super (longOption, shortOption, argDesc, help, longoptHasArg, subopts);
  }
  
  public LayoutConfigOption parse (String args) {
    super._parse (args);
    
    if (!Layout.isaLayout (primary)) {
        throw new IllegalArgumentException (
              "--layout requires a valid MultiGraph layout algorithm name");
    }
    return this;
  }
}
