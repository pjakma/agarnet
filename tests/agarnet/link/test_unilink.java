package agarnet.link;

import java.util.Arrays;
import org.junit.* ;
//import agarnet.link.*;
import org.nongnu.multigraph.debug;
import static org.junit.Assert.* ;

public class test_unilink {
  unilink<String> link;

  private byte [] msg (int seq, int pad) {
    final byte [] header
        = new byte [] {(byte) 0xde, (byte) 0xad,
                       (byte) 0xbe, (byte) 0xef,
                       (byte) (seq >> 24 & 0xff),
                       (byte) (seq >> 16 & 0xff),
                       (byte) (seq >> 8  & 0xff),
                       (byte) (seq & 0xff),
    };
    byte [] msg = new byte [header.length + pad];
    Arrays.fill (msg, (byte) 0xff);
    System.arraycopy (header, 0, msg, 0, header.length);
    return msg;
  }

  private boolean cmp_msg (byte [] msga, byte [] msgb) {
    return Arrays.equals (msga, msgb);
  }

  /* basic check that latency works from 1 up */
  private void testlatency (String test, int latency, int bandwidth, int padding) {
    link = new unilink<String> ("1", bandwidth, latency);
    int nummsgs = latency;

    System.out.println ("\ntest " + test + " latency, "
                         + " latency: " + latency
                         + " bandwidth: " + bandwidth
                         + " padding: " + padding);

    debug.println ("add messages");
    for (int i = 0; i < nummsgs; i++) {
      assertTrue (link.offer (msg (i, padding)));
      link.tick ();
      debug.println ("after tick " + i + " " + link);
    }

    /* how many more ticks needed for bandwidth? */
    int msgticks = (int) Math.ceil (msg (1,padding).length / bandwidth)
                   + latency;
    int totalticks = msgticks * nummsgs;

    for (int i = nummsgs; i <= totalticks; i++) {
      link.tick ();
      debug.println ("tick over " + link);
    }

    debug.println ("dequeue");
    for (int i = 0; i < nummsgs; i++) {
      byte [] m = link.poll ();
      debug.println ("after dequeue " + i + " " + link);
      assertTrue (m != null);
      assertTrue (cmp_msg (m, msg(i,padding)));
    }
    assertTrue (link.poll () == null);
  }

