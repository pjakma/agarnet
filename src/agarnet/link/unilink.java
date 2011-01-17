package agarnet.link;

import java.util.LinkedList;
import java.util.Queue;

import org.nongnu.multigraph.debug;

import agarnet.framework.abstract_tickable;
import agarnet.framework.resetable;

/**
 * This class models a unidirectional, communications link, with
 * the following configurable characteristics:
 * 
 * - Latency: The amount of time, in ticks, it takes for data that is 
 *            transmitted on the link to be received by the other end of the
 *            link.
 * - Bandwidth: The amount of data, in bytes, that can be transmitted in any given
 *              time quanta (tick).
 * - Buffer size: The total amount of data that can be queued to await transmission. 
 *                 Most use-cases will not try to restrict how much can be buffered.
 *
 * Note that the bandwidth is also effectively an MTU, and users should not submit
 * data blocks larger than this value, as the link does not as present account for
 * over-sized blocks.
 */
public class unilink<I> extends abstract_tickable
                        implements resetable {
  /**
   * An identifier for this link
   */
  final I id;
  
  /* The buffering capacity of this link, in bytes */
  public final int capacity;
  public final int bandwidth;
  /* The latency of this link in ticks */
  public final int latency;
  
  private Queue<byte []> txbuf = new LinkedList<byte []> ();
  private Queue<byte []> rxbuf = new LinkedList<byte []> ();
  /* made available directly to link for performance reasons */
  int buffered = 0;
  /* to emulate an infinite MTU */
  int waitforbytes =  -1;
  
  /* we want to maintain a queue as a fixed-size circular buffer, to simulate 
   * the latency of the link.
   * 
   * this lets us 'tick' the qhead around the circle to simulate the passage of
   * time.
   */
  private Queue<byte []> [] linkq;
  private int qhead = 0;
  
  @SuppressWarnings ("unchecked")
  private void _init_linkq () {
    linkq = (Queue<byte []> []) new LinkedList [latency];
    
    for (int i = 0; i < linkq.length; i++)
      linkq[i] = new LinkedList<byte []> ();
  }
  
  private void _sanity_check_args (Object id, int bandwidth, int latency,
                                   int capacity) {
    if (id == null)
      throw new IllegalArgumentException ("Link ID must not be null");
    if (bandwidth < 1)
      throw new IllegalArgumentException ("Bandwidth must be > 0");
    if (latency < 1)
      throw new IllegalArgumentException ("Latency must be > 0");
    if (capacity < 1)
      throw new IllegalArgumentException ("Capacity must be > 0");
  }
  
  public unilink (I id, int bandwidth, int latency, int capacity) {
    _sanity_check_args (id, bandwidth, latency, capacity);
    
    this.id = id;
    this.capacity = capacity;
    this.latency = latency;
    this.bandwidth = bandwidth;
    _init_linkq ();
    _sanity_check_buffered ();
  }
  public unilink (I id, int bandwidth, int latency) {
    _sanity_check_args (id, bandwidth, latency, Integer.MAX_VALUE);
    
    this.id = id;
    this.capacity = Integer.MAX_VALUE;
    this.latency = latency;
    this.bandwidth = bandwidth;
    _init_linkq ();
  }
  

  private int _bytes_in_queue (Queue<byte []> q) {
    int bytes = 0;
    for (byte [] ba : q)
      bytes += ba.length;
    return bytes;
  }
  
  private int _bytes_in_linkq () {
    int linkqsize = 0;
    for (Queue<byte []> q : linkq)
      linkqsize += _bytes_in_queue (q);
    return linkqsize;
  }
  
  private void _sanity_check_buffered () {
    if (!debug.applies ())
      return;
    
    int linkqsize = _bytes_in_linkq ();
    int rxbytes = _bytes_in_queue (rxbuf),
        txbytes = _bytes_in_queue (txbuf);
    
    if (linkqsize + rxbytes + txbytes != buffered)
      throw new AssertionError ("buffered doesn't add up: "
                                    + linkqsize + " + "
                                    + txbuf + " + "
                                    + rxbuf + " != "
                                    + buffered);
  }
  
  /*** Queue methods ***
   * 
   * NB, we don't implement the Queue i'face cause it drags in all the
   * Collections interface methods. 
   */
  public boolean offer (byte [] data) {
    if (data.length + buffered > capacity)
      return false;
    
    _sanity_check_buffered ();
    
    /* XXX: need to ensure data is <= bandwidth for a tick and we need
     * ability to split it up if too big.
     */
    if (txbuf.offer (data)) {
      buffered += data.length;
      debug.printf ("%s, enq. msg %s, len %d (buf: %d)\n",
                   this, data, data.length, buffered);
      _sanity_check_buffered ();
      return true;
    }
    _sanity_check_buffered ();
    return false;
  }
  
  public byte [] peek () {
    return rxbuf.peek ();
  }
  
  public byte [] poll () {
    _sanity_check_buffered ();
    
    byte [] data = rxbuf.poll ();
    
    if (data != null) {
      buffered -= data.length;
      debug.printf ("%s, deq. msg %s, len %d (buf: %d)\n",
                    this, data, data.length, buffered);
    }
    
    _sanity_check_buffered ();
    
    return data;
  }
  
  @Override
  public void reset () {
    _sanity_check_buffered ();
    txbuf.clear ();
    rxbuf.clear ();
    buffered = 0;
    _sanity_check_buffered ();
  }
  
  public int size () {
    _sanity_check_buffered ();
    return buffered;
  }
  
  private void linkq_to_rx () {
    Queue<byte []> l;
    
    _sanity_check_buffered ();
    
    /* dequeue from the latency linkq onto the rxbuf */
    if ((l = linkq[qhead]) != null) {
      byte [] data;
      while ((data = l.peek ()) != null)
        if (rxbuf.offer (data))
          l.poll ();
    }
    _sanity_check_buffered ();
  }
  
  private void tx_to_linkq () {
    _sanity_check_buffered ();
    
    byte [] data = txbuf.poll ();
    Queue<byte []> l = linkq[qhead];
    int bytes = 0;
    
    if (data == null)
      return;
    
    /* linkq_to_rx should have cleared previously used lists */
    assert (l.size () == 0);
    
    /* We can always move at least one data object onto the linkq */
    bytes += data.length;
    
    l.add (data);
    linkq[qhead] = l;
    
    _sanity_check_buffered ();
    
    /* Now see if we can fit in more, without losing anything */
    while ((data = txbuf.peek ()) != null
           && bytes + data.length <= bandwidth) {
      l.add (data);
      bytes += data.length;
      txbuf.poll ();
    }
    
    _sanity_check_buffered ();
  }
  
  public void tick () {
    _sanity_check_buffered ();
    
    linkq_to_rx ();    
    tx_to_linkq ();
    
    qhead++;
    qhead %= linkq.length;
    
    _sanity_check_buffered ();
  }
  
  public String toString () {
    _sanity_check_buffered ();
    return "ul: " + id.toString () + " bw/lat/buf: " + bandwidth + "/" + latency
           + "/" + buffered;
  }
}
