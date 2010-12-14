package agarnet;

import java.awt.Dimension;
import java.util.Observable;
import java.util.Set;
import org.nongnu.multigraph.*;

import agarnet.link.link;

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
	 * retrieve the appropriate node for the given ID
	 */
	/**
	 * Transmit the packet from one node to the other, I.e. link-layer. 
	 * This should therefore only be called from the bottom of the host's
	 * protocol stack, usually.
	 * @param
	 */
	abstract public boolean tx (I from, I to, byte [] data);
	
	/**
	 * Map from the N-typed Node object from which the network Graph is made
	 * to the I-typed IDs the Simulation generally uses for nodes.
	 * 
	 * Using these methods is a layering violation and generally discouraged. 
	 * However there may be certain, very special cases where this is justified.
	 */
	abstract public H id2node (I node);
	abstract public I node2id(H node);
}
