/*
 * Message.java
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

package gnu.mail.handler;


import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import jakarta.activation.UnsupportedDataTypeException;
import jakarta.mail.MessageAware;
import jakarta.mail.MessageContext;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

/**
 * A JAF data content handler for the message/* family of MIME content
 * types.
 */
public abstract class Message
  implements DataContentHandler
{

  /**
   * Our favorite data flavor.
   */
  protected ActivationDataFlavor flavor;

  /**
   * Constructor specifying the data flavor.
   * @param mimeType the MIME content type
   * @param description the description of the content type
   */
  protected Message(String mimeType, String description)
  {
    flavor = new ActivationDataFlavor(jakarta.mail.Message.class, mimeType,
        description);
  }

  /**
   * Returns an array of DataFlavor objects indicating the flavors the data
   * can be provided in.
   * @return the DataFlavors
   */
  public ActivationDataFlavor[] getTransferDataFlavors()
  {
    ActivationDataFlavor[] flavors = new ActivationDataFlavor[1];
    flavors[0] = flavor;
    return flavors;
  }

  /**
   * Returns an object which represents the data to be transferred.
   * The class of the object returned is defined by the representation class
   * of the flavor.
   * @param flavor the data flavor representing the requested type
   * @param source the data source representing the data to be converted
   * @return the constructed object
   */
public Object getTransferData(ActivationDataFlavor flavor, DataSource source)
    throws IOException {
  if (this.flavor.equals(flavor)) {
    return getContent(source);
  }
  throw new IOException("Unsupported flavor: " + flavor);
}




  /**
   * Return an object representing the data in its most preferred form.
   * Generally this will be the form described by the first data flavor
   * returned by the <code>getTransferDataFlavors</code> method.
   * @param source the data source representing the data to be converted
   * @return a message
   */
  public Object getContent(DataSource source)
    throws IOException
  {
    try
    {
      Session session = null;
      if (source instanceof MessageAware)
      {
        MessageAware ma = (MessageAware)source;
        MessageContext context = ma.getMessageContext();
        session = context.getSession();
      }
      else
      {
        Properties props = null;
        /* Do we want to pass the system properties in? */
        session = Session.getDefaultInstance(props, null);
      }
      InputStream in = source.getInputStream();
      return new MimeMessage(session, in);
    }
    catch (MessagingException e)
    {
      e.printStackTrace();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Convert the object to a byte stream of the specified MIME type and
   * write it to the output stream.
   * @param object the object to be converted
   * @param mimeType the requested MIME content type to write as
   * @param out the output stream into which to write the converted object
   */
  public void writeTo(Object object, String mimeType, OutputStream out)
    throws IOException
  {
    if (object instanceof jakarta.mail.Message)
    {
      try
      {
       ((jakarta.mail.Message)object).writeTo(out);
      }
      catch (MessagingException e)
      {
        /* not brilliant as we lose any associated exception */
        throw new IOException(e.getMessage());
      }
    }
    else
      throw new UnsupportedDataTypeException();
  }

}
