/*
 * BodyTerm.java
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
import jakarta.mail.Multipart;
import jakarta.mail.Part;

/**
 * A textual search of the message content.
 * Body searches are done only on:
 * <ol>
 * <li>single-part messages whose primary-type is <code>text</code> OR
 * <li>multipart/mixed messages whose first body part's primary-type is
 * <code>text</code>.
 * </ol>
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class BodyTerm
  extends StringTerm
{

  /**
   * Constructor.
   * @param pattern the text to search for
   */
  public BodyTerm(String pattern)
  {
    super(pattern);
  }

  /**
   * Returns true only if the search text was found in the message body.
   */
  public boolean match(Message msg)
  {
    try
      {
        Part part = msg;
        String contentType = part.getContentType();
        if (contentType.regionMatches(true, 0, "text/", 0, 5))
          {
            return super.match((String) part.getContent());
          }
        else if (contentType.regionMatches(true, 0, "multipart/mixed", 0, 15))
          {
            part = ((Multipart) part.getContent()).getBodyPart(0);
            contentType = part.getContentType();
            if (contentType.regionMatches(true, 0, "text/", 0, 5))
              {
                return super.match((String) part.getContent());
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
    return ((other instanceof BodyTerm) && super.equals(other));
  }

}

