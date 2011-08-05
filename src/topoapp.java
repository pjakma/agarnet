import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Observer;

import javax.swing.JFrame;

import org.nongnu.multigraph.*;

import org.nongnu.multigraph.layout.*;

import basicp2psim.simhost;
import basicp2psim.protocols.peer.peer;

import agarnet.anipanel;
import agarnet.framework.AbstractCliApp;
import agarnet.link.*;
import agarnet.protocols.*;
import agarnet.variables.*;
import agarnet.variables.atoms.*;

/**
 * Basic test application to just create topologies and print them out, without
 * running any protocol simulations.
 * @author paul
 *
 */
public class topoapp extends AbstractCliApp<simhost> implements Observer {    
  @SuppressWarnings("unchecked")
  private protocol<Long> [] new_protstack_peer () {
    return new protocol [] {
      //new transport_protocol<Long, simhost, link<simhost>> (this, network),
      new peer<Long,simhost> (this),
    };
  }
  
  
  private simhost get_host (simhost.Node type, boolean movable,
                            protocol<Long> [] protos) {
    simhost s = new simhost (this, type, movable, protos.clone ());
    s.setId (idmap.get (s));
    return s;
  }
  /* default get_host */
  protected simhost get_host () {
    return get_host (simhost.Node.peer, true, new_protstack_peer ());
  }
  
  public topoapp (Dimension d) {
    super (d);
  }
  
  protected void add_initial_hosts () {
    
    /* create a network */
    for (int i = 0; i < conf_nodes.get (); i++) {
      simhost p; 
          
      p = get_host (simhost.Node.peer, true, new_protstack_peer ());
          
      network.add (p);
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
    LinkedList<LongOpt> lopts = new LinkedList<LongOpt> ();
    
    /* Add P2P sim specific config-vars */
    confvars.addAll (new ArrayList<ConfigurableOption> (Arrays.asList (
                            conf_nodes, conf_adjmatrix)));
    
    for (ConfigurableOption cv : confvars)
      lopts.add (cv.lopt);
    
    debug.println ("str: " + ConfigurableOption.get_optstring (confvars));
    Getopt g = new Getopt("simapp", args,
                          ConfigurableOption.get_optstring (confvars),
                          lopts.toArray (new LongOpt [0]));
    
    while ((c = g.getopt ()) != -1) {
      switch (c) {
        case '?':
          usage ("Unknown argument: " + (char)g.getOptopt ());
          break;
        case 'a':
          conf_adjmatrix.set (true);
          break;
        case 'n':
          conf_nodes.parse (g.getOptarg ());
          break;
        case 'P':
          conf_period.parse (g.getOptarg ());
          break;
        case 'd':
          BooleanVar db
          = ((BooleanVar)conf_debug.subopts.get ("debug"));
          db.set (true);
          conf_debug.parse (g.getOptarg ());
          break;
        case 'D':
          conf_degrees.set (true);
          break;
        case 'g':
          conf_gui.set (true);
          break;
        case 't':
          conf_topology.parse (g.getOptarg ());
          break;
        case 'K':
          conf_kshell_stats.set (true);
          break;
        case 'l':
          conf_layout.parse (g.getOptarg ());
          break;
        case 'T':
          conf_path_stats.set (true);
          break;
        case 'R':
          conf_random_tick.set (true);
          break;
        case 's':
          conf_sleep.parse (g.getOptarg ());
          break;
        case 'M':
          conf_model_size.parse (g.getOptarg ());
          break;
      }
    }
    
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
    s.add_initial_hosts ();
    Layout.factory ("Random", s.network, s.model_size, 10).layout (1);
    
    long start = System.currentTimeMillis ();
    s.rewire ();
    long fin = System.currentTimeMillis ();
    
    s.describe_begin ();
    System.out.printf ("Runtime: %.3f seconds\n", (float)(fin - start) / 1000);
    s.describe_end ();
    if (conf_adjmatrix.get ()) {
      System.out.println ("Adjacency matrix:");
      AdjacencyMatrix.sparse (System.out, s.network);
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
