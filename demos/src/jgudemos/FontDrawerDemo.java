/*
 * Copyright (c) 2007 Erik Tollerud (erik.tollerud@gmail.com) All Rights Reserved.
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
 * The names of Erik Tollerud, Sun Microsystems, Inc. or the names of
 * contributors may not be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. ERIK TOLLERUD,
 * SUN MICROSYSTEMS, INC. ("SUN"), AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL ERIK
 * TOLLERUD, SUN, OR SUN'S LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT
 * OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR
 * PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF ERIK
 * TOLLERUD OR SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 */

package jgudemos;

import net.java.joglutils.jogltext.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.Animator;

/**
 * This Demo application uses the Lighting class and the FontDrawer class to render 3D text into a GLJFrame.
 * See the console for keyboard commands and command line option instructions.
 * @author Erik J. Tollerud
 * @created January 12, 2007
 */
public class FontDrawerDemo {
    
    /**
     * Generates {@link net.java.joglutils.GLJFrame}
     * Generates a GLJFrame with a FontDrawer demo. Console output describes input.
     * @param args Command Line argument order: textDepth xRotspeed yRotspeed zRotspeed filled flatnormal
     *
     */
    
    
    static String helpString = "Keyboard Inputs (case sensitive)\n" +
            "r: toggle rotation\n" +
            "R: change rotation speeds\n" +
            "n: toggle flat normals\n" +
            "f: toggle filled text\n" +
            "t: edit text\n" +
            "</>: decrease/increase text depth\n" +
            "F: change font\n" +
            "s: resize font\n" +
            "l: light position\n" +
            "F1/?: Display help";
    
    static float[] light_ambient = { 0.0f, 0.0f, 0.0f, 1.0f };
    static float[] light_diffuse = { 1.0f, 1.0f, 1.0f, 1.0f };
    static float[] light_specular = { 0.0f, 0.0f, 0.0f, 0.0f };
    static float[] light_position = { 0.0f, 0.0f, 1.0f, 0.0f };
    static boolean rotating = true;
    
    
    public static void main(String[] args) {
        final float[] rotSteps = {0.0f, 0.3f, 0.0f};
        
        System.out.println("Option Command line argument order (first 4 numerical, last 2 boolean)\ntextDepth xRotspeed yRotspeed zRotspeed filled flatnormal");
        System.out.println(helpString);
        
        final String[] argsFin = args;
        Font font = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[5];
        final FontDrawer dttf = new FontDrawer(font);
        final StringBuffer upperStr = new StringBuffer("0,0");
        final StringBuffer lowerStr = new StringBuffer("-1,-1");
        
        GLEventListener listener = new GLEventListener() {
            GLU glu;
            float xrot,yrot,zrot;
            float dpth = 0.2f;
            boolean filled = true, fnorm = true;
            //net.java.joglutils.lighting.Light lt;
            //net.java.joglutils.lighting.Material mt;
            
            
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }
            
            public void init(GLAutoDrawable drawable) {
                drawable.setGL(new DebugGL(drawable.getGL()));
                glu = new GLU();
                switch(argsFin.length) {
                    case 6:
                        fnorm = Boolean.parseBoolean(argsFin[5]);
                    case 5:
                        filled = Boolean.parseBoolean(argsFin[4]);
                    case 4:
                        rotSteps[2] = Float.parseFloat(argsFin[3]);
                    case 3:
                        rotSteps[1] = Float.parseFloat(argsFin[2]);
                    case 2:
                        rotSteps[0] = Float.parseFloat(argsFin[1]);
                    case 1:
                        dpth = Float.parseFloat(argsFin[0]);
                }
                dttf.setDepth(dpth);
                dttf.setFill(filled);
                dttf.setNormal(FontDrawer.NormalMode.FLAT);
                
                xrot = 0;yrot = 0;zrot = 0;
                GL gl = drawable.getGL();
                // lt = new net.java.joglutils.lighting.Light(gl);
                //mt = new net.java.joglutils.lighting.Material(gl);
                
                // lt.setLightPosition(0,0,1);
                //   lt.setSpecular(new Color(0,0,0));
                //   lt.setAmbient(new Color(0,0,0));
                
                //  lt.enable();
                //  lt.apply();
                //mt.apply();
                gl.glColorMaterial( GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE ) ;
                float[] mamb = {0,0,0,0};
                gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT,mamb,0);
                
                gl.glEnable(GL.GL_DEPTH_TEST);
                gl.glEnable(GL.GL_LIGHTING);
                gl.glEnable(GL.GL_LIGHT0);
                gl.glEnable(GL.GL_NORMALIZE);
                
                
                
                
                gl.glClearColor(0.3f,0.5f,0.2f,0);
            }
            
            public void drawAxis(GL gl)  {
                gl.glDisable(GL.GL_LIGHTING);
                gl.glBegin(GL.GL_LINES);
                gl.glColor3f(1,0,0);
                gl.glVertex3i(0,0,0);
                gl.glVertex3i(10,0,0);
                gl.glColor3f(0,1,0);
                gl.glVertex3i(0,0,0);
                gl.glVertex3i(0,10,0);
                gl.glColor3f(0,0,1);
                gl.glVertex3i(0,0,0);
                gl.glVertex3i(0,0,10);
                gl.glEnd();
                gl.glEnable(GL.GL_LIGHTING);
            }
            
