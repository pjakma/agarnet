package agarnet.protocols.host;

import agarnet.framework.Simulation;
import agarnet.protocols.protocol;

public class AnimatableHost<I,N,T> extends PositionableHost<I,N> {
  private T type;
  
  protected AnimatableHost () {}
  public AnimatableHost (Simulation<I,N> sim, T type,
                           boolean movable, 
                           protocol<I> [] protocols) {
    super (sim, movable, protocols);
    this.type = type;
  }
  
  public T get_type () { return type; }
}
