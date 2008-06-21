/**
 *   Simple bouncing text example.  Illustrates use of the TextRenderer3D class
 *   
 *   Ric Wright
 *   rkwright@geofx.com
 *   June 2008
 */
package jgudemos;

import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUquadric;
import javax.vecmath.Point3f;

import com.geofx.opengl.util.TextRenderer3D;
import com.sun.opengl.util.Animator;

/**
 * Simple class to demonstrate the use of compile/call with
 * TextRenderer3D
 * 
 */
public class BouncingText3D implements GLEventListener
{
	TextRenderer3D		tr3;
	
	float[] 			LightDiffuse =	 { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] 			LightAmbient =	 { 0.8f, 0.8f, 0.8f, 1.0f };
	float[] 			LightPosition =	 { 1.0f, 1.0f, 1.0f, 0.0f };
	float[]				mat_specular = { 1.0f, 1.0f, 1.0f, 1.0f };
	float[]				mat_ambient_magenta = { 1.0f, 0.0f, 1.0f, 1.0f };
	float[]     		mat_shininess = { 100.0f };	
	protected Random	random = new Random();

	public static final int NUM_ITEMS = 20;
	public static final int MAX_ITEMS = 200;
	private static int 	numItems = NUM_ITEMS;

	private ArrayList<TextInfo3D> textInfo = new ArrayList<TextInfo3D>();

	private GLU glu = new GLU();
	protected GLUquadric QUADRIC = glu.gluNewQuadric();

	/**
	 * Main entry point for the app.  The only argument that is parsed 
	 * out is the number of items
	 * @param args
	 */
	public static void main(String[] args)
	{
		if (args != null && Array.getLength(args) > 0)
		{
			numItems = Integer.parseInt(args[0].trim());
			if (numItems == 0)
				numItems = NUM_ITEMS;
			else if (numItems > MAX_ITEMS)
				numItems = MAX_ITEMS;
		}
		
		Frame frame = new Frame("Bouncing Text 3D");
		GLCanvas canvas = new GLCanvas();

		canvas.addGLEventListener(new BouncingText3D());
		frame.add(canvas);
		frame.setSize(800, 600);
		final Animator animator = new Animator(canvas);
		frame.addWindowListener(new WindowAdapter()
		{

			@Override
			public void windowClosing(WindowEvent e)
			{
				// Run this on another thread than the AWT event queue to
				// make sure the call to Animator.stop() completes before
				// exiting
				new Thread(new Runnable()
				{

					public void run()
					{
						animator.stop();
						System.exit(0);
					}
				}).start();
			}
		});
		
		// Center frame
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
		animator.start();
	}

