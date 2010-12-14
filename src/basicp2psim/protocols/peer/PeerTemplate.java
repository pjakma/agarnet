package basicp2psim.protocols.peer;

import basicp2psim.protocols.peer.data.file;
import agarnet.framework.resetable;

public interface PeerTemplate<N> extends resetable {
  
  /**
   * Whether the message received from the given node should be accepted.
   * @param from The node the message was received from.
   * @param msg The message received
   * @return True if the message should be processed further,
   *         false if it should be dropped and no further action taken
   *         on this mesage.
   */
  boolean should_accept (N from, file msg);
  
  /**
   * Whether the message proposed to be sent to the given node should
   * be transmitted or not.
   * @param to The node being considered
   * @param msg The message being sent
   * @return True if the message should be sent to the peer, false if
   *         it should be dropped.
   */
  boolean should_send (N to, file msg);
  
  /**
   * Whether the message should be stored in the local database of messages.
   * @param msg The message received
   * @return True if the message should be retained in the local store of messages,
   *         false if it should be ignored.
   */
  boolean should_store (file msg);
  
  /**
   * Whether the node has the given message
   * @param msg The message to query the peer for
   * @return True if the peer has the given message stored.
   */
  boolean has_stored (file msg);
  
  //voMessagid send (file msg);
  //void send (N to, file msg);
}
