package agarnet;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.Dimension;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Set;

import javax.swing.JFrame;

import org.nongnu.multigraph.*;

import org.nongnu.multigraph.layout.*;
import org.nongnu.multigraph.metrics.*;
import org.nongnu.multigraph.rewire.*;

import agarnet.anipanel.Node;
import agarnet.link.*;
import agarnet.perturb.RandomMove;
import agarnet.protocols.*;
import agarnet.protocols.peer.leech;
import agarnet.protocols.peer.peer;
import agarnet.protocols.peer.seed;
import agarnet.variables.*;
import agarnet.variables.atoms.*;


public class simapp extends AbstractLongSim<simhost> implements Observer {  
  private Random r = new Random ();
  private anipanel<Long, simhost> ap;
  private RandomMove<simhost,link<simhost>> moverewire = null;
  
  private link<simhost> _gen_link (simhost from, simhost to,
                                int maxbandwidth, int maxlatency) {
    unilink<simhost> ul1, ul2;
    ul1 = new unilink<simhost> (from, 1 + r.nextInt (maxbandwidth),
                                         1 + r.nextInt (maxlatency));
    ul2 = new unilink<simhost> (to, ul1.bandwidth, ul1.latency);
    return new link<simhost> (ul1, ul2);
  }
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
  
  private simhost get_host (anipanel.Node type, boolean movable,
                            protocol<Long> [] protos) {
    simhost s = new simhost (this, type, movable, protos.clone ());
    s.setId (idmap.get (s));
    return s;
  }
  
  public simapp (Dimension d) {
    super (d);
    
    /* create a network */
    for (int i = 0; i < conf_peers.get (); i++) {
      simhost p; 
          
      if (conf_leeches.get () < 1
          && r.nextFloat () <= conf_leeches.get ())
    	p = get_host (anipanel.Node.leech, true, new_protstack_leech ());
      else
        p = get_host (anipanel.Node.peer, true, new_protstack_peer ());
          
      network.add (p);
    }
      
    if (conf_leeches.get () >= 1) {
      for (int i = 0; i < conf_leeches.get (); i++) {
        simhost p = get_host (anipanel.Node.leech, true, new_protstack_leech ());
        network.add (p);
      }
    }
      
    network.add (get_host (anipanel.Node.seed, false, new_protstack_seed ()));
  }
  
  final static IntConfigOption conf_peers, conf_runs,
                               conf_sleep, conf_period;
  final static DimensionConfigOption conf_model_size;
  final static BooleanConfigOption conf_debug, conf_gui;
  final static LayoutConfigOption conf_layout;
  final static NumberProbabilityConfigOption conf_leeches;
  final static TopologyConfigOption conf_topology;
  final static SuboptConfigOption conf_seeds, conf_perturb;

  //final static SuboptConfigOption conf_topology;
  
