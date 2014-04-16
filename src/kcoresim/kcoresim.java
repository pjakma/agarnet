package kcoresim;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Observer;
import java.util.TreeMap;

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
import agarnet.perturb.*;
import java.util.List;
import java.util.Set;


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
    kcore<simhost> kcore
      = conf_basic_kcore.get () ? new kcore<simhost> (this)
                                : new opt_kcore<simhost> (this);

    return conf_srtp.get () ? new protocol [] {
                                new protocol_srtp<> (),
                                new protocol_logical_clock<> (),
                                kcore,
                              }
                            : new protocol [] {
                                new protocol_logical_clock<> (),
                                kcore,
                            };
  }
  
  /* Whether last runs had a global v dist kcore mismatch */
  private boolean mismatch = false;
  
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

  final static BooleanConfigOption conf_print_nodes = new BooleanConfigOption (
        "print-nodes", 'N',
        "Print per-node information after each run").set (false);
  final static BooleanConfigOption conf_basic_kcore = new BooleanConfigOption (
        "basic-kcore", 'B',
        "Run the basic kcore algorithm").set (false);
  final static IntConfigOption conf_nodes = new IntConfigOption (
        "nodes", 'p', "<number>",
        "number of nodes to create",
        LongOpt.REQUIRED_ARGUMENT, 1, Integer.MAX_VALUE)
        .set (9);
  
  final static SuboptConfigOption conf_perturb
    = new SuboptConfigOption (
      "perturb", 'b', "<simulation perturbation options>",
      "Perturbation of the simulation across runs",
      LongOpt.REQUIRED_ARGUMENT,
      new ConfigOptionSet () {{
        put (new BooleanVar ("perturb", "Pertub simulation on each iteration"));
        put (new IntVar ("max","Max. # of perturbations to make",
                         0, Integer.MAX_VALUE).set (0));
        put ("diff", new ObjectVar [] {
             new StringVar ("diff", "diff blah blah"),
             new StringVar ("test", "test param how do we do this?"),
        });
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
    confvars.add (conf_print_nodes);
    confvars.add (conf_basic_kcore);

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
        case 'B':
          conf_basic_kcore.set (true);
          break;
        case 'N':
          conf_print_nodes.set (true);
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

  private class histogramme<T> {
    class totals {
      long keys;
      long counts;
    }
    Map<T,Long> hist = new TreeMap ();

    void inc (T key) {
      Long val = hist.get (key);
      hist.put (key, (val != null) ? ++val : 1);
    }

    Set<Entry<T,Long>> entries () {
      return hist.entrySet ();
    }

    long total () {
      long total = 0;
      for (long l : hist.values ()) {
        total += l;
      }
      return total;
    }
  }
  
  private void describe_net (Iterable<simhost> nodes, int maxk) {
    int count[] = new int [network.max_nodal_degree () + 1];
    long dcount[] = new long [count.length];
    
    int sentcount[] = new int [count.length];
    int rcvcount[] = new int [count.length];
    int broadcastcount[] = new int [count.length];
    //long llccount[] = new long [count.length];

    histogramme<Long> lchist = new histogramme<> ();
    histogramme<Long> msghist = new histogramme<> ();
    histogramme<Long> bcasthist = new histogramme<> ();

    //Map<Long,Long> lchist = new TreeMap<> ();
    //Map<Long,Long> msghist = new TreeMap<> ();
    //Map<Long,Long> bcasthist = new TreeMap<> ();
    
    long ktotaldelta = 0;
    int maxdk = 0;
    int kmaxmismatch = 0;
    
    for (simhost h : nodes) {
      if (maxk > 0) {
        sentcount[h.gkc().k] += h.stat_get (stat.sent);
        rcvcount[h.gkc ().k] += h.stat_get (stat.recvd);
        Long bcasts = (long) (h.stat_get (stat.sent) / h.getMass ());
        broadcastcount[h.gkc ().k] += bcasts;

        Long l;
        //Long l = lchist.get (h.logical_time ());
        //lchist.put (h.logical_time (), (l != null ? ++l : 1));
        lchist.inc (h.logical_time ());
        
        msghist.inc (h.stat_get (stat.sent));
        //msghist.put (h.stat_get (stat.sent), (l != null ? ++l : 1));
        
        //l = bcasthist.get (bcasts);
        bcasthist.inc (bcasts);
        
        count[h.gkc().k]++;
        if (h.stat_get (stat.stored) > maxk)
          System.out.println ("dist kshell higher than maxk? " +
                              h.stat_get (stat.stored) + "," + maxk);
        else
          dcount[(int) h.stat_get (stat.stored)]++;
        maxdk = Math.max ((int) h.stat_get (stat.stored), maxdk);
      }
      
      if (/*debug.applies () ||*/ h.stat_get (stat.stored) != h.gkc().k) {
        ktotaldelta += h.stat_get (stat.stored) - h.gkc().k;
        if (h.stat_get (stat.stored) != h.gkc().k) {
          mismatch = true;
          kmaxmismatch = Math.max (kmaxmismatch, h.gkc().k);
        }
        debug.levels l = debug.level ();
        debug.level (debug.levels.DEBUG);
        System.out.printf ("\nincorrect host: %s\n%s\n", h, h.kcorestring ());
        debug.level (l);

        for (simhost neigh : network.successors (h))
          System.out.printf ("  -> %s\n", neigh);

        System.out.printf ("  unemptied links:\n");
        for (Edge<simhost, link<simhost>> e : network.edges (h))
          if (e.label ().size () > 0)
            System.out.printf ("    %s\n", e);
      }
    }
    for (int i = 0; i <= maxk || i <= maxdk; i++) {
      System.out.printf ("shell %2d : %d nodes%s\n", i, count[i],
                         count[i] != dcount[i] ? (" ("+ dcount[i] + ")")
                                               : "");
    }
    
    if (mismatch) {
      debug.println ("printing core " + kmaxmismatch);
      for (simhost h : network) {
        if (h.gkc().k >= kmaxmismatch)
          debug.println (h.kcorestring ());
      }
      debug_mismatches ();
    }
    
    for (int i = 0; i <= maxk; i++) {
      System.out.printf ("shell %2d : %6d sent, %6d rcvd, %6d bcasts\n", 
                         i, sentcount[i], rcvcount[i], broadcastcount[i]);
    }
    
    for (Entry<Long, Long> e : lchist.entries ()) {
      System.out.printf ("hist lc: %6d %6d\n", e.getKey (), e.getValue ());
    }
    System.out.printf ("hist lc tot: %d\n", lchist.total ());

    for (Entry<Long, Long> e : msghist.entries ()) {
      System.out.printf ("hist msg: %6d %6d\n", e.getKey (), e.getValue ());
    }
    System.out.printf ("hist msg tot: %d\n", lchist.total ());

    for (Entry<Long, Long> e : bcasthist.entries ()) {
      System.out.printf ("hist bcast: %6d %6d\n", e.getKey (), e.getValue ());
    }
    System.out.printf ("hist bcast tot: %d\n", lchist.total ());

    System.out.println ("kmax: " + maxk);
    System.out.println ("KDI: " + ktotaldelta);
    System.out.println ("Mismatch: " + mismatch);
  }
  
  @Override
  protected void describe_end () {
    if (get_runs () > 0)
      describe_net (network, kshell.calc (network));

    if (conf_print_nodes.get ()) {
      System.out.printf ("Per-node-info: # ID, degree, "
                         +"kbound,   kgen,   sent,   recvd\n");
      for (simhost h : network) {
        System.out.printf ("Node %8d : %8d %8d %8d %8d %8d\n",
                           h.getId (),
                           h.degree (),
                           h.kbound (),
                           h.kgen (),
                           h.stat_get (stat.sent),
                           h.stat_get (stat.recvd)
                );

      }
    }
    System.out.printf ("End Per-node-info\n");
    
    long maxltime = 0;
    for (simhost h : network) {
      maxltime = Math.max (maxltime, h.logical_time ());
    }
    System.out.println ("Logical clock: " + maxltime);
    
    super.describe_end ();
  }
  
  @Override
  protected boolean has_converged () {
    /* link activity is sufficient to determine convergence in kcore */
    return true;
  }

  @Override
  protected void perturb () {
    return;
  }

  private RemoveAddEach<simhost,link<simhost>> ra_each = null;

  private void run_perturb_desc (List<Edge<simhost,link<simhost>>> edges,
                                 String sense) {
    System.out.printf ("# Perturb %s edges: id, degree, kcore\n",
                       sense);
      for (Edge<simhost,link<simhost>> e : edges) {
        System.out.printf ("Perturb %s edge: %d %d %d ←→ %d %d %d\n",
                           sense,
                           e.from ().getId (),
                           e.from ().degree (),
                           e.from ().kbound (),
                           e.to ().getId (),
                           e.to ().degree (),
                           e.to ().kbound ());
      }
  }
  private void run_perturb (int run) {
    if (!((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      reset_network ();
      return;
    }

    if (run == 0)
      return;
    
    System.out.println ("# Perturb: doing cross-run perturb");


    for (simhost h : network) {
      h.stats_reset ();
    }
    
    if (ra_each == null) {
      float remove_edges_conf
              = ((NumberProbVar)conf_perturb.subopts.get ("links")).get ();
      int max = ((IntVar) conf_perturb.subopts.get ("max")).get ();
      ra_each = new RemoveAddEach<> (network, remove_edges_conf, max);
    }

    if (ra_each.last_removed_edges ().size () > 0)
      run_perturb_desc (ra_each.last_removed_edges (), "added");
    
    List<Edge<simhost,link<simhost>>> removed = ra_each.perturb ();

    if (removed.size () > 0)
      run_perturb_desc (removed, "removed");
  }

  private RandomRemove<simhost,link<simhost>> random_remove = null;
  /* run perturb to debug mismatches by removing edges to search for
   * smaller graphs that still show the mismatch. Not needed any more.
   * left in for reference.
   */
  @SuppressWarnings ("unused")
  private void run_perturb_mismatch_debug (int run) {
    if (((BooleanVar)conf_perturb.subopts.get ("perturb")).get ()) {
      System.out.println ("# doing cross-run perturb");

      if (random_remove == null) {
        float remove_edges_conf
                = ((NumberProbVar)conf_perturb.subopts.get ("links")).get ();
        random_remove = new RandomRemove (network, remove_edges_conf);
      }

      if (!mismatch)
        random_remove.restore ();

      random_remove.clear_removed_edges ();
      
      if (run > 0){
        random_remove.remove ();
      }
    }
    reset_network ();
  }

  @Override
  protected void run_setup (int run) {
    if (run == 0)
      super.run_setup (run);

    run_perturb (run);

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
