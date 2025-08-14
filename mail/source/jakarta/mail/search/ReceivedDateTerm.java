/*
 * ReceivedDateTerm.java
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

import java.util.Date;
import jakarta.mail.Message;

/**
 * A comparison of message received dates.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class ReceivedDateTerm
  extends DateTerm
{

  /**
   * Constructor.
   * @param comparison the comparison operator
   * @param date the date for comparison
   */
  public ReceivedDateTerm(int comparison, Date date)
  {
    super(comparison, date);
  }

  /**
   * Returns true only if the given message's received date matches this
   * term.
   */
  public boolean match(Message msg)
  {
    try
      {
        Date d = msg.getReceivedDate();
        if (d != null)
          {
            return super.match(d);
          }
      }
    catch (Exception e)
      {
      }
    return false;
  }

  public boolean equals(Object other)
  {
    return (other instanceof ReceivedDateTerm && super.equals(other));
  }

}

