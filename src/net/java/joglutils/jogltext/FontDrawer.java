package net.java.joglutils.jogltext;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.io.*;
import java.text.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.*;

/**
 *  This class renders a TrueType Font into OpenGL
 *
 *@author     Davide Raccagni
 *@author     Erik Tollerud
 *@created    January 29, 2004
 */
public class FontDrawer {
    
    private Font font;
    private float depth;
    
    private boolean edgeOnly;
    private boolean flatNorm;
    
    public FontDrawer(Font font) {
        this.font = font;
        depth = 0;
        edgeOnly = false;
        flatNorm = true;
    }
    
    public void setFont(Font font) {
        this.font = font;
    }
    
    public Font getFont() {
        return this.font;
    }
    
    public void setDepth(float depth) {
        if (depth <= 0)
            this.depth = 0;
        else
            this.depth = depth;
    }
    
    public float getDepth() {
        return this.depth;
    }
    
    public void setFill(boolean fill) {
        this.edgeOnly = !fill;
    }
    
    public boolean isFill() {
        return !this.edgeOnly;
    }
    
    public void setFlatNormals( boolean flat ) {
        this.flatNorm = flat;
    }
    
    public boolean isFlatNormals() {
        return this.flatNorm;
    }
    
    public void drawString(String str, GLU glu, GL gl) {
        GlyphVector gv = font.createGlyphVector(
                new FontRenderContext(new AffineTransform(), true, true),
                new StringCharacterIterator(str));
        GeneralPath gp = (GeneralPath)gv.getOutline();
        PathIterator pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
        gl.glNormal3i(0,0,1);
        tesselateFace(glu, gl, pi, pi.getWindingRule(), this.edgeOnly);
        if (this.depth != 0.0) {
            pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
            gl.glNormal3i(0,0,-1);
            tesselateFace(glu, gl, pi, pi.getWindingRule(), this.edgeOnly, this.depth);
            pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
            if (this.flatNorm)
                drawSidesFlatNorm(gl,pi,this.edgeOnly,this.depth);
            else
                drawSidesAvgNorm(gl,pi,this.edgeOnly,this.depth);
            
        }
    }
    
    public void drawString(String str, GLU glu, GL gl, float xOff, float yOff, float zOff) {
        gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glTranslatef(xOff,yOff,zOff);
        this.drawString(str,glu,gl);
        gl.glPopMatrix();
        gl.glPopAttrib();
    }
    
    private void tesselateFace(GLU glu, GL gl, PathIterator pi, int windingRule, boolean justBoundary) {
        tesselateFace(glu,gl,pi,windingRule,justBoundary,0);
    }
    
