/* This file is part of 'agarnet'
 *
 * Copyright (C) 2009, 2011, 2013, 2014, 2015, 2019 Paul Jakma
 *
 * agarnet is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3, or (at your option) any
 * later version.
 * 
 * agarnet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.   
 *
 * You should have received a copy of the GNU General Public License
 * along with agarnet.  If not, see <http://www.gnu.org/licenses/>.
 */
package agarnet.framework;

import gnu.getopt.LongOpt;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.nongnu.multigraph.AdjacencyMatrix;
import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.EdgeLabeler;
import org.nongnu.multigraph.NodeLabeler;
import org.nongnu.multigraph.debug;
import org.nongnu.multigraph.layout.*;
import org.nongnu.multigraph.metrics.TraversalMetrics;
import org.nongnu.multigraph.rewire.*;
import org.nongnu.multigraph.structure.kshell;
import org.nongnu.multigraph.structure.kshell_node;

import agarnet.anipanel;
import agarnet.adjacency_list_reader;
import agarnet.as_graph_reader;
import agarnet.link.*;
import agarnet.protocols.host.AnimatableHost;
import agarnet.protocols.protocol_stats.stat;
import agarnet.variables.*;
import agarnet.variables.atoms.*;
import java.util.InputMismatchException;

/**
 * An abstract parent class containing common functionality for a command-line interface
 * simulation interface, with optional GUI visualisation. @see basicp2psim.simap for an
 * example concrete application.
 * 
 * @author paul
 *
 * @param <H> The simulation host type.
 */
