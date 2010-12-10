package agarnet;

import agarnet.protocols.protocol;
import agarnet.protocols.host.PositionableHost;

/* This is a host type for:
 * 
 * PositionableHost, for a simulation using Long identifiers, and protocols.
 * 
 * Basically, this is what Java makes you do to get typedefs.
 */
public class simhost extends PositionableHost<Long, simhost, anipanel.Node> {
  private static final long serialVersionUID = -7814606266303045281L;
  
  @SuppressWarnings("unused")
  private simhost () {}
  
  public simhost (Simulation<Long,simhost> simapp, 
                  anipanel.Node type, boolean movable,
                  protocol<Long> [] pcols) {
    super (simapp, type, movable, pcols);
  }
}
