package agarnet.variables;

import agarnet.variables.atoms.FloatVar;
import agarnet.variables.atoms.NumberProbVar;

/* ConfigurableOption wrapper around NumeberProb */
public class NumberProbabilityConfigOption extends ConfigurableOption {
  final NumberProbVar val;
  
  public NumberProbabilityConfigOption (String longOption,
                                        char shortOption,
                                        String argDesc, String help,
                                        int longoptHasArg) {
    super (longOption, shortOption, argDesc, help, longoptHasArg);
    val = new NumberProbVar (longOption, help);
  }
  
  public NumberProbabilityConfigOption set (float num) {
    val.set (num);
    return this;
  }
  
  /**
   * Parse the argument
   * @param arg
   */
  public NumberProbabilityConfigOption parse (String arg)
                               throws IllegalArgumentException {
    val.set (arg);
    return this;
  }
  
  public float get () {
    return val.get ();
  }
  
  public String toString () {
    return val.toString ();
  }
}
