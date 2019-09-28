package basicp2psim.protocols.peer.data;

public class block_descriptor {
  public final file f;
  public data_block d;

  public block_descriptor (file f, data_block data) {
   this.f = f;
   this.d = data;
  }
  
  public block_descriptor (file f) {
    this.f = f;
  }
	
  public void set (data_block d) {
    this.d = d;
  }
}
