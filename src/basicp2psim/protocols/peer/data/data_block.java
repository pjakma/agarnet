package basicp2psim.protocols.peer.data;

/*
 * This represents an anonymous block of data.
 * 
 * Blocks are tied into files by way block_descriptors. This class is fairly
 * insubstantial, it just holds data - but it must be separate from the
 * descriptor as the descriptor will always be small and the block potentially
 * large. a P2P system likely would need to be able to transfer information
 * describing blocks without actually having to transfer the data.
 */
public class data_block {
  final byte [] data;
  final block_descriptor bd;
  
  public data_block (block_descriptor bd, byte [] data) {
    this.data = data;
    this.bd = bd;
  }
  
  public int size () {
    return data.length;
  }
}
