/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 */

package net.java.joglutils.msg.test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.*;
import java.awt.image.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.event.*;

import javax.media.opengl.*;

import net.java.joglutils.msg.actions.*;
import net.java.joglutils.msg.collections.*;
import net.java.joglutils.msg.math.*;
import net.java.joglutils.msg.misc.*;
import net.java.joglutils.msg.nodes.*;

/** A test implementing a 3D display shelf component. */

public class DisplayShelf extends Container {
  private GLCanvas canvas;

  private float DEFAULT_ASPECT_RATIO = 0.665f;
  // This also affects the spacing
  private float DEFAULT_HEIGHT = 1.5f;

  // The camera
  private PerspectiveCamera camera;

  static class TitleGraph {
    String url;
    Separator sep   = new Separator();
    Transform xform = new Transform();
    Texture2  texture = new Texture2();
    Coordinate3 coords = new Coordinate3();

    TitleGraph(String url) {
      this.url = url;
    }
  }

  private Group root;
  private Separator imageRoot;
  private String[] images;
  private List<TitleGraph> titles = new ArrayList<TitleGraph>();
  private int targetIndex;
  private JSlider slider;
  // This encodes both the current position and the animation alpha
  private float currentIndex;
  // If the difference between the current index and target index is >
  // EPSILON, then we will continue repainting
  private static final float EPSILON = 1.0e-3f;
  private SystemTime time;
  private boolean animating;
  // A scale factor for the animation speed
  private static final float ANIM_SCALE_FACTOR = 3.0f;
  // The rotation angle of the titles
  private static final float ROT_ANGLE = (float) Math.toRadians(60);
  // Constant rotations
  private static final Rotf POS_ANGLE = new Rotf(Vec3f.Y_AXIS,  ROT_ANGLE);
  private static final Rotf NEG_ANGLE = new Rotf(Vec3f.Y_AXIS, -ROT_ANGLE);

  private void computeCoords(Coordinate3 coordNode, float aspectRatio) {
    Vec3fCollection coords = coordNode.getData();
    if (coords == null) {
      coords = new Vec3fCollection();
      Vec3f zero = new Vec3f();
      for (int i = 0; i < 6; i++) {
        coords.add(zero);
      }
      coordNode.setData(coords);
    }
    // Now compute the actual values
    Vec3f lowerLeft  = new Vec3f(-0.5f * DEFAULT_HEIGHT * aspectRatio, 0, 0);
    Vec3f lowerRight = new Vec3f( 0.5f * DEFAULT_HEIGHT * aspectRatio, 0, 0);
    Vec3f upperLeft  = new Vec3f(-0.5f * DEFAULT_HEIGHT * aspectRatio, DEFAULT_HEIGHT, 0);
    Vec3f upperRight = new Vec3f( 0.5f * DEFAULT_HEIGHT * aspectRatio, DEFAULT_HEIGHT, 0);
    // First triangle
    coords.set(0, upperRight);
    coords.set(1, upperLeft);
    coords.set(2, lowerLeft);
    // Second triangle
    coords.set(3, upperRight);
    coords.set(4, lowerLeft);
    coords.set(5, lowerRight);
  }

  private void startLoading() {
    final List<TitleGraph> queuedGraphs = new ArrayList<TitleGraph>();
    queuedGraphs.addAll(titles);

    Thread loaderThread = new Thread(new Runnable() {
        public void run() {
          while (queuedGraphs.size() > 0) {
            TitleGraph graph = queuedGraphs.remove(0);
            BufferedImage img = null;
            try {
              img = ImageIO.read(new URL(graph.url));
            } catch (Exception e) {
              System.out.println("Exception loading " + graph.url + ":");
              e.printStackTrace();
            }
            if (img != null) {
              graph.texture.setTexture(img, false);
              // Figure out the new aspect ratio based on the image's width and height
              float aspectRatio = (float) img.getWidth() / (float) img.getHeight();
              // Compute new coordinates
              computeCoords(graph.coords, aspectRatio);
              // Schedule a repaint
              canvas.repaint();
            }
          }
        }
      });
    // Avoid having the loader thread preempt the rendering thread
    loaderThread.setPriority(Thread.MIN_PRIORITY + 1);
    loaderThread.start();
  }

  private void setTargetIndex(int index) {
    this.targetIndex = index;
    if (!animating) {
      time.rebase();
    }
    canvas.repaint();
  }

