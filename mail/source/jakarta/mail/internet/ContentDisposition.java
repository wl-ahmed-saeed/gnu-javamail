/*
 * ContentDisposition.java
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

package jakarta.mail.internet;

/**
 * A MIME Content-Disposition value.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class ContentDisposition
{

  /*
   * The disposition value.
   */
  private String disposition;

  /*
   * The parameters.
   */
  private ParameterList list;

  /**
   * Constructor for an empty Content-Disposition.
   */
  public ContentDisposition()
  {
  }

  /**
   * Constructor.
   * @param disposition the disposition value
   * @param list the parameters
   */
  public ContentDisposition(String disposition, ParameterList list)
  {
    this.disposition = disposition;
    this.list = list;
  }

  /**
   * Constructor that parses a Content-Disposition value from an RFC 2045
   * string representation.
   * @param s the Content-Disposition value
   * @exception ParseException if there was an error in the value
   */
  public ContentDisposition(String s)
    throws ParseException
  {
    HeaderTokenizer ht = new HeaderTokenizer(s, HeaderTokenizer.MIME);
    HeaderTokenizer.Token token = ht.next();
    if (token.getType() != HeaderTokenizer.Token.ATOM)
      {
        throw new ParseException();
      }

    disposition = token.getValue();

    s = ht.getRemainder();
    if (s != null)
      {
        list = new ParameterList(s);
      }
  }

  /**
   * Returns the disposition value.
   */
  public String getDisposition()
  {
    return disposition;
  }

  /**
   * Returns the specified parameter value, or <code>null</code> if this
   * parameter is not present.
   * @param name the parameter name
   */
  public String getParameter(String name)
  {
    return (list != null) ? list.get(name) : null;
  }

  /**
   * Returns the parameters, or <code>null</code> if there are no
   * parameters.
   */
  public ParameterList getParameterList()
  {
    return list;
  }

  /**
   * Sets the disposition value.
   * @param disposition the disposition value
   */
  public void setDisposition(String disposition)
  {
    this.disposition = disposition;
  }

  /**
   * Sets the specified parameter.
   * @param name the parameter name
   * @param value the parameter value
   */
  public void setParameter(String name, String value)
  {
    if (list == null)
      {
        list = new ParameterList();
      }
    list.set(name, value);
  }

  /**
   * Sets the parameters.
   * @param list the parameters
   */
  public void setParameterList(ParameterList list)
  {
    this.list = list;
  }

  /**
   * Returns an RFC 2045 string representation of this Content-Disposition.
   */
  public String toString()
  {
    if (disposition == null)
      {
        return "";
      }
    if (list == null)
      {
        return (disposition == null) ? "" : disposition;
      }
    else
      {
        StringBuffer buffer = new StringBuffer();
        buffer.append(disposition);

        // Add the parameters, using the toString(int) method
        // which allows the resulting string to fold properly onto the next
        // header line.
        int used = buffer.length() + 21; // "Content-Disposition: ".length()
        buffer.append(list.toString(used));
        return buffer.toString();
      }
  }

}

