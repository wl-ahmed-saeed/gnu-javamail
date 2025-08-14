/*
 * MessageChangedEvent.java
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

import jakarta.mail.Message;

/**
 * A message change event.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class MessageChangedEvent
  extends MailEvent
{

  /**
   * The message's flags changed.
   */
  public static final int FLAGS_CHANGED = 1;

  /**
   * The message's envelope (headers, but not content) changed.
   */
  public static final int ENVELOPE_CHANGED = 2;

  /**
   * The event type.
   */
  protected int type;

  /**
   * The message that changed.
   */
  protected transient Message msg;

  /**
   * Constructor.
   * @param source the owner folder
   * @param type the type of change (FLAGS_CHANGED or ENVELOPE_CHANGED)
   * @param msg the changed message
   */
  public MessageChangedEvent(Object source, int type, Message msg)
  {
    super(source);
    this.msg = msg;
    this.type = type;
  }

  /**
   * Returns the type of this event.
   */
  public int getMessageChangeType()
  {
    return type;
  }

  /**
   * Returns the changed message.
   */
  public Message getMessage()
  {
    return msg;
  }

  /**
   * Invokes the appropriate listener method.
   */
  public void dispatch(Object listener)
  {
    ((MessageChangedListener) listener).messageChanged(this);
  }

}

