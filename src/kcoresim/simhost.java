package kcoresim;

import java.awt.Color;

import org.nongnu.multigraph.debug;
import org.nongnu.multigraph.structure.kshell_node;
import org.nongnu.multigraph.structure.kshell_node_data;

import agarnet.framework.Simulation;
import agarnet.protocols.protocol;
import agarnet.protocols.protocol_logical_clock;

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
  private kcore<simhost> kcore_protocol = null;
  private protocol_logical_clock<Long> lc_proto = null;
  
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
                  protocol<Long> [] pcols) {
    super (simapp, true, pcols);
    
    for (int i = 0; i < pcols.length; i++) {
      if (pcols[i] instanceof kcore<?>)
        kcore_protocol = (kcore<simhost>) pcols[i];
      if (pcols[i] instanceof protocol_logical_clock<?>)
        lc_proto = (protocol_logical_clock<Long>) pcols[i];
    }
    if (kcore_protocol == null)
      throw new java.lang.IllegalArgumentException ("kcore protocol required in the stack.");
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
    return kcore_protocol.toString ();
  }
  public int kbound () {
    return kcore_protocol.kbound;
  }
  public long kgen () {
    return kcore_protocol.generation;
  }
  public long degree () {
    return kcore_protocol.connected.size ();
  }
  
  public long logical_time () {
    return lc_proto != null ? lc_proto.time () : -1;
  }
  private int debug_kcore () {
    debug.levels dl = debug.level ();
    debug.level (debug.levels.DEBUG);
    
    int kbound = kcore_protocol.calc_kbound ();
    
    debug.level (dl);
    
    return kbound;
  }
}
