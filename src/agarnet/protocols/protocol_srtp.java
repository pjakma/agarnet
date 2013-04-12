package agarnet.protocols;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import org.nongnu.multigraph.debug;

import agarnet.data.marshall;

/**
 * Simple, Reliable, Transport protocol.
 * 
 * A very basic acking protocol, with a rudimentary last-RTT resend
 * window.
 * 
 * @author paul
 *
 * @param <N>
 */
public class protocol_srtp<N extends Serializable> extends AbstractProtocol<N> {
  static public class srtp_msg<N> implements Serializable, Externalizable {
    private static final long serialVersionUID = 3047384440150564207L;
    N src;
    N dst;
    long seq;
    long ack = 0;
    byte [] data = null;
    
    public srtp_msg () { super (); };
    
    public srtp_msg (N src, N dst, byte [] data, long seq) {
      this.src = src;
      this.dst = dst;
      this.seq = seq;
      this.data = data;
    }
    
    @SuppressWarnings ("unchecked")
    @Override
    public void readExternal (ObjectInput oi) throws IOException,
        ClassNotFoundException {
      src = (N) oi.readObject ();
      dst = (N) oi.readObject ();
      seq = oi.readLong ();
      ack = oi.readLong ();
      int len = oi.readInt ();
      if (len > 0) {
        data = new byte[len];
        if (oi.read (data, 0, len) != len)
          throw new IOException ("Unable to read data!");
      }
    }

    @Override
    public void writeExternal (ObjectOutput oo) throws IOException {
      oo.writeObject (src);
      oo.writeObject (dst);
      oo.writeLong (seq);
      oo.writeLong (ack);
      if (data != null) {
        oo.writeInt (data.length);
        oo.write (data, 0, data.length);
      } else
        oo.writeInt (0);
    }
    
    @Override
    public String toString () {
      return String.format ("%4d -> %4d seq %6d ack %6d dlen %6d",
                            src, dst, seq, ack,
                            (data != null ? data.length : 0));
   }
    
    
  }
  
  class neighbour<I> {
    final I id;
    long next_seq;
    long recvd;
    long last_ack_ticked;
    /* last_seq_ticked is >= 0 if we're in RTT measuring mode, and
     * holds the seq we started measuring RTT on. Once we
     * get an ACK for that seq (or higher) we measure the RTT, and
     * set last_seq_ticked back to -1, meaning next send can
     * start another RTT measure.
     */
    long last_seq_ticked;
    long rtt;
    Queue<srtp_msg<N>> tx = new LinkedList<srtp_msg<N>> ();
    Queue<srtp_msg<N>> retx = new LinkedList<srtp_msg<N>> ();
    
    private void reset () {
      /* have to pick some initial window value,
       * arbitrary.
       */
      last_ack_ticked = 4;
      next_seq = recvd = 0;
      last_seq_ticked = -1;
      rtt = 1;
      tx.clear ();
      retx.clear ();
    }
        
    neighbour (I id) { reset (); this.id = id; }
    
    private void measure_rtt (long ticks, srtp_msg<N> m) {
      last_ack_ticked = ticks;
      
      /* measure the RTT when new data is acked, and we're
       * trying to measure RTT
       */
      if (last_seq_ticked == -1) return;
      
      /* ack of data since we set a tick? */
      if (m.ack <= last_seq_ticked) return;
      
      rtt = ticks - last_seq_ticked;
      last_seq_ticked = -1;
    }
    
    boolean recv (srtp_msg<N> m) {
      if (m.seq == recvd && m.data != null) {
        recvd += m.data.length;
        return true;
      }
      return false;
    }
    
    /* cross-off all messages acked from
     * the retx list, by this new message.
     */
    void process_ack (srtp_msg<N> m) {
      srtp_msg<N> retxm;
      while ((retxm = retx.peek ()) != null 
             && (retxm.seq + retxm.data.length) <= m.ack)
        recvd += retx.poll ().data.length;
    }
    
    private srtp_msg<N> prep_send (srtp_msg<N> m) {
      /* go to RTT measure mode if not in, and
       * we're sending a data-bearing packet
       * that remote will have to ack.
       */
      if (last_seq_ticked == -1 && m.data != null)
        last_seq_ticked = m.seq;
      m.ack = recvd + 1;
      return m;
    }
    
    srtp_msg<N> next_to_send () {
      srtp_msg<N> m;
      if ((protocol_srtp.this.ticks - last_ack_ticked > rtt) 
          && (m = retx.peek ()) != null)
        return prep_send (m);
      if ((m = tx.poll ()) != null) {
        retx.add (m);
        return prep_send (m);
      }
      return null;
    }
    
    void enqueue (N src, N dst, byte [] data) {
      debug.printf ("%s: ");
      tx.add (new srtp_msg<N> (src, dst, data, next_seq));
      next_seq += data.length;
    }
    
    boolean has_data () {
      return (tx.size () > 0 || retx.size () > 0);
    }
    
    public String toString () {
      return "SRTP neigh " + id 
                           + ": recvd: " + recvd
                           + " next_seq: " + next_seq
                           + " rtt: " + rtt;
    }
  }
  
  Map<N, neighbour<N>> neighbours = new HashMap<N, neighbour<N>> ();
  private neighbour<N> get_neighbour (N id) {
    neighbour<N> n;
    
    if ((n = neighbours.get (id)) != null)
      return n;
    
    neighbours.put (id, (n = new neighbour<N> (id)));
    
    return n;
  }
  
  private int send (neighbour<N> n) {
    srtp_msg<N> m;
    
    if ((m = n.next_to_send ()) != null) {
      debug.printf ("%s send: %s\n", this.selfId, m);
      send (m.dst, m);
      return 1;
    }
    
    return 0;
  }
  
  private void send_ack (N dst, neighbour<N> n) {
    debug.printf ("%s send ack to %s\n", selfId, dst);
    send (dst, n.prep_send (new srtp_msg<N> (selfId, dst, null, n.next_seq)));
  }
  
  @Override
  public void up (N src, byte [] data) {
    srtp_msg<N> m = null;
    
    try {
      m = marshall.deserialise (m, data);
    } catch (Exception e) {
      debug.printf (debug.levels.ERROR, "Unhandled message! %s\n",
                                         e.getMessage ());
      return;
    }
    
    debug.printf ("%s up: %s\n", selfId, m);
    
    neighbour<N> n = get_neighbour (src);
    
    n.measure_rtt (ticks, m);
    
    n.process_ack (m);
    
    if (send (n) == 0 && m.data != null)
     send_ack (src, n);
    
    if (n.recv (m))
      above.up (src, m.data);
  }

  @Override
  public void down (N dst, byte [] data) {
    neighbour<N> n = get_neighbour (dst);
    
    n.enqueue (this.selfId, dst, data);
  }

  @Override
  public void tick () {
    super.tick ();
    
    for (neighbour<N> n : neighbours.values ())
      if (n.has_data ())
        send (n);
  }

  @Override
  public void reset () {
    super.reset ();
    neighbours.clear ();
  }
}
