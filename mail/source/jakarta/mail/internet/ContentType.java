/*
 * ContentType.java
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

import java.text.MessageFormat;
import java.util.ResourceBundle;

/**
 * A MIME Content-Type value.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class ContentType
{

  private static final ResourceBundle L10N
    = ResourceBundle.getBundle("jakarta.mail.internet.L10N");

  /*
   * The primary type.
   */
  private String primaryType;

  /*
   * The subtype.
   */
  private String subType;

  /*
   * The parameters.
   */
  private ParameterList list;

  /**
   * Constructor for an empty Content-Type.
   */
  public ContentType()
  {
  }

  /**
   * Constructor.
   * @param primaryType the primary type
   * @param subType the subtype
   * @param list the parameters
   */
  public ContentType(String primaryType, String subType, ParameterList list)
  {
    this.primaryType = primaryType;
    this.subType = subType;
    this.list = list;
  }

  /**
   * Constructor that parses a Content-Type value from an RFC 2045 string
   * representation.
   * @param s the Content-Type value
   * @exception ParseException if an error occurred during parsing
   */
  public ContentType(String s)
    throws ParseException
  {
    HeaderTokenizer ht = new HeaderTokenizer(s, HeaderTokenizer.MIME);
    HeaderTokenizer.Token token = ht.next();
    if (token.getType() != HeaderTokenizer.Token.ATOM)
      {
        String m = L10N.getString("err.expected_primary_type");
        Object[] args = new Object[] { s };
        throw new ParseException(MessageFormat.format(m, args));
      }
    primaryType = token.getValue();
    token = ht.next();
    if (token.getType() != 0x2f) // '/'
      {
        String m = L10N.getString("err.expected_slash");
        Object[] args = new Object[] { s };
        throw new ParseException(MessageFormat.format(m, args));
      }
    token = ht.next();
    if (token.getType() != HeaderTokenizer.Token.ATOM)
      {
        String m = L10N.getString("err.expected_subtype");
        Object[] args = new Object[] { s };
        throw new ParseException(MessageFormat.format(m, args));
      }
    subType = token.getValue();
    s = ht.getRemainder();
    if (s != null)
      {
        list = new ParameterList(s);
      }
  }

  /**
   * Returns the primary type.
   */
  public String getPrimaryType()
  {
    return primaryType;
  }

  /**
   * Returns the subtype.
   */
  public String getSubType()
  {
    return subType;
  }

  /**
   * Returns the MIME type string, without the parameters.
   */
  public String getBaseType()
  {
    if (primaryType == null || subType == null)
      {
        return null;
      }
    StringBuffer buffer = new StringBuffer();
    buffer.append(primaryType);
    buffer.append('/');
    buffer.append(subType);
    return buffer.toString();
  }

  /**
   * Returns the specified parameter value.
   */
  public String getParameter(String name)
  {
    return (list == null) ? null : list.get(name);
  }

  /**
   * Returns the parameters.
   */
  public ParameterList getParameterList()
  {
    return list;
  }

  /**
   * Sets the primary type.
   */
  public void setPrimaryType(String primaryType)
  {
    this.primaryType = primaryType;
  }

  /**
   * Sets the subtype.
   */
  public void setSubType(String subType)
  {
    this.subType = subType;
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
   * @param list the Parameter list
   */
  public void setParameterList(ParameterList list)
  {
    this.list = list;
  }

  /**
   * Returns an RFC 2045 string representation of this Content-Type.
   */
  public String toString()
  {
    if (primaryType == null || subType == null)
      {
        return "";
      }

    StringBuffer buffer = new StringBuffer();
    buffer.append(primaryType);
    buffer.append('/');
    buffer.append(subType);
    if (list != null)
      {
        // Add the parameters, using the toString(int) method
        // which allows the resulting string to fold properly onto the next
        // header line.
        int used = buffer.length() + 14; // "Content-Type: ".length()
        buffer.append(list.toString(used));
      }
    return buffer.toString();
  }

  /**
   * Returns true if the specified Content-Type matches this Content-Type.
   * Parameters are ignored.
   * <p>
   * If the subtype of either Content-Type is the special character '*',
   * the subtype is ignored during the match.
   * @param cType the Content-Type for comparison
   */
  public boolean match(ContentType cType)
  {
    if (!primaryType.equalsIgnoreCase(cType.getPrimaryType()))
      {
        return false;
      }
    String cTypeSubType = cType.getSubType();
    if (subType.charAt(0) == '*' || cTypeSubType.charAt(0) == '*')
      {
        return true;
      }
    return subType.equalsIgnoreCase(cTypeSubType);
  }

  /**
   * Returns true if the specified Content-Type matches this Content-Type.
   * Parameters are ignored.
   * <p>
   * If the subtype of either Content-Type is the special character '*',
   * the subtype is ignored during the match.
   * @param s the RFC 2045 string representation of the Content-Type to match
   */
  public boolean match(String s)
  {
    try
      {
        return match(new ContentType(s));
      }
    catch (ParseException e)
      {
        return false;
      }
  }

}

