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

import net.java.joglutils.*;
import javax.media.opengl.*;

/**
 *
 * @author Erik J. Tollerud
 * @created January 12, 2007
 */
public class BasicGLJFrameDemo {
    
    public static void main(String[] args) {
        GLJFrame gljf = new GLJFrame(new GLEventListener() {
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
            }

            public void init(GLAutoDrawable drawable) {
            }

            public void display(GLAutoDrawable drawable) {
                GL gl = drawable.getGL();
                gl.glColor3f(1.0f,0.323f,0.8f);
                gl.glBegin(GL.GL_TRIANGLE_STRIP);
                gl.glVertex2d(0,0);
                gl.glVertex2d(0.5,1.0);
                gl.glVertex2d(-0.3,-0.9);
                gl.glEnd();
            }

            public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
            }
            
        });
        gljf.setDefaultCloseOperation(gljf.EXIT_ON_CLOSE);
        gljf.setVisible(true);
    }
    
}
