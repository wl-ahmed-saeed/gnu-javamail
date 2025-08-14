/*
 * IMAPMultipartDataSource.java
 * Copyright (C) 2003, 2013 Chris Burdess <dog@gnu.org>
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

package gnu.mail.providers.imap;

import java.util.List;
import jakarta.mail.BodyPart;
import jakarta.mail.MessagingException;
import jakarta.mail.MultipartDataSource;
import jakarta.mail.internet.MimePart;
import jakarta.mail.internet.MimePartDataSource;

import gnu.inet.imap.BODYSTRUCTURE;

/**
 * An IMAP multipart component.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.5
 */
final class IMAPMultipartDataSource
  extends MimePartDataSource
  implements MultipartDataSource
{

  final IMAPMessage message;
  final List<BODYSTRUCTURE.Part> mparts;
  final String section;
  final BodyPart[] parts;

  IMAPMultipartDataSource(IMAPMessage message, MimePart part,
                          BODYSTRUCTURE.Multipart mp, String section)
  {
    super(part);
    this.message = message;
    this.section = section;
    mparts = mp.getParts();
    parts = new BodyPart[mparts.size()];
  }

  public BodyPart getBodyPart(int index)
    throws MessagingException
  {
    if (parts[index] == null)
      {
        String s = Integer.toString(index + 1);
        if (section != null)
          {
            s = new StringBuilder(section).append('.').append(s).toString();
          }
        parts[index] = new IMAPBodyPart(message,
                                        mparts.get(index),
                                        s);

      }
    return parts[index];
  }

  public int getCount()
  {
    return parts.length;
  }

}
