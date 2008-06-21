/**
 *   Simple test of the TextRenderer3D class.
 *   
 *   Ric Wright
 *   rkwright@geofx.com
 *   June 2008
 */
package jgudemos;

import com.geofx.opengl.util.TextRenderer3D;
import com.sun.opengl.util.Animator;

import java.awt.Font;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

/**
 * TestRenderer3D 
 * Hello World style test of the TextRenderer3D
 * 
 */
public class TestRenderer3D implements GLEventListener
{
	TextRenderer3D	tr3;
	float[] 		LightDiffuse =	 { 1.0f, 1.0f, 1.0f, 1.0f };
	float[] 		LightAmbient =	 { 0.8f, 0.8f, 0.8f, 1.0f };
	float[] 		LightPosition =	 { 1.0f, 1.0f, 1.0f, 0.0f };
	float[]			mat_specular = { 1.0f, 1.0f, 1.0f, 1.0f };
	float[]			mat_ambient_magenta = { 1.0f, 0.0f, 1.0f, 1.0f };
	float[]     	mat_shininess = { 100.0f };	
	
	public static void main(String[] args)
	{
		Frame frame = new Frame("Simple JOGL Application");
		GLCanvas canvas = new GLCanvas();

		canvas.addGLEventListener(new TestRenderer3D());
		frame.add(canvas);
		frame.setSize(640, 480);
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
		System.out.println("init GL called.  GL Class: " + gl.getClass().getName() 
					+ " and this: " + this.getClass().getName());

		gl.setSwapInterval(1);
		
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glShadeModel(GL.GL_SMOOTH); 
		
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, LightAmbient, 0);
		gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, LightDiffuse, 0);

		gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, LightPosition, 0);

		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LESS);
		         
		// Be sure to use a font name on your system otherwise you will get the default
		tr3 = new TextRenderer3D(new Font("Times New Roman", Font.TRUETYPE_FONT, 3), 0.25f);   
	}

	/**
	 * The shape or size of the viewport (client frame) has changed.  We need to re-init
	 * the matrix stack, i.e. the GL_PROJECTION and then initialize back to the GL_MODELVIEW
	 */
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
	{
		GL gl = drawable.getGL();
		GLU glu = new GLU();

		if (height <= 0)  // avoid a divide by zero error!
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
		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();

		gl.glTranslatef(1.5f, 0.0f, -6.0f);
		gl.glRotatef(45.0f, 0, 1, 0);
	
		String str = "abcde";
		Rectangle2D rect = tr3.getBounds(str, 0.25f);
		
		float offX = (float) rect.getCenterX();
		float offY = (float) rect.getCenterY();
		float offZ = tr3.getDepth() / 2.0f;
		
		gl.glEnable(GL.GL_LIGHTING);
		gl.glEnable(GL.GL_LIGHT0);
		
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);
		gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT_AND_DIFFUSE, mat_ambient_magenta, 0);

        tr3.draw(str, -offX, offY, -offZ, 1.0f);	

		// Flush all drawing operations to the graphics card
		gl.glFlush();
	}

	/**
	 * This method essentially always ignored.
	 */
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
	{
	}
}