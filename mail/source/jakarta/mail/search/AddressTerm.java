/*
 * AddressTerm.java
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

/**
 * A comparison of message addresses.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public abstract class AddressTerm
  extends SearchTerm
{

  /**
   * The address.
   */
  protected Address address;

  protected AddressTerm(Address address)
  {
    this.address = address;
  }

  /**
   * Returns the address to match.
   */
  public Address getAddress()
  {
    return address;
  }

  /**
   * Returns true if the specified address matches the address specified in
   * this term.
   */
  protected boolean match(Address address)
  {
    return address.equals(this.address);
  }

  public boolean equals(Object other)
  {
    return ((other instanceof AddressTerm) &&
       ((AddressTerm) other).address.equals(address));
  }

  public int hashCode()
  {
    return address.hashCode();
  }

}