  private boolean recompute(boolean force) {
    if (!force) {
      if (Math.abs(targetIndex - currentIndex) < EPSILON)
        return false;
    }

    time.update();
    float deltaT = (float) time.deltaT();

    // Make the animation speed independent of frame rate
    currentIndex = currentIndex + (targetIndex - currentIndex) * deltaT * ANIM_SCALE_FACTOR;

    // Now recompute the position of the camera
    camera.setPosition(new Vec3f(currentIndex, 0.5f * DEFAULT_HEIGHT, DEFAULT_HEIGHT));

    // Now recompute the orientations of each title
    int firstIndex  = (int) Math.floor(currentIndex);
    int secondIndex = (int) Math.ceil(currentIndex);

    float alpha = currentIndex - firstIndex;

    int idx = 0;
    for (TitleGraph graph : titles) {
      if (idx < firstIndex) {
        graph.xform.getTransform().setRotation(POS_ANGLE);
        graph.xform.getTransform().setTranslation(new Vec3f(idx, 0, 0));
      } else if (idx > secondIndex) {
        graph.xform.getTransform().setRotation(NEG_ANGLE);
        graph.xform.getTransform().setTranslation(new Vec3f(idx, 0, 0));
      } else if (idx == firstIndex) {
        // Interpolate
        graph.xform.getTransform().setRotation(new Rotf(Vec3f.Y_AXIS, alpha * ROT_ANGLE));
        graph.xform.getTransform().setTranslation(new Vec3f(idx, 0, (1.0f - alpha) * 0.4f * DEFAULT_HEIGHT));
      } else {
        // Interpolate
        graph.xform.getTransform().setRotation(new Rotf(Vec3f.Y_AXIS, (1.0f - alpha) * -ROT_ANGLE));
        graph.xform.getTransform().setTranslation(new Vec3f(idx, 0, alpha * 0.4f * DEFAULT_HEIGHT));
      }

      ++idx;
    }

    return true;
  }

