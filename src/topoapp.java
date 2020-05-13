/* This file is part of 'agarnet'
 *
 * Copyright (C) 2011, 2013, 2014 Paul Jakma
 * Copyright (c) Facebook, Inc. and its affiliates
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
public class topoapp extends AbstractCliApp<String, simhost<String>> implements Observer {
  @SuppressWarnings({"unchecked","rawtypes"})
  private protocol<String> [] new_protstack_peer () {
    return new protocol [] {
      //new transport_protocol<String, simhost<String>, link<simhost<String>>> (this, network),
      new peer<String,simhost<String>> (this),
    };
  }
  
  
  private simhost<String> get_host (String id, simhost.Node type, boolean movable,
                            protocol<String> [] protos) {
    simhost<String> s;
    if ((s = id2node (id)) != null)
      return s;
    s = new simhost<String> (this, type, movable, protos.clone ());
    s.setId (id);
    new_node (id, s);
    return s;
  }
  /* default get_host */
  protected simhost<String> get_host (String id) {
    return get_host (id, simhost.Node.peer, true, new_protstack_peer ());
  }
  
  @Override
  public simhost<String> str2node (String sl) {
    return get_host (sl);
  }
  
  public topoapp (Dimension d) {
    super (d);
  }
  
  protected void add_initial_hosts () {
    /* create a network */
    for (int i = 0; i < conf_nodes.get (); i++) {
      network.add (get_host (String.valueOf (i)));
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

    if (conf_help.get ())
      usage (null, 0);
    dump_arg_state ();
    
    topoapp s = new topoapp (conf_model_size.get ());
        
    JFrame jf = null;
    if (conf_gui.get ()) {
      anipanel.options opts
        = new anipanel.options ().antialiasing (conf_antialias.get ());
      s.ap = new anipanel<String,simhost<String>> (s, opts);
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
    /* topoapp only does topology setup, then nothing more */
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
    
    for (simhost<String> h : network) {
      f = h.getSize ();
      break;
    }
    
    for (simhost<String> p : network) {
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