            public void display(GLAutoDrawable drawable) {
                GL gl = drawable.getGL();
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
                
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glLoadIdentity();
                //glu.gluPerspective(90,1,0.001,10);
                //gl.glFrustum(-1.5f,1.5f,-1.5f,1.5f,1,5);
                gl.glOrtho(-1.5,1.5,-1.5,1.5,-5,5);
                
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glLoadIdentity();
                
                //Manual lighting activation before transform
                gl.glLightfv(GL.GL_LIGHT0, GL.GL_AMBIENT, light_ambient,0);
                gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, light_diffuse,0);
                gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, light_specular,0);
                gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light_position,0);
                
                glu.gluLookAt(0,0,2,0,0,0,0,1,0);
                //drawAxis(gl);
                gl.glRotatef(xrot,1.0f,0,0);
                gl.glRotatef(yrot,0,1.0f,0);
                gl.glRotatef(zrot,0,0,1.0f);
                
                
                
                if (rotating) {
                    xrot+=rotSteps[0];
                    yrot+=rotSteps[1];
                    zrot+=rotSteps[2];
                }
                
                
                
                drawAxis(gl);
                dttf.drawString(upperStr.toString(),glu,gl);
                dttf.drawString(lowerStr.toString(),glu,gl,-0.8f,-0.8f,0);
            }
            
            public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
            }
            
        };
        final net.java.joglutils.GLJFrame gljf = new net.java.joglutils.GLJFrame("FontDrawerDemo", listener, 600, 600);
        gljf.setDefaultCloseOperation(gljf.EXIT_ON_CLOSE);
        gljf.addKeyListener(new java.awt.event.KeyAdapter() {
            
            public void keyPressed(KeyEvent e) {
                char test = e.getKeyChar();
                switch (e.getKeyChar()) {
                    case 'f':
                        FontDrawer fd = dttf;
                        dttf.setFill(!dttf.isFill());
                        break;
                    case 'n':
                        switch(dttf.getNormal()) {
                            case NONE:
                                dttf.setNormal(FontDrawer.NormalMode.FLAT);
                                break;
                            case FLAT:
                                dttf.setNormal(FontDrawer.NormalMode.AVERAGED);
                                break;
                            case AVERAGED:
                                dttf.setNormal(FontDrawer.NormalMode.NONE);
                                break;
                        }
                        break;
                    case 'r':
                        Animator anim = gljf.getAnimator();
                        if(rotating) {
                            anim.stop();
                            rotating = false;
                        } else {
                            anim.start();
                            rotating = true;
                        }
                        break;
                    case 'R':
                        try {
                            String strRes;
                            strRes = JOptionPane.showInputDialog("X Rotation Speed?",Float.toString(rotSteps[0]));
                            if (strRes != null) {
                                rotSteps[0] = Float.parseFloat(strRes);
                                strRes = JOptionPane.showInputDialog("Y Rotation Speed?",Float.toString(rotSteps[1]));
                                if (strRes != null) {
                                    rotSteps[1] = Float.parseFloat(strRes);
                                    strRes = JOptionPane.showInputDialog("Z Rotation Speed?",Float.toString(rotSteps[2]));
                                    if (strRes != null)
                                        rotSteps[2] = Float.parseFloat(strRes);
                                }
                            }
                        } catch(Exception ignore) {
                        }
                        break;
                    case '>':
                    case '.':
                        float depthStep = dttf.getFont().getSize()/20.0f;
                        dttf.setDepth(dttf.getDepth() + depthStep);
                        break;
                    case '<':
                    case ',':
                        depthStep = dttf.getFont().getSize()/20.0f;
                        dttf.setDepth(dttf.getDepth() - depthStep);
                        break;
                    case 't':
                    case 'T':
                        String up = JOptionPane.showInputDialog("Upper Text:",upperStr.toString());
                        upperStr.delete(0,upperStr.length());
                        upperStr.append(up);
                        String dn = JOptionPane.showInputDialog("Lower Text:",lowerStr.toString());
                        lowerStr.delete(0,lowerStr.length());
                        lowerStr.append(dn);
                        break;
                    case 'F':
                        Font[] fonts = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
                        
                        JPanel pan = new JPanel();
                        JComboBox cb = new JComboBox();
                        for (Font f : fonts)
                            cb.addItem(f);
                        cb.setSelectedItem(dttf.getFont());
                        pan.add(cb);
                        net.java.joglutils.JPanelDialog jpd = new net.java.joglutils.JPanelDialog("Choose Font",pan);
                        if (jpd.showAsModal())
                            dttf.setFont((Font)cb.getSelectedItem());
                        break;
                    case 's':
                        Font currFont = dttf.getFont();
                        String resultStr = JOptionPane.showInputDialog("Font Size?",Integer.toString(currFont.getSize()));
                        float targSize = Float.parseFloat(resultStr);
                        dttf.setFont(currFont.deriveFont(currFont.getStyle(),targSize));
                        break;
                    case 'l':
                        try {
                            String xStr = JOptionPane.showInputDialog(null,"Light x-position?",Float.toString(light_position[0]));
                            light_position[0] = Float.parseFloat(xStr);
                            String yStr = JOptionPane.showInputDialog(null,"Light y-position?",Float.toString(light_position[1]));
                            light_position[1] = Float.parseFloat(yStr);
                            String zStr = JOptionPane.showInputDialog(null,"Light z-position?",Float.toString(light_position[2]));
                            light_position[2] = Float.parseFloat(zStr);
                            String wStr = JOptionPane.showInputDialog(null,"Light w-position?",Float.toString(light_position[3]));
                            light_position[3] = Float.parseFloat(wStr);
                        } catch(Exception ignore) {
                        }
                        break;
                    case '?':
                        System.out.println(helpString);
                        JOptionPane.showMessageDialog(null,helpString);
                        break;
                    default:
                        switch (e.getKeyCode()) {
                            case KeyEvent.VK_F1:
                                System.out.println(helpString);
                                JOptionPane.showMessageDialog(null,helpString);
                                break;
                        }
                }
                gljf.repaint();
            }
        });
        gljf.generateAnimator();
        gljf.setVisible(true);
    }
}
