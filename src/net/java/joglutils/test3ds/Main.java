/*
 * Copyright (c) 2006 Greg Rodgers All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * The names of Greg Rodgers, Sun Microsystems, Inc. or the names of
 * contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. GREG RODGERS,
 * SUN MICROSYSTEMS, INC. ("SUN"), AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL GREG
 * RODGERS, SUN, OR SUN'S LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF GREG
 * RODGERS OR SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package net.java.joglutils.test3ds;

import com.sun.opengl.util.Animator;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.media.opengl.*;
import javax.media.opengl.awt.*;
import javax.media.opengl.glu.GLU;

public class Main {
    /** Creates a new instance of Main */
    public Main() {
    }

    public static void main(String[] args)
    {
        Frame frame = new Frame();
        GLCanvas canvas = new GLCanvas();
        canvas.addGLEventListener(new Renderer());
        frame.add(canvas);
        frame.setSize(600, 600);
        final Animator animator = new Animator(canvas);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
              // Run this on another thread than the AWT event queue to
              // make sure the call to Animator.stop() completes before
              // exiting
              new Thread(new Runnable() {
                  public void run() {
                    animator.stop();
                    System.exit(0);
                  }
                }).start();
            }
          });
        frame.show();
        animator.start();
    }

    static class Renderer implements GLEventListener
    {
        private MyModel model = new MyModel();

        public void display(GLAutoDrawable gLDrawable)
        {
            final GL2 gl = gLDrawable.getGL().getGL2();
            gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
            gl.glLoadIdentity();

            gl.glPushMatrix();
                gl.glScalef(0.000004f, 0.000004f, 0.000004f);
                model.render(gLDrawable);
            gl.glPopMatrix();

            gl.glFlush();
        }


        public void dispose(GLAutoDrawable drawable) {}

        /** Called by the drawable immediately after the OpenGL context is
         * initialized for the first time. Can be used to perform one-time OpenGL
         * initialization such as setup of lights and display lists.
         * @param gLDrawable The GLDrawable object.
         */
        public void init(GLAutoDrawable gLDrawable)
        {
            final GL2 gl = gLDrawable.getGL().getGL2();

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.3f);
            gl.glClearDepth(1.0f);
            gl.glEnable(GL2.GL_DEPTH_TEST);
            gl.glDepthFunc(GL2.GL_LEQUAL);
            gl.glHint(GL2.GL_PERSPECTIVE_CORRECTION_HINT, GL2.GL_NICEST);

            if (!model.isLoaded())
                model.load(gLDrawable, "globe.3ds");
        }


        /** Called by the drawable during the first repaint after the component has
         * been resized. The client can update the viewport and view volume of the
         * window appropriately, for example by a call to
         * GL.glViewport(int, int, int, int); note that for convenience the component
         * has already called GL.glViewport(int, int, int, int)(x, y, width, height)
         * when this method is called, so the client may not have to do anything in
         * this method.
         * @param gLDrawable The GLDrawable object.
         * @param x The X Coordinate of the viewport rectangle.
         * @param y The Y coordinate of the viewport rectanble.
         * @param width The new width of the window.
         * @param height The new height of the window.
         */
        public void reshape(GLAutoDrawable gLDrawable, int x, int y, int width, int height)
        {
            final GL2 gl = gLDrawable.getGL().getGL2();
            final GLU glu = new GLU();

            if (height <= 0) // avoid a divide by zero error!
                height = 1;
            final float h = (float)width / (float)height;
            gl.glViewport(0, 0, width, height);
            gl.glMatrixMode(GL2.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glOrtho(-1000, 1000, -1000, 1000, -10000, 10000);
            gl.glMatrixMode(GL2.GL_MODELVIEW);
            gl.glLoadIdentity();
        }
    }
}
