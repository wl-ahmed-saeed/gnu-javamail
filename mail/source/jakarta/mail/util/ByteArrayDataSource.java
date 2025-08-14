/*
 * ByteArrayDataSource.java
 * Copyright (C) 2005, 2013 The Free Software Foundation
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

package jakarta.mail.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import jakarta.activation.DataSource;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParseException;

/**
 * Data source backed by a byte array.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 * @since JavaMail 1.4
 */
public class ByteArrayDataSource
  implements DataSource
{

  private static final ResourceBundle L10N
    = ResourceBundle.getBundle("jakarta.mail.util.L10N");

  private byte[] data;

  private String type;

  private String name = "";

  /**
   * Constructor with a byte array.
   * @param data the byte array
   * @param type the MIME type
   */
  public ByteArrayDataSource(byte[] data, String type)
  {
    this.data = data;
    this.type = type;
  }

  /**
   * Constructor with an input stream.
   * @param is the input stream (will be read to end but not closed)
   * @param type the MIME type
   */
  public ByteArrayDataSource(InputStream is, String type)
    throws IOException
  {
    ByteArrayOutputStream sink = new ByteArrayOutputStream();
    byte[] buf = new byte[4096];
    for (int len = is.read(buf); len != -1; len = is.read(buf))
      sink.write(buf, 0, len);
    data = sink.toByteArray();
    this.type = type;
  }

  /**
   * Constructor with a String.
   * The MIME type should include a charset parameter specifying the charset
   * to use to encode the string; otherwise, the platform default is used.
   * @param data the string
   * @param type the MIME type
   */
  public ByteArrayDataSource(String data, String type)
    throws IOException
  {
    try
      {
        ContentType ct = new ContentType(type);
        String charset = ct.getParameter("charset");
        String jcharset = (charset == null) ?
          MimeUtility.getDefaultJavaCharset() :
          MimeUtility.javaCharset(charset);
        if (jcharset == null)
          throw new UnsupportedEncodingException(charset);
        this.data = data.getBytes(jcharset);
        this.type = type;
      }
    catch (ParseException e)
      {
        String m = L10N.getString("err.parse_type");
        Object[] args = new Object[] { type };
        IOException e2 = new IOException(MessageFormat.format(m, args));
        e2.initCause(e);
        throw e2;
      }
  }

  /**
   * Returns an input stream for the data.
   */
  public InputStream getInputStream()
    throws IOException
  {
    return new ByteArrayInputStream(data);
  }

  /**
   * Returns an output stream.
   * The output stream throws an exception if written to.
   */
  public OutputStream getOutputStream()
    throws IOException
  {
    return new ErrorOutputStream();
  }

  /**
   * Returns the MIME type of the data.
   */
  public String getContentType()
  {
    return type;
  }

  /**
   * Returns the name of the data.
   */
  public String getName()
  {
    return name;
  }

  /**
   * Sets the name of the data.
   * @param name the new name
   */
  public void setName(String name)
  {
    this.name = name;
  }

  static class ErrorOutputStream
    extends OutputStream
  {

    public void write(int c)
      throws IOException
    {
      String m = L10N.getString("err.write_not_allowed");
      throw new IOException(m);
    }

    public void write(byte[] b, int off, int len)
      throws IOException
    {
      String m = L10N.getString("err.write_not_allowed");
      throw new IOException(m);
    }

  }
}
