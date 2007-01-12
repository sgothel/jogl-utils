/*
 * Copyright (c) 2006 Erik Tollerud (erik.tollerud@gmail.com) All Rights Reserved.
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
 * The names of Erik Tollerud, Davide Raccagni, Sun Microsystems, Inc. or the names of
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

package net.java.joglutils.jogltext;

import java.awt.*;
import java.awt.geom.*;
import java.awt.font.*;
import java.text.*;
import javax.media.opengl.*;
import javax.media.opengl.glu.*;

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
        gl.glNormal3i(0,0,-1);
        tesselateFace(glu, gl, pi, pi.getWindingRule(), this.edgeOnly);
        if (this.depth != 0.0) {
            pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
            gl.glNormal3i(0,0,1);
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
                        } else if (thirdNorm == null) {
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

