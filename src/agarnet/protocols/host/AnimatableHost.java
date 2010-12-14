package agarnet.protocols.host;

import agarnet.framework.Coloured;
import agarnet.framework.Simulation;
import agarnet.protocols.protocol;

public abstract class AnimatableHost<I,N> extends PositionableHost<I,N>
                                 implements Coloured {
  protected AnimatableHost () {}
  public AnimatableHost (Simulation<I,N> sim,
                           boolean movable, 
                           protocol<I> [] protocols) {
    super (sim, movable, protocols);
  }
}
