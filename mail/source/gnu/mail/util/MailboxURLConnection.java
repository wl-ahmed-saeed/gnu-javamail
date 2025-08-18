/*
 * MailboxURLConnection.java
 * Copyright (C) 2004, 2013 The Free Software Foundation
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

package gnu.mail.util;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Folder;
import jakarta.mail.Header;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.URLName;
import jakarta.mail.internet.MailDateFormat;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.search.SearchTerm;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * A URLConnection that can be used to access mailboxes using the JavaMail
 * API.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.5
 */
public class MailboxURLConnection
  extends URLConnection
{

  /**
   * The mail store.
   */
  protected Store store;

  /**
   * The mail folder.
   */
  protected Folder folder;

  /**
   * The mail message, if the URL represents a message.
   */
  protected Message message;

  /**
   * The headers to return.
   */
  protected Map headers;
  private List headerKeys;

  /**
   * Constructs a new mailbox URL connection using the specified URL.
   * @param url the URL representing the mailbox to connect to
   */
  public MailboxURLConnection(URL url)
  {
    super(url);
  }

  /**
   * Connects to the mailbox.
   */
  public synchronized void connect()
    throws IOException
  {
    if (connected)
      {
        return;
      }

    try
      {
        Session session = Session.getDefaultInstance(System.getProperties());
        URLName urlName = asURLName(url);
        store = session.getStore(urlName);
        folder = store.getDefaultFolder();

        // Resolve to a folder
        String path = url.getPath();
        if ("/".equals(path))
          {
            folder = folder.getFolder("INBOX");
          }
        else
          {
            if (path.charAt(0) == '/')
              {
                path = path.substring(1);
              }
            int si = path.indexOf('/');
            while (si != -1 && path.length() > 0)
              {
                String comp = path.substring(0, si);
                path = path.substring(si + 1);
                folder = folder.getFolder(comp);
              }
          }

        if (!folder.exists())
          {
            throw new FileNotFoundException(path);
          }

        folder.open(Folder.READ_ONLY);
        headers = Collections.EMPTY_MAP;
        headerKeys = Collections.EMPTY_LIST;

        // Find message if requested
        String ref = url.getRef();
        if (ref != null)
          {
            SearchTerm term = new HeaderTerm("Message-Id", ref);
            Message[] messages = folder.search(term);
            if (messages.length == 0)
              {
                throw new FileNotFoundException(ref);
              }
            message = messages[0];
            headers = new HashMap();
            headerKeys = new ArrayList();
            if (message instanceof MimeMessage)
              {
                MimeMessage mm = (MimeMessage) message;
                Enumeration e = mm.getAllHeaderLines();
                while (e.hasMoreElements())
                  {
                    Header header = (Header) e.nextElement();
                    headerKeys.add(header.getName());
                    headers.put(header.getName(), header.getValue());
                  }
              }
          }
        folder.close(false);
      }
    catch (MessagingException e)
      {
        Throwable cause = e.getCause();
        if (cause instanceof IOException)
          {
            throw (IOException) cause;
          }
        throw new IOException(e.getMessage(), e);
      }
    connected = true;
  }

  public String getHeaderField(int index)
  {
    return getHeaderField(getHeaderFieldKey(index));
  }

  public String getHeaderFieldKey(int index)
  {
    return (String) headerKeys.get(index);
  }

  public String getHeaderField(String name)
  {
    return (String) headers.get(name);
  }

  public Map getHeaderFields()
  {
    return Collections.unmodifiableMap(headers);
  }

  public Object getContent()
    throws IOException
  {
    if (message != null)
      {
        return message;
      }
    return folder;
  }

  public InputStream getInputStream()
    throws IOException
  {
    PipedOutputStream pos = new PipedOutputStream();
    Runnable writer = (message == null) ?
      new FolderWriter(folder, pos) :
      new MessageWriter(message, pos);
    Thread thread = new Thread(writer, "MailboxURLConnection.getInputStream");
    thread.start();
    return new PipedInputStream(pos);
  }

  /**
   * Converts a URL into a URLName.
   */
  protected static URLName asURLName(URL url)
  {
    String protocol = url.getProtocol();
    String host = url.getHost();
    int port = url.getPort();
    String userInfo = url.getUserInfo();
    String username = null;
    String password = null;
    String path = url.getPath();

    if (userInfo != null)
      {
        int ci = userInfo.indexOf(':');
        username = (ci != -1) ? userInfo.substring(0, ci) : userInfo;
        password = (ci != -1) ? userInfo.substring(ci + 1) : null;
      }

    return new URLName(protocol, host, port, path, username, password);
  }

  static class MessageWriter
    implements Runnable
  {

    Message message;
    OutputStream out;

    MessageWriter(Message message, OutputStream out)
    {
      this.message = message;
      this.out = out;
    }

    public void run()
    {
      try
        {
          message.writeTo(out);
        }
      catch (Exception e)
        {
          if (e instanceof RuntimeException)
            {
              throw (RuntimeException) e;
            }
          throw (RuntimeException) new RuntimeException().initCause(e);
        }
    }

  }

  static class FolderWriter
    implements Runnable
  {

    Folder folder;
    OutputStream out;

    FolderWriter(Folder folder, OutputStream out)
    {
      this.folder = folder;
      this.out = out;
    }

    public void run()
    {
      try
        {
          MailDateFormat mf = new MailDateFormat();
          XMLOutputFactory f = XMLOutputFactory.newFactory();
          XMLStreamWriter w = f.createXMLStreamWriter(out);
          w.writeStartDocument();
          w.writeStartElement("folder");
          w.writeAttribute("name", folder.getName());
          w.writeAttribute("fullName", folder.getFullName());
          if (folder.exists())
            {
              folder.open(Folder.READ_ONLY);
              w.writeAttribute("messageCount",
                               Integer.toString(folder.getMessageCount()));
              w.writeAttribute("unreadMessageCount",
                               Integer.toString(folder.getUnreadMessageCount()));
              int type = folder.getType();
              if ((type & Folder.HOLDS_FOLDERS) != 0)
                {
                  Folder[] l = folder.list();
                  for (int i = 0; i < l.length; i++)
                    {
                      w.writeStartElement("folder");
                      w.writeAttribute("name", l[i].getName());
                      w.writeEndElement();
                    }
                }
              if ((type & Folder.HOLDS_MESSAGES) != 0)
                {
                  FetchProfile fp = new FetchProfile();
                  fp.add(FetchProfile.Item.ENVELOPE);
                  Message[] m = folder.getMessages();
                  folder.fetch(m, fp);
                  for (int i = 0; i < m.length; i++)
                    {
                      String subject = m[i].getSubject();
                      Date sentDate = m[i].getSentDate();
                      w.writeStartElement("message");
                      if (subject != null)
                        {
                          w.writeAttribute("subject", subject);
                        }
                      if (sentDate != null)
                        {
                          w.writeAttribute("sentDate", mf.format(sentDate));
                        }
                      writeAddresses(w, "from", m[i].getFrom());
                      writeAddresses(w, "replyTo", m[i].getReplyTo());
                      writeAddresses(w, "to",
                                     m[i].getRecipients(Message.RecipientType.TO));
                      writeAddresses(w, "cc",
                                     m[i].getRecipients(Message.RecipientType.CC));
                      w.writeEndElement();
                    }
                }
              folder.close(false);
            }
          w.writeEndElement();
          w.writeEndDocument();
          out.flush();
        }
      catch (Exception e)
        {
          if (e instanceof RuntimeException)
            {
              throw (RuntimeException) e;
            }
          throw (RuntimeException) new RuntimeException().initCause(e);
        }
    }

    void writeAddresses(XMLStreamWriter w, String name, Address[] a)
      throws XMLStreamException
    {
      if (a != null)
        {
          for (int i = 0; i < a.length; i++)
            {
              w.writeStartElement(name);
              w.writeCharacters(a[i].toString());
              w.writeEndElement();
            }
        }
    }

  }

}
