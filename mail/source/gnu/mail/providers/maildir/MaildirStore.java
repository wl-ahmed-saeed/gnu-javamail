/*
 * MaildirStore.java
 * Copyright (C) 2003, 2005 Chris Burdess <dog@gnu.org>
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

package gnu.mail.providers.maildir;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.URLName;

import gnu.inet.util.TraceLevel;
import gnu.mail.treeutil.StatusEvent;
import gnu.mail.treeutil.StatusListener;
import gnu.mail.treeutil.StatusSource;

/**
 * The storage class implementing the Maildir mailbox format.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public final class MaildirStore
  extends Store
  implements StatusSource
{

  static final Logger logger =
    Logger.getLogger("gnu.mail.util.providers.maildir");

  static final Level MAILDIR_TRACE = new TraceLevel("maildir");

  private static final char separatorChar = '/';

  private List statusListeners = new ArrayList();

  /**
   * Constructor.
   */
  public MaildirStore(Session session, URLName urlname)
  {
    super(session, urlname);
    if (session.getDebug())
      {
        logger.setLevel(MAILDIR_TRACE);
      }
  }

  /**
   * There isn't a protocol to implement, so this method just returns.
   */
  protected boolean protocolConnect(
      String host,
      int port,
      String username,
      String password)
    throws MessagingException
  {
    return true;
  }

  /**
   * Returns the default folder.
   */
  public Folder getDefaultFolder()
    throws MessagingException
  {
    // If the url used to contruct the store references a file directly,
    // return this file.
    if (url!=null)
    {
      String file = url.getFile();
      if (file!=null && file.length()>0)
        return getFolder(file);
    }
    // Otherwise attempt to return a sensible root folder.
    String home = session.getProperty("mail.maildir.home");
    if (home==null)
    {
      try
      {
        home = System.getProperty("user.home");
        if (!exists(home))
          home = null;
      }
      catch (SecurityException e)
      {
        log("access denied reading system properties");
      }
    }
    home = toFilename(home);
    return new MaildirFolder(this, home, true, false);
  }

  /**
   * Returns the folder with the specified filename.
   */
  public Folder getFolder(String filename)
    throws MessagingException
  {
    boolean inbox = false;
    if ("inbox".equalsIgnoreCase(filename))
    {
      // First try the session property mail.mbox.inbox.
      String maildir = session.getProperty("mail.maildir.maildir");
      if (!isMaildir(maildir))
      {
        // Try some common(UNIX) locations.
        try
        {
          String userhome = System.getProperty("user.home");
          maildir = userhome+"/Maildir";
          if (!isMaildir(maildir))
            maildir = null;
        }
        catch (SecurityException e)
        {
          // not allowed to read system properties
          log("unable to access system properties");
        }
      }
      if (maildir!=null)
      {
        filename = maildir;
        inbox = true;
      }
      // otherwise we assume it is actually a file called "inbox"
    }
    filename = toFilename(filename);
    return new MaildirFolder(this, filename, false, inbox);
  }

  /*
   * Convert into a valid filename for this platform
   */
  String toFilename(String filename)
  {
    StringBuffer buf = new StringBuffer();
    if (filename.length()<1 || filename.charAt(0)!=separatorChar)
      buf.append(File.separator);
    if (separatorChar!=File.separatorChar)
      buf.append(filename.replace(separatorChar, File.separatorChar));
    else
      buf.append(filename);
    return buf.toString();
  }

  /*
   * Indicates whether the file referred to by the specified filename exists.
   */
  private boolean exists(String filename)
  {
    if (filename!=null)
    {
      File file = new File(filename);
      if (separatorChar!=File.separatorChar)
        file = new File(filename.replace(separatorChar, File.separatorChar));
      return file.exists();
    }
    return false;
  }

  private boolean isMaildir(String path)
  {
    if (path==null)
      return false;
    File file = new File(path);
    if (separatorChar!=File.separatorChar)
      file = new File(path.replace(separatorChar, File.separatorChar));
    return file.exists() && file.isDirectory();
  }

  /**
   * Returns the folder specified by the filename of the URLName.
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

  Session getSession()
  {
    return session;
  }

  /**
   * Print a log message.
   */
  void log(String message)
  {
    logger.log(MAILDIR_TRACE, message);
  }

  // -- StatusSource --

  /**
   * Adds a status listener to this store.
   * The listener will be informed of state changes during potentially
   * lengthy procedures(opening and closing mboxes).
   * @param l the status listener
   * @see #removeStatusListener
   */
  public void addStatusListener(StatusListener l)
  {
    synchronized (statusListeners)
    {
      statusListeners.add(l);
    }
  }

  /**
   * Removes a status listener from this store.
   * @param l the status listener
   * @see #addStatusListener
   */
  public void removeStatusListener(StatusListener l)
  {
    synchronized (statusListeners)
    {
      statusListeners.remove(l);
    }
  }

  /**
   * Processes a status event.
   * This dispatches the event to all the registered listeners.
   * @param event the status event
   */
  protected void processStatusEvent(StatusEvent event)
  {
    StatusListener[] listeners;
    synchronized (statusListeners)
    {
      listeners = new StatusListener[statusListeners.size()];
      statusListeners.toArray(listeners);
    }
    switch (event.getType())
    {
      case StatusEvent.OPERATION_START:
        for (int i=0; i<listeners.length; i++)
          listeners[i].statusOperationStarted(event);
        break;
      case StatusEvent.OPERATION_UPDATE:
        for (int i=0; i<listeners.length; i++)
          listeners[i].statusProgressUpdate(event);
        break;
      case StatusEvent.OPERATION_END:
        for (int i=0; i<listeners.length; i++)
          listeners[i].statusOperationEnded(event);
        break;
    }
  }

}
