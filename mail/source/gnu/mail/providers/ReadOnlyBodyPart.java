/*
 * ReadOnlyBodyPart.java
 * Copyright (C) 2013 Chris Burdess <dog@gnu.org>
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

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeBodyPart;

/**
 * A read-only MIME body part, suitable for subclassing in providers.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.5
 */
public abstract class ReadOnlyBodyPart
  extends MimeBodyPart
{

  public ReadOnlyBodyPart()
  {
  }

  public ReadOnlyBodyPart(InputStream in)
    throws MessagingException
  {
    super(in);
  }

  public ReadOnlyBodyPart(InternetHeaders headers, byte[] content)
    throws MessagingException
  {
    super(headers, content);
  }

  public void setDisposition(String disposition)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setContentID(String cid)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setContentMD5(String md5)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setContentLanguage(String[] languages)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setDescription(String description)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setDescription(String description, String charset)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setFileName(String filename)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setDataHandler(DataHandler dh)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setContent(Object o, String type)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setText(String text)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setText(String text, String charset)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setText(String text, String charset, String subtype)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void setContent(Multipart mp)
    throws MessagingException
  {
    throw new IllegalWriteException();
  }

  public void attachFile(File file)
    throws IOException, MessagingException
  {
    throw new IllegalWriteException();
  }

  public void attachFile(String file)
    throws IOException, MessagingException
  {
    throw new IllegalWriteException();
  }

  public void attachFile(File file, String contentType, String encoding)
    throws IOException, MessagingException
  {
    throw new IllegalWriteException();
  }

  public void attachFile(String file, String contentType, String encoding)
    throws IOException, MessagingException
  {
    throw new IllegalWriteException();
  }

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

}
