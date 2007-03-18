/*
 * Copyright (c) 2007 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 */

package net.java.joglutils.msg.actions;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.util.texture.*;

import net.java.joglutils.msg.elements.*;
import net.java.joglutils.msg.math.*;
import net.java.joglutils.msg.misc.*;
import net.java.joglutils.msg.nodes.*;

/** An action which performs rendering of a scene graph via OpenGL. <P>

    When applied to the root of the scene graph, this action does not
    perform any clearing of the color or depth buffer; this is the
    responsibility of the caller. The render action pushes, pops, and
    initializes enough OpenGL state to isolate itself, at least in
    theory, from any surrounding OpenGL state that the application may
    have set up. There should in theory be no user-visible OpenGL side
    effects as a result of rendering with this action.
*/

public class GLRenderAction extends Action {
  // Boilerplate
  private static State defaults = new State();
  /** Returns the default state all instances of this class are initialized with. */
  public static State getDefaultState() {
    return defaults;
  }

  private State state = new State(defaults);
  public State getState() {
    return state;
  }

  static {
    // FIXME: may need to rethink when and where these elements are enabled

    // In Open Inventor, this is folded into the node's initialization
    // (i.e., the node needs to know which elements it might affect).
    // That in theory allows for fewer elements to be enabled, but it
    // isn't clear that this makes a difference, or that it results in
    // correct code elsewhere where certain elements implicitly expect
    // others to be enabled.
    GLBlendElement            .enable(getDefaultState());
    GLColorElement            .enable(getDefaultState());
    GLCoordinateElement       .enable(getDefaultState());
    GLModelMatrixElement      .enable(getDefaultState());
    GLProjectionMatrixElement .enable(getDefaultState());
    GLViewingMatrixElement    .enable(getDefaultState());
    GLTextureCoordinateElement.enable(getDefaultState());
    GLTextureElement          .enable(getDefaultState());
  }

  // For automatically setting the aspect ratios of cameras we encounter
  private float curAspectRatio = 1.0f;

  private int applyDepth = 0;

  public void apply(Node node) {
    GL gl = GLU.getCurrentGL();
    int depth = applyDepth++;
    try {
      if (depth == 0) {
        // Applying to the root of the scene graph
        // Push necessary GL state
        // FIXME: add in additional bits as we add more capabilities
        gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_TRANSFORM_BIT);
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glColor4f(1, 1, 1, 1);
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glLoadIdentity();
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();
        gl.glPushClientAttrib(GL.GL_CLIENT_VERTEX_ARRAY_BIT);
        // Figure out the aspect ratio of the current viewport
        int[] viewport = new int[4];
        gl.glGetIntegerv(GL.GL_VIEWPORT, viewport, 0);
        curAspectRatio = (float) viewport[2] / (float) viewport[3];
      }
      super.apply(node);
    } finally {
      if (depth == 0) {
        gl.glPopClientAttrib();
        gl.glPopAttrib();
      }
      --applyDepth;
    }
  }

  public void visit(Blend blend) {
    GLBlendElement.set(state,
                       blend.getEnabled(),
                       blend.getBlendColor(),
                       blend.getSourceFunc(),
                       blend.getDestFunc(),
                       blend.getBlendEquation());
  }

  public void visit(Color4 colors) {
    GLColorElement.set(state, colors.getData().getData());
  }

  public void visit(Coordinate3 coords) {
    GLCoordinateElement.set(state, coords.getData().getData());
  }

  public void visit(IndexedTriangleSet tris) {
    throw new RuntimeException("Not yet implemented");
  }

  public void visit(PerspectiveCamera camera) {
    // FIXME: unclear whether we should be doing this, or whether we
    // should have a mechanism which doesn't require mutation of the
    // camera
    camera.setAspectRatio(curAspectRatio);

    GLViewingMatrixElement.set(state, camera.getViewingMatrix());
    GLProjectionMatrixElement.set(state, camera.getProjectionMatrix());
  }

  public void visitPre(Separator sep) {
    state.push();
  }

  public void visitPost(Separator sep) {
    state.pop();
  }

  public void visit(Texture2 texture) {
    GLTextureElement.set(state, texture.getTexture(), texture.getTexEnvMode());
  }

  public void visit(TextureCoordinate2 texCoords) {
    GLTextureCoordinateElement.set(state, texCoords.getData().getData());
  }

  public void visit(Transform transform) {
    GLModelMatrixElement.mult(state, transform.getTransform());
  }

  public void visit(TriangleSet tris) {
    if (CoordinateElement.get(state) != null) {
      // OK, we have coordinates to send down, at least

      GL gl = GLU.getCurrentGL();

      Texture tex = GLTextureElement.get(state);
      boolean haveTexCoords = (GLTextureCoordinateElement.get(state) != null);

      if (tex != null) {
        // Set up the texture matrix to uniformly map [0..1] to the used
        // portion of the texture image
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPushMatrix();
        gl.glLoadTransposeMatrixf(getTextureMatrix(tex).getRowMajorData(), 0);
        gl.glMatrixMode(GL.GL_MODELVIEW);
      } else if (haveTexCoords) {
        // Want to turn off the use of texture coordinates to avoid errors
        // FIXME: not 100% sure whether we need to do this, but think we should
        gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
      }

      // For now, assume the triangle set and the number of available
      // coordinates match -- may want to add debugging information
      // for this later
      gl.glDrawArrays(GL.GL_TRIANGLES, 0, 3 * tris.getNumTriangles());

      if (tex != null) {
        gl.glMatrixMode(GL.GL_TEXTURE);
        gl.glPopMatrix();
        gl.glMatrixMode(GL.GL_MODELVIEW);
      } else if (haveTexCoords) {
        // Might want this the next time we render a shape
        gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
      }
    }
  }

  private Mat4f textureMatrix = new Mat4f();
  private Mat4f getTextureMatrix(Texture texture) {
    textureMatrix.makeIdent();
    TextureCoords coords = texture.getImageTexCoords();
    // Horizontal scale
    textureMatrix.set(0, 0, coords.right() - coords.left());
    // Vertical scale (may be negative if texture needs to be flipped vertically)
    float vertScale = coords.top() - coords.bottom();
    textureMatrix.set(1, 1, vertScale);
    textureMatrix.set(0, 3, coords.left());
    textureMatrix.set(1, 3, coords.bottom());
    return textureMatrix;
  }
}
