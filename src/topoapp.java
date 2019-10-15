import gnu.getopt.LongOpt;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Observer;

import javax.swing.JFrame;

import org.nongnu.multigraph.*;

import org.nongnu.multigraph.layout.*;

import basicp2psim.simhost;
import basicp2psim.protocols.peer.peer;

import agarnet.anipanel;
import agarnet.framework.AbstractCliApp;
import agarnet.protocols.*;
import agarnet.variables.*;

/**
 * Basic test application to just create topologies and print them out, without
 * running any protocol simulations.
 * @author paul
 *
 */
public class topoapp extends AbstractCliApp<Long, simhost> implements Observer {    
  @SuppressWarnings("unchecked")
  private protocol<Long> [] new_protstack_peer () {
    return new protocol [] {
      //new transport_protocol<Long, simhost, link<simhost>> (this, network),
      new peer<Long,simhost> (this),
    };
  }
  
  
  private simhost get_host (Long id, simhost.Node type, boolean movable,
                            protocol<Long> [] protos) {
    simhost s;
    if ((s = id2node (id)) != null)
      return s;
    s = new simhost (this, type, movable, protos.clone ());
    s.setId (id);
    new_node (id, s);
    return s;
  }
  /* default get_host */
  protected simhost get_host (Long id) {
    return get_host (id, simhost.Node.peer, true, new_protstack_peer ());
  }
  
  @Override
  public simhost str2node (String sl) {
    long l;
    try {
      l = Long.parseLong (sl);
    } catch (NumberFormatException e) {
      debug.println (debug.levels.WARNING, "Invalid host identifier: " + sl);
      return null;
    }
    return get_host (l);
  }
  
  public topoapp (Dimension d) {
    super (d);
  }
  
  protected void add_initial_hosts () {
    
    /* create a network */
    for (int i = 0; i < conf_nodes.get (); i++) {      
      network.add (get_host ((long) i));
    }
  }
  
  /* topo simulation specific variables */
  final static IntConfigOption conf_nodes
    = new IntConfigOption (
        "nodes", 'n', "<number>",
        "number of nodes to create",
        LongOpt.REQUIRED_ARGUMENT, 1, Integer.MAX_VALUE)
      .set (10);
  
  public static void main(String args[]) {    
    int c;
    /* Add topoapp specific config-vars */
    confvars.addAll (new ArrayList<ConfigurableOption> (Arrays.asList (
                                                                  conf_nodes,
                                                                  conf_adjmatrix)));
    
    if ((c = ConfigurableOption.getopts ("topoapp", args, confvars)) != 0)
          usage ("Unknown argument: " + (char) c);
    
    dump_arg_state ();
    
    topoapp s = new topoapp (conf_model_size.get ());
        
    JFrame jf = null;
    if (conf_gui.get ()) {
      s.ap = new anipanel<Long,simhost> (s);
      jf = new JFrame ();
      jf.add (s.ap);
      jf.pack ();
      jf.setTitle ("Topology");
      jf.setDefaultCloseOperation (javax.swing.WindowConstants.EXIT_ON_CLOSE);
      jf.setSize (1000, 1000 * s.model_size.height/s.model_size.width);
      jf.setVisible (true);
    }
    
    /* Use random as initial layout */
    Layout.factory ("Random", s.network, s.model_size, 10).layout (1);
    s.main_loop ();
  }
  
  protected void run () {
    rewire ();
  }
  
  @Override
  protected void describe_end (int runs) {
    super.describe_end (runs);
    if (conf_adjmatrix.get ()) {
      System.out.println ("Adjacency matrix:");
      AdjacencyMatrix.sparse (System.out, network);
    }
  }


  @Override
  protected boolean has_converged () {
    float f = 0;
    
    for (simhost h : network) {
      f = h.getSize ();
      break;
    }
    
    for (simhost p : network) {
      if (p.getSize () != f)
        return false;
    }
    return true;
  }

  @Override
  protected void perturb () {
    return;
  }
}
