package agarnet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JPanel;

import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.layout.Vector2D;

import agarnet.framework.Simulation;
import agarnet.link.link;
import agarnet.protocols.host.AnimatableHost;

public class anipanel<I, H extends AnimatableHost<Long,H,anipanel.Node>>
		extends JPanel
		implements Observer {
  /**
   * 
   */
  private static final long serialVersionUID = 4635455780045359317L;
  Simulation<I,H> s;
  static final Color line = new Color (160,0,0);
  static final Color line_used = new Color (230,140,0);
  static final Color peer = Color.BLUE.darker ();
  static final Color seed = Color.BLUE.brighter ().brighter ();
  static final Color leech = Color.GREEN;
  private boolean mouse_in_panel = false;
  private boolean textlabels = true;
  private int mouse_x;
  private int mouse_y;
  
  enum Node {
    seed, leech, peer;
    private final static Color colours[] = {
      Color.CYAN.brighter ().brighter ().brighter (),
      Color.GREEN,
      Color.YELLOW,
    };
    private static Map<String,Node> peer2node = new HashMap <String,Node> ();
    static {
      peer2node.put (agarnet.protocols.peer.peer.class.getName (), peer);
      peer2node.put (agarnet.protocols.peer.seed.class.getName (), seed);
      peer2node.put (agarnet.protocols.peer.leech.class.getName (), leech);
    }
    public Color colour () {
      return colours[this.ordinal ()];
    }
    public static Node toNode (String name) {
      Node n = peer2node.get (name);
      return (n != null) ? n : peer;
    }
  }
  
  anipanel (Simulation<I,H> s) {
    this.s = s;
    
    s.addObserver (this);
    
    //this.setSize (1000, 600);
    this.setBackground (new Color (0,0,0));
    this.addMouseListener (new java.awt.event.MouseAdapter () {
      @Override
      public void mouseExited (MouseEvent e) {
        mouse_in_panel = false;
      }

      @Override
      public void mouseMoved (MouseEvent e) {
        mouse_x = e.getX ();
        mouse_y = e.getY ();
      }

      @Override
      public void mouseEntered (MouseEvent e) {
        mouse_in_panel = true;
      }
        
      }
    );
  }
  
  double noderadius () {
    double noderadius;
    int dmin = Math.min (s.model_size.height, s.model_size.width);
    
    /* Scale the radius so as to keep the area constant relative to 
     * area of largest square in model.
     * 
     * I.e. the nodesize should appear to stay the same, as we see it,
     * regardless of whether the model area is increased/reduced.
     */
    noderadius = dmin*dmin;
    noderadius = Math.sqrt (noderadius);
    noderadius /= Math.PI;
    /* Nodes should be scaled to the number of nodes in the model,
     * to a maximum size of a low % the min model dimension.
     */
    noderadius *= 0.05;
    
    //debug.println ("dmin: " + dmin + ", root(size): "
    //                + Math.sqrt (s.network.size ()));
    
    //noderadius = Math.min (noderadius, 0.08 * dmin/Math.sqrt (s.network.size ()));
    noderadius = 0.06 * dmin/Math.sqrt (s.network.size ());
    
    //debug.println ("" + noderadius);
    
    return noderadius;
  }
  protected void paintComponent (Graphics g1) {
    Dimension d = this.getSize ();
    Graphics2D g = (Graphics2D) g1;
    double noderadius = noderadius ();
    
    double scaleX = (double) d.width / s.model_size.width,
           scaleY = (double) d.height / s.model_size.height;
    
    super.paintComponent (g);
    
    /* Looks good, but is fantastically slow for some reason */
    //g.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
    //                    RenderingHints.VALUE_ANTIALIAS_ON);
    
    AffineTransform t
      = AffineTransform.getTranslateInstance (d.getWidth () / 2,
                                              d.getHeight () / 2);
    t.scale (Math.min (scaleX, scaleY), Math.min (scaleX, scaleY));
    
    AffineTransform save = g.getTransform ();
    g.setTransform (t);
    
    if (textlabels)
      g.setFont (new Font (Font.SANS_SERIF, Font.PLAIN, (int) noderadius));
    
    try {
      for (H p : s.network) {
        Point2D pos = p.getPosition ();
        
        for (Edge<H, link<H>> edge : s.network.edges (p)) {
          /* we only want to process an edge once, luckily edges in an
           * undirected graph still have a polarity we can filter on
           */
          if (edge.from () != p)
            continue;
          
          Point2D pos2 = edge.to ().getPosition ();
          Vector2D vec = new Vector2D (pos2);
          vec.minus (pos);
          vec.times (0.5);
          vec.plus (pos);
          
          g.setColor (line);
          if (edge.label ().size () > 0)
            g.setColor (line_used);
          else
            g.setColor (line);
          
          g.drawLine ((int) pos.getX (), (int) pos.getY (),
                      (int) pos2.getX (),(int) pos2.getY ());
          if (textlabels) {
            g.drawString ("bw: " + edge.label ().get (p).bandwidth,
                          (int) vec.x, (int) vec.y - (int) noderadius/2);
            g.drawString ("lat: " + edge.label ().get (p).latency,
                          (int) vec.x, (int) vec.y + (int) noderadius/2);
          }
          
        }
      }
      
      Point2D.Double mousep = new Point2D.Double ();
      
      if (mouse_in_panel)
        t.inverseTransform (new Point2D.Double (mouse_x, mouse_y), mousep);
      
      for (H p : s.network) {
        boolean show_tip = false;
        
        g.setColor (p.get_type ().colour ());
        
        Point2D pos = p.getPosition ();
        
        if (mouse_in_panel) {
          if (mouse_x > pos.getX () - noderadius &&
              mouse_x < pos.getX () + noderadius &&
              mouse_y > pos.getY () - noderadius &&
              mouse_y < pos.getY () + noderadius)
            show_tip = true;
        }
        
        g.fillOval ((int)(pos.getX () - noderadius),
                    (int)(pos.getY () - noderadius),
                    (int)noderadius * 2, (int)noderadius * 2); 
        
        g.setColor (Color.gray);
        
        if (textlabels)
          g.drawString (Integer.toString ((int)p.getSize ()),
                        (int)pos.getX () - (int)noderadius/2,
                        (int)pos.getY () + (int)noderadius/4);
        
        if (show_tip) {
          g.fillRect ((int)(pos.getX () - noderadius),
                      (int)(pos.getY () + noderadius), 
                      (int) noderadius * 2, (int)noderadius * 2);
          g.setColor (Color.red);
          g.drawString (Integer.toString (Math.round (p.getSize ())),
                        (int)(pos.getX () - noderadius * 0.75),
                        (int)(pos.getY () + noderadius * 0.75));
        }
        
      }
    } catch (NoninvertibleTransformException e) {
      e.printStackTrace();
      System.exit (1);
    } finally {
      g.setTransform (save);
    }
    
  }

  public void update (Observable arg0, Object arg1) {
    repaint ();
  }
}
