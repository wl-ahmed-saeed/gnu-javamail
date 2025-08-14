/*
 * StoreClosedException.java
 * Copyright (C) 2002, 2013 The Free Software Foundation
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

package jakarta.mail;

/**
 * An exception thrown when a method is invoked on a message or folder
 * whose owner store has been closed for some reason.
 * <p>
 * The <code>connect</connect> method may be invoked on the store object to
 * reconnect, but any references to existing folders and messages should be
 * considered invalid.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class StoreClosedException
  extends MessagingException
{

  /*
   * The store.
   */
  private final Store store;

  public StoreClosedException(Store store)
  {
    this(store, null);
  }

  public StoreClosedException(Store store, String message)
  {
    super(message);
    this.store = store;
  }

  /**
   * @since JavaMail 1.5
   */
  public StoreClosedException(Store store, String message, Exception e)
  {
    super(message, e);
    this.store = store;
  }

  /**
   * Returns the store.
   */
  public Store getStore()
  {
    return store;
  }

}
