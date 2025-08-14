/*
 * MimePartDataSource.java
 * Copyright (C) 2002, 2005, 2013 The Free Software Foundation
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

package jakarta.mail.internet;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.UnknownServiceException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import jakarta.activation.DataSource;
import jakarta.mail.MessageAware;
import jakarta.mail.MessageContext;
import jakarta.mail.MessagingException;

/**
 * A data source that manages content for a MIME part.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class MimePartDataSource
  implements DataSource, MessageAware
{

  private static final ResourceBundle L10N
    = ResourceBundle.getBundle("jakarta.mail.internet.L10N");

  /**
   * The part.
   * @since JavaMail 1.4
   */
  protected MimePart part;

  /*
   * Manages a MessageContext on behalf of the part.
   * @see #getMessageContext
   */
  private MessageContext context;

  /**
   * Constructor with a MIME part.
   */
  public MimePartDataSource(MimePart part)
  {
    this.part = part;
  }

  /**
   * Returns an input stream from the MIME part.
   * <p>
   * This method applies the appropriate transfer-decoding, based on the
   * Content-Transfer-Encoding header of the MimePart.
   */
  public InputStream getInputStream()
    throws IOException
  {
    try
      {
        InputStream is;
        if (part instanceof MimeBodyPart)
          {
            is = ((MimeBodyPart) part).getContentStream();
          }
        else if (part instanceof MimeMessage)
          {
            is = ((MimeMessage) part).getContentStream();
          }
        else
          {
            String m = L10N.getString("err.unknown_part_type");
            Object[] args = new Object[] { part.getClass().getName() };
            throw new IOException(MessageFormat.format(m, args));
          }

        String encoding = part.getEncoding();
        return (encoding != null) ? MimeUtility.decode(is, encoding) : is;
      }
    catch (MessagingException e)
      {
        throw (IOException) new IOException().initCause(e);
      }
  }

  public OutputStream getOutputStream()
    throws IOException
  {
    throw new UnknownServiceException();
  }

  public String getContentType()
  {
    try
      {
        return part.getContentType();
      }
    catch (MessagingException e)
      {
        return null;
      }
  }

  public String getName()
  {
    // Shouldn't this return the filename parameter of the
    // Content-Disposition of a MimeBodyPart, if available?
    return "";
  }

  /**
   * Returns the message context for the current part.
   */
  public MessageContext getMessageContext()
  {
    if (context == null)
      {
        context = new MessageContext(part);
      }
    return context;
  }

}

