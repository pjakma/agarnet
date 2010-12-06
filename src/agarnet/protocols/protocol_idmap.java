package agarnet.protocols;

/**
 * A protocol which also remaps lower-level identifiers to another system
 * of identifiers. (e.g. like mapping hostnames to IP addresses, or mapping
 * IP addresses to/from MAC addresses).
 * 
 * @author paul
 * @param N the type of the host identifiers for the internet to lower layers
 * @param U the type of the host identifiers presented to higher layers
 */
public interface protocol_idmap<N,U> extends protocol<N> {
  void down (U dst, byte [] data);
}
