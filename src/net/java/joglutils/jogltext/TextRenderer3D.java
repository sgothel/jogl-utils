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
 *
 *
 * Cleaned up, added correct normal calculations and removed extraneous stuff.
 * Also modified method signatures to match, as closely as reasonable, those
 * of TextRenderer.  Also added support for compiling the shapes to display lists
 * on the fly.  Note that the class is now dependent on Java3D for the vecmath
 * package.
 * - Ric Wright, rkwright@geofx.com, June 2008
 */

package net.java.joglutils.jogltext;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.text.StringCharacterIterator;
import java.util.ArrayList;
import java.util.Iterator;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.fixedfunc.GLLightingFunc;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUtessellator;
import com.jogamp.opengl.glu.GLUtessellatorCallback;
import com.jogamp.opengl.glu.GLUtessellatorCallbackAdapter;
import com.jogamp.opengl.math.VectorUtil;

/**
 * This class renders a TrueType Font into OpenGL
 *
 * @author Davide Raccagni
 * @author Erik Tollerud
 * @created January 29, 2004
 */
public class TextRenderer3D
{
	private Font 	font;

	private float 	depth = 0.1f;

	private boolean edgeOnly = false;

	private boolean calcNormals = true;;
	private float	flatness = 0.0001f;

	private final float[] vecA = new float[3];
	private final float[] vecB = new float[3];
	private final float[] normal = new float[3];

	private final GLU 	glu = new GLU();
        private final GL2 		gl = GLU.getCurrentGL().getGL2();

	private int lastIndex = -1;
	private final ArrayList<Integer> listIndex = new ArrayList<Integer>();

	/**
	 * Intstantiates a new TextRenderer3D initially rendering in the specified
	 * font.
	 *
	 * @param font - the initial font for this TextRenderer3D
	 *        depth - the extruded depth for the font
	 * @throws java.lang.NullPointerException
	 *             if the supplied font is null
	 */
	public TextRenderer3D(final Font font, final float depth) throws NullPointerException
	{
		if (font == null)
			throw new NullPointerException("Can't use a null font to create a TextRenderer3D");
		this.font = font;
		this.depth = depth;
	}

	/**
	 * Specifies which font to render with this TextRenderer3D
	 *
	 * @param font
	 *            a font to use for rendering *
	 * @throws java.lang.NullPointerException
	 *             if the supplied font is null
	 */
	public void setFont(final Font font) throws NullPointerException
	{
		if (font == null)
			throw new NullPointerException("Can't set a TextRenderer3D font to null");
		this.font = font;
	}

	/**
	 * Retrieves the Font currently associated with this TextRenderer3D
	 *
	 * @return the Font in which this object renders strings
	 */
	public Font getFont()
	{
		return this.font;
	}

	/**
	 * Determines how long the sides of the rendered text is. In the special
	 * case of 0, the rendering is 2D.
	 *
	 * @param depth
	 *            specifies the z-size of the rendered 3D text. Negative numbers
	 *            will be set to 0.
	 */
	public void setDepth(final float depth)
	{
		if (depth <= 0)
			this.depth = 0;
		else
			this.depth = depth;
	}

	/**
	 * Retrieves the z-depth used for this TextRenderer3D's text rendering.
	 *
	 * @return the z-depth of the rendered 3D text.
	 */
	public float getDepth()
	{
		return this.depth;
	}

	/**
	 * Sets if the text should be rendered as filled polygons or wireframe.
	 *
	 * @param fill
	 *            if true, uses filled polygons, if false, renderings are
	 *            wireframe.
	 */
	public void setFill(final boolean fill)
	{
		this.edgeOnly = !fill;
	}

	/**
	 * Determines if the text is being rendered as filled polygons or
	 * wireframes.
	 *
	 * @return if true, uses filled polygons, if false, renderings are
	 *         wireframe.
	 */
	public boolean isFill()
	{
		return !this.edgeOnly;
	}

	/**
	 * Set the flatness to which the glyph's curves will be flattened
	 *
	 * @return
	 */
	public float getFlatness()
	{
		return flatness;
	}

