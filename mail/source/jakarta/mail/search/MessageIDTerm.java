/*
 * MessageIDTerm.java
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
 * A comparison for the RFC822 "Message-Id", a string message identifier
 * for Internet messages that is supposed to be unique per message.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class MessageIDTerm
  extends StringTerm
{

  /**
   * Constructor.
   * @param msgid the Message-Id to search for
   */
  public MessageIDTerm(String msgid)
  {
    super(msgid);
  }

  /**
   * Returns true if the given message's Message-Id matches the
   * Message-Id specified in this term.
   */
  public boolean match(Message msg)
  {
    try
      {
        String[] messageIDs = msg.getHeader("Message-ID");
        if (messageIDs != null)
          {
            for (int i = 0; i < messageIDs.length; i++)
              {
                if (super.match(messageIDs[i]))
                  {
                    return true;
                  }
              }
          }
      }
    catch (Exception e)
      {
      }
    return false;
  }

  public boolean equals(Object other)
  {
    return (other instanceof MessageIDTerm && super.equals(other));
  }

}

