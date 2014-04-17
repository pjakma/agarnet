package kcoresim;

import agarnet.link.link;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.Graph;
import org.nongnu.multigraph.debug;

/**
 *
 * @author paul
 */
public class remove_kcore {
  Graph<simhost,link<simhost>> network;
  private List<Edge<simhost,link<simhost>>> removed_edges
          = new ArrayList<> ();
  
  public List<Edge<simhost,link<simhost>>> removed_edges () {
    return removed_edges;
  }

  /**
   * Remove the lowest shell (typically the 1-shell).
   */
  public List<Edge<simhost,link<simhost>>> perturb_remove_lowest_shell () {
    System.out.println ("# perturb: remove lowest shell");
    int min = Integer.MAX_VALUE;

    for (simhost h : network) {
      debug.printf ("consider %s: %d vs %d\n", h,  h.gkc ().k, min);
      if (h.gkc ().k > 0 && h.gkc ().k < min)
        min = h.gkc ().k;
    }

    if (min == 0)
      return null;

    System.out.println ("# removing the " + min + "-shell");

    List<Edge<simhost,link<simhost>>> to_remove = new ArrayList<> ();

    for (simhost h : network)
      if (h.gkc().k == min)
        to_remove.addAll (network.edges (h));

    for (Edge<simhost,link<simhost>> e : to_remove)
      network.remove (e.from (), e.to (), e.label ());

    for (Edge<simhost,link<simhost>> e : to_remove) {
      if (network.nodal_outdegree (e.from ()) == 0)
        network.remove (e.from ());
      if (network.nodal_outdegree (e.to ()) == 0)
        network.remove (e.to ());
    }

    removed_edges.addAll (to_remove);
    return Collections.unmodifiableList (to_remove);
  }
}
