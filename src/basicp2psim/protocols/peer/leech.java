package basicp2psim.protocols.peer;

import java.util.Set;

import basicp2psim.protocols.peer.data.file;

import agarnet.framework.Simulation;
import agarnet.protocols.transport.data.message;

/** 
 * A peer which accepts messages, but never sends any on
 */
public class leech<I,N> extends peer<I,N> {

  public leech (Simulation<I,N> sim) {
    super (sim);
  }
  
  public leech () {
    super (null);
  }

  @Override
  public boolean should_send (I to, file msg) {
    return false;
  }
  
}
