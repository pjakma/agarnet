/* This file is part of 'agarnet'
 *
 * Copyright (C) 2010, 2011, 2018, 2019 Paul Jakma
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
import java.util.Set;
import java.util.HashSet;
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
  static final Color line_highlight = new Color (240,143, 143);
  private options opts = new options().textlabels (true)
                                      .always_show_tips (false);;
  protected AffineTransform model_transform;
  protected double noderadius;
  protected mouse_state mouse_state = new mouse_state ();
  
  public static class options {
    public boolean always_show_tips = false;
    public boolean textlabels = true;
    /* Looks good, but is fantastically slow on OpenJDK for some reason */
    public boolean antialiasing = false;
    
    public options always_show_tips (boolean v) {
      always_show_tips = v;
      return this;
    }
    public options textlabels (boolean v) {
      textlabels = v;
      return this;
    }
    public options antialiasing (boolean v) {
      antialiasing = v;
      return this;
    }    
  }

  /* Don't have an index to map mouse (x,y) to nodes.
   * And probably don't want one either.
   *
   * Instead the mouse-related state is simply recorded here in the event.
   * Then the actions are driven by the paint loop, which iterates over
   * the nodes and calls mouse-state.update for each nodes to apply
   * the mouse-state to the node, as necessary.
   */
  protected class mouse_state implements java.awt.event.MouseListener, 
                                         java.awt.event.MouseMotionListener {
    private Point position = new Point ();
    private boolean is_in_panel = false;
    private boolean pressed = false;
    private H node_pressed = null;
    private boolean dragging = false;
    private boolean clicked = false;
    
    Set<H> nodes_selected = new HashSet<H> ();
    // Deliberately restricted to one, to minimise the cost
    // of the "is this a node_over?" check in the update loop.
    //
    // Overlapping nodes, implementation details will determine which
    // node can be selected. I'm fine with that. 
    //
    // TBD: Implement a GUI with an API with sprites/nodes where the
    // API can directly determine which object a mouse event applies to.
    // E.g., JavaFX?
    private H node_over = null;
    
    @Override
    public void mouseExited (MouseEvent ev) {
      is_in_panel = false;
      pressed = false;
    }

    @Override
    public void mouseEntered (MouseEvent ev) {
      is_in_panel = true;
      mouse_to_model (ev, position);
    }
    
    @Override
    public void mouseClicked(MouseEvent ev) {
      clicked = true;
    }

    @Override
    public void mousePressed (MouseEvent ev) {
      pressed = true;
    }

    @Override
    public void mouseReleased (MouseEvent ev) {
      pressed = false;
      dragging = false;
      node_pressed = null;
    }
    public void mouseMoved (MouseEvent ev) {
      mouse_to_model (ev, position);
      anipanel.this.repaint ();
    }

    @Override
    public void mouseDragged (MouseEvent ev) {
      dragging = true;
      mouseMoved (ev);
    }
    
    Point position () {
      if (is_in_panel)
        return position;
      return null;
    }
    
    protected boolean is_over (H p) {
      Point2D pos = p.getPosition ();
      
      if (is_in_panel) {
        if (position.x > pos.getX () - noderadius &&
            position.x < pos.getX () + noderadius &&
            position.y > pos.getY () - noderadius &&
            position.y < pos.getY () + noderadius) {
          return true;
        }
      }
      return false;
    }
    
    /* Host specific update */
    protected void update (H p) {
      if (!is_over (p)) {
        if (node_over == p)
          node_over = null;
        return;
      }
      node_over = p;
    }
    
    /* Per animation loop general update */
    protected void update () {
      /* Event state transitions are:
       * 
       *   Pressed -> Released -> Clicked
       * | Pressed -> dragging -> Released
       *
       */
       
      /* Transition into clicked | dragging, record the pressed node */
      if (dragging || clicked) {
        if (node_over != null && node_pressed == null) {
          node_pressed = node_over;
        }
      }
      
      /* handle clicks */
      if (clicked && node_pressed != null) {
        /* Selection state change for the node */
        if (!nodes_selected.remove (node_pressed))
          nodes_selected.add (node_pressed);
        node_pressed = null;
        clicked = false;
      }
      
      /* handle drags */
      if (dragging && node_pressed != null) {
        node_pressed.setPosition (new Vector2D (position));
      }
    }
  }
  
  public anipanel (Simulation2D<I,H> s) {
    this.s = s;
    
    
    s.addObserver (this);
    
    model_transform = create_model_transform ();
    noderadius = noderadius ();
    
    //this.setSize (1000, 600);
    this.setBackground (new Color (0,0,0));
    this.setFocusable (true);
    
    this.addMouseListener (mouse_state);
    this.addMouseMotionListener (mouse_state);
    
    this.addComponentListener(new java.awt.event.ComponentAdapter () {
      @Override
      public void componentResized (ComponentEvent e) {
        model_transform = create_model_transform ();
        noderadius = noderadius ();
      }
    });
    
    this.addKeyListener(new java.awt.event.KeyListener () {
      @Override
      public void keyTyped(java.awt.event.KeyEvent e) {
        System.out.println("key typed!" + e.getKeyChar ());
        switch (e.getKeyChar ()) {
          case 't':
          case 'T':
            opts.always_show_tips = !opts.always_show_tips;
            break;
          case 'a':
          case 'A':
            opts.antialiasing = !opts.antialiasing;
            break;
        }
      }
      @Override
      public void keyPressed (java.awt.event.KeyEvent e) {
        System.out.println("key pressed!" + e.getKeyChar ());
      }
      @Override
      public void keyReleased (java.awt.event.KeyEvent e) {
        System.out.println("key released!" + e.getKeyChar ());
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
  

  protected void drawEdge (Graphics2D g, Edge<H, link<H>> edge, 
                           H p1, Point2D pos1, Point2D pos2,
                           Color line_color) {
    Vector2D vec = new Vector2D (pos2);

    vec.minus (pos1);
    vec.times (0.5);
    vec.plus (pos1);
    
    g.setColor (line_color);
    
    g.drawLine ((int) pos1.getX (), (int) pos1.getY (),
                (int) pos2.getX (), (int) pos2.getY ());
    if (opts.textlabels) {
      g.drawString ("bw: " + edge.label ().get (p1).bandwidth,
                    (int) vec.x, (int) vec.y - (int) noderadius/2);
      g.drawString ("lat: " + edge.label ().get (p1).latency,
                    (int) vec.x, (int) vec.y + (int) noderadius/2);
    }
  }
  
  protected void drawNode (Graphics2D g, H p) {
    boolean mouse_over = false;
    
    g.setColor (p.colour ());
    
    mouse_state.update (p);
    Point2D pos = p.getPosition ();
    
    g.fillOval ((int)(pos.getX () - noderadius),
                (int)(pos.getY () - noderadius),
                (int)noderadius * 2, (int)noderadius * 2); 
    
    g.setColor (Color.gray);
    
    if (opts.textlabels)
      g.drawString (Integer.toString ((int)p.getSize ()),
                    (int)pos.getX () - (int)noderadius/2,
                    (int)pos.getY () + (int)noderadius/4);

    
  }

  protected void draw_node_tip (Graphics2D g, H p) {
    FontMetrics fm = g.getFontMetrics();
    String text = p.getId ().toString ();
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
  
  protected void paintComponent (Graphics g1) {
    Graphics2D g = (Graphics2D) g1;
    LinkedList<H> nodes_mouseover = new LinkedList<> ();
    
    super.paintComponent (g);
    
    if (opts.antialiasing) {
      g.setRenderingHint (RenderingHints.KEY_ANTIALIASING,
                          RenderingHints.VALUE_ANTIALIAS_ON);
    }
    AffineTransform save = g.getTransform ();
    g.transform (model_transform);
    
    g.setFont (new Font (Font.SANS_SERIF, Font.PLAIN, (int) noderadius));
    
    try {
      mouse_state.update ();
      
      /* draw the edges */
      for (H p : s.network) {
        Point2D pos = p.getPosition ();
        
        mouse_state.update (p);
        
        /* nodes under the mouse have drawing deferred to end
         * if there were a lot, could sort, but there shouldn't be. 
         */
        if (mouse_state.is_over (p)) {
          nodes_mouseover.add (p);
          continue;
        }
        
        for (Edge<H, link<H>> edge : s.network.edges (p)) {
          /* we only want to process an edge once, luckily edges in an
           * undirected graph still have a polarity we can filter on
           */
          if (edge.from () != p)
            continue;
          
          H p2 = edge.to ();
          Point2D pos2 = p2.getPosition ();

          Color line_colour = line;
          
          if (edge.label ().size () > 0)
            line_colour = line_used;
          
          drawEdge (g, edge, p, pos, pos2, line_colour);
        }

      }
      
      /* Now draw the edges of mouse-over nodes, so they're on top */
      for (H p : nodes_mouseover) {
        for (Edge<H, link<H>> edge : s.network.edges (p)) {
          if (edge.from () != p)
            continue;
          Point2D pos = p.getPosition ();
          drawEdge (g, edge, p, pos, edge.to().getPosition (), line_highlight);
        }
      }
      
      /* draw the nodes, in a second pass so they don't get edges drawn over,
       * and so all nodes (inc. disconnected) are drawn
       */
      for (H p : s.network) {
        drawNode (g, p);
      }
      
      /* Another pass if tool tip popups should always be shown */
      if (opts.always_show_tips) {
        for (H p : s.network)
          if (!mouse_state.is_over (p))
            draw_node_tip (g, p);
      }
      
      /* draw tip for mouse-over nodes on top */
      for (H p : nodes_mouseover) {
        draw_node_tip (g, p);      
      }
    } finally {
      g.setTransform (save);
    }
    
  }

  public void update (Observable arg0, Object arg1) {
    repaint ();
  }
}
