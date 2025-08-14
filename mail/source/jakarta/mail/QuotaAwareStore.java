/*
 * QuotaAwareStore.java
 * Copyright (C) 2005 The Free Software Foundation
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
 * Interface implemented by mail stores that support quotas.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @see RFC 2087
 * @version 1.5
 * @since JavaMail 1.4
 */
public interface QuotaAwareStore
{

  /**
   * Returns the quotas for the given quota root.
   * @param root the quota root
   * @exception MessagingException if the QUOTA extension is not supported
   */
  Quota[] getQuota(String root)
    throws MessagingException;

  /**
   * Sets the quotas for the quota root specified in the quota argument.
   * @param quota the quota
   * @exception MessagingException if the QUOTA extension is not supported
   */
  void setQuota(Quota quota)
    throws MessagingException;

}
