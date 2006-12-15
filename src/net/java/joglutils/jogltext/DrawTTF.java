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
public class DrawTTF extends GLUtessellatorCallbackAdapter  {
    
    private GL gl;
    
    public DrawTTF(GL gl) {
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
    
    
    protected void drawString(GLU glu, GL gl) {
        try {
            Font font = Font.createFont(Font.TRUETYPE_FONT,
                    new FileInputStream("monotxt_.ttf")).deriveFont(100.0f);
            GlyphVector gv = font.createGlyphVector(
                    new FontRenderContext(new AffineTransform(), true, true),
                    new StringCharacterIterator("DR"));
            GeneralPath gp = (GeneralPath)gv.getOutline();
            PathIterator pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
            tesselate(glu, gl, pi, pi.getWindingRule(), false);
        } catch (Exception x) {
        }
    }
    
    public void draw(GL gl, PathIterator pi) {
        boolean closed = false;
        
        for (; !pi.isDone(); pi.next()) {
            double[] coords = new double[6];
            
            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    if (closed) {
                        gl.glEnd();
                        closed = false;
                    }
                    gl.glBegin(GL.GL_LINE_STRIP);
                case PathIterator.SEG_LINETO:
                    gl.glVertex2d(coords[0], coords[1]);
                    break;
                case PathIterator.SEG_CLOSE:
                    gl.glEnd();
                    closed = true;
                    break;
            }
        }
        
        if (!closed) {
            gl.glEnd();
        }
    }
    
    public void tesselate(GLU glu, GL gl, PathIterator pi, int windingRule, boolean justBoundary) {
        GLUtessellatorCallback aCallback = new DrawTTF(gl);
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
            double[] coords = new double[6];
            
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
    }
    public static void main(String[] args) {
        
        GLEventListener listener = new GLEventListener() {
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }

            public void init(GLAutoDrawable drawable) {
            }

            public void display(GLAutoDrawable drawable) {
                GL gl = drawable.getGL();
                final DrawTTF dttf = new DrawTTF(gl);
                
                gl.glBegin(GL.GL_TRIANGLES);
                gl.glVertex2d(0.23,0.89);
                gl.glVertex2d(0.35,-0.89);
                gl.glVertex2d(0.23,0.1);
                gl.glEnd();
                
                dttf.drawString(new GLU(),gl);
            }

            public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
            }
            
        };
        net.java.joglutils.GLJFrame gljf = new net.java.joglutils.GLJFrame("title", listener, 200, 200);
        gljf.setDefaultCloseOperation(gljf.EXIT_ON_CLOSE);
        gljf.setVisible(true);
    }
}

