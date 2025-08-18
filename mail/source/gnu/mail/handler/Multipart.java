/*
 * Multipart.java
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
import java.io.IOException;
import java.io.OutputStream;
import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import jakarta.activation.UnsupportedDataTypeException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMultipart;

/**
 * A JAF data content handler for the multipart/* family of MIME content
 * types.
 * This provides the basic behaviour for any number of MimeMultipart-handling
 * subtypes which simply need to override their default constructor to provide
 * the correct MIME content-type and description.
 */
public class Multipart
  implements DataContentHandler
{

  /**
   * Our favorite data flavor.
   */
  protected ActivationDataFlavor flavor;

  /**
   * Generic constructor.
   */
  public Multipart()
  {
    this("multipart/*", "multipart");
  }

  /**
   * Constructor specifying the data flavor.
   * @param mimeType the MIME content type
   * @param description the description of the content type
   */
  public Multipart(String mimeType, String description)
  {
    flavor = new ActivationDataFlavor(jakarta.mail.internet.MimeMultipart.class,
        mimeType, description);
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
   * @return a byte array
   */
  public Object getContent(DataSource source)
    throws IOException
  {
    try
    {
      return new MimeMultipart(source);
    }
    catch (MessagingException e)
    {
      /* This loses any attached exception */
      throw new IOException(e.getMessage());
    }
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
    if (object instanceof MimeMultipart)
    {
      try
      {
       ((MimeMultipart)object).writeTo(out);
      }
      catch (MessagingException e)
      {
        /* This loses any attached exception */
        throw new IOException(e.getMessage());
      }
    }
    else
      throw new UnsupportedDataTypeException();
  }

}
