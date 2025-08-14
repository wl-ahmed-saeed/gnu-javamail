/*
 * StoreEvent.java
 * Copyright (C) 2002 The Free Software Foundation
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

import jakarta.mail.Store;

/**
 * A store notification event.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class StoreEvent
  extends MailEvent
{

  /**
   * Indicates that this message is an ALERT.
   */
  public static final int ALERT = 1;

  /**
   * Indicates that this message is a NOTICE.
   */
  public static final int NOTICE = 2;

  /**
   * The event type.
   */
  protected int type;

  /**
   * The message text to be presented to the user.
   */
  protected String message;

  /**
   * Constructor.
   * @param source the store
   * @param type the type of event (ALERT or NOTICE)
   * @param message the notification message
   */
  public StoreEvent(Store source, int type, String message)
  {
    super(source);
    this.type = type;
    this.message = message;
  }

  /**
   * Returns the type of this event.
   */
  public int getMessageType()
  {
    return type;
  }

  /**
   * Returns the notification message.
   */
  public String getMessage()
  {
    return message;
  }

  /**
   * Invokes the appropriate listener method.
   */
  public void dispatch(Object listener)
  {
   ((StoreListener) listener).notification(this);
  }

}
