/*
 * RecipientStringTerm.java
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

import jakarta.mail.Address;
import jakarta.mail.Message;

/**
 * A string comparison of the recipient headers in a message.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 * @since JavaMail 1.1
 */
public final class RecipientStringTerm
  extends AddressStringTerm
{

  /**
   * The recipient type.
   */
  private Message.RecipientType type;

  /**
   * Constructor.
   * @param type the recipient type
   * @param address the address pattern for comparison
   */
  public RecipientStringTerm(Message.RecipientType type, String pattern)
  {
    super(pattern);
    this.type = type;
  }

  /**
   * Return the recipient type.
   */
  public Message.RecipientType getRecipientType()
  {
    return type;
  }

  /**
   * Returns true only if the address specified in this term is a substring
   * of the recipient address in the given message.
   */
  public boolean match(Message msg)
  {
    try
      {
        Address[] addresses = msg.getRecipients(type);
        if (addresses != null)
          {
            for (int i = 0; i < addresses.length; i++)
              {
                if (super.match(addresses[i]))
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
    return (other instanceof RecipientStringTerm &&
           ((RecipientStringTerm) other).type.equals(type) &&
            super.equals(other));
  }

  public int hashCode()
  {
    return type.hashCode() + super.hashCode();
  }

}

