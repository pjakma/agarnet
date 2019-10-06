package agarnet;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;

import org.junit.* ;
import static org.junit.Assert.* ;

import org.nongnu.multigraph.Graph;
import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.SimpleGraph;
import org.nongnu.multigraph.debug;

import agarnet.framework.Coloured;
import java.awt.Color;

public class test_adjacency_list_reader {
  static class test_node implements Coloured {
    static Map<String, test_node> str2node = new HashMap<> ();
    final String label;
    Color colour = Color.white;
    
    private void _add_map () {
      str2node.put (this.label, this);
    }
    test_node (String label) {
      this.label = label;
      _add_map ();
    }
    
    test_node (String label, Color c) {
      this.label = label;
      this.colour = c;
      _add_map ();
    }
    
    @Override
    public boolean equals (Object o) {
      if (this == o) return true;
      if (o == null || !(o instanceof test_node)) return false;
      
      return this.label.equals ( ((test_node) o).label);
    }
    @Override
    public int hashCode () {
      return label.hashCode ();
    }
    
    public Color colour () { return colour; }
    public void colour (Color c) { colour = c; }
    public String toString () {
      return "node(" + label + "," + colour + ")";
    }
  }
  
  static class test_edge {
    final test_node from;
    final test_node to;
    final int weight;
    
    test_edge (test_node from, test_node to) {
      this.from = from;
      this.to = to;
      this.weight = 1;
    }
    test_edge (test_node from, test_node to, int weight) {
      this.from = from;
      this.to = to;
      this.weight = weight;
    }
    
    @Override
    public boolean equals (Object o) {
      if (this == o) return true;
      if (o == null || !(o instanceof test_edge)) return false;
      
      test_edge other = (test_edge) o;
      return from.equals (other.from) &&
             to.equals (other.to) &&
             weight == other.weight;
    }
    @Override
    public int hashCode () {
      return Objects.hash (from, to, weight);
    }
    @Override 
    public String toString () {
      return "test_edge(from: " + from + ", to: " + to + ", w:" + weight + ")";
    }
    
    static test_edge test_edge (String a, Color ca, 
                                String b, Color cb) {
      return new test_edge (new test_node (a, ca), new test_node (b, cb));
    }
    static test_edge test_edge (String a, String b) {
      return new test_edge (new test_node (a), new test_node (b));
    }
    static test_edge test_edge (String a, Color ca, 
                                String b, Color cb, int weight) {
      return new test_edge (new test_node (a, ca), 
                            new test_node (b, cb),
                            weight);
    }
    static test_edge test_edge (String a, String b, int weight) {
      return new test_edge (new test_node (a), new test_node (b), weight);
    }
  }
  
  private adjacency_list_reader.weight_labeler<test_node, test_edge> 
  labeler () {
    return new adjacency_list_reader.weight_labeler<test_node, test_edge> () {
      @Override
      public test_edge edge (test_node from, test_node to) {
        return new test_adjacency_list_reader.test_edge (from, to);
      }
      @Override
      public test_edge edge (test_node from, test_node to, int weight) {
        return new test_adjacency_list_reader.test_edge (from, to, weight);
      }
      @Override
      public test_node node (String node) {
        return new test_adjacency_list_reader.test_node (node);
      }
    };
  }

  class tuple<X> {
    final X a, b;
    tuple (X a, X b) {
      this.a = a;
      this.b = b;
    }
  }
  
  /* General test logic */  
  private void do_test (String data, test_edge [] edges, 
                        tuple<test_edge> [] no_edges) {
    debug.println("Test input:\n" + data);
    InputStream is = new ByteArrayInputStream(data.getBytes());
    Graph<test_node, test_edge> graph = new SimpleGraph<> ();
    adjacency_list_reader<test_node, test_edge> ar = 
      new adjacency_list_reader<> (graph, labeler(), is);
    try {
      ar.parse();
    } catch (Exception e) {
      e.printStackTrace();
      fail("Exception occured: " + e);
    }

    for (test_edge t: edges) {
      test_node [] nodes = { t.from, t.to };
      for (test_node n : nodes) {
        test_node n2 = test_node.str2node.get (n.label);
        debug.printf ("node: %s, got: %s\n", n, n2);
        assertNotNull (test_node.str2node.get (n.label));
        assertEquals (test_node.str2node.get (n.label), n);
        assertEquals (test_node.str2node.get (n.label).colour, n.colour);
        assertTrue (graph.contains(test_node.str2node.get (n.label)));
      }
      debug.printf ("lookup edge %s -> %s\n", t.from.label, t.to.label);
      Edge<test_node, test_edge> graph_edge 
        = graph.edge(test_node.str2node.get (t.from.label),
                     test_node.str2node.get (t.to.label));
      assertNotNull (graph_edge);
      debug.printf ("graph_edge: %s, %s\n", graph_edge, t);
      debug.printf ("%d and %d\n", t.weight, graph_edge.weight());
      assertEquals (t.weight, graph_edge.weight());
      
      Edge<test_node, test_edge> reverse_edge
        = graph.edge(test_node.str2node.get (t.to.label),
                     test_node.str2node.get (t.from.label));
      assertNotNull (reverse_edge);
      debug.printf ("reverse_edge: %s\n", graph_edge);
      //assertTrue (graph_edge.from().equals(reverse_edge.to()));
      //assertTrue (graph_edge.to().equals(reverse_edge.from()));
      
    }
    for (tuple<test_edge> tup : no_edges) {
      assertNull (graph.edge(test_node.str2node.get (tup.a),
                            test_node.str2node.get (tup.b)));
    }
  }
  
