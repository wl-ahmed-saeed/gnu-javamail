/*
 * MessageNumberTerm.java
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
 * A comparison of message numbers.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class MessageNumberTerm
  extends IntegerComparisonTerm
{

  /**
   * Constructor.
   * @param number the message number to match
   */
  public MessageNumberTerm(int number)
  {
    super(EQ, number);
  }

  /**
   * Returns true if the message number of the given message is equal to the
   * message number specified in this term.
   */
  public boolean match(Message msg)
  {
    try
      {
        return super.match(msg.getMessageNumber());
      }
    catch (Exception e)
      {
      }
    return false;
  }

  public boolean equals(Object other)
  {
    return (other instanceof MessageNumberTerm && super.equals(other));
  }

}

