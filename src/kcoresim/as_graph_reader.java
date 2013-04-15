package kcoresim;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.Scanner;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.nongnu.multigraph.Graph;

public class as_graph_reader<N,E> {
  FileInputStream fis = null;
  PushbackInputStream pb = null;
  Scanner scr = null;
  Pattern aslatency = Pattern.compile ("(\\d+),(\\d*([.]\\d+)?)");
  Graph<N,E> network;
  as_graph_reader.labeler<N,E> labeler = null;
  
  interface labeler<N,E> {
    public E edge (int from, int to, double latency);
    public E edge (int from, int to);
    public N node (int node);
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
    
    if (b[0] == (GZIPInputStream.GZIP_MAGIC >> 2)
        && b[1] == (GZIPInputStream.GZIP_MAGIC & 0xff))
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
  
  private void parse_line_as_as (int from_as) {
    int to_as = scr.nextInt ();
    network.set (labeler.node (from_as),
                 labeler.node (to_as),
                 labeler.edge (from_as, to_as));
  }
  
  private void parse_line_as_latency (int from_as) {
    @SuppressWarnings ("unused")
    double from_as_latency = scr.nextDouble ();
    while (scr.findInLine (aslatency) != null) {
      MatchResult m = scr.match ();
      int to_as = Integer.parseInt (m.group (1));
      double to_as_latency = Double.parseDouble (m.group (2));
      
      network.set (labeler.node (from_as),
                   labeler.node (to_as),
                   labeler.edge (from_as, to_as, to_as_latency));
    }
  }
  
  public void parse_line () {
    int from_as = scr.nextInt ();
    
    if (scr.hasNextDouble ())
      parse_line_as_latency (from_as);
    else
      parse_line_as_as (from_as);
  }
  
  public void parse () throws IOException {
    while (scr.hasNext ())
      parse_line ();
    close ();
  }
}
