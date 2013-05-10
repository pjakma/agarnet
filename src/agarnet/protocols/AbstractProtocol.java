package agarnet.protocols;

import java.io.IOException;

import org.nongnu.multigraph.debug;

import agarnet.data.marshall;

public abstract class AbstractProtocol<N>
                implements protocol<N>, protocol_stats {
  protected protocol<N> above;
  protected protocol<N> below;
  protected long [] stats = new long [stat.values ().length];
  protected long ticks = 0;
  private final int stat_length = stat.values ().length;
  private boolean changed = false;
  
  /* Unique ID for this instance of this type of protocol - it may
   * be shared across protocols within the same host
   */
  protected N selfId = null;
  
  @Override
  public void insert (protocol<N> above, protocol<N> below) {
    this.above = above;
    this.below = below;
    stats_reset ();
  }
  public protocol<N> setId (N id) {
    selfId = id;
    
    if (above != null)
      above.setId (id);
    
    return this;
  }
  public N getId () {
    return selfId;
  }
  
  protected void stats_inc (stat s) {
    stats[s.ordinal ()]++;
  }
  public long stat_get (stat s) {
    return stats[s.ordinal ()];
  }
  /* this is for performance reasons primarily. */
  public long stat_get (int ordinal) {
    if (ordinal >= stat_length)
      throw new ArrayIndexOutOfBoundsException (ordinal);
    
    return stats[ordinal];
  }
  
  public String toString () {
    return selfId.toString ();
  }
  
  public void stats_reset () {
    for (stat s : stat.values ())
      stats[s.ordinal ()] = 0;
    ticks = 0;
  }
  public void stats_reset (stat s) {
    stats[s.ordinal ()] = 0;
  }
  
  public void reset () {
    above = below = null;
    selfId = null;
    stats_reset ();
    clearChanged ();
  }
  
  public void tick () { ticks++; clearChanged (); }
  public void link_update () {}
  
  /* These are in the spirit of the Observable class, however the decision
   * to not implement the rest of Observable is a deliberate one. Such
   * a role is best left to a general host class alone.
   *
   * /* The simulator's default run() loop will consider that a host that
   * hasChanged() still has more work to do, and keep the simulation running
   * for at least another tick.
   */
  protected void clearChanged () {
    changed = false;
  }
  /* The simulator's default run() loop will consider that a host that
   * hasChanged() still has more work to do, and keep the simulation running
   * for at least another tick.
   */
  public boolean hasChanged () {
    return changed;
  }
  /* The simulator's default run() loop will consider that a host that
   * hasChanged() still has more work to do, and keep the simulation running
   * for at least another tick.
   */
  protected  void setChanged () {
    changed = true;
  }
  
  @Override
  public void down (N dst, byte [] data) {
    throw new UnsupportedOperationException (
              "application protocol doesn't accept messsages from above");
  }
  
  /**
   * Convenience function for protocol implementations: Serialise the
   * given object and send it to the destination.
   * @param to Destination node
   * @param msg Message object to serialise and send.
   */
  protected void send (N to, agarnet.serialisable msg) {
    byte [] data;
    try {
      data = msg.serialise ();
    } catch (IOException e) {
      debug.println ("Weird, couldn't serialise message!");
      e.printStackTrace();
      return;
    }
    send (to, data);
  }
  
  /**
   * Convenience function for protocol implementations. Send
   * the given packet to the destination, incrementing the
   * sent packets statistics.
   * @param to
   * @param data
   */
  protected void send (N to, byte [] data) {
    below.down (to, data);
    stats_inc (stat.sent);
  }
}