	/**
	 * Initialize the GL instance.  Set up the lights and other
	 * variables and conditions specific to this class
	 */
	public void init(GLAutoDrawable drawable)
	{
		GL gl = drawable.getGL();
		System.out.println("INIT GL IS: " + gl.getClass().getName());

		System.out.println("init GL called.  GL Class: " + gl.getClass().getName() 
				+ " and this: " + this.getClass().getName());

		gl.setSwapInterval(1);

		// Setup the drawing area and shading mode
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glShadeModel(GL.GL_SMOOTH); 
		
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, LightAmbient, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, LightDiffuse, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, LightPosition, 0);

		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);

		gl.glEnable(GL.GL_BLEND);
		gl.glEnable(GL.GL_LINE_SMOOTH);		
		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

		// Note that it has to be a TRUETYPE font - not OpenType.  Apparently, AWT can't
		// handle CFF glyphs
		tr3 = new TextRenderer3D(new Font("Times New Roman", Font.TRUETYPE_FONT, 3), 0.25f);   
		
		// Create random text
		textInfo.clear();
		for (int i = 0; i < numItems; i++)
		{
			textInfo.add(randomTextInfo());
		}

	}

	/**
	 * The shape or size of the viewport (client frame) has changed.  We need to re-init
	 * the matrix stack, i.e. the GL_PROJECTION and then initialize back to the GL_MODELVIEW
	 */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		GL gl = drawable.getGL();

		if (height <= 0)   // avoid a divide by zero error!
			height = 1;
		
		final float h = (float) width / (float) height;
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluPerspective(45.0f, h, 1.0, 20.0);
		
		gl.glMatrixMode(GL.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	/**
	 * Display needs to be re-rendered.  This is where all the heavy-lifting
	 * gets done.
	 */
	public void display(GLAutoDrawable drawable)
	{
		GL gl = drawable.getGL();

		// Clear the drawing area
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);

		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();

		gl.glTranslatef(0.0f, 0.0f, -3.0f);
		gl.glRotatef(45.0f, 1, 0, 0);
		gl.glRotatef(-30.0f, 0, 1, 0);
	
		try
		{
			gl.glEnable(GL.GL_LIGHTING);
			gl.glEnable(GL.GL_LIGHT0);

			drawAxes(gl);
		
			for (Iterator iter = textInfo.iterator(); iter.hasNext();)
			{
				TextInfo3D info = (TextInfo3D) iter.next();
				
				updateTextInfo( info );
					
				gl.glPushAttrib(GL.GL_TRANSFORM_BIT);
				gl.glMatrixMode(GL.GL_MODELVIEW);
				gl.glPushMatrix();
				gl.glEnable( GL.GL_NORMALIZE);
				
				gl.glTranslatef(info.position.x, info.position.y, info.position.z);
				gl.glRotatef(info.angle.x, 1, 0, 0);
				gl.glRotatef(info.angle.y, 0, 1, 0);
				gl.glRotatef(info.angle.z, 0, 0, 1);
				
				// System.out.println(" x,y,z: " + info.position.x + " " + info.position.y + " " + info.position.z + " angle: " + info.angle );
			
				gl.glMaterialfv(GL.GL_FRONT_AND_BACK, GL.GL_AMBIENT_AND_DIFFUSE, info.material, 0);

				tr3.call(info.index);
				
				gl.glPopMatrix();
				gl.glPopAttrib();	
			}
			           
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * This method essentially always ignored.
	 */
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
	{
	}
	
	//------------------ Private Stuff below here ------------------------------
	private static final float 	INIT_ANG_VEL_MAG = 0.3f;
	private static final float 	INIT_VEL_MAG = 0.25f;
	private static final float 	MAX_BOUNDS = 1.5f;
	private static final float  SCALE_FACTOR = 0.05f;

	// Information about each piece of text
	private static class TextInfo3D
	{
		Point3f	angularVelocity;
		Point3f	velocity;
		Point3f	position;
		Point3f	angle;
		float 	h;
		float 	s;
		float 	v;
		int		index;		// display list index
		float 	curTime;	// Cycle the saturation
		float[] material = new float[4];

		// Cache of the RGB color
		float 	r;
		float 	g;
		float 	b;

		String 	text;
	}

	Point3f		tmp = new Point3f();
	
	private void updateTextInfo( TextInfo3D info )
	{
		// Update velocities and positions of all text
		float deltaT = 0.1f; 

		// Randomize things a little bit every little once in a while
		if (random.nextInt(10000) == 0)
		{
			info.angularVelocity = randomRotation(INIT_ANG_VEL_MAG, INIT_ANG_VEL_MAG, INIT_ANG_VEL_MAG);
			info.velocity = randomVelocity(INIT_VEL_MAG, INIT_VEL_MAG, INIT_VEL_MAG);
		}

		// Now update angles and positions
		tmp.set(info.angularVelocity);
		tmp.scale(deltaT*deltaT);
		info.angle.add(tmp);
		
		tmp.set(info.velocity);
		tmp.scale(deltaT);
		info.position.add(tmp);

		// Wrap angles and positions
		info.angle.x = clampAngle(info.angle.x);
		info.angle.y = clampAngle(info.angle.y);
		info.angle.z = clampAngle(info.angle.z);
	
		info.velocity.x = clampBounds(info.position.x, info.velocity.x );
		info.velocity.y = clampBounds(info.position.y, info.velocity.y );
		info.velocity.z = clampBounds(info.position.z, info.velocity.z );
	}

	private float clampBounds( float pos, float velocity )
	{
		if (pos < -MAX_BOUNDS || pos > MAX_BOUNDS)
		{
			velocity *= -1.0f;
		}
		
		return velocity;
	}
	
	private float clampAngle(float angle)
	{
		if (angle < 0)
		{
			angle += 360;
		}
		else if (angle > 360)
		{
			angle -= 360;
		}
		
		return angle;
	}
	
	private TextInfo3D randomTextInfo()
	{
		TextInfo3D info = new TextInfo3D();
		info.text = randomString();
		info.angle = randomRotation(INIT_ANG_VEL_MAG, INIT_ANG_VEL_MAG, INIT_ANG_VEL_MAG);
		info.position = randomVector(MAX_BOUNDS, MAX_BOUNDS, MAX_BOUNDS);
		
		
		Rectangle2D rect = tr3.getBounds(info.text, SCALE_FACTOR);

		float offX = (float) rect.getCenterX();
		float offY = (float) rect.getCenterY();
		float offZ = tr3.getDepth() / 2.0f;

		tr3.setDepth(0.1f + random.nextFloat() * 2.0f);
		info.index = tr3.compile(info.text, -offX, offY, -offZ, SCALE_FACTOR);

		info.angularVelocity = randomRotation(INIT_ANG_VEL_MAG, INIT_ANG_VEL_MAG, INIT_ANG_VEL_MAG);
		info.velocity = randomVelocity(INIT_VEL_MAG, INIT_VEL_MAG, INIT_VEL_MAG);

		Color c = randomColor();
		c.getColorComponents(info.material);
		// Color doesn't set the opacity,so set it to some random non-zero value
		info.material[3] = random.nextFloat() * 0.9f + 0.1f;
	
		return info;
	}

	private String randomString()
	{
		switch (random.nextInt(3))
		{
		case 0:
			return "OpenGL";
		case 1:
			return "Java3D";
		default:
			return "JOGL";
		}
	}

	private Point3f randomVector(float x, float y, float z)
	{
		return new Point3f(x * random.nextFloat(), y * random.nextFloat(), z * random.nextFloat());
	}

	private Point3f randomVelocity(float x, float y, float z)
	{
		return new Point3f(x * (random.nextFloat() - 0.5f), y * (random.nextFloat() - 0.5f), z * (random.nextFloat() - 0.5f));
	}

	private Point3f randomRotation(float x, float y, float z)
	{
		return new Point3f(random.nextFloat() * 360.0f, random.nextFloat() * 360.0f, random.nextFloat() * 360.0f);
	}
	
	private Color randomColor()
	{
		// Get a bright and saturated color
		float r = 0;
		float g = 0;
		float b = 0;
		float s = 0;
		do
		{
			r = random.nextFloat();
			g = random.nextFloat();
			b = random.nextFloat();

			float[] hsb = Color.RGBtoHSB((int) (255.0f * r), (int) (255.0f * g), (int) (255.0f * b), null);
			s = hsb[1];
		} 
		while ((r < 0.6f && g < 0.6f && b < 0.6f) || s < 0.8f);
		
		return new Color(r, g, b);
	}
	
	// draw some striped-pole axes for visdual reference
	protected void drawAxes(GL gl)
	{
		float[]			mat_ambient_red = { 1.0f, 0.0f, 0.0f, 1.0f };
		float[]			mat_ambient_green = { 0.0f, 1.0f, 0.0f, 1.0f };
		float[]			mat_ambient_blue = { 0.0f, 0.0f, 1.0f, 1.0f };
		
		drawAxis(gl, 2, mat_ambient_red);

		drawAxis(gl, 0, mat_ambient_blue);

		drawAxis(gl, 1, mat_ambient_green);
	}

	// draw a single striped pole axis
	private void drawAxis(GL gl, int rot, float[] material )
	{
		float[]			mat_ambient_grey = { 0.5f, 0.5f, 0.5f, 1.0f };
		final  double AXIS_RADIUS =	0.01;
		final  int	  AXIS_HEIGHT =	5;
		final  float  AXIS_STEP  =	0.25f;

		gl.glPushMatrix();
		
		if (rot == 1)
			gl.glRotatef(90, 1, 0, 0);
		else if (rot == 0)
			gl.glRotatef(90, 0, 1, 0);
			
		gl.glTranslatef(0.0f, 0.0f, (float)-AXIS_HEIGHT/2.0f);
		
		float 	pos = -AXIS_HEIGHT/2.0f;
		int		i = 0;
		while ( pos < AXIS_HEIGHT/2.0f )
		{
			if ((i++ & 1)==0)
				gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, material, 0);
			else
				gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, mat_ambient_grey, 0);
				
			glu.gluCylinder(QUADRIC, AXIS_RADIUS, AXIS_RADIUS, AXIS_STEP, 8, 1);
			gl.glTranslatef(0.0f, 0.0f, AXIS_STEP);
			pos += AXIS_STEP;
		}
		
		gl.glPopMatrix();
	}
}