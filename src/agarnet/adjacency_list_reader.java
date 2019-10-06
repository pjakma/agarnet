package agarnet;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.NumberFormatException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.awt.Color;

import org.nongnu.multigraph.Graph;
import org.nongnu.multigraph.debug;

import agarnet.framework.Coloured;
/**
 * Parse graphs given in the adjacency list format "ID ID ..."
 * Supported formats are:
 *
 * 1. Simple adjacency format (space separated strings):
 *    <from node> <to node>
 *
 * 2. Adjacency + weight format (space seperated)
 *    <from node> <to node> <integer edge weight>
 *
 * 3. Adjacency + colour format:
 *    <from> #<hex rgb triple> <to> #<hex RGB triple>
 * 
 *    Where hex rgb triple is, e.g., A9bf01
 *
 * 4. Adjacency + weight + colour format:
 *    <from> #<hex rgb triple> <to> #<hex RGB triple> <weight>
 * 
 * The format to use to parse the file with is selected automatically
 * according to the first line in the file.  You can not mix formats within
 * a file.
 *
 * The input file may be GZip compressed, it will be automatically
 * decompressed on the fly.
 *
 * @author Paul Jakma
 * @param <N> Node type
 * @param <E> Edge type.
 */
public class adjacency_list_reader<N,E> {
  /**
   * Interface for an edge labeler object, which the user must supply to the
   * constructor of this class, allowsing the creation of node and edge
   * "labels" (objects) to be delegated back to the user.
   *
   * Slightly different requirements/context means MultiGraph.EdgeLabeler
   * would be too awkard to re-use here.
   */
  public interface labeler<N,E> {
    public E edge (N from, N to);
    public N node (String node);
  }
  public interface weight_labeler<N,E> extends labeler<N,E> {
    public E edge (N from, N to, int weight);
  }
  
  FileInputStream fis = null;
  PushbackInputStream pb = null;
  Scanner scr = null;
  final static String sre_float = "[+-]?(?:\\d*[.])?\\d+";
  final static String sre_int = "\\d+";
  final static String sre_str = "(\\p{Alnum}+)|\"(\\p{Alnum}+)\"";
  final static String sre_rgb = "#[a-fA-F0-9]{6}";
  private acceptpattern use_ap;
  Graph<N,E> network;
  adjacency_list_reader.weight_labeler<N,E> labeler = null;
    
  protected static abstract class acceptpattern {
    final Pattern re;
    final String name;
    protected acceptpattern (String name, String re) {
      this.name = name;
      this.re = Pattern.compile (re);
    }
    abstract void parse_line (MatchResult res);
  }
  protected acceptpattern [] acceptpatterns;
  
  protected void init_acceptpatterns () {
    acceptpatterns = new acceptpattern []{
      /* "Node Node" format */
      new acceptpattern ("from_node to_node", 
                         String.format ("(%s)\\s+(%s)\\s*",
                                        sre_str, sre_str) ) {
        @Override
        void parse_line (MatchResult m) {
          String sfrom = m.group (2) != null ? m.group(2) : m.group(3);
          String sto = m.group (5) != null ?  m.group(5) : m.group(6);
          N from = labeler.node (sfrom);
          N to = labeler.node (sto);

          debug.printf ("setting %s (%s) to %s (%s)\n",
                       from, sfrom, to, sto);

          network.remove (from, to);
          network.set (from, to, labeler.edge (from, to));
        }
      },
      /* "Node Node <weight>" format */
      new acceptpattern ("from_node to_node weight", 
                         String.format ("(%s)\\s+(%s)\\s+(%s)\\s*",
                                        sre_str, sre_str, sre_int) ) {
        @Override
        void parse_line (MatchResult m) {
          String sfrom = m.group (2) != null ? m.group(2) : m.group(3);
          String sto = m.group (5) != null ?  m.group(5) : m.group(6);
          N from = labeler.node (sfrom);
          N to = labeler.node (sto);
          int weight = Integer.parseInt (m.group (7));
          E edge = labeler.edge (from, to, weight);

          debug.printf ("setting %s (%s) to %s (%s), Edge %s (%f)\n",
                       from, sfrom, to, sto, edge, weight);

          network.remove (from, to);
          network.set (from, to, edge, weight);
        }
      },
      /* "Node RGB Node RGB" format */
      new acceptpattern ("from_node to_node #RGB", 
                         String.format ("(%s)\\s+(%s)\\s+(%s)\\s+(%s)\\s*",
                                        sre_str, sre_rgb, sre_str, sre_rgb) ) {
        @Override
        void parse_line (MatchResult m) {
          String sfrom = m.group (2) != null ? m.group(2) : m.group(3);
          String sto = m.group (6) != null ?  m.group(6) : m.group(7);
          N from = labeler.node (sfrom);
          N to = labeler.node (sto);
          Color from_colour = Color.decode (m.group (4));
          Color to_colour = Color.decode (m.group (8));

          if (from instanceof Coloured) {
            ((Coloured) from).colour (from_colour);
            ((Coloured) to).colour (to_colour);
          }
          debug.printf ("setting %s (%s) [%s] to %s (%s) [%s]\n",
                       from, sfrom, from_colour,
                       to, sto, to_colour);
          network.remove (from, to);
          network.set (from, to, labeler.edge (from, to));
        }
      },
      /* "Node RGB Node RGB weight" format */
      new acceptpattern ("from_node to_node #RGB Weight", 
                         String.format ("(%s)\\s+(%s)\\s+(%s)\\s+(%s)\\s+(%s)\\s*",
                                        sre_str, sre_rgb,
                                        sre_str, sre_rgb,
                                        sre_int) ) {
        @Override
        void parse_line (MatchResult m) {
          String sfrom = m.group (2) != null ? m.group(2) : m.group(3);
          String sto = m.group (6) != null ?  m.group(6) : m.group(7);
          N from = labeler.node (sfrom);
          N to = labeler.node (sto);
          Color from_colour = Color.decode (m.group (4));
          Color to_colour = Color.decode (m.group (8));
          int weight = Integer.parseInt (m.group (9));
          E edge = labeler.edge (from, to, weight);


          if (from instanceof Coloured) {
            ((Coloured) from).colour (from_colour);
            ((Coloured) to).colour (to_colour);
          }
          
          debug.printf ("setting %s (%s) [%s] to %s (%s) [%s], Edge %s (%d)\n",
                       from, sfrom, from_colour,
                       to, sto, to_colour,
                       edge, weight);

          network.remove (from, to);
          network.set (from, to, edge, weight);
        }
      },
    };
  }

