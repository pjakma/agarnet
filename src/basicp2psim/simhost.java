package basicp2psim;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.io.Serializable;

import org.nongnu.multigraph.structure.kshell_node;
import org.nongnu.multigraph.structure.kshell_node_data;

import agarnet.framework.Simulation;
import agarnet.protocols.protocol;
import agarnet.protocols.host.AnimatableHost;

/* This is a host type for:
 * 
 * PositionableHost, for a simulation using I identifiers, and protocols.
 * 
 * Basically, this is what Java makes you do to get typedefs.
 */
public class simhost<I extends Serializable> extends AnimatableHost<I, simhost<I>>
                     implements kshell_node {
  private static final long serialVersionUID = -7814606266303045281L;
  public final Node type;
  
  public enum Node {
    seed, leech, peer;
    private final static Color colours[] = {
      Color.CYAN.brighter ().brighter ().brighter (),
      Color.GREEN,
      Color.YELLOW,
    };
    private static Map<String,Node> peer2node = new HashMap <String,Node> ();
    static {
      peer2node.put (basicp2psim.protocols.peer.peer.class.getName (), peer);
      peer2node.put (basicp2psim.protocols.peer.seed.class.getName (), seed);
      peer2node.put (basicp2psim.protocols.peer.leech.class.getName (), leech);
    }
    public Color colour () {
      return colours[this.ordinal ()];
    }
    public static Node toNode (String name) {
      Node n = peer2node.get (name);
      return (n != null) ? n : peer;
    }
  }
  
  public simhost (Simulation<I,simhost<I>> simapp, 
                  Node type, boolean movable,
                  protocol<I> [] pcols) {
    super (simapp, movable, pcols);
    this.type = type;
  }

  @Override
  public Color colour () {
    return type.colour ();
  }
  
  private final kshell_node_data gkc = new kshell_node_data ();
  @Override
  public kshell_node_data gkc () {
    return gkc;
  }
}