    private void tesselateFace(GLU glu, GL gl, PathIterator pi, int windingRule, boolean justBoundary,double tessZ) {
        GLUtessellatorCallback aCallback = new GLUtesselatorCallbackImpl(gl);
        GLUtessellator tess = glu.gluNewTess();
        
        glu.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, aCallback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_END, aCallback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_ERROR, aCallback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, aCallback);
        glu.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, aCallback);
        
        glu.gluTessNormal(tess, 0.0, 0.0, -1.0);
        
        switch (windingRule) {
            case PathIterator.WIND_EVEN_ODD:
                glu.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
                break;
            case PathIterator.WIND_NON_ZERO:
                glu.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);
                break;
        }
        
        if (justBoundary) {
            glu.gluTessProperty(tess, GLU.GLU_TESS_BOUNDARY_ONLY, GL.GL_TRUE);
        } else {
            glu.gluTessProperty(tess, GLU.GLU_TESS_BOUNDARY_ONLY, GL.GL_FALSE);
        }
        
        glu.gluTessBeginPolygon(tess, (double[])null);
        
        for (; !pi.isDone(); pi.next()) {
            double[] coords = new double[3];
            coords[2] = tessZ;
            
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    glu.gluTessBeginContour(tess);
                    break;
                case PathIterator.SEG_LINETO:
                    glu.gluTessVertex(tess, coords, 0, coords);
                    break;
                case PathIterator.SEG_CLOSE:
                    glu.gluTessEndContour(tess);
                    break;
            }
        }
        glu.gluTessEndPolygon(tess);
        
        glu.gluDeleteTess(tess);
    }
    
    private void drawSidesFlatNorm(GL gl, PathIterator pi, boolean justBoundary,float tessZ) {
        //TODO: work out texture coords
        //TODO:work out double vs. float
        
        if (justBoundary) 
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL.GL_FILL);
        
        float[] lastCoord = new float[3];
        float[] firstCoord = new float[3];
        for(float[] coords = new float[6];!pi.isDone();pi.next()) {
            switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    gl.glBegin(GL.GL_QUADS);
                    lastCoord[0] = coords[0];
                    lastCoord[1] = coords[1]; 
                    firstCoord[0] = coords[0];
                    firstCoord[1] = coords[1]; 
                    break;
                case PathIterator.SEG_LINETO:
                    //Normal: {deltay,-deltax,0}
                    gl.glNormal3f(lastCoord[1]-coords[1],coords[0]-lastCoord[0],0);
                    lastCoord[2] = 0;
                    gl.glVertex3fv(lastCoord,0);
                    lastCoord[2] = tessZ;
                    gl.glVertex3fv(lastCoord,0);
                    coords[2] = tessZ;
                    gl.glVertex3fv(coords,0);
                    coords[2] = 0;
                    gl.glVertex3fv(coords,0);
                    
                    lastCoord[0] = coords[0];
                    lastCoord[1] = coords[1];
                    break;
                case PathIterator.SEG_CLOSE:
                    //Normal: {deltay,-deltax,0}
                    gl.glNormal3f(lastCoord[1]-firstCoord[1],firstCoord[0]-lastCoord[0],0);
                    lastCoord[2] = 0;
                    gl.glVertex3fv(lastCoord,0);
                    lastCoord[2] = tessZ;
                    gl.glVertex3fv(lastCoord,0);
                    firstCoord[2] = tessZ;
                    gl.glVertex3fv(firstCoord,0);
                    firstCoord[2] = 0;
                    gl.glVertex3fv(firstCoord,0);
                    gl.glEnd();
                    break;
                default:
                    throw new JogltextException("PathIterator segment not SEG_MOVETO, SEG_LINETO, SEG_CLOSE");
            }
        }
    }
    
    
    private void drawSidesAvgNorm(GL gl, PathIterator pi, boolean justBoundary,float tessZ) {
        //TODO: work out texture coords
        //TODO: improve performance
        
        if (justBoundary) 
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL.GL_FILL);
        
        
        float[] firstCoord = null;
        float[] secondCoord = null;
        float[] thirdCoord = null;
        float[] secondNorm = null;
        float[] thirdNorm = null;
        float[] twoBackCoord = null;
        float[] oneBackCoord = null;
        for(float[] coords = new float[6];!pi.isDone();pi.next()) {
            switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    firstCoord = new float[3];
                    firstCoord[0] = coords[0];
                    firstCoord[1] = coords[1];
                    gl.glBegin(GL.GL_QUAD_STRIP);
                    break;
                case PathIterator.SEG_LINETO:
                    if (secondCoord == null) {
                        secondCoord = new float[3];
                        secondCoord[0] = coords[0];
                        secondCoord[1] = coords[1];
                        twoBackCoord = firstCoord.clone();
                        oneBackCoord = secondCoord.clone();
                    } else {
                        float avgdeltax = oneBackCoord[0] - twoBackCoord[0] + coords[0] - oneBackCoord[0];
                        float avgdeltay = oneBackCoord[1] - twoBackCoord[1] + coords[1] - oneBackCoord[1];
                        if (thirdCoord == null) {
                            thirdCoord = new float[3];
                            thirdCoord[0] = coords[0];
                            thirdCoord[1] = coords[1];
                        }
                        if (secondNorm == null) {
                            secondNorm = new float[3];
                            secondNorm[0] = avgdeltay;
                            secondNorm[1] = -avgdeltax;
                            secondNorm[2] = 0;
                        }
                        else if (thirdNorm == null) {
                            thirdNorm = new float[3];
                            thirdNorm[0] = avgdeltay;
                            thirdNorm[1] = -avgdeltax;
                            thirdNorm[2] = 0;
                        }
                        gl.glNormal3f(avgdeltay,-avgdeltax,0);
                        oneBackCoord[2] = 0.0f;
                        gl.glVertex3fv(oneBackCoord,0);
                        oneBackCoord[2] = tessZ;
                        gl.glVertex3fv(oneBackCoord,0);
                                                
                        //copy to not have to recreate
                        twoBackCoord[0] = oneBackCoord[0];
                        twoBackCoord[1] = oneBackCoord[1];
                        oneBackCoord[0] = coords[0];
                        oneBackCoord[1] = coords[1];
                    }
                        break;
                case PathIterator.SEG_CLOSE:
                    float avgdeltax = oneBackCoord[0] - twoBackCoord[0] + firstCoord[0] - oneBackCoord[0];
                    float avgdeltay = oneBackCoord[1] - twoBackCoord[1] + firstCoord[1] - oneBackCoord[1];
                    gl.glNormal3f(avgdeltay,-avgdeltax,0);
                    oneBackCoord[2] = 0.0f;
                    gl.glVertex3fv(oneBackCoord,0);
                    oneBackCoord[2] = tessZ;
                    gl.glVertex3fv(oneBackCoord,0);
                    
                    avgdeltax = firstCoord[0] - oneBackCoord[0] + secondCoord[0] - firstCoord[0];
                    avgdeltay = firstCoord[1] - oneBackCoord[1] + secondCoord[1] - firstCoord[1];
                    gl.glNormal3f(avgdeltay,-avgdeltax,0);
                    firstCoord[2] = 0.0f;
                    gl.glVertex3fv(firstCoord,0);
                    firstCoord[2] = tessZ;
                    gl.glVertex3fv(firstCoord,0);
                    
                    avgdeltax = secondCoord[0] - firstCoord[0] + thirdCoord[0] - secondCoord[0];
                    avgdeltay = secondCoord[1] - firstCoord[1] + thirdCoord[1] - secondCoord[1];
                    gl.glNormal3f(avgdeltay,-avgdeltax,0);
                    //gl.glNormal3fv(secondNorm,0);
                    secondCoord[2] = 0.0f;
                    gl.glVertex3fv(secondCoord,0);
                    secondCoord[2] = tessZ;
                    gl.glVertex3fv(secondCoord,0);
                    
                    /*gl.glNormal3fv(thirdNorm,0);
                    thirdCoord[2] = 0.0f;
                    gl.glVertex3fv(thirdCoord,0);
                    thirdCoord[2] = tessZ;
                    gl.glVertex3fv(thirdCoord,0);*/
                    gl.glEnd();
                    firstCoord = null;
                    secondCoord = null;
                    thirdCoord = null;
                    secondNorm = null;
                    thirdNorm = null;
                    twoBackCoord = null;
                    oneBackCoord = null;
                    break;
                default:
                    throw new JogltextException("PathIterator segment not SEG_MOVETO, SEG_LINETO, SEG_CLOSE");
            }
        }
    }
    
    
    /**
     * Generates {@link GLJFrame} 
     * Generates a GLJFrame with a FontDrawer demo. Console output describes input.
     * @param args Command Line argument order: textDepth xRotspeed yRotspeed zRotspeed filled flatnormal
     *
     */
    public static void main(String[] args) {
        System.out.println("Option Command line argument order (first 4 numerical, last 2 boolean)\ntextDepth xRotspeed yRotspeed zRotspeed filled flatnormal");
        System.out.println("Keyboard Inputs (case sensitive)\nr: toggle rotation\nn: toggle flat normals\nf: toggle filled text\nt: change text\nF: change font");
        final String[] argsFin = args;
        Font font = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts()[5];
        final FontDrawer dttf = new FontDrawer(font);
        final StringBuffer upperStr = new StringBuffer("0,0");
        final StringBuffer lowerStr = new StringBuffer("-1,-1");
                
        GLEventListener listener = new GLEventListener() {
            GLU glu;
            float xrot,yrot,zrot,xstep = 0,ystep = 0.1f,zstep = 0;
            float dpth = 0.2f;
            boolean filled = true, fnorm = true;
            net.java.joglutils.lighting.Light lt;
            net.java.joglutils.lighting.Material mt;
            
            
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
                        zstep = Float.parseFloat(argsFin[3]);
                    case 3:
                        ystep = Float.parseFloat(argsFin[2]);
                    case 2:
                        xstep = Float.parseFloat(argsFin[1]);
                    case 1:
                        dpth = Float.parseFloat(argsFin[0]);
                }
                dttf.setDepth(dpth);
                dttf.setFill(filled);
                dttf.setFlatNormals(fnorm);
                
                xrot = 0;yrot = 0;zrot = 0;
                GL gl = drawable.getGL();
                lt = new net.java.joglutils.lighting.Light(gl);
                //mt = new net.java.joglutils.lighting.Material(gl);
                
                
                lt.setLightPosition(0,0,1);
                lt.enable();
                lt.apply();
                //mt.apply();
                gl.glColorMaterial ( GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE ) ;
                
                gl.glEnable(GL.GL_DEPTH_TEST);
                gl.glEnable(GL.GL_LIGHTING);
                gl.glEnable(GL.GL_NORMALIZE);
                
                gl.glClearColor(0.3f,0.5f,0.2f,0);
            }
            
            public void display(GLAutoDrawable drawable) {
                GL gl = drawable.getGL();
                
                gl.glMatrixMode(GL.GL_PROJECTION);
                gl.glLoadIdentity();
                //glu.gluPerspective(90,1,0.001,10);
                //gl.glFrustum(-1.5f,1.5f,-1.5f,1.5f,1,5);
                
                gl.glMatrixMode(GL.GL_MODELVIEW);
                gl.glLoadIdentity();
                //glu.gluLookAt(0,0,-2,0,0,1,0,1,0);
                gl.glRotatef(xrot,1.0f,0,0);
                gl.glRotatef(yrot,0,1.0f,0);
                gl.glRotatef(zrot,0,0,1.0f);
                xrot+=xstep;
                yrot+=ystep;
                zrot+=zstep;
                
                
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
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
                switch (e.getKeyChar()) {
                    case 'f':
                        FontDrawer fd = dttf;
                        dttf.setFill(!dttf.isFill());
                        break;
                    case 'n':
                        dttf.setFlatNormals(!dttf.isFlatNormals());
                        break;
                    case 'r':
                        Animator anim = gljf.getAnimator();
                        if(anim.isAnimating())
                            anim.stop();
                        else
                            anim.start();
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
                        String up = javax.swing.JOptionPane.showInputDialog("Upper Text:",upperStr.toString());
                        upperStr.delete(0,upperStr.length());
                        upperStr.append(up);
                        String dn = javax.swing.JOptionPane.showInputDialog("Lower Text:",lowerStr.toString());
                        lowerStr.delete(0,lowerStr.length());
                        lowerStr.append(dn);
                        break;
                    case 'F':
                        //TODO:implement font change
                        break;
                }
                gljf.repaint();
            }
        });
        gljf.generateAnimator();
        gljf.setVisible(true);
    }
    
    private class GLUtesselatorCallbackImpl extends javax.media.opengl.glu.GLUtessellatorCallbackAdapter {
        private GL gl;
        
        public GLUtesselatorCallbackImpl(GL gl) {
            this.gl = gl;
        }
        
        public void begin(int type) {
            gl.glBegin(type);
        }
        
        public void vertex(java.lang.Object vertexData) {
            double[] coords = (double[])vertexData;
            
            gl.glVertex3dv(coords,0);
        }
        
        public void end() {
            gl.glEnd();
        }
    }
}

