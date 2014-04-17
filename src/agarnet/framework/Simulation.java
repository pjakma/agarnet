package agarnet.framework;

import java.awt.Dimension;
import java.util.Observable;
import java.util.Set;
import org.nongnu.multigraph.*;

import agarnet.link.link;

/**
 * This class represents the Simulation to hosts within the Simulation. It is
 * the interface though which hosts interact with the underlying Simulation.
 * @author paul
 * @param <I> The Id type used to identify hosts nodes
 * @param <H> The type used for Hosts in the Simulation
 */
public abstract class Simulation<I,H> extends Observable {
  public final Graph<H,link<H>> network;
  public final Dimension model_size;
  
  public Simulation (Graph<H,link<H>> g, Dimension d) {
    this.model_size = d;
    this.network = g;
  }
	
  /**
   * @param The node to query the simulation about
   * @return The set of nodes which are connected to the given node.
   */
  abstract public Set<I> connected (I node);
	
  /**
   * Transmit the packet from one node to the other, I.e. link-layer. 
   * This should therefore only be called from the bottom of the host's
   * protocol stack, usually.
   * @param from Id of the sending node
   * @param to  Id of the node to send to
   * @param data Opaque data
   */
  abstract public boolean tx (I from, I to, byte [] data);
  
  /**
   * Map from the H-typed Host object from which the network Graph is made
   * to the I-typed IDs the Simulation generally uses for nodes.
   * 
   * Using these methods is a layering violation and generally discouraged. 
   * However there may be certain, very special cases where this is
   * justified.
   *
   * @param node The node object to retrieve the I typed Id for
   * @return The Id of the given node
	 */
   abstract public I node2id(H node);
   /**
	 * retrieve the appropriate Host for the given ID
   * @param node the node Id to lookup
   * @return the Host object for the given Id
	 */
   abstract public H id2node (I node);
}
