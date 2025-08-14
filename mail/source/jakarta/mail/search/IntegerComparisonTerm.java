/*
 * IntegerComparisonTerm.java
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
 * An integer comparison.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public abstract class IntegerComparisonTerm
  extends ComparisonTerm
{

  /**
   * The number.
   */
  protected int number;

  protected IntegerComparisonTerm(int comparison, int number)
  {
    this.comparison = comparison;
    this.number = number;
  }

  /**
   * Returns the number to compare with.
   */
  public int getNumber()
  {
    return number;
  }

  /**
   * Returns the type of comparison.
   */
  public int getComparison()
  {
    return super.comparison;
  }

  protected boolean match(int i)
  {
    switch (comparison)
      {
      case LE:
        return i <= number;
      case LT:
        return i < number;
      case EQ:
        return i == number;
      case NE:
        return i != number;
      case GT:
        return i > number;
      case GE:
        return i >= number;
      }
    return false;
  }

  public boolean equals(Object other)
  {
    return (other instanceof IntegerComparisonTerm &&
       ((IntegerComparisonTerm) other).number == number &&
        super.equals(other));
  }

  public int hashCode()
  {
    return number + super.hashCode();
  }

}

