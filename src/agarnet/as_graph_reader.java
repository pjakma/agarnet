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

public class as_graph_reader<N,E> {
  FileInputStream fis = null;
  PushbackInputStream pb = null;
  Scanner scr = null;
  final String sre_double = "\\d*([.]\\d+)?";
  final String sre_asn = "\\d+";
  final String sre_aslatency = "(" +sre_asn+ "),(" + sre_double + ")";
  Pattern aslatency = Pattern.compile (sre_aslatency);
  Pattern aslatencyform = Pattern.compile ("("+sre_asn+")\\s(" +sre_double+ ")\\s(" +sre_aslatency+ ")*\\s*$");
  Pattern asasform = Pattern.compile ("("+sre_asn+")\\s+("+sre_asn+")\\s*");
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
  
  private void parse_line_as_as (MatchResult m) {
    N from_as = labeler.node (m.group (1));
    N to_as = labeler.node (m.group (2));
    
    debug.printf ("setting %s (%s) to %s (%s)\n",
                 from_as, m.group (1), to_as, m.group (2));
    
    network.remove (from_as, to_as);
    network.set (from_as, to_as, labeler.edge (from_as, to_as));
  }
  
  private void parse_line_as_latency (MatchResult m) {
    /*N from_as = labeler.node (Integer.toString (scr.nextInt ()));
    @SuppressWarnings ("unused")
    double from_as_latency = scr.nextDouble ();
    scr.
    while (scr.hasNext ()) {
      if (scr.findInLine (aslatency) == null)
        throw new InputMismatchException ("Bad AS-Latency input");*/
      
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
  
  public void parse_line (String line) {
    //debug.printf ("%s\n%s\n", aslatencyform, asasform);
    Matcher res;
    
    debug.println ("Parsing line: " + line);
    
    if ((res = aslatencyform.matcher (line)).matches ())
      parse_line_as_latency (res);
    else if ((res = asasform.matcher (line)).matches ())
      parse_line_as_as (res);
    else {
      //scr.findInLine (Pattern.compile (""));
      throw new InputMismatchException ("Unrecognised input: "
                                        + line);
    }
    debug.printf ("matched: %s\n", res.group (0));
  }
  
  public void parse () throws IOException {
    network.plugObservable ();
    while (scr.hasNextLine ())
      parse_line (scr.nextLine ());
    close ();
    network.unplugObservable ();
  }
}
