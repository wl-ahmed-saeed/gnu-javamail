/*
 * AndTerm.java
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

import jakarta.mail.Message;

/**
 * A logical AND of a number of search terms.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public final class AndTerm
  extends SearchTerm
{

  /**
   * The target terms.
   */
  private SearchTerm[] terms;

  /**
   * Constructor with two terms.
   * @param t1 the first term
   * @param t2 the second term
   */
  public AndTerm(SearchTerm t1, SearchTerm t2)
  {
    terms = new SearchTerm[2];
    terms[0] = t1;
    terms[1] = t2;
  }

  /**
   * Constructor with multiple terms.
   * @param t the terms
   */
  public AndTerm(SearchTerm[] t)
  {
    terms = new SearchTerm[t.length];
    System.arraycopy(t, 0, terms, 0, t.length);
  }

  /**
   * Returns the search terms.
   */
  public SearchTerm[] getTerms()
  {
    return (SearchTerm[]) terms.clone();
  }

  /**
   * Returns true only if all the terms match the specified message.
   */
  public boolean match(Message message)
  {
    for (int i = 0; i < terms.length; i++)
      {
        if (!terms[i].match(message))
          {
            return false;
          }
      }
    return true;
  }

  public boolean equals(Object other)
  {
    if (other instanceof AndTerm)
      {
        AndTerm andterm = (AndTerm)other;
        if (andterm.terms.length != terms.length)
          {
            return false;
          }
        for (int i = 0; i < terms.length; i++)
          {
            if (!terms[i].equals(andterm.terms[i]))
              {
                return false;
              }
          }
        return true;
      }
    return false;
  }

  public int hashCode()
  {
    int acc = 0;
    for (int i = 0; i < terms.length; i++)
      {
        acc += terms[i].hashCode();
      }
    return acc;
  }

}

