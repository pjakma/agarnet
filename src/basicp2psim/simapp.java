package basicp2psim;

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
import org.nongnu.multigraph.metrics.*;

import basicp2psim.protocols.peer.leech;
import basicp2psim.protocols.peer.peer;
import basicp2psim.protocols.peer.seed;


import agarnet.anipanel;
import agarnet.framework.AbstractCliApp;
import agarnet.link.*;
import agarnet.perturb.RandomMove;
import agarnet.protocols.*;
import agarnet.variables.*;
import agarnet.variables.atoms.*;


public class simapp extends AbstractCliApp<Long, simhost> implements Observer {  
  private RandomMove<simhost,link<simhost>> moverewire = null;
  
  /* Java's handling of generic arrays seems less than useful. Rather than
   * restricting up-conversions (i.e. casting foo<specific_type> [] to 
   * foo<Object> [] - which is what leads to problems) they instead decide
   * to disallow the creation of generic arrays. In doing so they have:
   * 
   * a) Forced everyone who wants a generic array to use casts
   * b) Not fixed the original problem (other than that you now no longer
   *    can blame the compiler for allowing the problem)
   * 
   * bah. Maybe there are other good implementation reasons for it, but
   * the reasoning in the "Generics Tutorial" PDF surely invites a better
   * solution.
   */
  @SuppressWarnings("unchecked")
  private protocol<Long> [] new_protstack_peer () {
    return new protocol [] {
      //new transport_protocol<Long, simhost, link<simhost>> (this, network),
      new peer<Long,simhost> (this),
    };
  }
  @SuppressWarnings("unchecked")
  private protocol<Long> [] new_protstack_leech () {
    return new protocol [] {
      //new transport_protocol<Long, simhost, link<simhost>> (this, network),
      new leech<Long,simhost> (this),
    };
  }
  @SuppressWarnings("unchecked")
  private protocol<Long> [] new_protstack_seed () {
    return new protocol [] {
      //new transport_protocol<Long, simhost, link<simhost>> (this, network),
      new seed<Long,simhost> (this,
           ((NumberVar)conf_seeds.subopts.get ("max")).get ().intValue (),
           ((NumberVar)conf_seeds.subopts.get ("period")).get ().intValue ()),
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
  @Override
  protected simhost get_host (Long id) {
    return get_host (id, simhost.Node.peer, true, new_protstack_peer ());
  }
  
  @Override
  public simhost str2node (String name) {
    long l;
    
    try {
      l = Long.parseLong (name);
    } catch (NumberFormatException e) {
      debug.println (debug.levels.WARNING, "Invalid host identifier: " + name);
      return null;
    }
    return get_host (l);
  }
  
  public simapp (Dimension d) {
    super (d);
  }
  
  @Override
  protected void add_initial_hosts () {
    long num = 1;
    
    /* create a network */
    for (int i = 0; i < conf_peers.get (); i++) {
      simhost p;
      
      if (conf_leeches.get () < 1
          && r.nextFloat () <= conf_leeches.get ())
        p = get_host (num++, simhost.Node.leech, true, new_protstack_leech ());
      else
        p = get_host (num++, simhost.Node.peer, true, new_protstack_peer ());
      
      network.add (p);
    }
      
    if (conf_leeches.get () >= 1) {
      for (int i = 0; i < conf_leeches.get (); i++)
        network.add (get_host (num++, simhost.Node.leech, true, new_protstack_leech ()));
    }
      
    network.add (get_host (num, simhost.Node.seed, false, new_protstack_seed ()));
  }
  
  /* peer2peer simulation specific variables */
  final static IntConfigOption conf_peers
    = new IntConfigOption (
      "peers", 'p', "<number>",
      "number of peers to create",
      LongOpt.REQUIRED_ARGUMENT, 1, Integer.MAX_VALUE)
      .set (10);
  final static NumberProbabilityConfigOption conf_leeches
    = new NumberProbabilityConfigOption (
        "leeches", 'L', "<number|probability>",
        "number of leeches to create / probability of node being a leech",
        LongOpt.REQUIRED_ARGUMENT)
        .parse ("5%");
  final static SuboptConfigOption conf_seeds
  = new SuboptConfigOption (
      "seeds", 'S', "<seed related options>",
      "Seed node options, i.e. nodes which originate data.",
      LongOpt.REQUIRED_ARGUMENT,
      new ConfigOptionSet () {{
        put (new IntVar (
            "max",
            "maximum number of files to seed",
            0, Integer.MAX_VALUE)
            .set (0));
        put (new NumberProbVar (
            "seeds",
            "number of seeds to create / probability of node being a seed")
            .set (1));
        put (new IntVar (
            "period",
            "period, in ticks, at which to originate new files",
            1, Integer.MAX_VALUE)
            .set (3));
            
      }});
  final static SuboptConfigOption conf_perturb
    = new SuboptConfigOption (
        "perturb", 'b', "<simulation perturbation options>",
        "Perturbation of the simulation, e.g. moving nodes.",
        LongOpt.REQUIRED_ARGUMENT,
        new ConfigOptionSet () {{
          put (new IntVar (
              "maxrange",
              "maximum range to rewire to",
              0, Integer.MAX_VALUE)
              .set (100));
          put (new BooleanVar ("perturb", "Move nodes on each iteration"));
          put (new FloatVar ("speed", "Initial speed for nodes"));
        }});
  
  public static void main(String args[]) {    
    int c;
    LinkedList<LongOpt> lopts = new LinkedList<LongOpt> ();
    
    /* Add P2P sim specific config-vars */
    confvars.addAll (new ArrayList<ConfigurableOption> (Arrays.asList (
                            conf_peers, conf_leeches, conf_perturb)));
    
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
        case 'p':
          conf_peers.parse (g.getOptarg ());
          break;
        case 'P':
          conf_period.parse (g.getOptarg ());
          break;
        case 'L':
          conf_leeches.parse (g.getOptarg ());
          break;
        case 'b':
          conf_perturb.parse (g.getOptarg ());
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
        case 'r':
          conf_runs.parse (g.getOptarg ());
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
        case 'S':
          conf_seeds.parse (g.getOptarg ());
          break;
        case 'M':
          conf_model_size.parse (g.getOptarg ());
          break;
      }
    }

