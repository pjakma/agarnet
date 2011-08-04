package kcoresim;

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
import org.nongnu.multigraph.structure.kshell;

import kcoresim.simhost;

import agarnet.anipanel;
import agarnet.framework.AbstractCliApp;
import agarnet.link.*;
import agarnet.protocols.protocol;
import agarnet.protocols.protocol_stats.stat;
import agarnet.variables.*;
import agarnet.variables.atoms.*;


public class kcoresim extends AbstractCliApp<simhost> implements Observer {
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
  private protocol<Long> [] new_protstack_kcore () {
    return new protocol [] {
      //new transport_protocol<Long, simhost, link<simhost>> (this, network),
      new kcore<simhost> (this),
    };
  }
  
  private simhost get_host (protocol<Long> [] protos) {
    simhost s = new simhost (this, protos.clone ());
    s.setId (idmap.get (s));
    return s;
  }
  public simhost get_host () {
    return get_host (new_protstack_kcore ());
  }
  
  public kcoresim (Dimension d) {
    super (d);
  }
  
  protected void add_initial_hosts () {
    /* create a network */
    for (int i = 0; i < conf_nodes.get (); i++) {
      simhost p; 
      
    	p = get_host ();
      
      network.add (p);
    }
  }
  
  final static IntConfigOption conf_nodes = new IntConfigOption (
        "nodes", 'p', "<number>",
        "number of nodes to create",
        LongOpt.REQUIRED_ARGUMENT, 1, Integer.MAX_VALUE)
        .set (10);
  
  public static void main(String args[]) {    
    int c;
    LinkedList<LongOpt> lopts = new LinkedList<LongOpt> ();
    
    /* Add kcore sim specific config-vars */
    confvars.add (conf_nodes);
    
    for (ConfigurableOption cv : confvars)
      lopts.add (cv.lopt);
    
    //System.out.println ("str: " + ConfigurableOption.get_optstring (confvars));
    Getopt g = new Getopt("simapp", args,
                          ConfigurableOption.get_optstring (confvars),
                          lopts.toArray (new LongOpt [0]));
    
    while ((c = g.getopt ()) != -1) {
      switch (c) {
        case '?':
          usage ("Unknown argument: " + (char)g.getOptopt ());
          break;
        case 'p':
          conf_nodes.parse (g.getOptarg ());
          break;
        case 'P':
          conf_period.parse (g.getOptarg ());
          break;
        case 'd':
          BooleanVar db
            = ((BooleanVar)conf_debug.subopts.get ("debug"));
          db.set (true);
          System.out.printf ("optarg: %s\n", g.getOptarg ());
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
        case 'l':
          conf_layout.parse (g.getOptarg ());
          break;
        case 'r':
          conf_runs.parse (g.getOptarg ());
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
    
    kcoresim s = new kcoresim (conf_model_size.get ());
    
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
    
    s.main_loop ();
  }
  
  private void describe_net () {
    describe_net (0);
  }
  
  private void describe_net (int maxk) {
    int count[] = new int [maxk];
    
    //debug.println (debug.levels.ERROR, "trigger..");
    
    for (simhost h : network) {
      if (maxk > 0)
        count[h.gkc().k]++;
      if (debug.applies () || h.stat_get (stat.stored) != h.gkc().k) {
        
        if (h.stat_get (stat.stored) != h.gkc().k)
          debug.printf (debug.levels.ERROR,
                        "Mismatch between distributed and global! DGMSMATCH\n");
        debug.printf ("host: %s\n", h);
        for (simhost neigh : network.successors (h))
          debug.printf ("  -> %s\n", neigh);
        debug.printf ("  links:\n");
        for (Edge<simhost, link<simhost>> e : network.edges (h))
          debug.printf ("    %s\n", e);
      }
    }
    for (int i = 0; i < count.length; i++) {
      System.out.printf ("shell %2d : %d nodes\n", i, count[i]);
    }
  }
  
  protected void describe_end () {
    int maxk = kshell.calc (network);
    
    describe_net (maxk);
    
    super.describe_end ();
  }
  
  protected boolean has_converged () {    
    return (sim_stats.get_ticks () > conf_period.get ());
  }
  
  @Override
  protected void perturb () {
    return;
  }

  @Override
  protected void rewire_update_hosts () {
    /* set the mass according to the degree, useful for things like
     * Force-Directed Layout. maxd is used by the kcore algorithm.
     */
    int maxd = network.max_nodal_degree ();
    for (simhost p : network) {
      p.setMass (network.nodal_outdegree (p));
      p.set_maxdegree (maxd);
      p.set_numnodes (network.size ());
    }
  }
}
