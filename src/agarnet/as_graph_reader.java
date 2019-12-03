/* This file is part of 'agarnet'
 *
 * Copyright (C) 2011, 2013, 2014, 2019 Paul Jakma
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
 * 2. Current UCLA IRL Topolite "links" file format:
 *    <from ASN> <to ASN> [optional count over time period]
 *    
 * 3. The old UCLA IRL "links" file format:
 *    <from ASN> <to ASN> <first seen> <last seen> <index in AS_GRAPH> [MRT record]
 *    
 *    The last 4 parameters are ignored for now, but should be integers. ASNs in "ASDot"
 *    format are accepted for this type, as they occur in the IRL dataset. 
 *    
 *    For bug compatibility, negative ASDots are also accepted for IRL format,
 *    IRLs parser/output seems to turn high ASNs into ASDots with a negative first
 *    portion.
 *
 * 4. AS-latency list format:
 *    <from ASN> <from ASN internal latency> [<to ASN> <edge latence>]*
 *    The internal latency is ignored. The edge latency will be passed to the
 *    users edge labeler. The latencies must be specified with a decimal
 *    separator, even if they are whole numbers, e.g. "1.0", not "1".
 *
 * The format to use to parse the file with is selected automatically according
 * to the first line in the file. You can not mix formats within a file.
 *
 * The input file may be GZip compressed, it will be automatically decompressed
 * on the fly.
 *
 * @author Paul Jakma
 * @param <N> Node type
 * @param <E> Edge type.
 */
public class as_graph_reader<N,E> extends adjacency_list_reader<N,E> {
  /**
   * Similar to the adjacency_list_reader.labeler, to allow the creation of
   * node and edge labels to be delegated back to the user.
   */
  public interface latency_labeler<N,E> 
         extends adjacency_list_reader.labeler<N,E> {
    public E getEdge (N from, N to, double latency);
  }
  
  final static String sre_double = "(([.]\\d+)|\\d+([.]\\d+)?)";
  final static String sre_asn = "\\d+";
  final static String sre_asdot = "\\d+[.]\\d+";
  final static String sre_tstamp = "\\d+";
  final static String sre_index = "\\d+";
  final static String sre_mrt = "\\S+";
  final static String sre_aslatency = "(" +sre_asn+ "),(" + sre_double + ")";
  final static Pattern re_asdot = Pattern.compile ("(\\d+)[.](\\d+)");
  final static Pattern re_asdot_irlbug = Pattern.compile ("(-?\\d+)[.](\\d+)");
  private latency_labeler<N,E> labeler = null;
  
  private String normalise_asn (String asn) {
    Matcher res;
    debug.println ("normalise " + asn);
    if ((res = re_asdot_irlbug.matcher (asn)).matches ()) {
      long high = Integer.parseInt (res.group (1));
      /* IRL ASdot bugginess */
      if (high < 0)
        high += 65536;
      int low = Integer.parseInt (res.group (2));
      long normas = (high << 16) | low;
      debug.println ("asdot " + asn + " hi/lo: " + (high << 16) + "/" + low + " to " + normas);
      return String.valueOf (normas);
    }
    return asn;
  }
  
  @Override
  protected void init_acceptpatterns () {
      acceptpatterns = new acceptpattern []{
        /* "ASN internal_latency [to_ASN_edge latency]+" format */
        new acceptpattern ("ASN latency list",
                           "("+sre_asn+")\\s("
                                   +sre_double+ ")\\s("
                                   +sre_aslatency+ ")+\\s*$") {
          @Override
          void parse_line (MatchResult m) {
            N from_as = labeler.getNode (m.group (1));
            @SuppressWarnings ("unused")
            double from_as_latency = Double.parseDouble (m.group (2));

            for (int i = 3; i < m.groupCount (); i++) {
              //MatchResult m = scr.match ();
              N to_as = labeler.getNode (m.group (i));
              double to_as_latency = Double.parseDouble (m.group (i + 1));
              E newlabel = labeler.getEdge (from_as, to_as, to_as_latency);

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
        /* "ASN ASN [optional count]" format */
        new acceptpattern ("ASN ASN", 
                           String.format ("(%s)\\s+(%s)\\s*(\\d+)?\\s*",
                                          sre_asn, sre_asn) ) {
          @Override
          void parse_line (MatchResult m) {
            N from_as = labeler.getNode (m.group (1));
            N to_as = labeler.getNode (m.group (2));

            debug.printf ("setting %s (%s) to %s (%s)\n",
                         from_as, m.group (1), to_as, m.group (2));

            network.remove (from_as, to_as);
            network.set (from_as, to_as, labeler.getEdge (from_as, to_as));
          }
        },
        /* Old IRL topology links format is supposed to be:
         * "ASN to_AS first_seen last_seen AS_PATH_pos [MRT record]"
         * timestamps are unix format.
         * 
         * But some files in 2011 seem to have:
         * "ASN tstamp tstamp to_ASN MRT-record"
         * 
         * Old IRL appears to have further oddities/bugs relating to ASDot.
         */
        new acceptpattern ("IRL",
                           String.format ("(?:%s)|(?:%s)",
                             String.format ("(%s|[-]?%s)\\s+(%s|[-]?%s)\\s+(%s)\\s+(%s)\\s+(%s).*$",
                                            sre_asn, sre_asdot, sre_asn, sre_asdot, 
                                            sre_tstamp, sre_tstamp, sre_index),
                             String.format ("(%s|[-]?%s)\\s+(?:%s)\\s+(?:%s)\\s+(%s|[-]?%s).*$",
                                            sre_asn, sre_asdot, sre_tstamp, sre_tstamp,
                                            sre_asn, sre_asdot))
                          ) {
          @Override
          void parse_line (MatchResult m) {
            /* for now, don't do anything with the extra info 
             * over AS AS format
             */
            
            /* This guff is needed cause of files in IRL data set that don't
             * match their described format, hence we have to match 2 formats.
             */
            int first = 1;
            for (int i = 1; i <= m.groupCount (); i++) {
              if (m.group (i) != null) {
                first = i;
                break;
              }
            }
            for (int i = 0; i <= m.groupCount (); i++)
              debug.printf ("match %d: %s\n", i, m.group (i));
            
            String sfrom = m.group (first);
            String sto = m.group (first + 1);
            N from_as = labeler.getNode (normalise_asn (sfrom));
            N to_as = labeler.getNode (normalise_asn (sto));

            debug.printf ("setting %s (%s) to %s (%s)\n",
                         from_as, sfrom, to_as, sto);

            network.remove (from_as, to_as);
            network.set (from_as, to_as, labeler.getEdge (from_as, to_as));
          }
        }
      };
  }

  public as_graph_reader (Graph<N,E> network, 
                       latency_labeler<N,E> labeler,
                       String fname) throws IllegalArgumentException,
                                            FileNotFoundException,
                                            IOException {
    super (network, fname);
    this.labeler = labeler;
  }
  
  public as_graph_reader (Graph<N,E> network,
                          latency_labeler<N,E> labeler,
                          InputStream is) {
    super (network, is);
    this.labeler = labeler;
  }
}
