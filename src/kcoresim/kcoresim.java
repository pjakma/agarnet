package kcoresim;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Observer;

import javax.swing.JFrame;

import org.nongnu.multigraph.*;
import org.nongnu.multigraph.layout.*;
import org.nongnu.multigraph.structure.kshell;

import agarnet.anipanel;
import agarnet.framework.AbstractCliApp;
import agarnet.link.*;
import agarnet.protocols.protocol;
import agarnet.protocols.protocol_srtp;
import agarnet.protocols.protocol_logical_clock;
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
    return conf_srtp.get () ? new protocol [] {
                                new protocol_srtp<> (),
                                new protocol_logical_clock<> (),
                                new kcore<simhost> (this),
                              }
                            : new protocol [] {
                                new protocol_logical_clock<> (),
                                new kcore<simhost> (this),
                            };
  }
  
  /* Whether last runs had a global v dist kcore mismatch */
  private boolean mismatch = false;
  private int removed_runs = 0;
  
  ArrayList<Edge<simhost,link<simhost>>> last_removed_edges
    = new ArrayList<Edge<simhost,link<simhost>>> ();
  
  private simhost get_host (Long id, protocol<Long> [] protos) {
    simhost s = new simhost (this, protos.clone ());
    s.setId (id);
    new_node (id, s);
    return s;
  }
  @Override
  protected simhost get_host (Long l) {
    return get_host (l, new_protstack_kcore ());
  }
  
  public kcoresim (Dimension d) {
    super (d);
  }
  
  @Override
  protected void add_initial_hosts () {
    /* create a network */
    for (int i = 0; i < conf_nodes.get (); i++)
    	network.add (get_host ((long) i));
  }
  
  final static IntConfigOption conf_nodes = new IntConfigOption (
        "nodes", 'p', "<number>",
        "number of nodes to create",
        LongOpt.REQUIRED_ARGUMENT, 1, Integer.MAX_VALUE)
        .set (9);
  
  final static SuboptConfigOption conf_perturb
  = new SuboptConfigOption (
      "perturb", 'b', "<simulation perturbation options>",
      "Perturbation of the simulation, e.g. moving nodes.",
      LongOpt.REQUIRED_ARGUMENT,
      new ConfigOptionSet () {{
        put (new IntVar (
            "ticks",
            "Number of ticks to perturb simulation at",
            0, Integer.MAX_VALUE)
            .set (10));
        put (new BooleanVar ("perturb", "Pertub simulation on each iteration"));
        put (new IntVar ("max","Max. # of perturbations to make",
                         0, Integer.MAX_VALUE).set (10));
        put (new NumberProbVar (
            "links",
            "number or percentage of edges to remove")
            .set (1));
      }});
  final static BooleanConfigOption conf_srtp = new BooleanConfigOption (
        "srtp", 'S',
        "Use Simple Reliable Transport Protocol").set (false);
  public static void main(String args[]) {    
    int c;
    LinkedList<LongOpt> lopts = new LinkedList<> ();
    
    /* Add kcore sim specific config-vars */
    confvars.add (conf_nodes);
    confvars.add (conf_perturb);
    confvars.add (conf_srtp);
    
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
        case 'b':
          conf_perturb.parse (g.getOptarg ());
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
        case 'R':
          conf_random_tick.set (true);
          break;
        case 's':
          conf_sleep.parse (g.getOptarg ());
          break;
        case 'S':
          conf_srtp.set (true);
          break;
        case 'T':
          conf_path_stats.set (true);
          break;
        case 'L':
          conf_link.parse (g.getOptarg ());
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
      jf.setTitle ("k-core Simulation/Model");
      jf.setDefaultCloseOperation (javax.swing.WindowConstants.EXIT_ON_CLOSE);
      jf.setSize (1000, 1000 * s.model_size.height/s.model_size.width);
      jf.setVisible (true);
    }
    /* Use random as initial layout */
    Layout.factory ("Random", s.network, s.model_size, 10).layout (1);
    
    s.main_loop ();
  }
  
  private void debug_mismatches () {
    File tmp;
    PrintStream out;
    
    debug.println (debug.levels.ERROR,
                   "Mismatch between distributed and global! DGMSMATCH");
    
    try {
      Calendar c = Calendar.getInstance ();
      
      String tstamp = String.format ("%02d%02d%02d", c.get (Calendar.YEAR),
                                     c.get (Calendar.MONTH) + 1,
                                     c.get (Calendar.DAY_OF_MONTH));
      tmp = File.createTempFile ("kcore-mismatch-debug." + tstamp + ".", ".txt");
      out = new PrintStream (tmp);
      System.out.println ("debug_mismatches: writing adjacency matrix to "
                  + tmp.getAbsolutePath ());
    }
    catch (IOException e1) {
      System.out.println ("debug_mismatches: Unable to create temp file!");
      out = System.err;
    }
    
    AdjacencyMatrix.sparse (out, network);
  }
  
  private void describe_net (int maxk) {
    int count[] = new int [network.max_nodal_degree () + 1];
    long dcount[] = new long [count.length];
    long ktotaldelta = 0;
    int maxdk = 0;
    int kmaxmismatch = 0;
    
    for (simhost h : network) {
      if (maxk > 0) {
        count[h.gkc().k]++;
        if (h.stat_get (stat.stored) > maxk)
          System.out.println ("dist kshell higher than maxk? " +
                              h.stat_get (stat.stored) + "," + maxk);
        else
          dcount[(int) h.stat_get (stat.stored)]++;
        maxdk = Math.max ((int) h.stat_get (stat.stored), maxdk);
      }
      
      if (debug.applies () || h.stat_get (stat.stored) != h.gkc().k) {
        ktotaldelta += h.stat_get (stat.stored) - h.gkc().k;
        if (h.stat_get (stat.stored) != h.gkc().k) {
          mismatch = true;
          kmaxmismatch = Math.max (kmaxmismatch, h.gkc().k);
        }
        System.out.printf ("\nincorrect host: %s\n%s\n", h, h.kcorestring ());
        for (simhost neigh : network.successors (h))
          System.out.printf ("  -> %s\n", neigh);
        System.out.printf ("  unemptied links:\n");
        for (Edge<simhost, link<simhost>> e : network.edges (h))
          if (e.label ().size () > 0)
            System.out.printf ("    %s\n", e);
      }
    }
    for (int i = 0; i <= maxk; i++) {
      System.out.printf ("shell %2d : %d nodes%s\n", i, count[i],
                         count[i] != dcount[i] ? (" ("+ dcount[i] + ")")
                                               : "");
    }
    for (int i = maxk; i < maxdk; i++)
      System.out.printf ("shell %2d : 0 nodes (%d)\n", i, dcount[i]);
    
    if (mismatch) {
      debug.println ("printing core " + kmaxmismatch);
      for (simhost h : network) {
        if (h.gkc().k >= kmaxmismatch)
          debug.println (h.kcorestring ());
      }
      debug_mismatches ();
    }
    System.out.println ("kmax: " + maxk);
    System.out.println ("KDI: " + ktotaldelta);
    System.out.println ("Mismatch: " + mismatch);
  }
  
  @Override
  protected void describe_end () {
    if (get_runs () > 0)
      describe_net (kshell.calc (network));
    
    long maxltime = 0;
    for (simhost h : network)
      maxltime = Math.max (maxltime, h.logical_time ());
    System.out.println ("Logical clock: " + maxltime);
    
    super.describe_end ();
  }
  
  @Override
  protected boolean has_converged () {    
    return true;
  }
  
  /* Add or remove a link from a randomly chosen pair of nodes */ 
  private void perturb_add_or_remove_one () {
    Iterator<simhost> ith = network.random_node_iterable ().iterator ();
    
    if (!ith.hasNext ())
      return;
    
    simhost h1 = ith.next ();
    
    if (!ith.hasNext ())
      return;
    
    simhost h2 = ith.next ();
    
    if (h1 == h2)
      return;
    
    if (network.is_linked (h1, h2)) {
      last_removed_edges.add (network.edge (h1, h2));
      debug.printf ("remove link %ld to %ld\n", h1.getId (), h2.getId ());
      network.remove (h1, h2);
    } else {
      debug.printf ("add link %ld to %ld\n", h1.getId (), h2.getId ());
      network.set (h1, h2, this.default_edge_labeler ().getLabel (h1, h2));
    }
  }
  
  /* remove one randomly chosen link */
  private void perturb_random_remove (int num) {
    Iterator<simhost> ith = network.random_node_iterable ().iterator ();
    
    System.out.printf ("removing %d edges\n", num);
    
    while (ith.hasNext () && num > 0) {
      simhost h = ith.next ();
      
      Iterator<Edge<simhost,link<simhost>>> ite = network.random_edge_iterable (h).iterator ();
      
      if (!ite.hasNext ())
        return;
      
      Edge<simhost,link<simhost>> e = ite.next ();
      last_removed_edges.add (e);
      System.out.printf ("removing %s\n", e);
      
      network.remove (h, e.to ());
      num--;
    }
  }
  
  /* remove the 1-shell */
  private void perturb_remove_lowest_shell () {
    System.out.println ("remove lowest shell");
    int min = Integer.MAX_VALUE;
    
    for (simhost h : network) {
      debug.printf ("consider %s: %d vs %d\n", h,  h.gkc ().k, min);
      if (h.gkc ().k > 0 && h.gkc ().k < min)
        min = h.gkc ().k;
    }
    
    if (min == 0)
      return;
    
    System.out.println ("# removing the " + min + "-shell");
    
    for (simhost h : network)
      if (h.gkc().k == min)
        last_removed_edges.addAll (network.edges (h));
    
    for (Edge<simhost,link<simhost>> e : last_removed_edges)
      network.remove (e.from (), e.to (), e.label ());
    
    for (Edge<simhost,link<simhost>> e : last_removed_edges) {
      if (network.nodal_outdegree (e.from ()) == 0)
        network.remove (e.from ());
      if (network.nodal_outdegree (e.to ()) == 0)
        network.remove (e.to ());
    }
  }
  
  @Override
  protected void perturb () {
    return;
  }
  
  @Override
  protected int get_runs () {
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      return Integer.MAX_VALUE;
    }
    return super.get_runs ();
  }
  private void run_perturb (int run) {
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      System.out.println ("# doing cross-run perturb");
      int maxperturbs = ((IntVar)conf_perturb.subopts.get ("max")).get ();
      float remove_edges_conf = ((NumberProbVar)conf_perturb.subopts.get ("links")).get ();
      int remove;
      
      remove = (int) (remove_edges_conf < 1 ? Math.max (network.size () * remove_edges_conf, 1)
                                            : remove_edges_conf);
      
      if (!mismatch && last_removed_edges.size () > 0) {
        System.out.println ("Perturb: Add " + last_removed_edges.size ());
        for (Edge<simhost,link<simhost>> e : last_removed_edges)
          network.set (e.from (), e.to (), e.label ());
        last_removed_edges.clear ();
        removed_runs++;
      }

      if (mismatch && last_removed_edges.size () > 0)
        last_removed_edges.clear ();
      
      if (run > 0){
        System.out.println ("Perturb: Remove " + remove);
        if ((maxperturbs <= 0) || (run <= maxperturbs))
          perturb_random_remove (remove);
      }
    }
  }

  @Override
  protected void run_setup (int run) {
    if (run == 0)
      super.run_setup (run);

    run_perturb (run);
    reset_network ();
    rewire_update_hosts ();

    /* ad-hoc FSM, eek */
    if (mismatch)
      mismatch = false;
    
  }
  
  @Override
  protected void rewire_update_hosts () {
    /* set the mass according to the degree, useful for things like
     * Force-Directed Layout.
     */
    int maxd = network.max_nodal_degree ();
    for (simhost p : network) {
      rewire_update_host (p);
      p.maxdegree (maxd);
      p.link_update ();
    }
    setChanged ();
  }
}
