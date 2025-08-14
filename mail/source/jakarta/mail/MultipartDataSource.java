/*
 * MultipartDataSource.java
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

import jakarta.activation.DataSource;

/**
 * A data source that contains body parts.
 *
 * @see DataSource
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public interface MultipartDataSource
  extends DataSource
{

  /**
   * Returns the number of body parts.
   */
  int getCount();

  /**
   * Returns the specified body part.
   * Body parts are numbered starting at 0.
   * @param index the index of the desired body part
   * @exception IndexOutOfBoundsException if the given index is out of range
   */
  BodyPart getBodyPart(int index)
    throws MessagingException;

}
