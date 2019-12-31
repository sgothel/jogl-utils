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

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import net.java.joglutils.msg.actions.GLRenderAction;
import net.java.joglutils.msg.collections.Vec2fCollection;
import net.java.joglutils.msg.collections.Vec3fCollection;
import net.java.joglutils.msg.collections.Vec4fCollection;
import net.java.joglutils.msg.math.Vec2f;
import net.java.joglutils.msg.math.Vec3f;
import net.java.joglutils.msg.math.Vec4f;
import net.java.joglutils.msg.nodes.Color4;
import net.java.joglutils.msg.nodes.Coordinate3;
import net.java.joglutils.msg.nodes.PerspectiveCamera;
import net.java.joglutils.msg.nodes.Separator;
import net.java.joglutils.msg.nodes.TextureCoordinate2;
import net.java.joglutils.msg.nodes.Transform;
import net.java.joglutils.msg.nodes.TriangleSet;

/** A very basic test of the Minimal Scene Graph library. */

public class Test {
  public static void main(final String[] args) {
    final Frame frame = new Frame("Minimal Scene Graph (MSG) Test");
    final GLCanvas canvas = new GLCanvas();
    canvas.addGLEventListener(new Listener());
    frame.add(canvas);
    frame.setSize(512, 512);
    frame.setVisible(true);
    frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(final WindowEvent e) {
          new Thread(new Runnable() {
              public void run() {
                System.exit(0);
              }
            }).start();
        }
      });
  }

  static class Listener implements GLEventListener {
    private Separator root;
    private GLRenderAction renderAction;

    public void init(final GLAutoDrawable drawable) {
      root = new Separator();
      final PerspectiveCamera cam = new PerspectiveCamera();
      cam.setPosition(new Vec3f(0, 0, 2));
      root.addChild(cam);
      final Coordinate3 coordNode = new Coordinate3();
      final Vec3fCollection coords = new Vec3fCollection();
      // First triangle
      coords.add(new Vec3f( 1,  1, 0));
      coords.add(new Vec3f(-1,  1, 0));
      coords.add(new Vec3f(-1, -1, 0));
      // Second triangle
      coords.add(new Vec3f( 1,  1, 0));
      coords.add(new Vec3f(-1, -1, 0));
      coords.add(new Vec3f( 1, -1, 0));
      coordNode.setData(coords);
      root.addChild(coordNode);

      // Texture coordinates
      final TextureCoordinate2 texCoordNode = new TextureCoordinate2();
      final Vec2fCollection texCoords = new Vec2fCollection();
      // First triangle
      texCoords.add(new Vec2f( 1,  1));
      texCoords.add(new Vec2f( 0,  1));
      texCoords.add(new Vec2f( 0,  0));
      // Second triangle
      texCoords.add(new Vec2f( 1,  1));
      texCoords.add(new Vec2f( 0,  0));
      texCoords.add(new Vec2f( 1,  0));
      texCoordNode.setData(texCoords);
      root.addChild(texCoordNode);

      // Colors
      final Color4 colorNode = new Color4();
      final Vec4fCollection colors = new Vec4fCollection();
      // First triangle
      colors.add(new Vec4f(1.0f, 1.0f, 1.0f, 1.0f));
      colors.add(new Vec4f(1.0f, 1.0f, 1.0f, 1.0f));
      colors.add(new Vec4f(0.0f, 0.0f, 0.0f, 0.0f));
      // Second triangle
      colors.add(new Vec4f(1.0f, 1.0f, 1.0f, 1.0f));
      colors.add(new Vec4f(0.0f, 0.0f, 0.0f, 0.0f));
      colors.add(new Vec4f(0.0f, 0.0f, 0.0f, 0.0f));
      colorNode.setData(colors);
      root.addChild(colorNode);

      final TriangleSet tris = new TriangleSet();
      tris.setNumTriangles(2);
      root.addChild(tris);

      // Testing transforms
      final Transform xform = new Transform();
      xform.getTransform().setTranslation(new Vec3f(2, -2, 0));
      //      xform.getTransform().setRotation(new Rotf(new Vec3f(0, 1, 0), (float) (-Math.PI / 4)));
      root.addChild(xform);

      root.addChild(tris);

      final GL gl = drawable.getGL();
      gl.glEnable(GL.GL_DEPTH_TEST);

      renderAction = new GLRenderAction();
    }

    public void display(final GLAutoDrawable drawable) {
      final GL gl = drawable.getGL();
      gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
      renderAction.apply(root);
    }

    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int w, final int h) {}
    public void dispose(final GLAutoDrawable drawable) {}
  }
}
