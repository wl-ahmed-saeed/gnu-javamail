/*
 * SizeTerm.java
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

package jakarta.mail.search;

import jakarta.mail.Message;

/**
 * A comparison of message sizes.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class SizeTerm
  extends IntegerComparisonTerm
{

  /**
   * Constructor.
   * @param comparison the comparison operator
   * @param size the size
   */
  public SizeTerm(int comparison, int size)
  {
    super(comparison, size);
  }

  /**
   * Returns true only if the size of the given message matches the
   * specified size using the specified operator.
   */
  public boolean match(Message msg)
  {
    try
      {
        int size = msg.getSize();
        if (size != -1)
          {
            return super.match(size);
          }
      }
    catch (Exception e)
      {
      }
    return false;
  }

  public boolean equals(Object other)
  {
    return (other instanceof SizeTerm && super.equals(other));
  }

}

