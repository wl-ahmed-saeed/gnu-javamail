/*
 * FlagTerm.java
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

package jakarta.mail.search;

import jakarta.mail.Flags;
import jakarta.mail.Message;

/**
 * A comparison of message flags.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class FlagTerm
  extends SearchTerm
{

  /**
   * Indicates whether to test for the presence or absence of the specified
   * flag. If true, test whether all the specified flags are present,
   * otherwise test whether all the specified flags are absent.
   */
  private boolean set;

  /**
   * The flags to test.
   */
  private Flags flags;

  /**
   * Constructor.
   * @param flags the flags to test
   * @param set whether to test for presence or absence of the specified
   * flags
   */
  public FlagTerm(Flags flags, boolean set)
  {
    this.flags = flags;
    this.set = set;
  }

  /**
   * Returns the flags to test.
   */
  public Flags getFlags()
  {
    return (Flags) flags.clone();
  }

  /**
   * Indicates whether to test for the presence or the absence of the
   * specified flags.
   */
  public boolean getTestSet()
  {
    return set;
  }

  /**
   * Returns true if the flags in the specified message match this term.
   */
  public boolean match(Message msg)
  {
    try
      {
        Flags messageFlags = msg.getFlags();
        if (set)
          {
            return messageFlags.contains(flags);
          }
        Flags.Flag[] systemFlags = flags.getSystemFlags();
        for (int i = 0; i < systemFlags.length; i++)
          {
            if (messageFlags.contains(systemFlags[i]))
              {
                return false;
              }
          }
        String[] userFlags = flags.getUserFlags();
        for (int i = 0; i < userFlags.length; i++)
          {
            if (messageFlags.contains(userFlags[i]))
              {
                return false;
              }
          }
        return true;
      }
    catch (Exception e)
      {
      }
    return false;
  }

  public boolean equals(Object other)
  {
    if (other instanceof FlagTerm)
      {
        FlagTerm ft = (FlagTerm) other;
        return (ft.set == set && ft.flags.equals(flags));
      }
    return false;
  }

  public int hashCode()
  {
    return set ?  flags.hashCode() : ~flags.hashCode();
  }

}

