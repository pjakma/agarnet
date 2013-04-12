package agarnet.protocols.host;

import org.nongnu.multigraph.layout.AbstractPositionableNode;

import agarnet.framework.Simulation;
import agarnet.protocols.protocol;
import agarnet.protocols.protocol_stats;

public class PositionableHost<I,N> extends AbstractPositionableNode
                                   implements protocol<I> {
  protected host<I,N> host;
  private boolean movable = true;
  protected Simulation<I,N> sim;
  
  protected PositionableHost () {}
  public PositionableHost (Simulation<I,N> sim,
                           boolean movable,
                           protocol<I> [] protocols) {
    this.host = new host<I,N> (sim, protocols);
    this.movable = movable;
    this.sim = sim;
  }
  
  public void reset () {
    host.reset ();
  }
  public long stat_get (protocol_stats.stat s) {
    return host.stat_get (s);
  }
  public long stat_get (int ordinal) {
    return host.stat_get (ordinal);
  }
  public void stats_reset () {
    host.stats_reset ();
  }
  public void stats_reset (protocol_stats.stat s) {
    host.stats_reset (s);
  }
  public void tick () {
    host.tick ();
  }
  public PositionableHost<I,N> setId (I id) {
    host.setId (id);
    return this;
  }
  public I getId () {
    return host.getId ();
  }
  
  public void down (I dst, byte [] data) {
    host.down (dst, data);
  }
  
  public float getSize () {
    return host.stat_get (protocol_stats.stat.stored);
  }

  public void setSize (float arg0) {}
  
  @Override
  public boolean isMovable () {
    return movable;
  }
  
  public String toString () {
    return host.toString ();
  }

  public void up (I src, byte [] data) {
    host.up (src, data);
  }

  @Override
  public void insert (protocol<I> above,
                      protocol<I> below) {
    host.insert (above, below);
  }
  
  @Override
  public void link_update () {
    host.link_update ();
  }
  @Override
  public boolean hasChanged () {
    return host.hasChanged ();
  }
  
}