public abstract class AbstractCliApp<I extends Serializable,
                                     H extends AnimatableHost<I,H> & kshell_node>
                extends AbstractSim<I, H> {
  /* Force layout can be re-used while the sim runs */
  protected Random r = new Random ();
  protected anipanel<I, H> ap;

  private int gen_rand_in_range_inc (String desc, int max, int min) {
    if (min < 0 || max - min < 0)
      throw new IllegalArgumentException (
                  "Illegal " + desc + " values, min: " + min + ", max: " + max);
    if (max == min)
      return min;
    return min + r.nextInt (max - min + 1);
  }
    
  protected link<H> _gen_link (H from, H to, int bandwidth,
                               int from_latency, int to_latency) {
    unilink<H> ul1, ul2;
    ul1 = new unilink<> (from, bandwidth, from_latency);
    ul2 = new unilink<> (to, bandwidth, to_latency);
    return new link<> (ul1, ul2);
  }
  
  /* Configuration option description & state variables */
  protected static final IntConfigOption conf_period = new IntConfigOption (
      "period", 'P', "<number>",
      "Period for repeating behaviour of certain objects, like seeds",
      LongOpt.REQUIRED_ARGUMENT, 0, Integer.MAX_VALUE)
      .set (2);
  
  protected static final NumberProbabilityConfigOption conf_runs
    = new NumberProbabilityConfigOption ("runs", 'r', "<runs>",
      "# of simulation runs to make. Edges are rewired, nodes reset.\n"
      + "Either as an absolute number of runs of the simulation, otherwise as\n"
      + "a proportion of # of edges if less than 1.",
      LongOpt.REQUIRED_ARGUMENT).set (1);
  
  protected static final IntConfigOption conf_sleep = new IntConfigOption (
      "sleep", 's', "<sleep>",
      "period or amount of time to sleep between ticks of the simulation",
      LongOpt.REQUIRED_ARGUMENT, 0, Integer.MAX_VALUE)
      .set (0);
  
  protected static final DimensionConfigOption conf_model_size = new DimensionConfigOption (
      "model-size", 'M', "<width>x<height>",
      "width and height of the simulation model",
      LongOpt.REQUIRED_ARGUMENT).set (new Dimension (1200, 800));
  
  protected static final TopologyConfigOption conf_topology
    = (TopologyConfigOption) new TopologyConfigOption (
      "topology", 't', "<topology>[,<topology specific options>]",
      "topology of graph, i.e. how nodes are linked together",
      LongOpt.REQUIRED_ARGUMENT,
      /* XXX: Do something better than static init of anon classes */
      new ConfigOptionSet () {{
        put ("AdjMatrix", new StringVar ("AdjMatrix",
                                    "Read in an adjacency-matrix to use"
                                    + " as the topology"
        ));
        put ("ASGraph", new StringVar ("ASGraph",
            "Read in an AS-graph, to use"
            + " as the topology"
        ));
        put ("AdjList", new StringVar ("AdjList",
            "Read in an adjacency-list file, to use as the topology"
        ));
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
              "ScaleFree", "Barabas/Albert scale-free model topology"),
            new StringVar (
              "m_mode", "Mode of the m-parameter," +
                        " 'strict' (default), 'min' or 'max'"),
            new IntVar ("m", "Links to add on each step",
                       1, Integer.MAX_VALUE),
            new IntVar ("a", "Additive bump to new nodes being likely to get linked",
                        0, Integer.MAX_VALUE),
        });
        
        put ("MultiClassScaleFree", new ObjectVar [] {
            new BooleanVar (
              "MultiClassScaleFree", "Barabas/Albert multi-class variant"),
            new StringVar (
              "m_mode", "Mode of the m-parameter," +
                        " 'strict' (default), 'min' or 'max'"),
            new IntVar ("m", "Links to add on each step",
                       1, Integer.MAX_VALUE),
            new IntVar ("a", "Additive bump to new nodes being likely to get linked",
                        0, Integer.MAX_VALUE),
            new IntVar ("p", "Links to add between similar nodes on each step",
                        0, Integer.MAX_VALUE),
        });
      }}).parse ("ScaleFree");
  
  protected static final LayoutConfigOption conf_layout = new LayoutConfigOption (
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
               "massexp", "Mass exponent, to modulate repulsive force by node mass"),
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
          
  }}).parse ("Force");
  
  protected static final DebugConfigOption conf_debug = new DebugConfigOption (
    "debug", 'd', "[<debug sub-options>]",
    "Enable debugging and configure debug-history",
    LongOpt.OPTIONAL_ARGUMENT,
    new ConfigOptionSet () {{
      put (new BooleanVar ("debug", "Enable/Disable debugging"));
      put (new BooleanVar ("invert", "Invert sense of any class/method/msg filter."));
      put (new StringVar (
           "level",
           "Debug level to set, defaults to ALL").set ("DEBUG"));
      put (new StringVar (
           "pushlevel",
           "Ring-buffer messages, only pushing when this level is logged"));
      put (new StringVar (
          "classfilter",
          "Filter log messages by source class, using given regex"));
      put (new StringVar (
          "methodfilter",
          "Filter log messages by source method, using given regex"));
      put (new StringVar (
          "msgfilter",
          "Filter log messages by content, using given regex"));
  }});

  protected static final SuboptConfigOption conf_link
    = new SuboptConfigOption (
      "link", 'L', "<Link sub-options>",
      "Simulation link parameters",
      LongOpt.REQUIRED_ARGUMENT,
      new ConfigOptionSet () {{
        put (new IntVar (
            "minbw",
            "Minimum bandwith of links, in bytes, 0 means infinite.",
            0, Integer.MAX_VALUE)
            .set (0));
        put (new IntVar (
            "maxbw",
            "Maximum bandwith of links, in bytes, 0 means infinite.",
            0, Integer.MAX_VALUE)
            .set (0));
        put (new IntVar (
            "minlat",
            "Minimum latency of links, in simulation ticks",
            1, Integer.MAX_VALUE)
            .set (1));
        put (new IntVar (
            "maxlat",
            "Maximum latency of links, in simulation ticks",
            1, Integer.MAX_VALUE)
            .set (1));
      }});

  /* booleans */
  protected static final BooleanConfigOption conf_help
    = new BooleanConfigOption (
        "help", 'h',
        "Show help").set (false);
  protected static final BooleanConfigOption conf_gui
    = new BooleanConfigOption (
        "gui", 'g',
        "enable the GUI/visualisation").set (false);
  protected static final BooleanConfigOption conf_degrees
  = new BooleanConfigOption (
      "degrees", 'D',
      "print out the final degree distribution of nodes").set (false);
  protected final static BooleanConfigOption conf_random_tick
    = new BooleanConfigOption (
        "randomtick", 'R',
        "tick nodes & edges in randomised order").set (false);
  protected static final BooleanConfigOption conf_path_stats
  = new BooleanConfigOption (
      "path-stats", 'T',
      "Print out stats on paths (expensive)").set (false);
  protected static final BooleanConfigOption conf_kshell_stats
    = new BooleanConfigOption (
      "kshell-stats", 'K',
      "Print out stats on k-shells").set (false);
  protected static final BooleanConfigOption conf_adjmatrix
    = new BooleanConfigOption (
        "adj-matrix", 'a',
        "print adjacency-matrix").set (false);
  protected static final BooleanConfigOption conf_antialias
    = new BooleanConfigOption (
        "anti-alias", 'A',
        "Enable anti-aliased GUI rendering").set (false);
  
  /* List of all the desired configuration options */
  protected static final List<ConfigurableOption> confvars
    = new ArrayList<> (Arrays.asList (
    /* ints */  conf_period, conf_runs, conf_sleep,
    /* bools */ conf_debug, conf_gui, conf_random_tick, conf_degrees,
                conf_path_stats, conf_kshell_stats, conf_help, conf_antialias,
    conf_model_size,
    conf_topology,
    conf_layout,
    conf_link));
  
  protected static void usage (String s) {
    usage (s, 1);
  }
  
  protected static void usage (String s, int exitcode) {
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

    sb.append ("\nSettings:\n");
    for (ConfigurableOption cv : confvars) {
      sb.append (cv.toString ());
      sb.append ("\n");
    }
    sb.append ("\n");
    
    System.out.println (sb);
    System.exit (exitcode);
  }

  protected static void dump_arg_state () {
    if (debug.applies ()) {
      for (ConfigurableOption cv : confvars) {
        debug.println (cv.toString ());
        
        if (cv.subopts != null)
          for (String bkey : cv.subopts.branch_keys ()) {
            
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
  
  /* callback interface to label a new edge */
  private EdgeLabeler<H, link<H>> _default_edge_labeler = null;
  protected EdgeLabeler<H, link<H>> default_edge_labeler () {
    if (_default_edge_labeler == null)
       _default_edge_labeler = new EdgeLabeler<H, link<H>> () {
          final int maxbw  = ((IntVar) conf_link.subopts.get ("maxbw")).get ();
          final int minbw  = ((IntVar) conf_link.subopts.get ("minbw")).get ();
          final int maxlat = ((IntVar) conf_link.subopts.get ("maxlat")).get ();
          final int minlat = ((IntVar) conf_link.subopts.get ("minlat")).get ();
          @Override
          public link<H> getEdge (H from, H to) {
            int bw = gen_rand_in_range_inc ("Bandwidth", maxbw, minbw);
            int lat = gen_rand_in_range_inc ("Latency", maxlat, minlat);
            return _gen_link (from, to, bw, lat, lat);
          }
          @Override
          public link<H> getLabel (H from, H to) {
            return getEdge (from, to);
          }
       };
    return _default_edge_labeler;
  }
  
  public abstract H str2node (String sl);
  
  /* callback to retrieve a host for the given string label */
  private NodeLabeler<H, link<H>> _node_labeller = 
    new NodeLabeler<H, link<H>> () {
      @Override
      public H getNode (String node) {
        return str2node (node);
      }
  };
  protected NodeLabeler<H, link<H>> default_node_labeler () {
    return _node_labeller;
  }

  /* adjacency-list / asgraph reader specific labellers */
  protected class base_reader_labeler
            implements adjacency_list_reader.labeler<H, link<H>> {
    final int maxbw  = ((IntVar) conf_link.subopts.get ("maxbw")).get ();
    final int minbw  = ((IntVar) conf_link.subopts.get ("minbw")).get ();
    final int maxlat = ((IntVar) conf_link.subopts.get ("maxlat")).get ();
    final int minlat = ((IntVar) conf_link.subopts.get ("minlat")).get ();
    final int bw = gen_rand_in_range_inc ("bandwidth", maxbw, minbw);
    final int lat = gen_rand_in_range_inc ("latency", maxlat, minlat);
    
    @Override
    public link<H> getEdge (H from, H to) {
      return _gen_link (from, to, bw, lat, lat);
    }
    @Override
    public link<H> getLabel (H from, H to) {
      return getEdge (from, to);
    }

    @Override
    public H getNode (String node) {
      return str2node (node);
    }
  }

  protected class adjlist_labeler
                  extends base_reader_labeler
                  implements adjacency_list_reader.weight_labeler<H, link<H>> {
    public link<H> getEdge (H from, H to, int weight) {
      Edge<H,link<H>> e = network.edge (from, to);
      
      /* We allow for assymetric latency */
      if (e != null) {
        unilink<H> tul = e.label ().get (to);
        
        return AbstractCliApp.this._gen_link (from, to, tul.bandwidth,
                                              weight,
                                              tul.latency);
      }
      return _gen_link (from, to, bw, weight, weight);
    }
  }
  private adjlist_labeler _default_adjlist_labeler = null;
  protected adjlist_labeler default_adjlist_labeler () {
    if (_default_adjlist_labeler == null)
       _default_adjlist_labeler = new adjlist_labeler ();
    return _default_adjlist_labeler;
  }
  
  protected class asgraph_labeler
                  extends base_reader_labeler
                  implements as_graph_reader.latency_labeler<H, link<H>> {
    /* Convert AS Graph seconds based latency to simulation tick latency */
    private int upscale (double latency) {
      int l = (int) Math.round (latency * 100);
      
      /* result must be >= 1 */
      return l >= 1 ? l : 1;
    }
    
    public link<H> getEdge (H from, H to, double latency) {
      Edge<H,link<H>> e = network.edge (from, to);
      
      /* We allow for assymetric latency */
      if (e != null) {
        unilink<H> tul = e.label ().get (to);
        
        return AbstractCliApp.this._gen_link (from, to, tul.bandwidth,
                                              upscale (latency),
                                              tul.latency);
      }
      return _gen_link (from, to, bw, upscale (latency), upscale (latency));
    }
  }
  private asgraph_labeler _default_asgraph_labeler = null;
  protected asgraph_labeler default_asgraph_labeler () {
    if (_default_asgraph_labeler == null)
       _default_asgraph_labeler = new asgraph_labeler ();
    return _default_asgraph_labeler;
  }

  public AbstractCliApp (Dimension d) {
    super (d);
    setup_debug ();
  }
  
  private Layout<H,link<H>> configured_layout;
  protected void initial_layout () {
    int maxiterations = (conf_layout.get ().equals ("Force")
        ? ((IntVar)conf_layout.subopts
                      .get ("Force", "maxiterations")).get ()
        : 10);
    if (configured_layout == null) {
      configured_layout = Layout.factory (conf_layout.get (), network,
                  new Dimension (model_size.width - (int)(2 * ap.noderadius ()),
                                 model_size.height - (int)(2 * ap.noderadius ())),
                                 maxiterations);
    
      if (configured_layout instanceof ForceLayout<?,?>) {      
        ForceLayout<H, link<H>> fl
           = (ForceLayout<H, link<H>>) configured_layout;
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
          if (bkey.equals ("massexp"))
            fl.setMassExponent (fval.get ());
          if (bkey.equals ("mintemp"))
            fl.setMintemp (fval.get ());
          if (bkey.equals ("minkve"))
            fl.setMinkve (fval.get ());
          if (bkey.equals ("jiggle"))
            fl.setJiggle (fval.get ());
        }
      }
    }
    while (layout ()) {
      if (!conf_gui.get ())
        continue;
      
      try {
        Thread.sleep (50);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }
    configured_layout.maxiterations (0);
    
    /* ForceLayout can continue to perturb position of nodes, if we wish.
     * Setup a daemon thread to run alongside the main thread, and graphics
     */
    if (conf_gui.get () && configured_layout instanceof ForceLayout) {
      Thread t = new Thread () {
  
        @Override
        public void run () {
          while (true) {
            try {
              Thread.sleep (50);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }
            layout ();
          }
        }
      };
      t.setDaemon (true);
      t.start ();
    }
  }

  protected boolean layout () {
    debug.println ("layout..");
    setChanged ();
    notifyObservers ();
    return configured_layout.layout (1);
  }

  @Override
  protected int get_runs () {
    float cruns = conf_runs.get ();
    int runs = (int) cruns;
    if (cruns < 1)
      runs = (int) (this.network.link_count () * cruns);
    return runs;
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
  protected boolean get_random_tick () {
    return conf_random_tick.get ();
  }
  
  /* rewire according to some algorithm */
  private void rewire_alg (TopologyConfigOption tconf, String alg) {
    /* Wire up the graph in some fashion */
    Rewire<H,link<H>> rw = Rewire.factory (alg, network, default_edge_labeler ());
    
    if (rw instanceof ScaleFreeRewire<?,?>) {
      IntVar m = (IntVar)tconf.subopts.get (alg,"m");
      IntVar a = (IntVar)tconf.subopts.get (alg,"a");
      StringVar m_mode = (StringVar)tconf.subopts.get (alg, "m_mode");
      ScaleFreeRewire<H,link<H>> sfr = (ScaleFreeRewire<H, link<H>>) rw;
      
      if (m.isSet ())
        sfr.m (m.get ());
      if (m_mode.isSet ())
        sfr.m_mode (m_mode.get ());
      if (a.isSet ())
        sfr.a (a.get ());
    }
    
    if (rw instanceof CartesianRewire<?,?>) {
      Layout.factory ("Random", network, model_size, 10).layout (1);
    }
    
    if (rw instanceof MultiClassScaleFreeRewire<?,?>) {
      MultiClassScaleFreeRewire<H,link<H>> mcsfr
        = (MultiClassScaleFreeRewire<H, link<H>>) rw;
      IntVar p = (IntVar)tconf.subopts.get (alg,"p");
      if (p.isSet ())
        mcsfr.p (p.get ());
    }
    
    System.out.println ("rewire_alg.... " + rw);
    rw.rewire ();
  }
  
  /* rewire the graph from a file input of topology */
  private void rewire_adjmatrix (TopologyConfigOption tconf) {
    String fname = ((StringVar)tconf.subopts.get (tconf.get (), "AdjMatrix")).get ();
    
    try {
      AdjacencyMatrix.parse (new FileInputStream (fname),
                             network, 
                             default_node_labeler (),
                             default_edge_labeler ());
    } catch (FileNotFoundException e) {
      System.out.println ("Unable to open " + fname);
      System.exit (1);
    }
  }
  
  /* rewire the graph from a file input of topology */
  private void rewire_asgraph (TopologyConfigOption tconf) {
    String fname = ((StringVar)tconf.subopts.get (tconf.get (), "ASGraph")).get ();
    try {
      as_graph_reader<H, link<H>> gr
        = new as_graph_reader<> (network, default_asgraph_labeler (), fname);
      gr.parse ();
    } catch (FileNotFoundException e) {
      System.out.println ("Unable to open " + fname);
      System.exit (1);
    } catch (InputMismatchException e) {
      System.out.println ("Error parsing + " + fname + ": " + e.getMessage ());
      System.exit (1);
    } catch (IOException e) {
      System.out.println ("I/O error parsing " + fname);
      System.out.println (e.getCause ());
      System.exit (1);
    } catch (Exception e) {
      System.out.print ("Exception parsing " + fname);
      if (e.getMessage () != null)
        System.out.println (": " + e.getMessage ());
      e.printStackTrace ();
      System.exit (1);
    }
  }
  /* TODO: Factor out common stuff with above. Would probably require
   * Class<...>.getConstructor based magic.
   */
  private void rewire_adjlist (TopologyConfigOption tconf) {
    String fname = ((StringVar)tconf.subopts.get (tconf.get (), "AdjList")).get ();
    try {
      adjacency_list_reader<H, link<H>> r
        = new adjacency_list_reader<> (network, default_adjlist_labeler (), fname);
      r.parse ();
    } catch (FileNotFoundException e) {
      System.out.println ("Unable to open " + fname);
      System.exit (1);
    } catch (InputMismatchException e) {
      System.out.println ("Error parsing + " + fname + ": " + e.getMessage ());
      System.exit (1);
    } catch (IOException e) {
      System.out.println ("I/O error parsing " + fname);
      System.out.println (e.getCause ());
      System.exit (1);
    } catch (Exception e) {
      System.out.print ("Exception parsing " + fname);
      if (e.getMessage () != null)
        System.out.println (": " + e.getMessage ());
      e.printStackTrace ();
      System.exit (1);
    }
  }
    
  @Override
  protected void rewire () {
    TopologyConfigOption tconf = conf_topology;
    String type = tconf.get ();
    switch (type) {
      case "AdjMatrix":
        rewire_adjmatrix (tconf);
        break;
      case "AdjList":
        rewire_adjlist (tconf);
        break;
      case "ASGraph":
        rewire_asgraph (tconf);
        break;
      default:
        rewire_alg (tconf, type);
        break;
    }
    
    if (conf_gui.get ())
      initial_layout ();
  }
    
  @Override
  protected void initial_setup () {
    /* file based? Then its done on each rewire */
    if (conf_topology != null && conf_topology.get () != null) {
      switch (conf_topology.get ()) {
        case "AdjMatrix":
        case "AdjList":
        case "ASGraph":
          return;
      }
    }
    
    /* otherwise, hosts may need to be created initially,
     * delegate to extending class.
     */
    add_initial_hosts ();
  }
  
  private void describe_links () {
    debug.println ("links:");
    for (H h : network) {
      for (Edge<H,link<H>> e: network.edges (h))
        debug.println (e.label ().get (h).toString ());
    }
  }

  protected void setup_debug () {
    /* NB: Be careful to avoid marshalling strings to pass to debug statements
     * otherwise you may take a good bit of the cost for the debug, even
     * when not enabled.
     * 
     * I.e. let the debug class do the marshalling via format statements.
     */
    if (conf_debug.subopts.get ("debug").isSet ()) {
      StringVar level = ((StringVar)conf_debug.subopts.get ("level"));
      debug.level (level.get ());
      
      StringVar pl = ((StringVar)conf_debug.subopts.get ("pushlevel"));
      if (pl.isSet ())
        debug.pushlevel (pl.get ());
      
      StringVar cf = ((StringVar)conf_debug.subopts.get ("classfilter"));
      if (cf.isSet ())
        debug.classfilter (cf.get ());
      
      StringVar mf = ((StringVar)conf_debug.subopts.get ("methodfilter"));
      if (mf.isSet ())
        debug.methodfilter (mf.get ());
      
      StringVar msgf = ((StringVar)conf_debug.subopts.get ("msgfilter"));
      if (msgf.isSet ())
        debug.msgfilter (msgf.get ());
      
      if (conf_debug.subopts.get ("invert").isSet ())
        debug.invert (true);
    }
  }

  @Override
  protected void describe_begin (int run) {
    debug.printf ("Network: %s\n", network);
    debug.printf ("idmap:\n%s", idmap);
    
    System.out.println ("\nGraph stats");
    System.out.println ("Run: " + run);
    System.out.println ("Nodes: " + network.size ());
    System.out.println ("Edges: " + TraversalMetrics.edges (network));
    System.out.println ("Average degree: " + network.avg_nodal_degree ());
    System.out.println ("Max degree: " + network.max_nodal_degree ());
    System.out.println ("Type: " + conf_topology.get ());
    System.out.println ("Period: " + conf_period.get ());
    
    if (conf_degrees.get ()) {
      System.out.println ("Distribution: ");
      int count = 0;
      for (int val : TraversalMetrics.degree_distribution (network)) {
        if (val > 0)
          System.out.println ("degree: " + count + " number: " + val);
        count++;
      }
      count = 0;
      for (float val : TraversalMetrics.norm_degree_distribution (network)) {
        if (val > 0)
          System.out.println ("P(degree): " + count + " number: " + val);
        count++;
      }
    }
  }
  
  /* k-shell related stats */
  private void describe_kshells () {
    int maxk = kshell.calc (network);
    int count[] = new int [maxk + 1];
    
    //debug.println (debug.levels.ERROR, "trigger..");
    
    for (H h : network) {
      if (maxk > 0)
        count[h.gkc().k]++;
    }
    for (int i = 0; i < count.length; i++) {
      System.out.printf ("shell %2d : %d nodes\n", i, count[i]);
    }
    debug.println ("shell state dump start (Id k_max)");
    for (H h : network) {
      debug.println (h.getId () + " " + h.gkc ().k);
    }
    debug.println ("shell state dump end");
  }
  
  /* path related statistics */
  private void describe_paths () {
    Map<String,Double> stats 
      = TraversalMetrics.stats (this.distance_map (), network);
    
    System.out.println ("Path max: " + stats.get ("max").intValue ());
    System.out.printf ("Path average: %.3f\n", stats.get ("avg"));
    System.out.printf ("Path stddev: %.4f\n", stats.get ("stddev"));
    System.out.printf ("Path stderr: %.4f\n", stats.get ("stderr"));
    System.out.println ("Radius: " + stats.get ("radius").intValue ());
    System.out.println ("Diameter: " + stats.get ("diameter").intValue ());
  }
  
  @Override
  protected void describe_end (int run) {
    if (debug.applies ())
      describe_links ();
    
    System.out.println ("Disconnected: "
                        + TraversalMetrics.count (network,
                          new TraversalMetrics.node_test<H> () {
                            @Override
                            public boolean test (H test) {
                              return (test.getSize () == 0);
                          }}));
    if (get_runs () > 0) {
      long nsent = 0;
      for (H p : network)
        nsent += p.stat_get (stat.sent);
      if (nsent != sim_stats.get_messages_sent ())
        System.out.printf ("Messages Sent Nodes %d != Sim %d!\n",
                           nsent, sim_stats.get_messages_sent ());
      System.out.println ("Messages Sent: " + sim_stats.get_messages_sent ());
      System.out.println ("Converged: " + has_converged ());
      System.out.println ("Ticks: " + sim_stats.get_ticks ());
      System.out.println ("Memory: " + Runtime.getRuntime ().totalMemory ()
                          + " " + (Runtime.getRuntime ().totalMemory () >> 20)
                          + " MiB");
    }
    if (conf_path_stats.get ()) {
      describe_paths ();
    }
    if (conf_kshell_stats.get ())
      describe_kshells ();
    
    /* this obviously must come last - for ease of parsing multiple runs */
    System.out.println ("End stats");
  }
}
