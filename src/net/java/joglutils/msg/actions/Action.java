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

import net.java.joglutils.msg.misc.*;
import net.java.joglutils.msg.nodes.*;

/** The base class of all actions, which are applied to nodes in the
    scene graph to implement operations such as rendering. <P>

    Subclasses of Action should define, by convention, a public static
    method <CODE>getDefaultState</CODE>, returning a {@link
    net.java.joglutils.msg.misc.State State} object, which is used to
    enable the elements in the state which should be updated by the
    action's traversal of nodes. Each Action instance maintains a
    State object internally which is initialized from this
    default. Note that different actions may enable different elements
    of the global state.
*/

public abstract class Action {

  /** Applies this Action to a particular node. This is how operations
      such as rendering are initiated. */
  public void apply(Node node) {
    node.doAction(this);
  }

  /** Returns the global state this action encompasses, which is
      altered by the nodes the action traverses. */
  public abstract State getState();

  // Visitor methods, one per node class

  // FIXME: should rethink this mechanism and make it extensible as
  // per the original Open Inventor
  public abstract void visit    (Blend blend);
  public abstract void visit    (Color4 colors);
  public abstract void visit    (Coordinate3 coords);
  public abstract void visit    (IndexedTriangleSet tris);
  public abstract void visit    (PerspectiveCamera camera);
  public abstract void visitPre (Separator sep);
  public abstract void visitPost(Separator sep);
  public abstract void visit    (Texture2 texture);
  public abstract void visit    (TextureCoordinate2 texCoords);
  public abstract void visit    (Transform transform);
  public abstract void visit    (TriangleSet tris);
}
