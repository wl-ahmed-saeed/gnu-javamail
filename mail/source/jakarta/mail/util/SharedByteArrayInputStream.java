/*
 * SharedByteArrayInputStream.java
 * Copyright (C) 2005 The Free Software Foundation
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

package jakarta.mail.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import jakarta.mail.internet.SharedInputStream;

/**
 * A byte array input stream that is shareable between multiple readers.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 * @since JavaMail 1.4
 */
public class SharedByteArrayInputStream
  extends ByteArrayInputStream
  implements SharedInputStream
{

  private int off;

  /**
   * Constructor with a byte array.
   * @param buf the byte array
   */
  public SharedByteArrayInputStream(byte[] buf)
  {
    super(buf);
    off = 0;
  }

  /**
   * Constructor with a byte array, offset and length.
   * @param buf the byte array
   * @param offset the offset at which to start
   * @param len the number of bytes to consider
   */
  public SharedByteArrayInputStream(byte[] buf, int off, int len)
  {
    super(buf, off, len);
    this.off = off;
  }

  /**
   * Returns the current position in the stream.
   */
  public long getPosition()
  {
    return (long) (pos - off);
  }

  /**
   * Returns a new shared input stream, representing the subset of this
   * stream's data from <code>start</code> to <code>end</code>.
   * @param start the starting offset within the stream
   * @param end the end offset within the stream (exclusive)
   */
  public InputStream newStream(long start, long end)
  {
    int len = (int) (end - start);
    return new SharedByteArrayInputStream(buf, off + (int) start, len);
  }

}
