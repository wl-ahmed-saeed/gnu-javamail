/*
 * AddressStringTerm.java
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

package jakarta.mail.search;

import jakarta.mail.Address;
import jakarta.mail.internet.InternetAddress;

/**
 * A string comparison of message addresses.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public abstract class AddressStringTerm
  extends StringTerm
{

  /**
   * Constructor.
   * @param pattern the address pattern for comparison
   */
  protected AddressStringTerm(String pattern)
  {
    super(pattern, true);
  }

  /**
   * Indicates whether the address pattern specified in the constructor is a
   * substring of the string representation of the given address.
   * @param a the address
   */
  protected boolean match(Address a)
  {
    if (a instanceof InternetAddress)
      {
        return super.match(((InternetAddress) a).toUnicodeString());
      }
    else
      {
        return super.match(a.toString());
      }
  }

  public boolean equals(Object other)
  {
    return ((other instanceof AddressStringTerm) && super.equals(other));
  }

}