  /* Test the basic "to from" adjacency list format */
  @Test public void test_to_from () {
    debug.level (debug.levels.DEBUG);
    String data = 
      "a b\n"     +
      "\"c\"\td\n" +
      "e \"f\"\n" +
      "foo     bar\n" +
      "\"war\"\t  dar \n" +
      "nar \"far\"\t  \t\n";
    
    test_edge [] edges = new test_edge [] {
      test_edge.test_edge ("a", "b"), 
      test_edge.test_edge ("c", "d"),
      test_edge.test_edge ("e", "f"),
      test_edge.test_edge ("foo", "bar"),
      test_edge.test_edge ("war", "dar"),
      test_edge.test_edge ("nar", "far"),
    };
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    tuple<test_edge> [] no_edges = (tuple<test_edge> []) new tuple [] {
      new tuple<> ("a", "c"),
      new tuple<> ("a", "d"),
      new tuple<> ("c", "b"),
      new tuple<> ("c", "a"),
    };
    do_test (data, edges, no_edges);
  }
  
  /* Test the "to colour from colour" format */
  @Test public void test_to_from_colour () {
    debug.level (debug.levels.DEBUG);
    String data = 
      "a #fff000 b #fff001\n"     +
      "\"c\" #fff002 d #fff003\n" +
      "e #fff004 \"f\" #fff005\n" +
      "foo #fff006 bar #fff007\t\n" +
      "\"war\" #fff008 dar #fff009 \t \n" +
      "nar #fff00A \"far\" #fff00b \n";
    
    test_edge [] edges = new test_edge [] {
      test_edge.test_edge ("a", Color.decode ("#fff000"),
                           "b", Color.decode ("#fff001")), 
      test_edge.test_edge ("c", Color.decode ("#fff002"),
                           "d", Color.decode ("#fff003")),
      test_edge.test_edge ("e", Color.decode ("#fff004"), 
                           "f", Color.decode ("#fff005")),
      test_edge.test_edge ("foo", Color.decode ("#fff006"),
                           "bar", Color.decode ("#fff007")),
      test_edge.test_edge ("war", Color.decode ("#fff008"),
                           "dar", Color.decode ("#fff009")),
      test_edge.test_edge ("nar", Color.decode ("#fff00a"),
                           "far", Color.decode ("#fff00B")),
    };
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    tuple<test_edge> [] no_edges = (tuple<test_edge> []) new tuple [] {
      new tuple<> ("a", "c"),
      new tuple<> ("a", "d"),
      new tuple<> ("c", "b"),
      new tuple<> ("c", "a"),
    };
    do_test (data, edges, no_edges);    
  }

  /* "to colour from colour weight" format */
  @Test public void test_to_from_colour_weight () {
    debug.level (debug.levels.DEBUG);
    String data = 
      "a #fff000 b #fff001 10\n"     +
      "\"c\" #fff002 d #fff003 20\n" +
      "e #fff004 \"f\" #fff005 30 \n" +
      "foo #fff006 bar #fff007\t40\n" +
      "\"war\" #fff008 dar #fff009 50\t\n" +
      "nar #fff00A \"far\" #fff00b\t 60 \t \n";

    test_edge [] edges = new test_edge [] {
      test_edge.test_edge ("a", Color.decode ("#fff000"),
                           "b", Color.decode ("#fff001"),
                           10), 
      test_edge.test_edge ("c", Color.decode ("#fff002"),
                           "d", Color.decode ("#fff003"),
                           20),
      test_edge.test_edge ("e", Color.decode ("#fff004"), 
                           "f", Color.decode ("#fff005"),
                           30),
      test_edge.test_edge ("foo", Color.decode ("#fff006"),
                           "bar", Color.decode ("#fff007"),
                           40),
      test_edge.test_edge ("war", Color.decode ("#fff008"),
                           "dar", Color.decode ("#fff009"),
                           50),
      test_edge.test_edge ("nar", Color.decode ("#fff00a"),
                           "far", Color.decode ("#fff00B"),
                           60),
    };
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    tuple<test_edge> [] no_edges = (tuple<test_edge> []) new tuple [] {
      new tuple<> ("a", "c"),
      new tuple<> ("a", "d"),
      new tuple<> ("c", "b"),
      new tuple<> ("c", "a"),
    };
    do_test (data, edges, no_edges);    
  }
}
