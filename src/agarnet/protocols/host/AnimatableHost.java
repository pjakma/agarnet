package agarnet.protocols.host;

import agarnet.framework.Coloured;
import agarnet.framework.Simulation;
import agarnet.protocols.protocol;

import java.awt.Color;

public abstract class AnimatableHost<I,N> extends PositionableHost<I,N>
                                 implements Coloured {
  protected AnimatableHost () {}
  public AnimatableHost (Simulation<I,N> sim,
                           boolean movable, 
                           protocol<I> [] protocols) {
    super (sim, movable, protocols);
  }
  
  private Color colour = new Color (255, 255, 255);
  public Color colour () {
    /* Color appears to be immutable, so this should always be fine */
    return colour;
  }
  public void colour (Color c) {
    colour = c;
  }
}
