/*
 * BodyPart.java
 * Copyright (C) 2002 The Free Software Foundation
 *
 * This file is part of GNU Classpath Extensions (classpathx).
 * For more information please visit https://www.gnu.org/software/classpathx/
 *
 * classpathx is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * classpathx is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with classpathx.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package jakarta.mail;

/**
 * A MIME body part. This is a MIME part occurring inside a multipart in the
 * message content.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public abstract class BodyPart
  implements Part
{

  /**
   * The Multipart object containing this BodyPart.
   */
  protected Multipart parent = null;

  /**
   * Returns the containing Multipart object, or <code>null</code> if not
   * known.
   */
  public Multipart getParent()
  {
    return parent;
  }

  void setParent(Multipart multipart)
  {
    parent = multipart;
  }

}
