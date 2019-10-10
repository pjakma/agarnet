package agarnet.framework;

import java.awt.Dimension;
import java.io.Serializable;

import org.nongnu.multigraph.Graph;

import agarnet.link.link;
import agarnet.protocols.host.PositionableHost;

/**
 * Simulation where hosts are positioned within a finite 2D space.
 * @author paul
 * @param <I> The Id type used to identify hosts nodes
 * @param <H> The type used for Hosts in the Simulation
 */
public abstract class Simulation2D<I extends Serializable,
                                   H extends PositionableHost<I,H>>
                extends Simulation<I,H> {
  public final Dimension model_size;

  public Simulation2D (Graph<H,link<H>> g, Dimension space) {
    super (g);
    this.model_size = space;
  }
}
