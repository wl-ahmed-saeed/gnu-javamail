/*
 * StringTerm.java
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

/**
 * A comparison of string values.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public abstract class StringTerm
  extends SearchTerm
{

  /**
   * The pattern to match.
   */
  protected String pattern;

  /**
   * Whether to ignore case during comparison.
   */
  protected boolean ignoreCase;

  protected StringTerm(String pattern)
  {
    this(pattern, true);
  }

  protected StringTerm(String pattern, boolean ignoreCase)
  {
    this.pattern = pattern;
    this.ignoreCase = ignoreCase;
  }

  /**
   * Returns the pattern to match.
   */
  public String getPattern()
  {
    return pattern;
  }

  /**
   * Indicates whether to ignore case during comparison.
   */
  public boolean getIgnoreCase()
  {
    return ignoreCase;
  }

  /**
   * Returns true if the specified pattern is a substring of the given string.
   */
  protected boolean match(String s)
  {
    int patlen = pattern.length();
    int len = s.length() - patlen;
    for (int i = 0; i <= len; i++)
      {
        if (s.regionMatches(ignoreCase, i, pattern, 0, patlen))
          {
            return true;
          }
      }
    return false;
  }

  public boolean equals(Object other)
  {
    if (other instanceof StringTerm)
      {
        StringTerm st = (StringTerm)other;
        if (ignoreCase)
          {
            return st.pattern.equalsIgnoreCase(pattern) &&
              st.ignoreCase == ignoreCase;
          }
        else
          {
            return st.pattern.equals(pattern) &&
              st.ignoreCase == ignoreCase;
          }
      }
    return false;
  }

  public int hashCode()
  {
    return (ignoreCase) ? pattern.hashCode() : ~pattern.hashCode();
  }

}

