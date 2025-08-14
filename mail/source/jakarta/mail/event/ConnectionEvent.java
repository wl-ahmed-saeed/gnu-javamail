/*
 * ConnectionEvent.java
 * Copyright (C) 2002 The Free Software Fooundation
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

package jakarta.mail.event;

/**
 * A connection event.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class ConnectionEvent
  extends MailEvent
{

  /**
   * A connection was opened.
   */
  public static final int OPENED = 1;

  /**
   * A connection was disconnected (not currently used).
   */
  public static final int DISCONNECTED = 2;

  /**
   * A connection was closed.
   */
  public static final int CLOSED = 3;

  /**
   * The event type.
   */
  protected int type;

  /**
   * Constructor.
   * @param source the source
   * @param type one of OPENED, DISCONNECTED, or CLOSED
   */
  public ConnectionEvent(Object source, int type)
  {
    super(source);
    this.type = type;
  }

  /**
   * Returns the type of this event.
   */
  public int getType()
  {
    return type;
  }

  /**
   * Invokes the appropriate listener method.
   */
  public void dispatch(Object listener)
  {
    ConnectionListener l = (ConnectionListener) listener;
    switch (type)
    {
      case OPENED:
        l.opened(this);
        break;
      case DISCONNECTED:
        l.disconnected(this);
        break;
      case CLOSED:
        l.closed(this);
        break;
    }
  }
}
