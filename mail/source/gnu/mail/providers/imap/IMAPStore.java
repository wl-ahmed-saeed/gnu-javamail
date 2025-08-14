/*
 * IMAPStore.java
 * Copyright (C) 2003, 2004, 2013 Chris Burdess <dog@gnu.org>
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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.URLDecoder;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringTokenizer;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.StreamHandler;
import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.MethodNotSupportedException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Quota;
import jakarta.mail.QuotaAwareStore;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.StoreClosedException;
import jakarta.mail.URLName;
import jakarta.mail.event.StoreEvent;
import javax.net.ssl.TrustManager;

import gnu.inet.imap.IMAPAdapter;
import gnu.inet.imap.IMAPCallback;
import gnu.inet.imap.IMAPConnection;
import gnu.inet.imap.IMAPConstants;
import gnu.inet.imap.Namespace;
import gnu.inet.util.LaconicFormatter;

/**
 * The storage class implementing the IMAP4rev1 mail protocol.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class IMAPStore
  extends Store
  implements QuotaAwareStore
{

  private static final ResourceBundle L10N =
    ResourceBundle.getBundle("gnu.mail.providers.L10N");

  /**
   * The connection to the IMAP server.
   */
  IMAPConnection connection = null;

  /**
   * Folder representing the root namespace of the IMAP connection.
   */
  IMAPFolder root = null;

  /**
   * The currently selected folder.
   */
  IMAPFolder selected = null;

  /**
   * Constructor.
   */
  public IMAPStore(Session session, URLName url)
  {
    super(session, url);
  }

  /**
   * Connects to the IMAP server and authenticates with the specified
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
      }
    if (host == null || username == null || password == null)
      {
        return false;
      }
    synchronized (this)
      {
        try
          {
            int connectionTimeout = getIntProperty("connectiontimeout");
            int timeout = getIntProperty("timeout");
            boolean tls = "imaps".equals(url.getProtocol());
            // Locate custom trust manager
            TrustManager tm = getTrustManager();
            connection = new IMAPConnection(host, port,
                                            connectionTimeout, timeout,
                                            tls, tm);
            if (session.getDebug())
              {
                Logger logger = connection.getLogger();
                logger.setLevel(IMAPConnection.IMAP_TRACE);
                Formatter formatter = new LaconicFormatter();
                Handler handler =
                  new StreamHandler(session.getDebugOut(), formatter);
                handler.setLevel(Level.ALL);
                logger.addHandler(handler);
              }

            final List<String> capabilities = new ArrayList<String>();
            IMAPAdapter callback = this.new DefaultAdapter(capabilities);
            connection.capability(callback);

            String sslTlsProtocolsProp = getProperty("ssl.protocols");
            if (sslTlsProtocolsProp != null) {
                String[] sslTlsProtocols = sslTlsProtocolsProp
                    .replaceAll(" ", "")
                    .replaceAll("[,]", " ")
                    .split(" ");

                connection.setSslTlsProtocolsIfAny(sslTlsProtocols);
            }

            // Ignore tls settings if we are making the connection
            // to a dedicated SSL port. (imaps)
            if (!tls && capabilities.contains(IMAPConstants.STARTTLS))
              {
                if (!propertyIsFalse("tls"))
                  {
                    if (tm == null)
                      {
                        tls = connection.starttls(callback);
                      }
                    else
                      {
                        tls = connection.starttls(callback, tm);
                      }
                    // Capabilities may have changed since STARTTLS
                    if (tls)
                      {
                        capabilities.clear();
                        connection.capability(callback);
                      }
                  }
              }
            if (!tls && "required".equals(getProperty("tls")))
              {
                throw new MessagingException(L10N.getString("err.no_tls"));
              }
            // Build list of available SASL mechanisms
            List<String> mechanisms = new ArrayList<String>();
            for (String cap : capabilities)
              {
                if (cap.startsWith("AUTH="))
                  {
                    mechanisms.add(cap.substring(5));
                  }
              }
            // User authentication
            if (!mechanisms.isEmpty())
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
                                                                    "imap",
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
                    Iterator<String> i = null;
                    if (authPrefs == null)
                      {
                        i = mechanisms.iterator();
                      }
                    else
                      {
                        StringTokenizer st =
                          new StringTokenizer(authPrefs, ",");
                        List<String> authPrefList = new ArrayList<String>();
                        while (st.hasMoreTokens())
                          {
                            authPrefList.add(st.nextToken());
                          }
                        i = authPrefList.iterator();
                      }
                    // Try each mechanism in the list in turn
                    while (i.hasNext())
                      {
                        String mechanism = i.next();
                        if (mechanisms.contains(mechanism) &&
                            connection.authenticate(mechanism, username,
                                                    password, callback))
                          {
                            return true;
                          }
                      }
                  }
              }
            if (capabilities.contains(IMAPConstants.LOGINDISABLED))
              {
                return false; // sorry
              }
            return connection.login(username, password, callback);
          }
        catch (UnknownHostException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
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
  public synchronized void close()
    throws MessagingException
  {
    if (connection != null)
      {
        synchronized (this)
          {
            try
              {
                IMAPAdapter callback = this.new DefaultAdapter(null);
                connection.logout(callback);
              }
            catch (IOException e)
              {
              }
            connection = null;
          }
      }
    super.close();
  }

  /**
   * Returns the root folder.
   */
  public Folder getDefaultFolder()
    throws MessagingException
  {
    if (root == null)
      {
        root = new IMAPFolder(this, "");
      }
    return root;
  }

  /**
   * Returns the folder with the specified name.
   */
  public Folder getFolder(String name)
    throws MessagingException
  {
    return new IMAPFolder(this, name);
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
        return getFolder(file);
      }
    catch (UnsupportedEncodingException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Uses a NOOP to ensure that the connection to the IMAP server is still
   * valid.
   */
  public boolean isConnected()
  {
    if (!super.isConnected())
      return false;
    try
      {
        synchronized (this)
          {
            IMAPCallback callback;
            if (selected == null)
              {
                callback = this.new DefaultAdapter(null);
              }
            else
              {
                callback = selected.callback;
              }
            connection.noop(callback);
          }
        return true;
      }
    catch (IOException e)
      {
        return false;
      }
  }

  /**
   * Returns the IMAP connection used by this store.
   * @exception StoreClosedException if the store is not currently connected
   */
  protected IMAPConnection getConnection()
    throws StoreClosedException
  {
    if (!super.isConnected())
      {
        throw new StoreClosedException(this);
      }
    return connection;
  }

  /**
   * Indicates whether the specified folder is selected.
   */
  protected boolean isSelected(IMAPFolder folder)
  {
    return folder.equals(selected);
  }

  /**
   * Sets the selected folder.
   */
  protected void setSelected(IMAPFolder folder)
  {
    selected = folder;
  }

  /**
   * Process an alert supplied by the server.
   */
  protected void processAlert(String message)
  {
    notifyStoreListeners(StoreEvent.ALERT, message);
  }

  /**
   * Returns a list of folders representing personal namespaces.
   * See RFC 2342 for details.
   */
  public Folder[] getPersonalNamespaces()
    throws MessagingException
  {
    if (!super.isConnected())
      {
        throw new StoreClosedException(this);
      }
    synchronized (this)
      {
        try
          {
            final List<Folder> acc = new ArrayList<Folder>();
            IMAPAdapter callback = new IMAPAdapter()
            {
              public void alert(String message)
              {
                processAlert(message);
              }
              public void namespace(List<Namespace> personal,
                                    List<Namespace> otherUsers,
                                    List<Namespace> shared)
              {
                if (personal != null)
                  {
                    for (Namespace ns : personal)
                      {
                        String d = ns.getHierarchyDelimiter();
                        char delimiter = (d == null) ? '\u0000' : d.charAt(0);
                        acc.add(new IMAPFolder(IMAPStore.this,
                                               ns.getPrefix(),
                                               delimiter));
                      }
                  }
              }
            };
            if (connection.namespace(callback))
              {
                Folder[] ret = new Folder[acc.size()];
                acc.toArray(ret);
                return ret;
              }
            return null;
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
  }

  /**
   * Returns a list of folders representing other users' namespaces.
   * See RFC 2342 for details.
   */
  public Folder[] getUserNamespaces(final String user)
    throws MessagingException
  {
    if (!super.isConnected())
      {
        throw new StoreClosedException(this);
      }
    synchronized (this)
      {
        try
          {
            final List<Folder> acc = new ArrayList<Folder>();
            IMAPAdapter callback = new IMAPAdapter()
            {
              public void alert(String message)
              {
                processAlert(message);
              }
              public void namespace(List<Namespace> personal,
                                    List<Namespace> otherUsers,
                                    List<Namespace> shared)
              {
                if (otherUsers != null)
                  {
                    for (Namespace ns : otherUsers)
                      {
                        String d = ns.getHierarchyDelimiter();
                        char delimiter = (d == null) ? '\u0000' : d.charAt(0);
                        acc.add(new IMAPFolder(IMAPStore.this,
                                               ns.getPrefix() + user,
                                               delimiter));
                      }
                  }
              }
            };
            if (connection.namespace(callback))
              {
                Folder[] ret = new Folder[acc.size()];
                acc.toArray(ret);
                return ret;
              }
            return null;
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
  }

  /**
   * Returns a list of folders representing shared namespaces.
   * See RFC 2342 for details.
   */
  public Folder[] getSharedNamespaces()
    throws MessagingException
  {
    if (!super.isConnected())
      {
        throw new StoreClosedException(this);
      }
    synchronized (this)
      {
        try
          {
            final List<Folder> acc = new ArrayList<Folder>();
            IMAPAdapter callback = new IMAPAdapter()
            {
              public void alert(String message)
              {
                processAlert(message);
              }
              public void namespace(List<Namespace> personal,
                                    List<Namespace> otherUsers,
                                    List<Namespace> shared)
              {
                if (shared != null)
                  {
                    for (Namespace ns: shared)
                      {
                        String d = ns.getHierarchyDelimiter();
                        char delimiter = (d == null) ? '\u0000' : d.charAt(0);
                        acc.add(new IMAPFolder(IMAPStore.this,
                                               ns.getPrefix(),
                                               delimiter));
                      }
                  }
              }
            };
            if (connection.namespace(callback))
              {
                Folder[] ret = new Folder[acc.size()];
                acc.toArray(ret);
                return ret;
              }
            return null;
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
  }

  /**
   * Returns the quota for the specified quota root.
   * @param root the quota root
   */
  public Quota[] getQuota(String root)
    throws MessagingException
  {
    if (!super.isConnected())
      {
        throw new StoreClosedException(this);
      }
    synchronized (this)
      {
        try
          {
            final List<Quota> acc = new ArrayList<Quota>();
            IMAPAdapter callback = new IMAPAdapter()
            {
              public void alert(String message)
              {
                processAlert(message);
              }
              public void quota(String quotaRoot,
                                Map<String,Integer> currentUsage,
                                Map<String,Integer> limit)
              {
                Quota quota = new Quota(quotaRoot);
                List<Quota.Resource> l = new ArrayList<Quota.Resource>();
                for (Iterator<String> i = currentUsage.keySet().iterator();
                     i.hasNext(); )
                  {
                    String resource = i.next();
                    l.add(new Quota.Resource(resource,
                                             currentUsage.get(resource),
                                             limit.get(resource)));
                  }
                quota.resources = new Quota.Resource[l.size()];
                l.toArray(quota.resources);
              }
            };
            if (connection.getquota(root, callback))
              {
                Quota[] ret = new Quota[acc.size()];
                acc.toArray(ret);
                return ret;
              }
            return new Quota[0];
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
  }

  /**
   * Sets the quota resource set for the specified quota root.
   * @param quota the mail quota
   */
  public void setQuota(Quota quota)
    throws MessagingException
  {
    if (!super.isConnected())
      {
        throw new StoreClosedException(this);
      }
    synchronized (this)
      {
        try
          {
            IMAPAdapter callback = new IMAPAdapter()
            {
              public void alert(String message)
              {
                processAlert(message);
              }
            };
            Map<String,Integer> resources =
              new LinkedHashMap<String,Integer>();
            if (quota.resources != null)
              {
                for (int i = 0; i < quota.resources.length; i++)
                  {
                    Quota.Resource r = quota.resources[i];
                    resources.put(r.name, (int) r.limit);
                  }
              }
            connection.setquota(quota.quotaRoot, resources, callback);
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
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
    String value = session.getProperty("mail.imap." + key);
    if (value == null)
      {
        value = session.getProperty("mail." + key);
      }
    return value;
  }

  class DefaultAdapter
    extends IMAPAdapter
  {

    private List<String> capabilities;

    DefaultAdapter(List<String> capabilities)
    {
      this.capabilities = capabilities;
    }

    public void alert(String message)
    {
      processAlert(message);
    }

    public void capability(List<String> caps)
    {
      if (capabilities != null)
        {
          capabilities.addAll(caps);
        }
    }

  }

}
