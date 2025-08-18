/*
 * POP3Store.java
 * Copyright (C) 1999, 2003, 2005 Chris Burdess <dog@gnu.org>
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

package gnu.mail.providers.pop3;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.URLName;
import javax.net.ssl.TrustManager;

import gnu.inet.pop3.POP3Connection;
import gnu.inet.util.LaconicFormatter;

/**
 * The storage class implementing the POP3 mail protocol.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @author <a href='mailto:nferrier@tapsellferrier.co.uk'>Nic Ferrier</a>
 * @version 1.3
 */
public final class POP3Store
  extends Store
{

  private static final ResourceBundle L10N =
    ResourceBundle.getBundle("gnu.mail.providers.L10N");

  /*
   * The connection.
   */
  POP3Connection connection;

  /*
   * The root folder.
   */
  POP3Folder root;

  /**
   * If rue, disable use of APOP.
   */
  boolean disableApop;

  /**
   * Constructor.
   */
  public POP3Store(Session session, URLName urlname)
  {
    super(session, urlname);
  }

  /**
   * Connects to the POP3 server and authenticates with the specified
   * parameters.
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
        if (port < 0)
          {
            port = POP3Connection.DEFAULT_PORT;
          }
      }
    if (host == null || username == null || password == null)
      {
        return false;
      }
    disableApop = false;
    synchronized (this)
      {
        try
          {
            int connectionTimeout = getIntProperty("connectiontimeout");
            int timeout = getIntProperty("timeout");
            boolean tls = "pop3s".equals(url.getProtocol());
            // Locate custom trust manager
            TrustManager tm = getTrustManager();
            connection = new POP3Connection(host, port,
                                            connectionTimeout, timeout,
                                            tls, tm);
            if (session.getDebug())
              {
                Logger logger = connection.getLogger();
                logger.setLevel(POP3Connection.POP3_TRACE);
                Formatter formatter = new LaconicFormatter();
                Handler handler =
                  new StreamHandler(session.getDebugOut(), formatter);
                handler.setLevel(Level.ALL);
                logger.addHandler(handler);
              }

            String sslTlsProtocolsProp = getProperty("ssl.protocols");
            if (sslTlsProtocolsProp != null) {
                String[] sslTlsProtocols = sslTlsProtocolsProp
                    .replaceAll(" ", "")
                    .replaceAll("[,]", " ")
                    .split(" ");

                connection.setSslTlsProtocolsIfAny(sslTlsProtocols);
              }

            // Disable APOP if necessary
            if (propertyIsFalse("apop"))
              {
                disableApop = true;
              }
            // Get capabilities
            List<String> capa = connection.capa();
            if (capa != null)
              {
                if (capa.contains("STLS"))
                  {
                    if (!tls && !propertyIsFalse("tls"))
                      {
                        if (tm == null)
                          {
                            tls = connection.stls();
                          }
                        else
                          {
                            tls = connection.stls(tm);
                          }
                        // Capabilities may have changed since STLS
                        if (tls)
                          {
                            capa = connection.capa();
                          }
                      }
                  }
                if (!tls && "required".equals(getProperty("tls")))
                  {
                    throw new MessagingException(L10N.getString("err.no_tls"));
                  }
                // Build list of SASL mechanisms
                List<String> authenticationMechanisms = null;
                for (Iterator i = capa.iterator(); i.hasNext(); )
                  {
                    String cap = (String) i.next();
                    if (cap.startsWith("SASL "))
                      {
                        if (authenticationMechanisms == null)
                          {
                            authenticationMechanisms = new ArrayList<String>();
                          }
                        authenticationMechanisms.add(cap.substring(5));
                      }
                  }
                // User authentication
                if (authenticationMechanisms != null &&
                    !authenticationMechanisms.isEmpty())
                  {
                    if (username == null || password == null)
                      {
                        PasswordAuthentication pa =
                          session.getPasswordAuthentication(url);
                        if (pa == null)
                          {
                            InetAddress addr = InetAddress.getByName(host);
                            pa = session.requestPasswordAuthentication(addr,
                                                                        port,
                                                                        "pop3",
                                                                        null,
                                                                        null);
                          }
                        if (pa != null)
                          {
                            username = pa.getUserName();
                            password = pa.getPassword();
                          }
                      }
                    if (username != null && password != null)
                      {
                        // Discover user ordering preferences for auth
                        // mechanisms
                        String authPrefs = getProperty("auth.mechanisms");
                        Iterator i = null;
                        if (authPrefs == null)
                          {
                            i = authenticationMechanisms.iterator();
                          }
                        else
                          {
                            StringTokenizer st =
                              new StringTokenizer(authPrefs, ",");
                            List authPrefList = Collections.list(st);
                            i = authPrefList.iterator();
                          }
                        // Try each mechanism in the list in turn
                        while (i.hasNext())
                          {
                            String mechanism = (String) i.next();
                            if (authenticationMechanisms.contains(mechanism) &&
                                connection.auth(mechanism, username,
                                                 password))
                              {
                                return true;
                              }
                          }
                      }
                  }
              }
            // Fall back to APOP or login
            if (!disableApop)
              {
                return connection.apop(username, password);
              }
            else
              {
                return connection.login(username, password);
              }
          }
        catch (UnknownHostException e)
          {
            throw new MessagingException(null, e);
          }
        catch (IOException e)
          {
            throw new MessagingException(null, e);
          }
      }
  }

  /**
   * Returns a trust manager used for TLS negotiation.
   */
  protected TrustManager getTrustManager()
    throws MessagingException
  {
    String tmt = getProperty("trustmanager");
    if (tmt == null)
      {
        return null;
      }
    else
      {
        try
          {
            // Instantiate the trust manager
            Class t = Class.forName(tmt);
            TrustManager tm = (TrustManager) t.newInstance();
            // If there is a setSession method, call it
            try
              {
                Class[] pt = new Class[] { Session.class };
                Method m = t.getMethod("setSession", pt);
                Object[] args = new Object[] { session };
                m.invoke(tm, args);
              }
            catch (NoSuchMethodException e)
              {
              }
            return tm;
          }
        catch (Exception e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
  }

  /**
   * Closes the connection.
   */
  public void close()
    throws MessagingException
  {
    if (connection != null)
      {
        synchronized (connection)
          {
            try
              {
                if (propertyIsTrue("rsetbeforequit"))
                  {
                    connection.rset();
                  }
                connection.quit();
              }
            catch (IOException e)
              {
                throw new MessagingException(null, e);
              }
          }
        connection = null;
      }
    super.close();
  }

  /**
   * Issues a NOOP to the POP server to determine whether the connection
   * is still alive.
   */
  public boolean isConnected()
  {
    if (!super.isConnected())
      return false;
    try
      {
        synchronized (connection)
          {
            connection.noop();
          }
        return true;
      }
    catch (IOException e)
      {
        return false;
      }
  }

  /**
   * Returns the root folder.
   */
  public Folder getDefaultFolder()
    throws MessagingException
  {
    synchronized (this)
      {
        if (root == null)
          {
            root = new POP3Folder(this, Folder.HOLDS_FOLDERS);
          }
      }
    return root;
  }

  /**
   * Returns the folder with the specified name.
   */
  public Folder getFolder(String s)
    throws MessagingException
  {
    return getDefaultFolder().getFolder(s);
  }

  /**
   * Returns the folder whose name is the file part of the specified URLName.
   */
  public Folder getFolder(URLName urlname)
    throws MessagingException
  {
    try
      {
        String file = URLDecoder.decode(urlname.getFile(), "UTF-8");
        return getDefaultFolder().getFolder(file);
      }
    catch (UnsupportedEncodingException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  // -- Utility methods --

  private int getIntProperty(String key)
  {
    String value = getProperty(key);
    if (value != null)
      {
        try
          {
            return Integer.parseInt(value);
          }
        catch (Exception e)
          {
          }
      }
    return -1;
  }

  private boolean propertyIsFalse(String key)
  {
    return "false".equals(getProperty(key));
  }

  private boolean propertyIsTrue(String key)
  {
    return "true".equals(getProperty(key));
  }

  /*
   * Returns the provider-specific or general mail property corresponding to
   * the specified key.
   */
  private String getProperty(String key)
  {
    String value = session.getProperty("mail.pop3." + key);
    if (value == null)
      {
        value = session.getProperty("mail." + key);
      }
    return value;
  }

}
