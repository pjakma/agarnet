package basicp2psim.protocols.peer;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.nongnu.multigraph.debug;

import basicp2psim.protocols.peer.data.file;

import agarnet.framework.Simulation;
import agarnet.protocols.AbstractProtocol;
import agarnet.protocols.protocol;

public class peer<I,N> extends AbstractProtocol<I>
                       implements PeerTemplate<I> {  
  /* track what's still to be sent to each neighbor */
  //Map<I, Queue<file>> neightxl = new HashMap<I, Queue<file>> ();
  /* Track what we know the neighbour must have in its DB */
  Map<I, Set<file>> neighdb = new HashMap<I, Set<file>> ();
  
  /* DB of received, distinct messages */
  Set<file> msgdb = new HashSet<file> ();
  /* for connected hosts */
  Simulation<I,N> sim;
  /* currently connected hosts */
  Set<I> connected;
  
  public peer (Simulation<I,N> sim) {
    this.sim = sim;
  }
  
  @Override
  public void reset () {
    super.reset ();
    msgdb.clear ();
    neighdb.clear ();
    connected = null;
  }

  public boolean should_accept (I from, file msg) {
    return !msgdb.contains (msg);
  }

  public boolean should_send (I to, file msg) {
    Set<file> db = neighdb.get (to);
    
    return (db == null || !db.contains (msg));
  }

  public boolean should_store (file msg) {
    return true;
  }
  
  @Override
  public void insert (protocol<I> above, protocol<I> below) {
    if (above != null)
      throw new UnsupportedOperationException (
      "application protocol doesn't accept messsages from above");
    
    super.insert (null, below);
  }

  /**
   * Receive message from peer
   * @param from Peer message was sent from
   * @param msg The message received
   */
  public void up (I from, byte [] data) {
    debug.printf ("peer %s: receive msg: %s from %s\n", this, data, from);
    file f = null;
    
    try {
      f = file.deserialise (data);
    } catch (Exception e) {
      debug.println ("Unhandled message: " + e);
      return;
    }

    stats_inc (stat.recvd);
    register_has_msg (from, f);
    
    if (!should_accept (from, f))
      return;
    
    if (should_store (f)) {
      msgdb.add (f);
      setChanged ();
      debug.printf ("peer %s: storing %s (from %s), db size %d\n",
                    this, f, from, msgdb.size ());
    }
    
    /* We've accepted a message, now flood it on to all other peers */
    for (I to : sim.connected (selfId))
      if (!to.equals (from) && should_send (to, f)) {
        debug.printf ("peer %s: forward %s from %s to %s\n",
                      this, f, from, to);
        send (to, data);
      }
    
    /* Acknowledge receipt to the sender by flooding back */
    send (from, data);
  }
  
  @Override
  public long stat_get (int ordinal) {
    if (ordinal == stat.stored.ordinal ())
      return msgdb.size ();
    return super.stat_get (ordinal);
  }
  
  @Override
  public boolean has_stored (file msg) {
    return msgdb.contains (msg);
  }
  
  protected void send (file msg) {
    if (should_store (msg))
      msgdb.add (msg);
    
    for (I host : sim.connected (selfId)) {
      debug.printf ("peer %s: consider sending %s to %s\n",
                    this, msg, host);
      if (should_send (host, msg))
        send (host, msg);
    }
  }
  
  private void register_has_msg (I node, file msg) {
    Set<file> db = neighdb.get (node);
    
    if (db == null) {
      db = new HashSet<file> ();
      neighdb.put (node, db);
    }
    
    debug.printf ("peer %s register: %s (%s) for %s\n",
                   this, msg.name, msg, node);
    
    if (!db.contains (msg))
      db.add (msg);
  }
  protected void send (I to, file msg) {    
    debug.printf ("peer %s: send %s to %s\n", this, msg, to);
    super.send (to, msg);
  }
  
  @Override
  public void link_update () {
    Set<I> newconnected = sim.connected (selfId);
    
    /* some kind of link event, resend our DB to every /new/ connected
     * neighbour, except where we're sure the neighbour already has that file.
     */
    if (newconnected != null)
      for (I neigh : newconnected)
        if (connected == null || !connected.contains (neigh))
          for (file msg : msgdb)
            if (should_send (neigh, msg))
              send (neigh, msg);
    connected = newconnected;
  }
}
