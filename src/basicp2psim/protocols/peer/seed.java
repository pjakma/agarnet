package basicp2psim.protocols.peer;

import java.util.Set;

import org.nongnu.multigraph.debug;

import basicp2psim.protocols.peer.data.file;

import agarnet.data.*;
import agarnet.framework.Simulation;

/**
 * A peer which, in addition to doing all the normal peer things, also
 * generates new messages to send on into the network, at some rate and up
 * to some maximum number.
 * @author paul
 *
 */
public class seed<I,N> extends peer<I,N> {
  private int seedcount = 0;
  private int max = 1;
  private int period = 1;
  
  /**
   * Create a new seed, to generate up to the given number of new messages.
   * 
   * @param sim The simulation framework
   * @param name A name for the peer.
   * @param max The maximum number of messages to generate, must be greater
   *            than 0 or it will be ignored.
   */
  public seed (Simulation<I,N> sim, int max) {
    super (sim);
    if (max > 0)
      this.max = max;
  }
  /**
   * Create a new seed, to generate up to the given number of new messages,
   * at the specified rate.
   * 
   * @param sim The simulation framework
   * @param name A name for the peer.
   * @param max The maximum number of messages to generate
   * @param period The period with which to generate new messages, in
   *               simulation ticks.
   */
  public seed (Simulation<I,N> sim, int max, int period) {
    super (sim);
    if (max > 0)
      this.max = max;
    if (period > 0)
      this.period = period;
  }
  
  private void seed_file () {
    file f = new file (selfId + "/" + seedcount++, "blah blah blah".getBytes ());
    debug.printf ("seed %s: seeding %s\n", selfId, f);
    this.send (f);
  }
  
  @Override
  public void tick () {
    debug.printf ("seed %s tick %d\n", selfId, ticks);
    
    if ((this.ticks % period == 0)
        && (max > 0 ? seedcount < max : true))
      seed_file ();
    
    super.tick ();
  }

  @Override
  public void reset () {
    super.reset ();
    seedcount = 0;
  }
}
