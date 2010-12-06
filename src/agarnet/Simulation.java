package agarnet;

import java.util.Set;

public interface Simulation<I,N> {
	/**
	 * @param The node to query the simulation about
	 * @return The set of nodes which are connected to the given node.
	 */
	public Set<I> connected (I node);
	
	/**
	 * retrieve the appropriate node for the given ID
	 */
	/**
	 * Transmit the packet from one node to the other, I.e. link-layer. 
	 * This should therefore only be called from the bottom of the host's
	 * protocol stack, usually.
	 * @param
	 */
	public boolean tx (I from, I to, byte [] data);
	
	/**
	 * Map from the N-typed Node object from which the network Graph is made
	 * to the I-typed IDs the Simulation generally uses for nodes.
	 * 
	 * Using these methods is a layering violation and generally discouraged. 
	 * However there may be certain, very special cases where this is justified.
	 */
	public N id2node (I node);
	public I node2id(N node);
}
