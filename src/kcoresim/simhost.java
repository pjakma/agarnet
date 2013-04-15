package kcoresim;

import java.awt.Color;

import org.nongnu.multigraph.debug;
import org.nongnu.multigraph.structure.kshell_node;
import org.nongnu.multigraph.structure.kshell_node_data;

import agarnet.framework.Simulation;
import agarnet.protocols.protocol;
import agarnet.protocols.host.AnimatableHost;

/* This is a host type for:
 * 
 * PositionableHost, for a simulation using Long identifiers, and protocols.
 * 
 * Basically, this is what Java makes you do to get typedefs.
 */
public class simhost extends AnimatableHost<Long, simhost>
                     implements kshell_node {
  private int maxdegree;
  private final int protocol_index;

  /* for globalkcore */
  private kshell_node_data gkc = new kshell_node_data ();
  public kshell_node_data gkc () {
    return gkc;
  }
  
  public void maxdegree (int maxdegree) {
    this.maxdegree = Math.max (maxdegree,1);
  }
  public int maxdegree () { return maxdegree; }
  
  @Override
  public void reset () {
    super.reset ();
    maxdegree = 0;
    gkc.reset ();
  }

  public simhost (Simulation<Long,simhost> simapp,
                  protocol<Long> [] pcols, int index) {
    super (simapp, true, pcols);
    protocol_index = index;
  }
  
  @Override
  public Color colour () {
    return Color.getHSBColor(getSize ()/maxdegree, 0.8f, 0.8f);
  }

  @Override
  public String toString () {
    return "(host: " + getId () + ", "
               + "d: " + stat_get (stat.stored) + ", "
               + "g: " + gkc.k + ", "
               + ((gkc.removed) ? "removed" : "")
               + (gkc.k == 0 || (stat_get (stat.stored) == gkc.k) ? ""
                                                    : (" - mismatch! " ))
         + ")";
  }
  
  public String kcorestring () {
    return ((kcore<simhost>)(host.protocols ()[protocol_index])).toString ();
  }
  public int kbound () {
    return ((kcore<simhost>)(host.protocols ()[protocol_index])).kbound;
  }
  public long kgen () {
    return ((kcore<simhost>)(host.protocols ()[protocol_index])).generation;
  }
  public long degree () {
    return ((kcore<simhost>)(host.protocols ()[protocol_index])).connected.size ();
  }
  
  private int debug_kcore () {
    debug.levels dl = debug.level ();
    debug.level (debug.levels.DEBUG);
    
    int kbound
      = ((kcore<simhost>)(host.protocols ()[protocol_index])).calc_kbound ();
    
    debug.level (dl);
    
    return kbound;
  }
}
