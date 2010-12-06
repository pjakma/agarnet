package agarnet.protocols;

public interface protocol_stats {
  void stats_reset ();
  void stats_reset (stat s);
  
  /* statistics */
  enum stat {
    /**
     * Number of messages sent down by this protocol
     */
    sent,
    /**
     * Number of messages sent down by this protocol
     */
    recvd,
    /**
     * Number of incoming messages dropped by this protocol
     */
    dropped,
    /**
     * An estimate of the state held by this protocol. Exactly what the
     * dimension is of this value is a matter for the protocol.
     */
    stored;
  };
  long stat_get (stat s);
  long stat_get (int ordinal);
}
