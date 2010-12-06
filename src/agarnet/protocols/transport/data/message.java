package agarnet.protocols.transport.data;

import java.io.Serializable;

public class message<N> implements Serializable {
  private static final long serialVersionUID = -6459204811746335463L;
  private final packet_header<N> header;
  private final byte [] data;
  
  public message (packet_header<N> hdr, byte [] data) {
    if (hdr == null)
      throw new IllegalArgumentException ("header must not be null");
    this.header = hdr;
    this.data = data;
  }
  
  public packet_header<N> getHeader () {
    return header;
  }
  public byte [] getData () {
    return data;
  }
}