  public DisplayShelf(Group root, String[] images) {
    this.images = images;
    this.root = root;
    time = new SystemTime();
    time.rebase();
    setLayout(new BorderLayout());
    camera = new PerspectiveCamera();
    camera.setNearDistance(0.1f);
    camera.setFarDistance(20.0f);
    canvas = new GLCanvas();
    canvas.addGLEventListener(new Listener());
    canvas.addMouseListener(new MListener());
    add(canvas, BorderLayout.CENTER);
    slider = new JSlider(0, images.length - 1, 0);
    slider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          setTargetIndex(slider.getValue());
        }
      });
    add(slider, BorderLayout.SOUTH);
  }

  class Listener implements GLEventListener {
    private GLRenderAction ra = new GLRenderAction();
    
    public void init(GLAutoDrawable drawable) {
      GL gl = drawable.getGL();

      // Build the scene graph
      root.removeAllChildren();

      // The images
      imageRoot = new Separator();

      // The mirrored images, under the floor
      Separator mirrorRoot = new Separator();

      Transform mirrorXform = new Transform();
      // Mirror vertically
      mirrorXform.getTransform().set(1, 1, -1.0f);
      mirrorRoot.addChild(mirrorXform);
      // Assume we know what we're doing here with setting per-vertex
      // colors for each piece of geometry in one shot
      Color4 colorNode = new Color4();
      Vec4fCollection colors = new Vec4fCollection();
      Vec4f fadeTop = new Vec4f(0.75f, 0.75f, 0.75f, 0.75f);
      Vec4f fadeBot = new Vec4f(0.25f, 0.25f, 0.25f, 0.25f);
      // First triangle
      colors.add(fadeTop);
      colors.add(fadeTop);
      colors.add(fadeBot);
      // Second triangle    
      colors.add(fadeTop);
      colors.add(fadeBot);
      colors.add(fadeBot);
      colorNode.setData(colors);
      mirrorRoot.addChild(colorNode);

      TriangleSet tris = new TriangleSet();
      tris.setNumTriangles(2);

      int i = 0;
      for (String image : images) {
        TitleGraph graph = new TitleGraph(image);
        titles.add(graph);
        computeCoords(graph.coords, DEFAULT_ASPECT_RATIO);
        graph.xform.getTransform().setTranslation(new Vec3f(i, 0, 0));
        Separator sep = graph.sep;
        sep.addChild(graph.xform);
        sep.addChild(graph.coords);
        sep.addChild(graph.texture);
        TextureCoordinate2 texCoordNode = new TextureCoordinate2();
        Vec2fCollection texCoords = new Vec2fCollection();
        // Texture coordinates for two triangles
        // First triangle
        texCoords.add(new Vec2f( 1,  1));
        texCoords.add(new Vec2f( 0,  1));
        texCoords.add(new Vec2f( 0,  0));
        // Second triangle
        texCoords.add(new Vec2f( 1,  1));
        texCoords.add(new Vec2f( 0,  0));
        texCoords.add(new Vec2f( 1,  0));
        texCoordNode.setData(texCoords);
        sep.addChild(texCoordNode);

        sep.addChild(tris);

        // Add this to each rendering root
        imageRoot.addChild(sep);
        mirrorRoot.addChild(sep);

        ++i;
      }

      // Now produce the floor geometry
      float minx = -i;
      float maxx = 2 * i;
      // Furthest back from the camera
      float minz = -2 * DEFAULT_HEIGHT;
      // Assume this will be close enough to cover all of the mirrored geometry
      float maxz =  DEFAULT_HEIGHT;
      Separator floorRoot = new Separator();
      Blend blend = new Blend();
      blend.setEnabled(true);
      blend.setSourceFunc(Blend.ONE);
      blend.setDestFunc(Blend.ONE_MINUS_SRC_ALPHA);
      floorRoot.addChild(blend);
      Coordinate3 floorCoords = new Coordinate3();
      floorCoords.setData(new Vec3fCollection());
      // First triangle
      floorCoords.getData().add(new Vec3f(maxx, 0, minz));
      floorCoords.getData().add(new Vec3f(minx, 0, minz));
      floorCoords.getData().add(new Vec3f(minx, 0, maxz));
      // Second triangle    
      floorCoords.getData().add(new Vec3f(maxx, 0, minz));
      floorCoords.getData().add(new Vec3f(minx, 0, maxz));
      floorCoords.getData().add(new Vec3f(maxx, 0, maxz));
      floorRoot.addChild(floorCoords);
      // Colors
      Vec4f gray = new Vec4f(0.4f, 0.4f, 0.4f, 0.4f);
      Vec4f clearGray = new Vec4f(0.0f, 0.0f, 0.0f, 0.0f);
      Color4 floorColors = new Color4();
      floorColors.setData(new Vec4fCollection());
      // First triangle
      floorColors.getData().add(gray);
      floorColors.getData().add(gray);
      floorColors.getData().add(clearGray);
      // Second triangle    
      floorColors.getData().add(gray);
      floorColors.getData().add(clearGray);
      floorColors.getData().add(clearGray);
      floorRoot.addChild(floorColors);
    
      floorRoot.addChild(tris);

      // Now set up the overall scene graph
      root.addChild(camera);
      root.addChild(imageRoot);
      root.addChild(mirrorRoot);
      root.addChild(floorRoot);

      startLoading();
      recompute(true);
    }

    public void display(GLAutoDrawable drawable) {
      // Recompute position of camera and orientation of images
      boolean repaintAgain = recompute(false);

      // Redraw
      GL gl = drawable.getGL();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
      ra.apply(root);

      if (repaintAgain) {
        animating = true;
        canvas.repaint();
      } else {
        animating = false;
      }
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
    }

    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {}
  }

  class MListener extends MouseAdapter {
    RayPickAction ra = new RayPickAction();

    public void mousePressed(MouseEvent e) {
      ra.setPoint(e.getX(), e.getY(), e.getComponent());
      // Apply to the scene root
      ra.apply(root);
      List<PickedPoint> pickedPoints = ra.getPickedPoints();
      Path p = null;
      if (!pickedPoints.isEmpty())
        p = pickedPoints.get(0).getPath();
      if (p != null && p.size() > 1) {
        int idx = imageRoot.findChild(p.get(p.size() - 2));
        if (idx >= 0) {
          // Need to keep the slider and this mechanism in sync
          slider.setValue(idx);
        }
      }
    }
  }

  public static void main(String[] args) {
    Frame f = new Frame("Display Shelf test");
    f.setLayout(new BorderLayout());
    f.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          new Thread(new Runnable() {
              public void run() {
                System.exit(0);
              }
            }).start();
        }
      });

    // The images to configure the shelf with
    String[] images = {
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.jsepedzf.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.wvbmknhn.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.oorrjicu.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.woofnkar.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.tapbaxpy.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.awlngumx.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.bpuzrjch.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.nqarjlzt.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.hgadlawz.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.sdfnrwzj.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.vtbicehh.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.lhgtckcs.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.tbwyqyqm.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.eimndamh.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.nxvdfcwt.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.njoydoqk.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.ikfbfqzh.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.niqwioqm.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.tqqldmqe.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.ynokefwv.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.jodjmgxs.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.yhdaeino.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.xmgrrxef.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.pahnmknr.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.sbkwhrik.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.hwbcjnfx.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.umbuvrfe.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.krksguze.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.jionwnuf.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.dgnjindw.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.wpfmtfzp.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.gcajwhco.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.glzycglj.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.pajmxsmk.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.lamcsbwx.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.nqvsikaq.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.elyzoipc.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.oidpsvzg.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.moyzjiht.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.qizpbris.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.uadqyjbr.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.pqzeferc.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.jhotijvb.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.asztraij.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.dricykdh.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.tpysowpf.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.cawuddxy.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.vmajyyha.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.tuyoxwib.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.sanzeosx.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/dj.zfqfgoas.200x200-75.jpg",
      "http://download.java.net/media/jogl/builds/ds_tmp/mzi.uswlslxx.200x200-75.jpg"
    };

    Separator root = new Separator();

    DisplayShelf shelf = new DisplayShelf(root, images);
    f.add(shelf);
    GraphicsDevice dev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    DisplayMode curMode = dev.getDisplayMode();
    int height = (int) (0.5f * curMode.getWidth());
    f.setSize(curMode.getWidth(), height);
    f.setLocation(0, (curMode.getHeight() - height) / 2);
    f.setVisible(true);
  }
}
