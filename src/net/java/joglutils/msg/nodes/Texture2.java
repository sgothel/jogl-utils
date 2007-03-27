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

package net.java.joglutils.msg.nodes;

import java.awt.image.*;
import java.io.*;
import java.net.*;

import javax.media.opengl.*;
import com.sun.opengl.util.texture.*;

import net.java.joglutils.msg.actions.*;
import net.java.joglutils.msg.elements.*;

/** Represents a two-dimensional texture. */

public class Texture2 extends Node {
  private TextureData data;
  private Texture texture;
  private int texEnvMode = MODULATE;
  private boolean dirty;

  static {
    // Enable the elements this node affects for known actions
    GLTextureElement.enable(GLRenderAction.getDefaultState());

    TextureElement  .enable(RayPickAction.getDefaultState());
  }

  /** Represents the OpenGL MODULATE texture environment mode. */
  public static final int MODULATE = 1;
  /** Represents the OpenGL DECAL texture environment mode. */
  public static final int DECAL    = 2;
  /** Represents the OpenGL BLEND texture environment mode. */
  public static final int BLEND    = 3;
  /** Represents the OpenGL REPLACE texture environment mode. */
  public static final int REPLACE  = 4;

  /** Initializes this texture from the given file. No OpenGL work is
      done during this call; it is done lazily when the Texture is
      fetched. */
  public void setTexture(File file, boolean mipmap, String fileSuffix) throws IOException {
    data = TextureIO.newTextureData(file, mipmap, fileSuffix);
    dirty = true;
  }

  /** Initializes this texture from the given InputStream. No OpenGL
      work is done during this call; it is done lazily when the
      Texture is fetched. */
  public void setTexture(InputStream stream, boolean mipmap, String fileSuffix) throws IOException {
    data = TextureIO.newTextureData(stream, mipmap, fileSuffix);
    dirty = true;
  }

  /** Initializes this texture from the given URL. No OpenGL work is
      done during this call; it is done lazily when the Texture is
      fetched. */
  public void setTexture(URL url, boolean mipmap, String fileSuffix) throws IOException {
    data = TextureIO.newTextureData(url, mipmap, fileSuffix);
    dirty = true;
  }

  /** Initializes this texture from the given BufferedImage. No OpenGL
      work is done during this call; it is done lazily when the
      Texture is fetched. */
  public void setTexture(BufferedImage image, boolean mipmap) {
    data = TextureIO.newTextureData(image, mipmap);
    dirty = true;
  }
  
  /** Initializes this texture from the given TextureData. No OpenGL
      work is done during this call; it is done lazily when the
      Texture is fetched. */
  public void setTexture(TextureData data) {
    this.data = data;
    dirty = true;
  }

  /** Fetches the Texture object associated with this Texture2 node.
      An OpenGL context must be current at the time this method is
      called or a GLException will be thrown. */
  public Texture getTexture() throws GLException {
    if (dirty) {
      if (texture != null) {
        texture.dispose();
        texture = null;
      }
      texture = TextureIO.newTexture(data);
      data = null;
      dirty = false;
    }
    return texture;
  }

  /** Sets the texture environment mode. Default is MODULATE. */
  public void setTexEnvMode(int mode) {
    if (mode < MODULATE || mode > REPLACE) {
      throw new IllegalArgumentException("Illegal texture environment mode");
    }
    this.texEnvMode = mode;
  }

  /** Returns the texture environment mode: one of MODULATE, DECAL,
      BLEND, or REPLACE. */
  public int getTexEnvMode() {
    return texEnvMode;
  }

  public void doAction(Action action) {
    if (TextureElement.isEnabled(action.getState())) {
      TextureElement.set(action.getState(), this);
    }
  }

  public void rayPick(RayPickAction action) {
    // FIXME: because of the issue of potentially not having an OpenGL
    // context at the time this is called, the TextureElement should
    // be updated to hold a reference to this node, and only the
    // GLTextureElement should poll the texture
  }
}
