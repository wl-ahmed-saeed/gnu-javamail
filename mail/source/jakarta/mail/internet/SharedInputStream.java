/*
 * SharedInputStream.java
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

package jakarta.mail.internet;

import java.io.InputStream;

/**
 * An InputStream backed by data that can be shared by multiple readers.
 * The users of such an InputStream can determine the current position in
 * the InputStream, and create new InputStreams representing a subset of
 * the data in the original InputStream.
 * The new InputStream will access the same underlying data as the original,
 * without copying the data in it.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public interface SharedInputStream
{

  /**
   * Returns the current position in the InputStream as an offset from the
   * beginning of the InputStream.
   */
  long getPosition();

  /**
   * Returns a new InputStream representing a subset of the data from this
   * InputStream, from <code>start</code> (inclusive) up to
   * <code>end</code> (exclusive). <code>start</code> must be non-negative.
   * If <code>end</code> is -1, the new stream ends at the same place
   * as this stream. The returned InputStream will also implement the
   * SharedInputStream interface.
   * @param start the start position
   * @param end the end position + 1
   */
  InputStream newStream(long start, long end);

}