    if (conf_help.get ())
      usage (null, 0);  
    dump_arg_state ();
    
    simapp s = new simapp (conf_model_size.get ());
    
    JFrame jf = null;
    if (conf_gui.get ()) {
      s.ap = new anipanel<Long,simhost> (s);
      jf = new JFrame ();
      jf.add (s.ap);
      jf.pack ();
      jf.setTitle ("P2P Simulation/Model");
      jf.setDefaultCloseOperation (javax.swing.WindowConstants.EXIT_ON_CLOSE);
      jf.setSize (1000, 1000 * s.model_size.height/s.model_size.width);
      jf.setVisible (true);
    }
    /* Use random as initial layout */
    Layout.factory ("Random", s.network, s.model_size, 10).layout (1);
    
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      s.moverewire = new RandomMove<simhost,link<simhost>> (
                      s.network, s.default_edge_labeler (), s.model_size,
                      ((FloatVar) conf_perturb.subopts.get ("speed")).get (),
                      ((IntVar) conf_perturb.subopts.get ("maxrange")).get ());
    }
    
    s.main_loop ();
  }
  
  @Override
  protected void describe_begin (int runs) {
    super.describe_begin (runs);
    System.out.println ("numleeches: " + conf_leeches.get ());
    System.out.println ("Leeches: "
                        + TraversalMetrics.count (network, 
                            new TraversalMetrics.node_test<simhost> () {
                              @Override
                              public boolean test (simhost test) {
                                return (test.type == simhost.Node.leech);
                              }
                            }));
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      System.out.println ("Type: perturb");
      System.out.println ("Range: " + 
                          ((IntVar)conf_perturb.subopts.get ("maxrange"))
                                                           .get ());
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
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      moverewire.rewire ();
      setChanged ();
    }
  }
}
