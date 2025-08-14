/*
 * Authenticator.java
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

import java.net.InetAddress;

/**
 * Callback object that can be used to obtain password information during
 * authentication. This normally occurs by prompting the user for a password
 * or retrieving it from secure storage.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public abstract class Authenticator
{

  private String defaultUserName;
  private int requestingPort = -1;
  private String requestingPrompt;
  private String requestingProtocol;
  private InetAddress requestingSite;

  final PasswordAuthentication requestPasswordAuthentication(
      InetAddress requestingSite,
      int requestingPort,
      String requestingProtocol,
      String requestingPrompt,
      String defaultUserName)
  {
    this.requestingSite = requestingSite;
    this.requestingPort = requestingPort;
    this.requestingProtocol = requestingProtocol;
    this.requestingPrompt = requestingPrompt;
    this.defaultUserName = defaultUserName;
    return getPasswordAuthentication();
  }

  /**
   * Returns the default user name.
   */
  protected final String getDefaultUserName()
  {
    return defaultUserName;
  }

  /**
   * Called when password authentication is needed.
   * This method should be overridden by subclasses.
   */
  protected PasswordAuthentication getPasswordAuthentication()
  {
    return null;
  }

  /**
   * Returns the port used in the request.
   */
  protected final int getRequestingPort()
  {
    return requestingPort;
  }

  /**
   * Returns the user prompt string for the request.
   */
  protected final String getRequestingPrompt()
  {
    return requestingPrompt;
  }

  /**
   * Returns the protocol for the request.
   */
  protected final String getRequestingProtocol()
  {
    return requestingProtocol;
  }

  /**
   * Returns the IP address of the request host,
   * or null if not available.
   */
  protected final InetAddress getRequestingSite()
  {
    return requestingSite;
  }

}