	/**
	 * Get the current flatness to which the glyph's curves will be flattened
	 *
	 * @return
	 */
	public void setFlatness(final float flatness)
	{
		this.flatness = flatness;
	}

	/**
	 * Sets whether the normals will eb calculated for each face
	 *
	 * @param mode
	 *            the mode to render in. Default is flat.
	 */
	public void setCalcNormals( final boolean normals)
	{
		this.calcNormals = normals;
	}

	/**
	 * Gets whether normals are being calculated
	 *
	 * @see setNormal
	 * @return the normal technique for this TextRenderer3D.
	 */
	public boolean getCalcNormals()
	{
		return this.calcNormals;
	}

	public void draw( final String str, final float xOff, final float yOff, final float zOff, final float scaleFactor )
	{
		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPushMatrix();
		gl.glEnable( GLLightingFunc.GL_NORMALIZE);

		gl.glScalef(scaleFactor, scaleFactor, scaleFactor);
		gl.glTranslatef(xOff, yOff, zOff);

		this.draw(str);

		gl.glPopMatrix();
		gl.glPopAttrib();
	}

	/**
	 * Renders a string into the specified GL object, starting at the (0,0,0)
	 * point in OpenGL coordinates.
	 *
	 * @param str
	 *            the string to render.
	 * @param glu
	 *            a GLU instance to use for the text rendering (provided to
	 *            prevent continuous re-instantiation of a GLU object)
	 * @param gl
	 *            the OpenGL context in which to render the text.
	 */
	public void draw( final String str )
	{
		final GlyphVector gv = font.createGlyphVector(new FontRenderContext(new AffineTransform(), true, true),
				                                new StringCharacterIterator(str));
		final GeneralPath gp = (GeneralPath) gv.getOutline();
		PathIterator pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), flatness);

		// dumpShape(gl, gp);

		if (calcNormals)
			gl.glNormal3f(0, 0, -1.0f);

		tesselateFace(glu, gl, pi, this.edgeOnly, 0.0f);

		if (this.depth != 0.0)
		{
			pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), flatness);

			if (calcNormals)
				gl.glNormal3f(0, 0, 1.0f);

			tesselateFace(glu, gl, pi, this.edgeOnly, this.depth);

			pi = gp.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0), flatness);

			// TODO: add diagonal corner/VBO technique

			drawSides(gl, pi, this.edgeOnly, this.depth);
		}
	}

	/**
	 * Creates the specified string as a display list.  Can subsequently be drawn
	 * by calling "call".
	 *
	 * @param str
	 * @param xOff
	 * @param yOff
	 * @param zOff
	 * @param scaleFactor
	 */
	public int compile( final String str, final float xOff, final float yOff, final float zOff, final float scaleFactor )
	{
		final int index = gl.glGenLists(1);
		gl.glNewList( index, GL2.GL_COMPILE);
		draw( str, xOff, yOff, zOff, scaleFactor);
		gl.glEndList();

		listIndex.add(index);
		return index;
	}

	/**
	 * Creates the specified string as a display list.  Can subsequently be drawn
	 * by calling "call".
	 *
	 * @param str
	 */
	public int compile( final String str )
	{
		final int index = gl.glGenLists(1);
		gl.glNewList( index, GL2.GL_COMPILE);
		draw( str );
		gl.glEndList();

		listIndex.add(index);

		return index;
	}

	/**
	 * Draws the current compiled string, if any.
	 *
	 */
	public void call()
	{
		if (lastIndex != -1)
			gl.glCallList(this.lastIndex);
	}

	/**
	 * Draws the specified compiled string, if any.
	 *
	 */
	public void call( final int index )
	{
		gl.glCallList( index );
	}

	/**
	 * Diposes of the ALL the current compiled shapes, if any.
	 *
	 */
	public void dispose()
	{
		for (final Iterator iter = listIndex.iterator(); iter.hasNext();)
		{
			final int index = (Integer) iter.next();
			gl.glDeleteLists( index, 1);
		}

		lastIndex = -1;
	}

	/**
	 * Diposes of the specified compiled shapes, if it is in the list.
	 * If it is the last-compiled, that index is cleared (set to -1)
	 *
	 */
	public void dispose( final int index )
	{
		for (final Iterator iter = listIndex.iterator(); iter.hasNext();)
		{
			final int i = (Integer) iter.next();
			if (i == index)
			{
				gl.glDeleteLists( index, 1);
				if (index == lastIndex)
					lastIndex = -1;
				break;
			}
		}
	}

	/**
	 * Get the bounding box for the supplied string with the current font, etc.
	 *
	 * @param str
	 * @return
	 */
	public Rectangle2D  getBounds( final String str )
	{
		final GlyphVector gv = font.createGlyphVector(new FontRenderContext(new AffineTransform(), true, true),
				                                new StringCharacterIterator(str));
		final GeneralPath gp = (GeneralPath) gv.getOutline();

		return gp.getBounds2D();
	}

	/**
	 * Get the bounding box for the supplied string for the current font and
	 * specified scale factor.
	 *
	 * @param str
	 * @param scaleFactor
	 * @return
	 */
	public Rectangle2D  getBounds( final String str, final float scaleFactor  )
	{
		gl.glPushAttrib(GL2.GL_TRANSFORM_BIT);
		gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
		gl.glPushMatrix();

		gl.glScalef(scaleFactor, scaleFactor, scaleFactor);

		final GlyphVector gv = font.createGlyphVector(new FontRenderContext(new AffineTransform(), true, true),
				                                new StringCharacterIterator(str));
		final GeneralPath gp = (GeneralPath) gv.getOutline();

		final Rectangle2D rect = gp.getBounds2D();

		gl.glPopMatrix();
		gl.glPopAttrib();

		return rect;
	}

	// construct the sides of each glyph by walking around and extending each vertex
	// out to the depth of the extrusion
	private void drawSides(final GL2 gl, final PathIterator pi, final boolean justBoundary, final float depth)
	{
		if (justBoundary)
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_LINE);
		else
			gl.glPolygonMode(GL.GL_FRONT_AND_BACK, GL2GL3.GL_FILL);

		final float[] lastCoord = new float[3];
		final float[] firstCoord = new float[3];
		final float[] coords = new float[6];

		while ( !pi.isDone() )
		{
			switch (pi.currentSegment(coords))
			{
				case PathIterator.SEG_MOVETO:
					gl.glBegin(GL2ES3.GL_QUADS);
					lastCoord[0] = coords[0];
					lastCoord[1] = coords[1];
					firstCoord[0] = coords[0];
					firstCoord[1] = coords[1];
					break;
				case PathIterator.SEG_LINETO:
					if (calcNormals)
						setNormal(gl, lastCoord[0]-coords[0], lastCoord[1]-coords[1], 0.0f,
								     0.0f, 0.0f, depth);

					lastCoord[2] = 0;
					gl.glVertex3fv(lastCoord, 0);
					lastCoord[2] = depth;
					gl.glVertex3fv(lastCoord, 0);
					coords[2] = depth;
					gl.glVertex3fv(coords, 0);
					coords[2] = 0;
					gl.glVertex3fv(coords, 0);

					if (calcNormals)
					{
						lastCoord[0] = coords[0];
						lastCoord[1] = coords[1];
					}
					break;
				case PathIterator.SEG_CLOSE:
					if(calcNormals)
						setNormal(gl, lastCoord[0]-firstCoord[0], lastCoord[1]-firstCoord[1], 0.0f,
								     0.0f, 0.0f, depth );

					lastCoord[2] = 0;
					gl.glVertex3fv(lastCoord, 0);
					lastCoord[2] = depth;
					gl.glVertex3fv(lastCoord, 0);
					firstCoord[2] = depth;
					gl.glVertex3fv(firstCoord, 0);
					firstCoord[2] = 0;
					gl.glVertex3fv(firstCoord, 0);
					gl.glEnd();
					break;
				default:
					throw new RuntimeException(
							"PathIterator segment not SEG_MOVETO, SEG_LINETO, SEG_CLOSE; Inappropriate font.");
			}

			pi.next();
		}
	}

	// simple convenience for calculating and setting the normal
	private void setNormal ( final GL2 gl, final float x1, final float y1, final float z1, final float x2, final float y2, final float z2 )
	{
	    vecA[0] = x1; vecA[1] = y1; vecA[2] = z1;
	    vecB[0] = x2; vecB[1] = y2; vecB[2] = z2;
	    final float[] normal = VectorUtil.normalizeVec3(VectorUtil.crossVec3(new float[3], vecA, vecB));
		gl.glNormal3f( normal[0], normal[1], normal[2] );
	}

	// routine that tesselates the current set of glyphs
	private void tesselateFace(final GLU glu, final GL2 gl, final PathIterator pi, final boolean justBoundary, final double tessZ)
	{
		final GLUtessellatorCallback aCallback = new GLUtesselatorCallbackImpl(gl);
		final GLUtessellator tess = GLU.gluNewTess();

		GLU.gluTessCallback(tess, GLU.GLU_TESS_BEGIN, aCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_END, aCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_ERROR, aCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_VERTEX, aCallback);
		GLU.gluTessCallback(tess, GLU.GLU_TESS_COMBINE, aCallback);

		GLU.gluTessNormal(tess, 0.0, 0.0, -1.0);

		if ( pi.getWindingRule() == PathIterator.WIND_EVEN_ODD)
			GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_ODD);
		else
			GLU.gluTessProperty(tess, GLU.GLU_TESS_WINDING_RULE, GLU.GLU_TESS_WINDING_NONZERO);

		if (justBoundary)
			GLU.gluTessProperty(tess, GLU.GLU_TESS_BOUNDARY_ONLY, GL.GL_TRUE);
		else
			GLU.gluTessProperty(tess, GLU.GLU_TESS_BOUNDARY_ONLY, GL.GL_FALSE);

		GLU.gluTessBeginPolygon(tess, (double[]) null);


		while (!pi.isDone())
		{
			final double[] coords = new double[3];
			coords[2] = tessZ;
			switch (pi.currentSegment(coords))
			{
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

			pi.next();
		}
		GLU.gluTessEndPolygon(tess);

		GLU.gluDeleteTess(tess);
	}

	// Private class that implements the required callbacks for the tesselator
	private class GLUtesselatorCallbackImpl extends GLUtessellatorCallbackAdapter
	{
		private final GL2 gl;

		public GLUtesselatorCallbackImpl(final GL2 gl)
		{
			this.gl = gl;
		}

		public void begin(final int type)
		{
			gl.glBegin(type);
		}

		public void vertex(final java.lang.Object vertexData)
		{
			final double[] coords = (double[]) vertexData;

			gl.glVertex3dv(coords, 0);
		}

		public void end()
		{
			gl.glEnd();
		}
	}


	//--------------- diagnostic utilities -----------------------------------------
	/*
	private void dumpShape(GL2 gl, GeneralPath path)
	{
		float[] coords = new float[6];
		int		count = 1;

		PathIterator pi = path.getPathIterator(AffineTransform.getScaleInstance(1.0, -1.0));
		FlatteningPathIterator	pif = new FlatteningPathIterator(pi, 0.005);

		while ( !pif.isDone() )
		{
			int type = pif.currentSegment(coords);
			dumpSegment(count++, coords, type );

			pif.next();
		}
	}

	private String Segment[] = { "MoveTo", "LineTo", "QuadTo", "CubicTo", "Close" };
	private String Winding[] = { "EvenOdd", "NonZero" };

	protected void dumpSegment( int num, float[] points, int type )
	{
		System.out.print(num + " " + Segment[type]);

		switch(type)
		{
		case PathIterator.SEG_MOVETO:
		case PathIterator.SEG_LINETO:
			System.out.print(" " + points[0] + "," + points[1] );
			break;

		case PathIterator.SEG_QUADTO:
			System.out.print(" " + points[0] + "," + points[1] + "  " + points[2] + "," + points[3] );
			break;

		case PathIterator.SEG_CUBICTO:
			System.out.print(" " + points[0] + "," + points[1] + "  " + points[2] + "," + points[3] + "  " + points[4] + "," + points[5]  );
			break;
		}

		System.out.println();
	}
	*/
}
