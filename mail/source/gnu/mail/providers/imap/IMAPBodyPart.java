/*
 * IMAPBodyPart.java
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import jakarta.activation.DataHandler;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.ContentDisposition;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParameterList;

import gnu.inet.imap.BODY;
import gnu.inet.imap.BODYSTRUCTURE;
import gnu.inet.imap.IMAPAdapter;
import gnu.inet.imap.IMAPCallback;
import gnu.inet.imap.IMAPConnection;
import gnu.inet.imap.FetchDataItem;
import gnu.inet.imap.Literal;
import gnu.inet.imap.MessageSet;
import gnu.inet.util.GetSystemPropertyAction;
import gnu.mail.providers.ReadOnlyBodyPart;

/**
 * A MIME body part of an IMAP multipart message.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.5
 */
final class IMAPBodyPart
  extends ReadOnlyBodyPart
{

  final IMAPMessage message;
  final BODYSTRUCTURE.Part part;
  final String section;
  private ContentType type;
  private ContentDisposition disposition;
  private boolean headersLoaded;
  private Literal literal;

  IMAPBodyPart(IMAPMessage message,
               BODYSTRUCTURE.Part part,
               String section)
  {
    this.message = message;
    this.part = part;
    this.section = section;
    ParameterList pl = new ParameterList();
    Map<String,String> params = part.getParameters();
    if (params != null)
      {
        for (String key : params.keySet())
          {
            pl.set(key, params.get(key));
          }
      }
    type = new ContentType(part.getPrimaryType(), part.getSubtype(), pl);
    if (part instanceof BODYSTRUCTURE.Multipart)
      {
        BODYSTRUCTURE.Disposition d =
          ((BODYSTRUCTURE.Multipart) part).getDisposition();
        if (d != null)
          {
            pl = new ParameterList();
            params = d.getParameters();
            if (params != null)
              {
                for (String key : params.keySet())
                  {
                    pl.set(key, params.get(key));
                  }
              }
            disposition = new ContentDisposition(d.getType(), pl);
          }
      }
  }

  public int getSize()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getSize();
      }
    return -1;
  }

  public int getLineCount()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.TextPart)
      {
        return ((BODYSTRUCTURE.TextPart) part).getLines();
      }
    return -1;
  }

  public String getContentType()
    throws MessagingException
  {
    return type.toString();
  }

  public String getDisposition()
    throws MessagingException
  {
    return (disposition == null) ?
      super.getDisposition() :
      disposition.toString();
  }

  public String getEncoding()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getEncoding();
      }
    return super.getEncoding();
  }

  public String getContentID()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getId();
      }
    return super.getContentID();
  }

  public String[] getContentLanguage()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.Multipart)
      {
        List<String> l = ((BODYSTRUCTURE.Multipart) part).getLanguage();
        String[] ret = new String[l.size()];
        l.toArray(ret);
        return ret;
      }
    return super.getContentLanguage();
  }

  public String getDescription()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getDescription();
      }
    return super.getDescription();
  }

  public String getFileName()
    throws MessagingException
  {
    String filename = null;
    if (disposition != null)
      {
        filename = disposition.getParameter("filename");
      }
    if (filename == null)
      {
        filename = type.getParameter("name");
      }
    if (filename != null)
      {
        PrivilegedAction a =
          new GetSystemPropertyAction("mail.mime.decodefilename");
        if ("true".equals(AccessController.doPrivileged(a)))
          {
            try
              {
                filename = MimeUtility.decodeText(filename);
              }
            catch (UnsupportedEncodingException e)
              {
                throw new MessagingException(null, e);
              }
          }
      }
    return filename;
  }

  protected InputStream getContentStream()
    throws MessagingException
  {
    if (literal == null)
      {
        fetchContent();
      }
    return literal.getInputStream();
  }

  public DataHandler getDataHandler()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.Multipart)
      {
        BODYSTRUCTURE.Multipart mp = (BODYSTRUCTURE.Multipart) part;
        dh = new DataHandler(new IMAPMultipartDataSource(message, this,
                                                         mp, section));
      }
    else if (part instanceof BODYSTRUCTURE.MessagePart)
      {
        BODYSTRUCTURE.MessagePart m = (BODYSTRUCTURE.MessagePart) part;
        String ms = section + ".1";
        dh = new DataHandler(new IMAPMessage(message, m, type, disposition,
                                             ms), type.toString());
      }
    return super.getDataHandler();
  }

  public String[] getHeader(String name)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getHeader(name);
  }

  public Enumeration getAllHeaders()
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getAllHeaders();
  }

  public Enumeration getMatchingHeaders(String[] names)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getMatchingHeaders(names);
  }

  public Enumeration getNonMatchingHeaders(String[] names)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getNonMatchingHeaders(names);
  }

  public Enumeration getAllHeaderLines()
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getAllHeaderLines();
  }

  public Enumeration getMatchingHeaderLines(String[] names)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getMatchingHeaderLines(names);
  }

  public Enumeration getNonMatchingHeaderLines(String[] names)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    return super.getNonMatchingHeaderLines(names);
  }

  protected void updateHeaders()
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    super.updateHeaders();
  }

  // -- Lazy loading stuff --

  void fetchContent()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(new StringBuilder("BODY.PEEK[")
                 .append(section)
                 .append("]")
                 .toString());
    fetch(commands, false);
  }

  void fetchHeaders()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(new StringBuilder("BODY.PEEK[")
                 .append(section)
                 .append(".HEADER]")
                 .toString());
    fetch(commands, true);
  }

  void fetch(List<String> commands, final boolean isHeader)
    throws MessagingException
  {
    final IMAPStore s = (IMAPStore) message.getFolder().getStore();
    IMAPConnection connection = s.connection;
    int msgnum = message.getMessageNumber();
    try
      {
        IMAPCallback callback = new IMAPAdapter()
        {
          public void alert(String message)
          {
            s.processAlert(message);
          }
          public void fetch(int message, List<FetchDataItem> data)
          {
            for (int i = 0; i < data.size(); i++)
              {
                FetchDataItem item = data.get(i);
                if (item instanceof BODY)
                  {
                    BODY body = (BODY) item;
                    if (isHeader)
                      {
                        Literal lh = body.getContents();
                        InputStream in = lh.getInputStream();
                        try
                          {
                            headers = new InternetHeaders(in);
                          }
                        catch (MessagingException e)
                          {
                            throw (RuntimeException) new RuntimeException()
                              .initCause(e);
                          }
                        finally
                          {
                            try
                              {
                                in.close();
                              }
                            catch (IOException e)
                              {
                              }
                          }
                      }
                    else
                      {
                        literal = body.getContents();
                      }
                  }
              }
          }
        };
        MessageSet msgs = new MessageSet();
        msgs.add(msgnum);
        connection.fetch(msgs, commands, callback);
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
    catch (RuntimeException e)
      {
        Throwable cause = e.getCause();
        if (cause instanceof MessagingException)
          {
            throw (MessagingException) cause;
          }
        throw e;
      }
  }

}
