/*
 * MessagingException.java
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

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * A general messaging exception.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class MessagingException
  extends Exception
{

  private Exception nextException;

  /**
   * Constructs a messaging exception with no detail message.
   */
  public MessagingException()
  {
    this(null, null);
  }

  /**
   * Constructs a messaging exception with the specified detail message.
   * @param message the detail message
   */
  public MessagingException(String message)
  {
    this(message, null);
  }

  /**
   * Constructs a messaging exception with the specified exception and detail
   * message.
   * @param message the detail message
   * @param exception the embedded exception
   */
  public MessagingException(String message, Exception exception)
  {
    super(message);
    initCause(null);
    nextException = exception;
  }

  /**
   * Returns the next exception chained to this one.
   * If the next exception is a messaging exception, the chain may extend
   * further.
   */
  public Exception getNextException()
  {
    return nextException;
  }

  public Throwable getCause()
  {
    return nextException;
  }

  /**
   * Adds an exception to the end of the chain.
   * If the end is not a messaging exception, this exception cannot be added
   * to the end.
   * @param exception the new end of the exception chain
   * @return true if this exception was added, false otherwise.
   */
  public synchronized boolean setNextException(Exception exception)
  {
    Exception e = this;
    while (e instanceof MessagingException &&
           ((MessagingException) e).nextException != null)
      {
         e = ((MessagingException) e).nextException;
      }
    if (e instanceof MessagingException)
      {
        ((MessagingException) e).nextException = exception;
        return true;
      }
    return false;
  }

  public String toString()
  {
    String s = super.toString();
    Exception next = nextException;
    if (next == null)
      {
        return s;
      }
    StringBuilder buf = new StringBuilder(s);
    while (next != null)
      {
        buf.append(";\n  nested exception is: \n\t");
        if (next instanceof MessagingException)
          {
            MessagingException e = (MessagingException) next;
            buf.append(e.stringForm());
            next = e.nextException;
          }
        else
          {
            buf.append(next.toString());
            next = null;
          }
      }
    return buf.toString();
  }

  private final String stringForm()
  {
    return super.toString();
  }

}
