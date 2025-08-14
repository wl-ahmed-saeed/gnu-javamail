/*
 * MessageContext.java
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
 * The context of a datum of message content.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class MessageContext
{

  /**
   * The part.
   */
  private Part part;

  /**
   * Creates a message context describing the given part.
   */
  public MessageContext(Part part)
  {
    this.part = part;
  }

  /**
   * Returns the part containing the content. This may be null.
   */
  public Part getPart()
  {
    return part;
  }

  /**
   * Returns the message that contains the content.
   */
  public Message getMessage()
  {
    Part p = part;
    while (p != null)
      {
        if (p instanceof Message)
          {
            return (Message) p;
          }
        if (p instanceof BodyPart)
          {
            BodyPart bp = (BodyPart) p;
            Multipart mp = bp.getParent();
            p = mp.getParent();
          }
        else
          {
            p = null;
          }
      }
    return null;
  }

  /**
   * Returns the session context.
   */
  public Session getSession()
  {
    Message message = getMessage();
    if (message != null)
      {
        return message.session;
      }
    return null;
  }

}
