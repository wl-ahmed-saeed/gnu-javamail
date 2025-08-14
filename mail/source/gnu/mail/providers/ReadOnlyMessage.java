/*
 * ReadOnlyMessage.java
 * Copyright (C) 2003 Chris Burdess <dog@gnu.org>
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

package gnu.mail.providers;

import java.io.InputStream;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeMessage;

/**
 * Abstract read-only message.
 * The superclass of mail provider messages that do not support message
 * editing in-place.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.0
 */
public abstract class ReadOnlyMessage extends MimeMessage
{

  protected ReadOnlyMessage(Folder folder, int msgnum)
  {
    super(folder, msgnum);
  }

  protected ReadOnlyMessage(Folder folder, InputStream in, int msgnum)
    throws MessagingException
  {
    super(folder, in, msgnum);
  }

  protected ReadOnlyMessage(Folder folder, InternetHeaders headers,
      byte[] content, int msgnum)
    throws MessagingException
  {
    super(folder, msgnum);
  }

  protected ReadOnlyMessage(MimeMessage message)
    throws MessagingException
  {
    super(message);
  }

  // -- content --

  public void setContent(Object o, String type)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setContent(Multipart mp)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  // -- headers --

  public void setHeader(String name, String value)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void addHeader(String name, String value)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void removeHeader(String name)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void addHeaderLine(String line)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  // -- flags --

  public void setFlags(Flags flag, boolean set)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  // -- general --

  public void saveChanges()
    throws MessagingException
  {
  }

}
