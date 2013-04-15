package agarnet;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.nongnu.multigraph.Graph;
import org.nongnu.multigraph.debug;

/**
 * Parse graphs given in the format "ID ID ...", often used by Internet
 * researchers to specify "AS graphs".
 * Supported formats are:
 *
 * 1. AS AS format:
 *    <from AS number> <to AS number>
 *
 * 2. UCLA IRL "links" file format:
 *    <from ASN> <to ASN> <first seen> <last seen> <index in AS_GRAPH>
 *    The last 3 parameters are ignored for now, but should be integers.
 *
 * 3. AS-latency list format:
 *    <from ASN> <from ASN internal latency> [<to ASN> <edge latence>]*
 *    The internal latency is ignored. The edge latency will be passed to the
 *    users edge labeler. The latencies must be specified with a decimal
 *    separator, even if they are whole numbers, e.g. "1.0", not "1".
 *
 * The format to use to parse the file with is selected automatically according
 * to the first line in the file. You can not mix formats within a file.
 *
 * @author Paul Jakma
 * @param <N> Node type
 * @param <E> Edge type.
 */
public class as_graph_reader<N,E> {
  FileInputStream fis = null;
  PushbackInputStream pb = null;
  Scanner scr = null;
  final String sre_double = "\\d*([.]\\d+)?";
  final String sre_asn = "\\d+";
  final String sre_tstamp = "\\d+";
  final String sre_index = "\\d+";
  final String sre_aslatency = "(" +sre_asn+ "),(" + sre_double + ")";

  private abstract class acceptpattern<N,E> {
    final Pattern re;
    protected acceptpattern (String re) { this.re = Pattern.compile (re); }
    abstract void parse_line (MatchResult res);
  }
  acceptpattern<N,E> [] acceptpatterns;

  private void init_acceptpatterns () {
      acceptpatterns = new acceptpattern []{
        /* "ASN internal_latency [to_ASN_edge latency]+" format */
        new acceptpattern ("("+sre_asn+")\\s(" +sre_double+ ")\\s(" +sre_aslatency+ ")*\\s*$") {
          @Override
          void parse_line (MatchResult m) {
            N from_as = labeler.node (m.group (1));
            @SuppressWarnings ("unused")
            double from_as_latency = Double.parseDouble (m.group (2));

            for (int i = 3; i < m.groupCount (); i++) {
              //MatchResult m = scr.match ();
              N to_as = labeler.node (m.group (i));
              double to_as_latency = Double.parseDouble (m.group (i + 1));
              E newlabel = labeler.edge (from_as, to_as, to_as_latency);

              debug.printf ("setting %s to %s\n", from_as, to_as);

              try {
                network.remove (from_as, to_as);
                network.set (from_as, to_as, newlabel);
              } catch (UnsupportedOperationException e) {
                debug.printf (debug.levels.ERROR, "Error setting %s -> %s (%f): %s\n",
                              from_as, to_as, to_as_latency, e.getMessage ());
                debug.printf (debug.levels.ERROR, "While parsing: %s\n", m.group (0));
              }
            }
          }
        },
        /* "ASN ASN" format */
        new acceptpattern ("("+sre_asn+")\\s+("+sre_asn+")\\s*") {
          @Override
          void parse_line (MatchResult m) {
            N from_as = labeler.node (m.group (1));
            N to_as = labeler.node (m.group (2));

            debug.printf ("setting %s (%s) to %s (%s)\n",
                         from_as, m.group (1), to_as, m.group (2));

            network.remove (from_as, to_as);
            network.set (from_as, to_as, labeler.edge (from_as, to_as));
          }
        },
        /* IRL format:
         * "ASN to_AS first_seen last_seen AS_PATH_pos"
         * timestamps are unix format.
         */
        new acceptpattern (String.format
                           ("(%s)\\s+(%s)\\s+(%s)\\s+(%s)\\s+(%s)\\s*",
                             sre_asn, sre_asn, sre_tstamp, sre_tstamp,
                             sre_index)) {
          @Override
          void parse_line (MatchResult m) {
            /* for now, don't do anything with the extra info 
             * over AS AS format
             */
            N from_as = labeler.node (m.group (1));
            N to_as = labeler.node (m.group (2));

            debug.printf ("setting %s (%s) to %s (%s)\n",
                         from_as, m.group (1), to_as, m.group (2));

            network.remove (from_as, to_as);
            network.set (from_as, to_as, labeler.edge (from_as, to_as));
          }
        }
      };
  }

  Graph<N,E> network;
  as_graph_reader.labeler<N,E> labeler = null;
  
  public interface labeler<N,E> {
    public E edge (N from, N to, double latency);
    public E edge (N from, N to);
    public N node (String node);
  }
  
  public void close () throws IOException {
    if (pb != null)
      pb.close ();
    if (fis != null)
      fis.close ();
    if (scr != null)
      scr.close ();
  }
  
  @Override
  protected void finalize () throws Throwable {
    super.finalize ();
    close ();
  }

  private InputStream get_inputstream (String fname) throws IllegalArgumentException,
                                                            FileNotFoundException,
                                                            IOException {
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
  
  public as_graph_reader (Graph<N,E> network, as_graph_reader.labeler<N,E> labeler,
                       String fname) throws IllegalArgumentException,
                                            FileNotFoundException,
                                            IOException {
    scr = new Scanner (get_inputstream (fname));
    this.network = network;
    this.labeler = labeler;
  }
  
  public as_graph_reader (Graph<N,E> network, as_graph_reader.labeler<N,E> labeler,
                       InputStream is) {
    scr = new Scanner (is);
    this.network = network;
    this.labeler = labeler;
  }
  
  public void parse_line (String line) {
    Matcher res;
    
    debug.println ("Parsing line:\n" + line);

    for (acceptpattern ap : acceptpatterns) {
      debug.println ("Try match: " + ap.re.pattern ());
      if ((res = ap.re.matcher (line)).matches ()) {
        ap.parse_line (res);
        debug.printf ("matched: %s\n", res.group (0));
        return;
      }
    }
    throw new InputMismatchException ("Unrecognised input: "
                                      + line);
  }
  
  public void parse () throws IOException {
    init_acceptpatterns ();
    while (scr.hasNextLine ())
      parse_line (scr.nextLine ());
    close ();
  }
}