  public void close () throws IOException {
    if (pb != null)
      pb.close ();
    if (fis != null)
      fis.close ();
    if (scr != null)
      scr.close ();
    pb = null;
    fis = null;
    scr = null;
  }
  
  @Override
  protected void finalize () throws Throwable {
    super.finalize ();
    close ();
  }

  private InputStream get_inputstream (String fname) throws IllegalArgumentException,
                                                            FileNotFoundException,
                                                            IOException {
    if (fname.contentEquals ("-"))
      return System.in;
    
    fis = new FileInputStream (fname);
    pb = new PushbackInputStream (fis, 2);
    byte [] b = new byte [2];
    
    if (pb.read (b) < 2) {
      fis.close ();
      pb.close ();
      throw new IllegalArgumentException ("file contains less than 2 bytes of data");
    }
    
    pb.unread (b);
    
    if (b[0] == 31 && b[1] == -117)
      return new GZIPInputStream (pb);
    
    return pb;
    
  }
  
  /* Internal, general-case constructors.  Public constructors likely
   * specialised as the labeler argument may need to be specific to a
   * derived class.
   *
   * Probably there should be a general abstract class, rather than stuffing
   * the general case + the special case for adjacency-lists into this one
   * class, but that seems like too much OO just for reading graph files
   */
  protected adjacency_list_reader (Graph<N,E> network, InputStream is) {
    scr = new Scanner (is);
    this.network = network;
  }
  protected adjacency_list_reader (Graph<N,E> network, String fname) 
                    throws FileNotFoundException,
                           IOException {
    this.network = network;
    scr = new Scanner (get_inputstream (fname));
  }
  
  /* Specialised public constructors for the adjacency-list + weight_labeler
   * case of this implementation.
   */
  public adjacency_list_reader (Graph<N,E> network,
                                weight_labeler<N,E> labeler,
                                String fname) 
                                throws IllegalArgumentException,
                                       FileNotFoundException,
                                       IOException {
    this (network, fname);
    this.labeler = labeler;
  }
  
  public adjacency_list_reader (Graph<N,E> network, 
                                weight_labeler<N,E> labeler,
                                InputStream is) {
    this (network, is);
    this.labeler = labeler;
  }
  
  public void parse_line (String line) {
    Matcher res;
    
    debug.printf ("Parsing line: %s\n", line);
    
    if (use_ap == null) {
      for (acceptpattern ap : acceptpatterns) {
        //debug.printf ("Try %s: %s\n", ap.name, ap.re.pattern ());
        if (ap.re.matcher (line).matches ()) {
          debug.printf ("Using: %s\n", ap.name);
          use_ap = ap;
          break;
        }
      }
    }
    
    if (use_ap != null && (res = use_ap.re.matcher (line)).matches ()) {
      for (int i = 1; i <= res.groupCount(); i++)
        debug.printf ("group %d: %s\n", i, res.group(i));
      use_ap.parse_line (res);
    } else {
      throw new InputMismatchException ("Unrecognised input: "
                                        + line);
    }
  }
  
  public void parse () throws IOException,
                              FileNotFoundException, 
                              InputMismatchException, 
                              NumberFormatException {
    init_acceptpatterns ();
    
    try {
      while (scr.hasNextLine ())
        parse_line (scr.nextLine ());
    } finally {
      close ();
    }
  }
}
