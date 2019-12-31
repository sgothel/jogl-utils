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

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.text.StringCharacterIterator;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;

/**
 *  This class renders a TrueType Font into OpenGL
 *
 *@author     Davide Raccagni
 *@author     Erik Tollerud
 *@created    January 29, 2004
 */
public class FontDrawer {

    public enum NormalMode {NONE, FLAT, AVERAGED};

    private Font font;
    private float depth;

    private boolean edgeOnly;
    private NormalMode normalMode;

    /**
     * Intstantiates a new FontDrawer initially rendering in the specified font.
     *
     * @param font the initial font for this FontDrawer
     * @throws java.lang.NullPointerException if the supplied font is null
     */
    public FontDrawer(final Font font) throws NullPointerException {
        if (font == null)
            throw new NullPointerException("Can't use a null font to create a FontDrawer");
        this.font = font;
        depth = 0;
        edgeOnly = false;
        normalMode = NormalMode.FLAT;
    }

    /**
     * Specifies which font to render with this FontDrawer
     * @param font a font to use for rendering
     ** @throws java.lang.NullPointerException if the supplied font is null
     */
    public void setFont(final Font font) throws NullPointerException {
        if (font == null)
            throw new NullPointerException("Can't set a FontDrawer font to null");
        this.font = font;
    }

    /**
     * Retrieves the Font currently associated with this FontDrawer
     * @return the Font in which this object renders strings
     */
    public Font getFont() {
        return this.font;
    }

    /**
     * Determines how long the sides of the rendered text is. In the special case of 0, the rendering is 2D.
     * @param depth specifies the z-size of the rendered 3D text. Negative numbers will be set to 0.
     */
    public void setDepth(final float depth) {
        if (depth <= 0)
            this.depth = 0;
        else
            this.depth = depth;
    }

    /**
     * Retrieves the z-depth used for this FontDrawer's text rendering.
     * @return the z-depth of the rendered 3D text.
     */
    public float getDepth() {
        return this.depth;
    }

    /**
     * Sets if the text should be rendered as filled polygons or wireframe.
     * @param fill if true, uses filled polygons, if false, renderings are wireframe.
     */
    public void setFill(final boolean fill) {
        this.edgeOnly = !fill;
    }

    /**
     * Determines if the text is being rendered as filled polygons or wireframes.
     * @return if true, uses filled polygons, if false, renderings are wireframe.
     */
    public boolean isFill() {
        return !this.edgeOnly;
    }

    /**
     * Sets technique for rendering Normals. Available options:
     * None: Performs no calls to glNormal* - best performance
     * Flat: Computes the normal as pointing straight away from the text face.
     * Averaged: Determines side edge normals as the average direction of the two sides (smoother shading).  Faces are still flat.
     *
     * @param mode the mode to render in.  Default is flat.
     */
    public void setNormal( final NormalMode mode ) {
        this.normalMode = mode;
    }

    /**
     * Determines which normal-calculation technique is being used.
     * @see setNormal
     * @return the normal technique for this FontDrawer.
     */
    public NormalMode getNormal() {
        return this.normalMode;
    }

    /**
     * Renders a string into the specified GL object, starting at the (0,0,0) point in OpenGL coordinates.
     * Note that this creates a new GLU instance everytime it is called, which will negatively impact performance.
     *
     * @param str the string to render.
     * @param gl the OpenGL context in which to render the text.
     */
    public void drawString(final String str, final GL2 gl) {
        drawString(str,new GLU(),gl);
    }

    /**
     * Renders a string into the specified GL object, starting at the (0,0,0) point in OpenGL coordinates.
     *
     * @param str the string to render.
     * @param glu a GLU instance to use for the text rendering (provided to prevent continuous re-instantiation of a GLU object)
     * @param gl the OpenGL context in which to render the text.
     */
    public void drawString(final String str, final GLU glu, final GL2 gl) {
        final GlyphVector gv = font.createGlyphVector(
                new FontRenderContext(new AffineTransform(), true, true),
                new StringCharacterIterator(str));
        final GeneralPath gp = (GeneralPath)gv.getOutline();
        PathIterator pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);

