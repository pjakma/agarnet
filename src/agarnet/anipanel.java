package agarnet;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.FontMetrics;

import java.util.LinkedList;
import java.util.Observable;
import java.util.Observer;
import java.io.Serializable;
import java.awt.event.ComponentEvent;

import javax.swing.JPanel;

import org.nongnu.multigraph.Edge;
import org.nongnu.multigraph.layout.Vector2D;

import agarnet.framework.Simulation2D;
import agarnet.link.link;
import agarnet.protocols.host.AnimatableHost;

public class anipanel<I extends Serializable, H extends AnimatableHost<I,H>>
		extends JPanel
		implements Observer {
  /**
   * 
   */
  private static final long serialVersionUID = 4635455780045359317L;
  Simulation2D<I,H> s;
  static final Color line = new Color (160,0,0);
  static final Color line_used = new Color (230,140,0);
  private boolean mouse_in_panel = false;
  private options opts = new options().textlabels (true)
                                      .always_show_tips (false);;
  private Point mouse_p = new Point();
  private Point mouse_pressed_p;
  private H host_dragging;
  protected AffineTransform model_transform;
  protected double noderadius;
  
  public static class options {
    public boolean always_show_tips = false;
    public boolean textlabels = true;
    
    public options always_show_tips (boolean v) {
      always_show_tips = v;
      return this;
    }
    public options textlabels (boolean v) {
      textlabels = v;
      return this;
    }
  }
  
  public anipanel (Simulation2D<I,H> s) {
    this.s = s;
    
    
    s.addObserver (this);
    
    model_transform = create_model_transform ();
    noderadius = noderadius ();
    
    //this.setSize (1000, 600);
    this.setBackground (new Color (0,0,0));
    
    this.addMouseListener (new java.awt.event.MouseAdapter () {
      
      @Override
      public void mouseExited (MouseEvent e) {
        mouse_in_panel = false;
      }

      @Override
      public void mouseEntered (MouseEvent e) {
        mouse_in_panel = true;
      }

      @Override
      public void mousePressed (MouseEvent e) {
        super.mousePressed (e);
        mouse_pressed_p = new Point ();
        mouse_to_model (e, mouse_pressed_p);
      }

      @Override
      public void mouseReleased (MouseEvent e) {
        super.mouseReleased (e);
        host_dragging = null;
        mouse_pressed_p = null;
      }
    });
    
    this.addMouseMotionListener (new java.awt.event.MouseMotionAdapter () {
      public void mouseMoved (MouseEvent e) {
        super.mouseMoved (e);
        mouse_to_model (e, mouse_p);
        anipanel.this.repaint ();
      }

      @Override
      public void mouseDragged (MouseEvent e) {
        super.mouseDragged (e);
        mouse_to_model (e, mouse_p);
        anipanel.this.repaint ();
      }
      
    });
    
    this.addComponentListener(new java.awt.event.ComponentAdapter () {
      @Override
      public void componentResized (ComponentEvent e) {
        model_transform = create_model_transform ();
        noderadius = noderadius ();
      }
    });
  }
  
  public anipanel (Simulation2D<I,H> s, anipanel.options opts) {
    this (s);
    this.opts = opts;
  }
  
  private void mouse_to_model (MouseEvent ev, Point m) {
    /* Mouse co-ordinates were reported with the default transform,
     * in the abstract/scaled Java Graphics pixel space
     */
    try {
      model_transform.inverseTransform (ev.getPoint (), m);
    } catch (NoninvertibleTransformException e) {
      e.printStackTrace();
      System.exit (1);
    }
  }
  
  public double noderadius () {
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
    
    noderadius = Math.min (noderadius, 0.1 * dmin/Math.sqrt (s.network.size ()));
    //noderadius = 0.06 * dmin/Math.sqrt (s.network.size ());
    
    //debug.println ("" + noderadius);
    
    return noderadius;
  }
  
  /** Transform from the simulation model co-ordinate reference system
   * to the Java graphics context pixel space.
   */ 
  protected AffineTransform create_model_transform () {
    /* In the simulation, the origin is the centre of the model bound (so,
     * e.g., a node could be at (-100,-50) or (-170,50), etc.).  The Java
     * graphics window has (0,0) top-left, as is common.
     *
     * Java thankfully provides a transform class to interpose between the
     * two, and translate back and forth.
     *
     * Scale the sim. model to fit into the window, and
     * linearly translate the co-ords from sim. space to java space. 
     *
     * HiDPI complicates things further.  HiDPI creates an additional level
     * of scaling between the device pixels and the 'pixels' the AWT and
     * Java Graphics API exposes to the user.  
     *
     * E.g., setting GDK_SCALE (e.g., for HiDPI displays) will set an
     * underlying default transform in the GraphicsConfiguration, which
     * should be preserved, otherwise the drawing will be shrunk relative to
     * the window and mouse co-ords.  Once GDK_SCALE is set (i.e., no longer
     * an identity transform) we end up with *3* co-ord systems:
     *
     * 1. The simulation model.
     * 2. The Java Graphics2D abstract pixel space (as scaled).
     * 3. The device pixels.
     *
     * The default transform on the Graphics2D context effectively creates
     * 2.  We modify that further here below to be able to issue draws in
     * terms of 1, correct for 3.  However, mouse events are reported still
     * in terms of 2 - as sadly our transform gets cleared.
     */
    Dimension d = this.getSize ();
    double scaleX = (double) d.width / s.model_size.width,
           scaleY = (double) d.height / s.model_size.height;
    
    AffineTransform t = new AffineTransform ();
    // (0,0) at centre -> (0,0) top-left
    t.translate (d.getWidth () / 2, d.getHeight () / (2));
    t.scale (Math.min (scaleX, scaleY), Math.min (scaleX, scaleY));
    
    return t;
  }

  protected void drawNode (Graphics2D g, H p, LinkedList<H> nodes_showtip) {
    boolean show_tip = false;
    
    g.setColor (p.colour ());
    
    Point2D pos = p.getPosition ();
    
    if (mouse_in_panel) {
      if (mouse_p.x > pos.getX () - noderadius &&
          mouse_p.x < pos.getX () + noderadius &&
          mouse_p.y > pos.getY () - noderadius &&
          mouse_p.y < pos.getY () + noderadius) {
        show_tip = true;
        
        if (mouse_pressed_p != null) {
          mouse_pressed_p = null;
          host_dragging = p;
        }
      }
      if (host_dragging != null) {
        host_dragging.setPosition (new Vector2D (mouse_p));
        pos = p.getPosition ();
      }
    }
    
    g.fillOval ((int)(pos.getX () - noderadius),
                (int)(pos.getY () - noderadius),
                (int)noderadius * 2, (int)noderadius * 2); 
    
    g.setColor (Color.gray);
    
    if (opts.textlabels)
      g.drawString (Integer.toString ((int)p.getSize ()),
                    (int)pos.getX () - (int)noderadius/2,
                    (int)pos.getY () + (int)noderadius/4);

    
    if (opts.always_show_tips || show_tip) {
      /* defer drawing of tips to end, to ensure tip box is on top. */
      nodes_showtip.add (p);
    }
  }
  
  protected void paintComponent (Graphics g1) {
    Graphics2D g = (Graphics2D) g1;
    LinkedList<H> nodes_showtip = new LinkedList<> ();      
    
    super.paintComponent (g);
    
    /* Looks good, but is fantastically slow for some reason */
    g.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
    AffineTransform save = g.getTransform ();
    g.transform (model_transform);
    
    g.setFont (new Font (Font.SANS_SERIF, Font.PLAIN, (int) noderadius));
    
    try {
      /* draw the edges */
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
          if (opts.textlabels) {
            g.drawString ("bw: " + edge.label ().get (p).bandwidth,
                          (int) vec.x, (int) vec.y - (int) noderadius/2);
            g.drawString ("lat: " + edge.label ().get (p).latency,
                          (int) vec.x, (int) vec.y + (int) noderadius/2);
          }
          
        }
      }
      
      /* draw the nodes, in a second pass so they don't get edges drawn over,
       * and so all nodes (inc. disconnected) are drawn
       */
      for (H p : s.network) {
        drawNode (g, p, nodes_showtip);
      }
      
      /* tool tip popup */
      for (H p : nodes_showtip) {
        String text = p.getId ().toString ();
        FontMetrics fm = g.getFontMetrics();
        Rectangle2D rect = fm.getStringBounds(text, g);
        Point2D pos = p.getPosition ();
        
        Point2D rpos = new Point2D.Double (
          pos.getX () - (rect.getWidth() / 2),
          pos.getY () + (noderadius * 1.05)
        );          
        g.setColor(Color.gray);
        g.fillRect ((int)rpos.getX (),
                    (int)rpos.getY (),
                    (int)rect.getWidth (), (int) rect.getHeight ());
        
        g.setColor (Color.red);
        g.drawString (text,
                      (int)rpos.getX (),
                      (int)(rpos.getY () + fm.getAscent()));
      }
    } finally {
      g.setTransform (save);
    }
    
  }

  public void update (Observable arg0, Object arg1) {
    repaint ();
  }
}
