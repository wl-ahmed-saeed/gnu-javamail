/*
 * NNTPTransport.java
 * Copyright (C) 2002 Chris Burdess <dog@gnu.org>
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

package gnu.mail.providers.nntp;

import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.StreamHandler;
import jakarta.mail.Address;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.URLName;
import jakarta.mail.event.TransportEvent;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.NewsAddress;
import javax.net.ssl.TrustManager;

import gnu.inet.nntp.NNTPConnection;
import gnu.inet.util.LaconicFormatter;

/**
 * An NNTP transport provider.
 * This uses an NNTPConnection to handle all the protocol-related
 * functionality.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.5
 */
public class NNTPTransport extends Transport
{

  NNTPConnection connection;

  /**
   * Constructor.
   * @param session the session
   * @param url the connection URL
   */
  public NNTPTransport(Session session, URLName url)
  {
    super(session, url);
  }

  /**
   * Performs the protocol connection.
   * @see NNTPStore#protocolConnect
   */
  protected boolean protocolConnect(String host, int port, String username,
                                    String password)
    throws MessagingException
  {
    if (connection != null)
      {
        return true;
      }
    if (host == null)
      {
        host = getProperty("host");
      }
    if (username == null)
      {
        username = getProperty("user");
      }
    if (port < 0)
      {
        port = getIntProperty("port");
      }
    if (host == null)
      {
        return false;
      }
    try
      {
        int connectionTimeout = getIntProperty("connectiontimeout");
        int timeout = getIntProperty("timeout");

        boolean tls = "nntps-post".equals(url.getProtocol());

        if (port < 0)
          {
            port = tls ? NNTPConnection.DEFAULT_SSL_PORT : NNTPConnection.DEFAULT_PORT;
          }
        // Locate custom TrustManager
        TrustManager tm = null;
        if (tls)
          {
            tm = getTrustManager();
          }
        if (port < 0)
          {
            port = NNTPConnection.DEFAULT_PORT;
          }

        connection = new NNTPConnection(host, port,
                                        connectionTimeout, timeout,
                                        tls, tm);
        if (session.getDebug())
          {
            connection.getLogger().setLevel(NNTPConnection.NNTP_TRACE);
            Formatter formatter = new LaconicFormatter();
            Handler handler =
              new StreamHandler(session.getDebugOut(), formatter);
            handler.setLevel(Level.ALL);
            connection.getLogger().addHandler(handler);
          }

        /*
         * FIXME First of all, capability list should be retrieved
         * in order to verify STARTTLS availability. However, capabilities
         * are matter of RFC 3977, which is not implemented yet.
         */
        if (!tls && propertyIsTrue("tls")) {
          tm = getTrustManager();

          if (tm == null) {
            tls = connection.starttls();
          } else {
            tls = connection.starttls(tm);
          }
        }

        /*
         * FIXME After STARTTLS, capabilities list should be refreshed.
         */

        if (username != null && password != null)
          {
            // TODO decide on authentication method
            // Original authinfo
            return connection.authinfo(username, password);
          }
        else
          {
            return true;
          }
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
    catch (SecurityException e)
      {
        if (username != null && password != null)
          {
            throw new AuthenticationFailedException(e.getMessage());
          }
        else
          {
            return false;
          }
      }
  }

  private TrustManager getTrustManager()
    throws MessagingException
  {
    String tmt = getProperty("trustmanager");
    if (tmt != null)
      {
        try
          {
            Class t = Class.forName(tmt);
            return (TrustManager) t.newInstance();
          } catch (Exception e) {
            throw new MessagingException(e.getMessage(), e);
          }
      }
    return null;
  }

  /**
   * Close the connection.
   * @see NNTPStore#close
   */
  public void close()
    throws MessagingException
  {
    try
      {
        synchronized (connection)
          {
            connection.quit();
          }
      }
    catch (IOException e)
      {
        if (!(e instanceof SocketException))
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
    super.close();
  }

  /**
   * Post an article.
   * @param message a MimeMessage
   * @param addresses an array of Address(ignored!)
   */
  public void sendMessage(Message message, Address[] addresses)
    throws MessagingException
  {
    // Ensure corrent recipient type, and that all newsgroup recipients are
    // of type NewsAddress.
    addresses = message.getRecipients(MimeMessage.RecipientType.NEWSGROUPS);
    boolean ok = (addresses.length > 0);
    if (!ok)
      {
        throw new MessagingException("No recipients specified");
      }
    for (int i = 0; i < addresses.length; i++)
      {
        if (!(addresses[i] instanceof NewsAddress))
          {
            ok = false;
            break;
          }
      }
    if (!ok)
      {
        throw new MessagingException("Newsgroup recipients must be "+
                                     "specified as type NewsAddress");
      }

    try
      {
        synchronized (connection)
          {
            OutputStream out = connection.post();
            message.writeTo(out);
            out.close();
          }
        notifyTransportListeners(TransportEvent.MESSAGE_DELIVERED,
                                 addresses,
                                 new NewsAddress[0],
                                 new NewsAddress[0],
                                 message);
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  private int getIntProperty(String key)
  {
    String value = getProperty(key);
    if (value != null)
      {
        try
          {
            return Integer.parseInt(value);
          }
        catch (RuntimeException e)
          {
          }
      }
    return -1;
  }

  /*
   * Returns the provider-specific or general mail property corresponding to
   * the specified key.
   */
  private String getProperty(String key)
  {
    String value = session.getProperty("mail.nntp." + key);
    if (value == null)
      {
        value = session.getProperty("mail." + key);
      }
    return value;
  }

  private boolean propertyIsTrue(String key)
  {
    return "true".equals(getProperty(key));
  }

}