        if (this.normalMode != NormalMode.NONE)
            gl.glNormal3f(0,0,-1.0f);
        tesselateFace(glu, gl, pi, pi.getWindingRule(), this.edgeOnly);
        if (this.depth != 0.0) {
            pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
            if (this.normalMode != NormalMode.NONE)
                gl.glNormal3f(0,0,1.0f);
            tesselateFace(glu, gl, pi, pi.getWindingRule(), this.edgeOnly, this.depth);
            pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), 1.0f);
            switch (this.normalMode) {
                //TODO: add diagonal corner/VBO technique
                case NONE:
                    drawSidesNoNorm(gl,pi,this.edgeOnly,this.depth);
                    break;
                case FLAT:
                    drawSidesFlatNorm(gl,pi,this.edgeOnly,this.depth);
                    break;
                case AVERAGED:
                    drawSidesAvgNorm(gl,pi,this.edgeOnly,this.depth);
                    break;
            }

        }

    }

    /**
     * Renders a string into the specified GL object, starting at the (xOff,yOff,zOff) point in OpenGL coordinates.
     * Note that this creates a new GLU instance everytime it is called, which will negatively impact performance.
     *
     * @param xOff the distance to translate the text in the x-direction
     * @param yOff the distance to translate the text in the y-direction
     * @param zOff the distance to translate the text in the z-direction
     * @param str the string to render.
     * @param gl the OpenGL context in which to render the text.
     */
    public void drawString(final String str, final GL2 gl, final float xOff, final float yOff, final float zOff) {
        drawString(str,new GLU(),gl,xOff,yOff,zOff);
    }

    /**
     * Renders a string into the specified GL object, starting at the (xOff,yOff,zOff) point in OpenGL coordinates.
     *
     * @param xOff the distance to translate the text in the x-direction
     * @param yOff the distance to translate the text in the y-direction
     * @param zOff the distance to translate the text in the z-direction
     * @param str the string to render.
     * @param glu a GLU instance to use for the text rendering (provided to prevent continuous re-instantiation of a GLU object)
     * @param gl the OpenGL context in which to render the text.
     */
    public void drawString(final String str, final GLU glu, final GL2 gl, final float xOff, final float yOff, final float zOff) {
        gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
        gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        gl.glPushMatrix();
        gl.glTranslatef(xOff,yOff,zOff);
        this.drawString(str,glu,gl);
        gl.glPopMatrix();
        gl.glPopAttrib();
    }

    private void tesselateFace(final GLU glu, final GL2 gl, final PathIterator pi, final int windingRule, final boolean justBoundary) {
        tesselateFace(glu,gl,pi,windingRule,justBoundary,0);
    }

    private void tesselateFace(final GLU glu, final GL2 gl, final PathIterator pi, final int windingRule, final boolean justBoundary,final double tessZ) {
        final GLUtessellatorCallback aCallback = new GLUtesselatorCallbackImpl(gl);
        final GLUtessellator tess = GLU.gluNewTess();

        GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, aCallback);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_END, aCallback);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, aCallback);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, aCallback);
        GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, aCallback);

        GLU.gluTessNormal(tess, 0.0, 0.0, -1.0);

        switch (windingRule) {
            case PathIterator.WIND_EVEN_ODD:
                GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
                break;
            case PathIterator.WIND_NON_ZERO:
                GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);
                break;
        }

        if (justBoundary) {
            GLU.gluTessProperty(tess, GLU.GLU_TESS_BOUNDARY_ONLY, GL.GL_TRUE);
        } else {
            GLU.gluTessProperty(tess, GLU.GLU_TESS_BOUNDARY_ONLY, GL.GL_FALSE);
        }

        GLU.gluTessBeginPolygon(tess, (double[])null);

        for (; !pi.isDone(); pi.next()) {
            final double[] coords = new double[3];
            coords[2] = tessZ;

            switch (pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    GLU.gluTessBeginContour(tess);
                    break;
                case PathIterator.SEG_LINETO:
                    GLU.gluTessVertex(tess, coords, 0, coords);
                    break;
                case PathIterator.SEG_CLOSE:
                    GLU.gluTessEndContour(tess);
                    break;
            }
        }
        GLU.gluTessEndPolygon(tess);

        GLU.gluDeleteTess(tess);
    }

    private void drawSidesNoNorm(final GL2 gl, final PathIterator pi, final boolean justBoundary,final float tessZ) {
        //TODO: texture coords

        if (justBoundary)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL2GL3.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL2GL3.GL_FILL);

        for(final float[] coords = new float[6];!pi.isDone();pi.next()) {
            final float[] currRender = new float[3];
            switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    gl.glBegin(GL2.GL_QUAD_STRIP);
                    currRender[0] = coords[0];
                    currRender[1] = coords[1];
                    currRender[2] = 0;
                    gl.glVertex3fv(currRender,0);
                    currRender[2] = tessZ;
                    gl.glVertex3fv(currRender,0);
                    break;
                case PathIterator.SEG_LINETO:
                    currRender[0] = coords[0];
                    currRender[1] = coords[1];
                    currRender[2] = 0;
                    gl.glVertex3fv(currRender,0);
                    currRender[2] = tessZ;
                    gl.glVertex3fv(currRender,0);
                    break;
                case PathIterator.SEG_CLOSE:
                    currRender[0] = coords[0];
                    currRender[1] = coords[1];
                    currRender[2] = 0;
                    gl.glVertex3fv(currRender,0);
                    currRender[2] = tessZ;
                    gl.glVertex3fv(currRender,0);
                    gl.glEnd();
                    break;
                default:
                    throw new JogltextException("PathIterator segment not SEG_MOVETO, SEG_LINETO, SEG_CLOSE; Inappropriate font.");
            }
        }
    }

    private void drawSidesFlatNorm(final GL2 gl, final PathIterator pi, final boolean justBoundary,final float tessZ) {
        //TODO: texture coords

        if (justBoundary)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL2GL3.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL2GL3.GL_FILL);

        final float[] lastCoord = new float[3];
        final float[] firstCoord = new float[3];
        for(final float[] coords = new float[6];!pi.isDone();pi.next()) {
            switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    gl.glBegin(GL2ES3.GL_QUADS);
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
                    throw new JogltextException("PathIterator segment not SEG_MOVETO, SEG_LINETO, SEG_CLOSE; Inappropriate font.");
            }
        }
    }


    private void drawSidesAvgNorm(final GL2 gl, final PathIterator pi, final boolean justBoundary,final float tessZ) {
        //TODO: improve performance with some form of caching?
        //TODO: texture coords
        //TODO: check last coord - might not quite be correct?

        if (justBoundary)
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL2GL3.GL_LINE);
        else
            gl.glPolygonMode(GL.GL_FRONT_AND_BACK,GL2GL3.GL_FILL);


        float[] firstCoord = null;
        float[] secondCoord = null;
        float[] thirdCoord = null;
        float[] twoBackCoord = null;
        float[] oneBackCoord = null;
        for(final float[] coords = new float[6];!pi.isDone();pi.next()) {
            switch(pi.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO:
                    firstCoord = new float[3];
                    firstCoord[0] = coords[0];
                    firstCoord[1] = coords[1];
                    gl.glBegin(GL2.GL_QUAD_STRIP);
                    break;
                case PathIterator.SEG_LINETO:
                    if (secondCoord == null) {
                        secondCoord = new float[3];
                        secondCoord[0] = coords[0];
                        secondCoord[1] = coords[1];
                        twoBackCoord = firstCoord.clone();
                        oneBackCoord = secondCoord.clone();
                    } else {
                        final float avgdeltax = oneBackCoord[0] - twoBackCoord[0] + coords[0] - oneBackCoord[0];
                        final float avgdeltay = oneBackCoord[1] - twoBackCoord[1] + coords[1] - oneBackCoord[1];
                        if (thirdCoord == null) {
                            thirdCoord = new float[3];
                            thirdCoord[0] = coords[0];
                            thirdCoord[1] = coords[1];

                        }
                        gl.glNormal3f(-avgdeltay,avgdeltax,0);
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
                    gl.glNormal3f(-avgdeltay,avgdeltax,0);
                    oneBackCoord[2] = 0.0f;
                    gl.glVertex3fv(oneBackCoord,0);
                    oneBackCoord[2] = tessZ;
                    gl.glVertex3fv(oneBackCoord,0);

                    avgdeltax = firstCoord[0] - oneBackCoord[0] + secondCoord[0] - firstCoord[0];
                    avgdeltay = firstCoord[1] - oneBackCoord[1] + secondCoord[1] - firstCoord[1];
                    gl.glNormal3f(-avgdeltay,avgdeltax,0);
                    firstCoord[2] = 0.0f;
                    gl.glVertex3fv(firstCoord,0);
                    firstCoord[2] = tessZ;
                    gl.glVertex3fv(firstCoord,0);

                    avgdeltax = secondCoord[0] - firstCoord[0] + thirdCoord[0] - secondCoord[0];
                    avgdeltay = secondCoord[1] - firstCoord[1] + thirdCoord[1] - secondCoord[1];
                    gl.glNormal3f(-avgdeltay,avgdeltax,0);
                    secondCoord[2] = 0.0f;
                    gl.glVertex3fv(secondCoord,0);
                    secondCoord[2] = tessZ;
                    gl.glVertex3fv(secondCoord,0);

                    gl.glEnd();
                    firstCoord = null;
                    secondCoord = null;
                    thirdCoord = null;
                    twoBackCoord = null;
                    oneBackCoord = null;
                    break;
                default:
                    throw new JogltextException("PathIterator segment not SEG_MOVETO, SEG_LINETO, SEG_CLOSEE; Inappropriate font.");
            }
        }
    }

    private class GLUtesselatorCallbackImpl extends GLUtessellatorCallbackAdapter {
        private final GL2 gl;

        public GLUtesselatorCallbackImpl(final GL2 gl) {
            this.gl = gl;
        }

        public void begin(final int type) {
            gl.glBegin(type);
        }

        public void vertex(final java.lang.Object vertexData) {
            final double[] coords = (double[])vertexData;

            gl.glVertex3dv(coords,0);
        }

        public void end() {
            gl.glEnd();
        }
    }
}

