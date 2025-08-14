/*
 * SendFailedException.java
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

package jakarta.mail;

/**
 * An exception thrown when a message cannot be sent.
 * <p>
 * It includes those addresses to which the message could not be
 * sent as well as the valid addresses to which the message was sent
 * and valid addresses to which the message was not sent.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class SendFailedException
  extends MessagingException
{

  protected transient Address[] invalid;
  protected transient Address[] validSent;
  protected transient Address[] validUnsent;

  public SendFailedException()
  {
  }

  public SendFailedException(String message)
  {
    super(message);
  }

  public SendFailedException(String message, Exception exception)
  {
    super(message, exception);
  }

  /**
   * Creates a send failed exception with the specified string and
   * addresses.
   * @param message the detail message
   * @param exception the embedded exception
   * @param validSent valid addresses to which message was sent
   * @param validUnsent valid addresses to which message was not sent
   * @param invalid the invalid addresses
   */
  public SendFailedException(String message, Exception exception,
                             Address[] validSent, Address[] validUnsent,
                             Address[] invalid)
  {
    super(message, exception);
    this.validSent = validSent;
    this.validUnsent = validUnsent;
    this.invalid = invalid;
  }

  /**
   * Returns the addresses to which this message was sent succesfully.
   */
  public Address[] getValidSentAddresses()
  {
    return validSent;
  }

  /**
   * Returns the addresses that are valid but to which this message was
   * not sent.
   */
  public Address[] getValidUnsentAddresses()
  {
    return validUnsent;
  }

  /**
   * Returns the addresses to which this message could not be sent.
   */
  public Address[] getInvalidAddresses()
  {
    return invalid;
  }

}