  final static ConfigurableOption [] confvars = new ConfigurableOption [] {
    
    /* integer variables */
    conf_peers = new IntConfigOption (
        "peers", 'p', "<number>",
        "number of peers to create",
        LongOpt.REQUIRED_ARGUMENT, 1, Integer.MAX_VALUE)
        .set (10),
    
    conf_leeches = new NumberProbabilityConfigOption (
        "leeches", 'L', "<number|probability>",
        "number of leeches to create / probability of node being a leech",
        LongOpt.REQUIRED_ARGUMENT)
        .parse ("5%"),
    
    conf_seeds = new SuboptConfigOption (
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
              
        }}),
    conf_perturb = new SuboptConfigOption (
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
        }}),
    conf_period = new IntConfigOption (
        "period", 'P', "<number>",
        "Period for repeating behaviour of certain objects, like seeds",
        LongOpt.REQUIRED_ARGUMENT, 0, Integer.MAX_VALUE)
        .set (5),
    
    conf_runs = new IntConfigOption ("runs", 'r', "<runs>",
        "# of simulation runs to make."
            + " Edges are rewired, nodes reset.",
        LongOpt.REQUIRED_ARGUMENT, 0, Integer.MAX_VALUE)
        .set (1),
    
    conf_sleep = new IntConfigOption (
        "sleep", 's', "<sleep>",
        "period or amount of time to sleep between ticks of the simulation",
        LongOpt.REQUIRED_ARGUMENT, 0, Integer.MAX_VALUE)
        .set (1000),
    
    /* Dimensions */
    conf_model_size = new DimensionConfigOption (
        "model-size", 'M', "<width>x<height>",
        "width and height of the simulation model",
        LongOpt.REQUIRED_ARGUMENT).set (new Dimension (1200, 800)),
    
    /* Eclipse insists the following cast is required.. Absolutely bizarre 
     * The standard javac compiler is fine without the cast.
     * Bizarre bug in the internal java compiler of Eclipse it seems.
     */
    conf_topology = (TopologyConfigOption) new TopologyConfigOption (
        "topology", 't', "<topology>[,<topology specific options>]",
        "topology of graph, i.e. how nodes are linked together",
        LongOpt.REQUIRED_ARGUMENT,
        new ConfigOptionSet () {{
          put ("Cartesian", new ObjectVar [] {
              new BooleanVar (
                "Cartesian", "Nodes linked to those other nodes which are" +
                             " within range."),
              new FloatVar ("range",
                 "distance within which nodes are in range of each other")
                .set (200),
          });
          
          put ("Lattice", new ObjectVar [] {
              new BooleanVar (
                "Lattice", "Lattice / grid topology"),
          });
          
          put ("Random", new ObjectVar [] {
              new BooleanVar (
                "Random", "Randomly connected topology"),
              new IntVar ("mindegree", "Minimum out degree for nodes",
                          0, Integer.MAX_VALUE).set (3),
          });
          
          put ("ScaleFree", new ObjectVar [] {
              new BooleanVar (
                "ScaleFree", "Barabas/Albert scale-free model topology")
          });
        }}).parse ("ScaleFree"),
    conf_layout = new LayoutConfigOption (
        "layout", 'l', "<layout>[,<layout specific options>]",
        "algorithm to use to layout the graph for visualisation",
        LongOpt.REQUIRED_ARGUMENT,
        new ConfigOptionSet () {{
          put (new IntVar (
              "maxiterations",
              "restrict algorithm to given max. iterations",
              1, Integer.MAX_VALUE)
              .set (120));
          put ("Force", new ObjectVar [] {
              new BooleanVar (
                  "Force", "Fruchterman-Rheingold force-directed layout"),
            	new FloatVar (
            	    "C", "scalar applied to the k constant of the algorithm"),
            	new FloatVar (
            	    "decay", "decay constant to apply to energy"),
            	new FloatVar (
            	    "mintemp","Minimum temperature algorithm can decay to",
            	    0, Float.MAX_VALUE),
              new FloatVar (
                  "minkve","Stop algorithm when kinetic energy goes below " +
                  		      "this value",
                   0, Float.MAX_VALUE),
              new FloatVar (
                  "jiggle","Scalar for a noise factor to add to algorithm",
                   0, Float.MAX_VALUE)
            });
            
            put ("Null", new ObjectVar [] {
              new BooleanVar ("Null", "Null layout"),
            });
            
            put ("Radial", new ObjectVar [] {
              new BooleanVar ("Radial", "Lay nodes out radially"),
            });
            
            put ("Random", new ObjectVar [] {
             new BooleanVar (
                "Random","randomly placed within the model dimension"),
            });
            
        }}).parse ("Force"),
         
    /* booleans */
    conf_debug = new BooleanConfigOption (
        "debug", 'd',
        "enable extensive debug print outs").set (false),
    
    conf_gui = new BooleanConfigOption (
        "gui", 'g',
        "enable the GUI/visualisation").set (false),
  };
  
  
  static void usage (String s) {
    if (s != null)
      System.out.println ("Error: " + s);
    
    String indent = "    ";
    StringBuilder sb = new StringBuilder ();
    sb.append ("Usage: java simapp\n");
    for (ConfigurableOption cv : confvars) {
      sb.append (indent);
      sb.append ("[-"); sb.append ((char) cv.lopt.getVal ());
      sb.append ("|--"); sb.append (cv.lopt.getName ());
      sb.append (" "); sb.append (cv.arg_desc);
      sb.append ("]\n");
    }
    sb.append (
        "\nProbability is specified either as a number, 0 <= x < 1, or as x%");
    sb.append ("\n");
    for (ConfigurableOption cv : confvars) {
      sb.append (indent); sb.append (cv.lopt.getName ());
      sb.append (": "); sb.append (cv.help);
      sb.append ("\n");
      
      if (cv.subopts != null) {
        boolean first = true;
        for (String branchkey : cv.subopts.branch_keys ()) {
          if (first) {
            sb.append (indent);
            sb.append ("  "); sb.append (cv.lopt.getName ());
            sb.append (" sub-options:\n");
          }
          
          sb.append (indent);
          sb.append ("   ");
          sb.append (branchkey);
          sb.append (": ");
          sb.append (cv.subopts.get (branchkey, branchkey).getDesc ());
          sb.append ("\n");
          
          for (String suboptkey : cv.subopts.subopt_keys (branchkey)) {
            ObjectVar ov = cv.subopts.get (branchkey, suboptkey);
            
            if (suboptkey.equals (branchkey))
              continue;
            
            sb.append (indent);
            sb.append ("     ");
            
            sb.append (ov.getName ());
            sb.append (": ");
            sb.append (ov.getDesc ());
            sb.append ("\n");
          }
          first = false;
        }

        for (String subopt : cv.subopts.subopt_keys ()) {
          ObjectVar ov = cv.subopts.get (subopt);
          sb.append (indent);
          sb.append ("  ");
          sb.append (ov.getName ());
          sb.append (": ");
          sb.append (ov.getDesc ());
          sb.append ("\n");
        }
      }
      sb.append ("\n");
    }
    System.out.println (sb);
    System.exit (1);
  }
  
  private static void dump_arg_state () {
    if (debug.applies ()) {
      for (ConfigurableOption cv : confvars) {
        debug.println (cv.toString ());
        
        if (cv.subopts != null)
          for (String bkey : cv.subopts.branch_keys ()) {
            //debug.println (cv.subopts[i].toString ());
            
            for (String subkey : cv.subopts.subopt_keys (bkey)) {
              ObjectVar ov = cv.subopts.get (bkey, subkey);
              
              if (ov.isSet ())
                debug.println (ov.toString ());
              else
                debug.println (ov.getName () + " not set");
            }
          }
      }
    }
  }
  
  public static void main(String args[]) {    
    int c;
    LinkedList<LongOpt> lopts = new LinkedList<LongOpt> ();
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
          debug.level = debug.levels.DEBUG;
          conf_debug.set (true);
          break;
        case 'g':
          conf_gui.set (true);
          break;
        case 't':
          conf_topology.parse (g.getOptarg ());
          break;
        case 'l':
          conf_layout.parse (g.getOptarg ());
          break;
        case 'r':
          conf_runs.parse (g.getOptarg ());
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
                      s.network, s.default_edge_labeler, s.model_size,
                      ((FloatVar) conf_perturb.subopts.get ("speed")).get (),
                      ((IntVar) conf_perturb.subopts.get ("maxrange")).get ());
    }
    
    s.main_loop ();
  }
  
  /* Common edgelabeler for the rewire algorithms */
  private EdgeLabeler<simhost, link<simhost>> default_edge_labeler
    = new EdgeLabeler<simhost, link<simhost>> () {
      public link<simhost> getLabel (simhost from, simhost to) {
            return _gen_link (from, to, 100, 3);
          }
  };
  
  protected void rewire () {
    TopologyConfigOption tconf = conf_topology;
    
    /* Wire up the graph in some fashion */ 
    if (tconf.get ().equals ("Random")) {
      new RandomRewire<simhost, link<simhost>> (network, default_edge_labeler,
                            ((IntVar)tconf.subopts.get ("Random",
                                                        "mindegree")).get ())
                                                        .rewire ();
    } else if (tconf.get ().equals ("ScaleFree")) {
      new ScaleFreeRewire<simhost, link<simhost>> (network,
                                                   default_edge_labeler)
                                                   .rewire ();
    } else if (tconf.get ().equals ("Lattice")) {
      new LatticeRewire<simhost, link<simhost>> (network, default_edge_labeler)
                                                             .rewire ();
    } else if (tconf.get ().equals ("Cartesian")) {
      Layout.factory ("Random", network, model_size, 10).layout (1);
      new CartesianRewire<simhost, link<simhost>> (network,
                                                   default_edge_labeler,
                          ((FloatVar) tconf.subopts.get ("Cartesian",
                                                        "range")).get ())
                                                        .rewire ();
    }
    
    /* set the mass according to the degree, useful for things like
     * Force-Directed Layout.
     */
    for (simhost p : network)
      p.setMass (network.nodal_outdegree (p));
    
    if (conf_gui.get ())
      layout ();
    
    setChanged ();
    
  }

  protected void describe_end () {
    System.out.println ("Disconnected: "
                        + TraversalMetrics.count (network,
                          new TraversalMetrics.node_test<simhost> () {
                            @Override
                            public boolean test (simhost test) {
                              return (test.getSize () == 0);
                          }}));

    System.out.println ("Messages Sent: " + sim_stats.get_messages_sent ());
    System.out.println ("Converged: " + has_converged ());
    System.out.println ("Ticks: " + sim_stats.get_ticks ());
    System.out.println ("Memory: " + Runtime.getRuntime ().totalMemory ()
                        + " " + (Runtime.getRuntime ().totalMemory () >> 20)
                        + " MiB");
    
    /* this obviously must come last - for ease of parsing multiple runs */
    System.out.println ("End stats");
  }
  
  protected void describe_begin () {
    /* NB: Be careful to avoid marshalling strings to pass to debug statements
     * otherwise you may take a good bit of the cost for the debug, even
     * when not enabled.
     * 
     * I.e. let the debug class do the marshalling via format statements.
     */
    debug.printf ("Network: %s\n", network);
    debug.printf ("idmap:\n%s", idmap);
    
    System.out.println ("\nGraph stats");
    System.out.println ("Nodes: " + network.size ());
    System.out.println ("Average degree: " + network.avg_nodal_degree ());
    System.out.println ("Max degree: " + network.max_nodal_degree ());
    System.out.println ("numleeches: " + conf_leeches.get ());
    System.out.println ("Leeches: "
                        + TraversalMetrics.count (network, 
                            new TraversalMetrics.node_test<simhost> () {
                              @Override
                              public boolean test (simhost test) {
                                return (test.get_type () == Node.leech);
                              }
                            }));
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      System.out.println ("Type: perturb");
      System.out.println ("Range: " + 
                          ((IntVar)conf_perturb.subopts.get ("maxrange"))
                                                           .get ());
    } else
      System.out.println ("Type: " + conf_topology.get ());
    System.out.println ("Period: " + conf_period.get ());
    System.out.println ("Distribution: ");
    int count = 0;
    for (int val : TraversalMetrics.degree_distribution (network))
      System.out.println (count++ + ": " + val);
  }
  
  private void layout () {
    int maxiterations = (conf_layout.get ().equals ("Force")
                      ? ((IntVar)conf_layout.subopts
                                    .get ("Force", "maxiterations")).get ()
                      : 10);
    Layout<simhost,link<simhost>> l
      = Layout.factory (conf_layout.get (), network,
                  new Dimension (model_size.width - (int)(2 * ap.noderadius ()),
                                 model_size.height - (int)(2 * ap.noderadius ())),
                                 maxiterations);
    
    if (l instanceof ForceLayout<?,?>) {      
      ForceLayout<simhost, link<simhost>> fl
         = (ForceLayout<simhost, link<simhost>>) l;
      /* If Java had a decent pre-processor I'd use defines for these
       * constants. It's not worth maintaining an enum for, and its
       * not worth further complexity.
       * 
       * A Map would be ideal, but you can't use initialisers with
       * maps. sigh.
       */
      for (String bkey : conf_layout.subopts.subopt_keys ("Force")) {
        FloatVar fval;
        ObjectVar v = conf_layout.subopts.get ("Force", bkey);
        
        if (!(v instanceof FloatVar))
          continue;
        
        if (!v.isSet ())
          continue;
        
        fval = (FloatVar) v;
        
        if (bkey.equals ("C"))
          fl.setC (fval.get ());
        if (bkey.equals ("mintemp"))
          fl.setMintemp (fval.get ());
        if (bkey.equals ("minkve"))
          fl.setMinkve (fval.get ());
        if (bkey.equals ("jiggle"))
          fl.setJiggle (fval.get ());
      }
    }
    
    /* graph layout */
    while (l.layout (1)) {
      setChanged ();
      notifyObservers ();
      
      try {
        Thread.sleep (50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
  }
  
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
  protected int get_runs () {
    return conf_runs.get ();
  }
  @Override
  protected int get_period () {
    return conf_period.get ();
  }
  @Override
  protected int get_sleep () {
    return conf_sleep.get ();
  }
  @Override
  protected void perturb () {
    if (!((BooleanVar)conf_perturb.subopts.get ("perturb")).get ())
      return;
   
    moverewire.rewire ();
    layout ();
    setChanged ();
  }
}
