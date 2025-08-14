/*
 * AuthenticationFailedException.java
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
 * An exception thrown to indicate that authentication failed during a
 * service's <code>connect</code> method.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class AuthenticationFailedException
  extends MessagingException
{

  public AuthenticationFailedException()
  {
  }

  public AuthenticationFailedException(String message)
  {
    super(message);
  }

  /**
   * @since JavaMail 1.5
   */
  public AuthenticationFailedException(String message, Exception e)
  {
    super(message, e);
  }

}