  /* check the txbuf works, i.e. pile messages up */
  private void testtxbuf (String test, int latency, int bandwidth, int padding) {
    link = new unilink<String> ("1", bandwidth, latency);
    int msgsz = msg (0, padding).length;

    System.out.println ("\ntest " + test + " txbuf: "
                         + " latency: " + latency
                         + " bandwidth: " + bandwidth
                         + " padding: " + padding);

    debug.println ("add messages");
    for (int i = 0; i < latency; i++) {
      assertTrue (link.offer (msg (i, padding)));
      debug.println ("\tmsg " + i + " " + link);
    }
    debug.println ("add ticked messages");
    for (int i = latency; i < latency * 2; i++) {
      assertTrue (link.offer (msg (i, padding)));
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    /* how many more ticks needed for bandwidth? */
    int nummsgs = latency * 2;
    int msgticks = (int) Math.ceil (msgsz / bandwidth) + latency;
    int totalticks = msgticks * nummsgs;

    debug.println ("tick across");
    for (int i = latency; i <= totalticks; i++) {
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    debug.println ("dequeue");
    for (int i = 0; i < nummsgs; i++) {
      byte [] m = link.poll ();
      debug.println ("\tafter dequeue " + i + " " + link);
      assertTrue (m != null);
      assertTrue (cmp_msg (m, msg(i,padding)));
    }
    assertTrue (link.poll () == null);
  }

  /* check the txbuf works, i.e. pile messages up
   *
   * more complex version that tries to have both txbuf and rxbuf piled
   * up
   */
  private void testtxbuf2 (String test, int latency, int bandwidth, int padding) {
    link = new unilink<String> ("1", bandwidth, latency);

    System.out.println ("\ntest " + test + " txbuf cmplex: "
                         + " latency: " + latency
                         + " bandwidth: " + bandwidth
                         + " padding: " + padding);

    debug.println ("add messages");
    for (int i = 0; i < latency; i++) {
      assertTrue (link.offer (msg (i, padding)));
      debug.println ("\tmsg " + i + " " + link);
    }
    debug.println ("add ticked messages");
    for (int i = latency; i < latency * 2; i++) {
      assertTrue (link.offer (msg (i, padding)));
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    /* how many more ticks needed for bandwidth? */
    int nummsgs = latency * 2;
    int msgticks = (int) Math.ceil (msg (1,padding).length / bandwidth)
                   + latency;
    int totalticks = msgticks * nummsgs;

    debug.println ("tick across");
    for (int i = latency; i < totalticks; i++) {
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    debug.println ("dequeue");
    for (int i = 0; i < nummsgs; i++) {
      byte [] m = link.poll ();
      debug.println ("\tafter dequeue " + i + " " + link);
      assertTrue (m != null);
      assertTrue (cmp_msg (m, msg(i,padding)));
      assertTrue (link.offer (m));
      debug.println ("\treinserted " + i + " " + link);
    }

    debug.println ("tick across again");
    for (int i = 0; i < totalticks; i++) {
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    debug.println ("dequeue again");
    for (int i = 0; i < nummsgs; i++) {
      byte [] m = link.poll ();
      debug.println ("\tafter dequeue " + i + " " + link);
      assertTrue (m != null);
      assertTrue (cmp_msg (m, msg(i,padding)));
    }

    assertTrue (link.poll () == null);
  }

  /* ring messages around several times
   */
  private void testring (String test, int latency, int bandwidth, int padding) {
    link = new unilink<String> ("1", bandwidth, latency);

    System.out.println ("\ntest " + test + " txbuf ring: "
                         + " latency: " + latency
                         + " bandwidth: " + bandwidth
                         + " padding: " + padding);

    debug.println ("add messages");
    for (int i = 0; i < latency; i++) {
      assertTrue (link.offer (msg (i, padding)));
      debug.println ("\tmsg " + i + " " + link);
    }
    debug.println ("add ticked messages");
    for (int i = latency; i < latency * 2; i++) {
      assertTrue (link.offer (msg (i, padding)));
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }
    
    /* how many more ticks needed for bandwidth? */
    int nummsgs = latency * 2;
    int msgticks = (int) Math.ceil (msg (1,padding).length / bandwidth)
                   + latency;
    int totalticks = msgticks * nummsgs;

    debug.println ("tick across");
    for (int i = latency; i < totalticks; i++) {
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    debug.println ("ring around");
    for (int i = 0; i < nummsgs * 10; i++) {
      int msgid = i % (latency*2);
      byte [] m = link.poll ();
      debug.println ("\tafter dequeue " + i + " " + msgid + " " + link);
      assertTrue (m != null);
      assertTrue (cmp_msg (m, msg(msgid,padding)));
      assertTrue (link.offer (m));
      debug.println ("\treinserted    " + i + "   " + link);
      link.tick ();
    }
    
    debug.println ("tick across again");
    for (int i = 0; i < totalticks; i++) {
      link.tick ();
      debug.println ("\tafter tick " + i + " " + link);
    }

    debug.println ("dequeue again");
    for (int i = 0; i < nummsgs; i++) {
      byte [] m = link.poll ();
      debug.println ("\tafter dequeue " + i + " " + link);
      assertTrue (m != null);
      assertTrue (cmp_msg (m, msg(i,padding)));
    }

    assertTrue (link.poll () == null);
  }

  public void testlatency() {
    for (int i = 1; i <= 5; i++)
      testlatency ("", i, msg (1, 0).length * i * 10, 0);
  }

  public void testtxbuf() {
    for (int i = 1; i <= 5; i++)
      testtxbuf ("", i, msg (1, 0).length * i * 10, 0);
    for (int i = 1; i <= 5; i++)
      testtxbuf2 ("", i, msg (1, 0).length * i * 10, 0);
    for (int i = 1; i <= 5; i++)
      testring ("", i, msg (1, 0).length * i * 10, 0);
  }
  
  //@Test(expected=AssertionError.class)
  @Test public void testbw () {
    debug.level (debug.levels.DEBUG);
    for (int b = 0; b <= 8; b += 2)
      for (int lat = 1; lat <= 10; lat++) {
        int bw = (int)Math.ceil(Math.pow (2, b));
        for (int pad = 0; pad <= bw * 3; pad += pad + 1) {
          System.out.println ("testbw " + bw + " " + pad);
          testlatency ("bw", lat, bw, pad);
          testtxbuf ("bw", lat, bw, pad);
          testtxbuf2 ("bw", lat, bw, pad);
          testring ("bw", lat, bw, pad);
        }
      }
  }
}
