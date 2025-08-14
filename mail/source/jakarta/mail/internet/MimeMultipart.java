/*
 * MimeMultipart.java
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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import jakarta.activation.DataSource;
import jakarta.mail.BodyPart;
import jakarta.mail.MessageAware;
import jakarta.mail.MessageContext;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.MultipartDataSource;

import gnu.inet.util.GetSystemPropertyAction;
import gnu.inet.util.LineInputStream;

/**
 * A MIME multipart container.
 * <p>
 * The default multipart subtype is "mixed". However, an application can
 * construct a MIME multipart object of any subtype using the
 * <code>MimeMultipart(String)</code> constructor.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class MimeMultipart
  extends Multipart
{

  private static final ResourceBundle L10N
    = ResourceBundle.getBundle("jakarta.mail.internet.L10N");

  /**
   * The data source supplying the multipart data.
   */
  protected DataSource ds;

  /**
   * Indicates whether the data from the input stream has been parsed yet.
   */
  protected boolean parsed;

  /**
   * Indicates whether the final boundary line of the multipart has been
   * seen.
   * @since JavaMail 1.5
   */
  protected boolean complete;

  /**
   * The preamble text before the first boundary line.
   * @since JavaMail 1.5
   */
  protected String preamble;

  /**
   * Flag corresponding to the
   * <code>mail.mime.multipart.ignoremissingendboundary</code>
   * property, set in {@link #initializeProperties} which is called from
   * constructors and the parse method.
   * @since JavaMail 1.5
   */
  protected boolean ignoreMissingEndBoundary = true;

  /**
   * Flag corresponding to the
   * <code>mail.mime.multipart.ignoremissingboundaryparameter</code>
   * property, set in {@link #initializeProperties} which is called from
   * constructors and the parse method.
   * @since JavaMail 1.5
   */
  protected boolean ignoreMissingBoundaryParameter = true;

  /**
   * Flag corresponding to the
   * <code>mail.mime.multipart.ignoreexistingboundaryparameter</code>
   * property, set in {@link #initializeProperties} which is called from
   * constructors and the parse method.
   * @since JavaMail 1.5
   */
  protected boolean ignoreExistingBoundaryParameter = false;

  /**
   * Flag corresponding to the
   * <code>mail.mime.multipart.allowempty</code>
   * property, set in {@link #initializeProperties} which is called from
   * constructors and the parse method.
   * @since JavaMail 1.5
   */
  protected boolean allowEmpty = false;

  /**
   * Constructor for an empty MIME multipart of type "multipart/mixed".
   */
  public MimeMultipart()
  {
    this("mixed");
  }

  /**
   * Constructor for an empty MIME multipart of the given subtype.
   */
  public MimeMultipart(String subtype)
  {
    initializeProperties();
    String boundary = MimeUtility.getUniqueBoundaryValue();
    ContentType ct = new ContentType("multipart", subtype, null);
    ct.setParameter("boundary", boundary);
    contentType = ct.toString();
    parsed = true;
  }

  /**
   * Constructor with a given data source.
   * @param ds the data source, which can be a MultipartDataSource
   */
  public MimeMultipart(DataSource ds)
    throws MessagingException
  {
    initializeProperties();
    if (ds instanceof MessageAware)
      {
        MessageContext mc = ((MessageAware) ds).getMessageContext();
        setParent(mc.getPart());
      }
    if (ds instanceof MultipartDataSource)
      {
        setMultipartDataSource((MultipartDataSource) ds);
        parsed = true;
      }
    else
      {
        this.ds = ds;
        contentType = ds.getContentType();
        parsed = false;
      }
  }

  /**
   * Constructor with the "mixed" subtype and specified body parts.
   * More body parts may be added.
   * @since JavaMail 1.5
   */
  public MimeMultipart(BodyPart... parts) throws MessagingException
  {
    this("mixed", parts);
  }

  /**
   * Constructor with the specified subtype and body parts.
   * More body parts may be added later.
   * @since JavaMail 1.5
   */
  public MimeMultipart(String subtype, BodyPart... parts)
    throws MessagingException
  {
    this(subtype);
    for (int i = 0; i < parts.length; i++)
      {
        addBodyPart(parts[i]);
      }
  }

  /**
   * Initialize various flags that control parsing behavior.
   * @since JavaMail 1.5
   */
  protected void initializeProperties()
  {
    PrivilegedAction a;
    a = new GetSystemPropertyAction("mail.mime.multipart." +
                                    "ignoremissingendboundary");
    ignoreMissingEndBoundary =
      !"false".equals(AccessController.doPrivileged(a));
    a = new GetSystemPropertyAction("mail.mime.multipart." +
                                    "ignoremissingboundaryparameter");
    ignoreMissingBoundaryParameter =
      !"false".equals(AccessController.doPrivileged(a));
    a = new GetSystemPropertyAction("mail.mime.multipart." +
                                    "ignoreexistingboundaryparameter");
    ignoreExistingBoundaryParameter =
      "true".equals(AccessController.doPrivileged(a));
    a = new GetSystemPropertyAction("mail.mime.multipart.allowempty");
    allowEmpty = "true".equals(AccessController.doPrivileged(a));
  }

  /**
   * Sets the subtype.
   */
  public void setSubType(String subtype)
    throws MessagingException
  {
    ContentType ct = new ContentType(contentType);
    ct.setSubType(subtype);
    contentType = ct.toString();
  }

  /**
   * Returns the number of component body parts.
   */
  public int getCount()
    throws MessagingException
  {
    synchronized (this)
      {
        parse();
        return super.getCount();
      }
  }

  /**
   * Returns the specified body part.
   * Body parts are numbered starting at 0.
   * @param index the body part index
   * @exception MessagingException if no such part exists
   */
  public BodyPart getBodyPart(int index)
    throws MessagingException
  {
    synchronized (this)
      {
        parse();
        return super.getBodyPart(index);
      }
  }

  /**
   * Returns the body part identified by the given Content-ID (CID).
   * @param CID the Content-ID of the desired part
   */
  public BodyPart getBodyPart(String CID)
    throws MessagingException
  {
    synchronized (this)
      {
        parse();
        int count = getCount();
        for (int i = 0; i < count; i++)
          {
            MimeBodyPart bp = (MimeBodyPart) getBodyPart(i);
            String contentID = bp.getContentID();
            if (contentID != null && contentID.equals(CID))
              {
                return bp;
              }
          }
        return null;
      }
  }

  /**
   * Updates the headers of this part to be consistent with its content.
   */
  protected void updateHeaders()
    throws MessagingException
  {
    if (parts == null)
      {
        return;
      }
    synchronized (parts)
    {
      int len = parts.size();
      for (int i = 0; i < len; i++)
        {
         ((MimeBodyPart) parts.get(i)).updateHeaders();
        }
    }
  }

  /**
   * Writes this multipart to the specified output stream.
   * This method iterates through all the component parts, outputting each
   * part separated by the Content-Type boundary parameter.
   */
  public void writeTo(OutputStream os)
    throws IOException, MessagingException
  {
    final String charset = "US-ASCII";
    final byte[] sep = { 0x0d, 0x0a };

    parse();
    ContentType ct = new ContentType(contentType);
    String boundaryParam = ct.getParameter("boundary");
    if (boundaryParam == null)
      {
        if (!ignoreMissingBoundaryParameter)
          {
            String m = L10N.getString("err.missing_boundary_parameter");
            throw new MessagingException(m);
          }
      }
    byte[] boundary = ("--" + boundaryParam).getBytes(charset);

    if (preamble != null)
      os.write(preamble.getBytes(charset));

    synchronized (parts)
      {
        int len = parts.size();
        for (int i = 0; i < len; i++)
          {
            os.write(boundary);
            os.write(sep);
            os.flush();
            ((MimeBodyPart) parts.get(i)).writeTo(os);
            os.write(sep);
          }
      }

    boundary = ("--" + boundaryParam + "--").getBytes(charset);
    os.write(boundary);
    os.write(sep);
    os.flush();
  }

  /**
   * Parses the body parts from this multipart's data source.
   */
  protected void parse()
    throws MessagingException
  {
    if (parsed)
      {
        return;
      }
    synchronized (this)
      {
        initializeProperties();
        InputStream is = null;
        SharedInputStream sis = null;
        try
          {
            is = ds.getInputStream();
            if (is instanceof SharedInputStream)
              {
                sis = (SharedInputStream) is;
              }
            // buffer it
            if (!(is instanceof ByteArrayInputStream) &&
                !(is instanceof BufferedInputStream))
              {
                is = new BufferedInputStream(is);
              }
            ContentType ct = new ContentType(contentType);
            String boundaryParam = ct.getParameter("boundary");
            String boundary = null;
            if (!ignoreExistingBoundaryParameter && boundaryParam != null)
              {
                boundary = "--" + boundaryParam;
              }
            if (boundary == null &&
                !ignoreMissingBoundaryParameter &&
                !ignoreExistingBoundaryParameter)
              {
                String m = L10N.getString("err.missing_boundary_parameter");
                throw new MessagingException(m);
              }

            LineInputStream lis = new LineInputStream(is);
            String line;
            StringBuilder preambleBuf = null;
            // Find start boundary
            while ((line = lis.readLine()) != null)
              {
                String l = trim(line);
                if (boundary == null && l.startsWith("--") &&
                    !l.endsWith("--"))
                  {
                    boundary = l.substring(2).trim();
                    break;
                  }
                else if (l.equals(boundary))
                  {
                    break;
                  }
                if (preambleBuf == null)
                  {
                    preambleBuf = new StringBuilder();
                  }
                preambleBuf.append(line);
                preambleBuf.append('\n');
              }
            if (preambleBuf != null)
              {
                preamble = preambleBuf.toString();
              }
            if (line == null && !allowEmpty)
              {
                String m = L10N.getString("err.missing_start_boundary");
                throw new MessagingException(m);
              }

            byte[] bbytes = boundary.getBytes("US-ASCII");
            long start = 0L, end = 0L;
            for (boolean done = false; !done;)
              {
                InternetHeaders headers = null;
                if (sis != null)
                  {
                    start = sis.getPosition();
                    do
                      {
                        line = trim(lis.readLine());
                      }
                    while (line != null && line.length() > 0);
                    if (line == null)
                      {
                        if (!ignoreMissingEndBoundary)
                          {
                            String m =
                              L10N.getString("err.missing_end_boundary");
                            throw new MessagingException(m);
                          }
                        break;
                      }
                  }
                else
                  {
                    headers = createInternetHeaders(is);
                  }
                ByteArrayOutputStream bos = null;
                if (sis == null)
                  {
                    bos = new ByteArrayOutputStream();
                  }

                // NB this routine uses the InputStream.mark() method
                // if it is not supported by the underlying stream
                // we will run into problems
                if (!is.markSupported())
                  {
                    String m = L10N.getString("err.mark_not_supported");
                    Object[] args = new Object[] { is.getClass().getName() };
                    m = MessageFormat.format(m, args);
                    throw new MessagingException(m);
                  }
                boolean eol = true;
                int last = -1;
                int afterLast = -1;
                while (true)
                  {
                    int c;
                    if (eol)
                      {
                        is.mark(bbytes.length + 1024);
                        int pos = 0;
                        while (pos < bbytes.length)
                          {
                            if (is.read() != bbytes[pos])
                              {
                                break;
                              }
                            pos++;
                          }

                        if (pos == bbytes.length)
                          {
                            c = is.read();
                            if (c == '-' && is.read() == '-')
                              {
                                done = true;
                                complete = true;
                                break;
                              }
                            while (c == ' ' || c == '\t')
                              {
                                c = is.read();
                              }
                            if (c == '\r')
                              {
                                is.mark(1);
                                if (is.read() != '\n')
                                  {
                                    is.reset();
                                  }
                                break;
                              }
                            if (c == '\n')
                              {
                                break;
                              }
                          }
                        if (bos != null && last != -1)
                          {
                            bos.write(last);
                            if (afterLast != -1)
                              {
                                bos.write(afterLast);
                              }
                            last = afterLast = -1;
                          }
                        is.reset();
                      }
                    c = is.read();
                    if (c < 0)
                      {
                        done = true;
                        break;
                      }
                    else if (c == '\r' || c == '\n')
                      {
                        eol = true;
                        if (sis != null)
                          {
                            end = sis.getPosition() - 1L;
                          }
                        last = c;
                        if (c == '\r')
                          {
                            is.mark(1);
                            if ((c = is.read()) == '\n')
                              {
                                afterLast = c;
                              }
                            else
                              {
                                is.reset();
                              }
                          }
                      }
                    else
                      {
                        eol = false;
                        if (bos != null)
                          {
                            bos.write(c);
                          }
                      }
                  }

                // Create a body part from the stream
                MimeBodyPart bp;
                if (sis != null)
                  {
                    bp = createMimeBodyPart(sis.newStream(start, end));
                  }
                else
                  {
                    bp = createMimeBodyPart(headers, bos.toByteArray());
                  }
                addBodyPart(bp);
              }

          }
        catch (IOException e)
          {
            throw new MessagingException(null, e);
          }
        parsed = true;
        if (!complete)
          {
            if (!ignoreMissingEndBoundary)
              {
                String m = L10N.getString("err.missing_end_boundary");
                throw new MessagingException(m);
              }
          }
      }
  }

  /**
   * Indicates whether the final boundary line for this multipart has been
   * parsed.
   * @since JavaMail 1.4
   */
  public boolean isComplete()
    throws MessagingException
  {
    return complete;
  }

  /**
   * Returns the preamble text (if any) before the first boundary line in
   * this multipart's body.
   * @since JavaMail 1.4
   */
  public String getPreamble()
    throws MessagingException
  {
    return preamble;
  }

  /**
   * Sets the preamble text to be emitted before the first boundary line.
   * @param preamble the preamble text
   * @since JavaMail 1.4
   */
  public void setPreamble(String preamble)
    throws MessagingException
  {
    this.preamble = preamble;
  }

  /*
   * Ensures that CR is stripped from the end of the given line.
   */
  private static String trim(String line)
  {
    if (line == null)
      {
        return null;
      }
    line = line.trim();
    int len = line.length();
    if (len > 0 && line.charAt(len - 1) == '\r')
      {
        line = line.substring(0, len - 1);
      }
    return line;
  }

  /**
   * Creates headers from the specified input stream.
   * @param is the input stream to read the headers from
   */
  protected InternetHeaders createInternetHeaders(InputStream is)
    throws MessagingException
  {
    return new InternetHeaders(is);
  }

  /**
   * Creates a MIME body part object from the given headers and byte content.
   * @param headers the part headers
   * @param content the part content
   */
  protected MimeBodyPart createMimeBodyPart(InternetHeaders headers,
                                            byte[] content)
    throws MessagingException
  {
    return new MimeBodyPart(headers, content);
  }

  /**
   * Creates a MIME body part from the specified input stream.
   * @param is the input stream to parse the part from
   */
  protected MimeBodyPart createMimeBodyPart(InputStream is)
    throws MessagingException
  {
    return new MimeBodyPart(is);
  }

}

