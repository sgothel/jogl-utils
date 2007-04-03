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
 */

package net.java.joglutils.msg.nodes;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.opengl.*;

import net.java.joglutils.msg.actions.*;
import net.java.joglutils.msg.elements.*;
import net.java.joglutils.msg.misc.*;

/** Represents a vertex/fragment shader. */
public class ShaderNode extends Node {

  private String vertexShaderCode;
  private String fragmentShaderCode;
  private Shader shader;
  private List<Shader> disposedShaders = new ArrayList<Shader>();

  static {
    // Enable the elements this node affects for known actions
    GLShaderElement.enable(GLRenderAction.getDefaultState());
  }

  /** Initializes this shader from the given String. No OpenGL work is
      done during this call; it is done lazily when the Shader is
      fetched. */
  public void setShader(String fragmentShaderCode) {
      disposeShader();
      this.vertexShaderCode = null;
      this.fragmentShaderCode = fragmentShaderCode;
  }

  /** Initializes this shader from the given String. No OpenGL work is
      done during this call; it is done lazily when the Shader is
      fetched. */
  public void setShader(String vertexShaderCode, String fragmentShaderCode) {
      disposeShader();
      this.vertexShaderCode = vertexShaderCode;
      this.fragmentShaderCode = fragmentShaderCode;
  }

  /** Fetches the Shader object associated with this ShaderNode.
      It is required to call this each frame during rendering.
      An OpenGL context must be current at the time this method is
      called or a GLException will be thrown. */
  public Shader getShader() throws GLException {
    lazyDispose();
    if (shader == null) {
        this.shader = new Shader(vertexShaderCode, fragmentShaderCode);
    }
    return shader;
  }

  public void doAction(Action action) {
    if (ShaderElement.isEnabled(action.getState())) {
      ShaderElement.set(action.getState(), this);
    }
  }

  private synchronized void disposeShader() {
    if (shader != null) {
      disposedShaders.add(shader);
      shader = null;
    }
  }

  private void lazyDispose() {
    while (!disposedShaders.isEmpty()) {
      Shader s = null;
      synchronized (this) {
        s = disposedShaders.remove(disposedShaders.size() - 1);
      }
      s.dispose();
    }
  }
}
