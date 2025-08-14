/*
 * ParameterList.java
 * Copyright (C) 2002, 2005, 2013 The Free Software Foundation
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

import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import gnu.inet.util.GetSystemPropertyAction;

/**
 * A list of MIME parameters. MIME parameters are name-value pairs
 * associated with a MIME header.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class ParameterList
{

  private static final ResourceBundle L10N =
    ResourceBundle.getBundle("jakarta.mail.internet.L10N");

  private static final boolean decodeParameters;
  private static final boolean encodeParameters;
  static
  {
    PrivilegedAction a;
    a = new GetSystemPropertyAction("mail.mime.decodeparameters");
    decodeParameters = !"false".equals(AccessController.doPrivileged(a));
    a = new GetSystemPropertyAction("mail.mime.encodeparameters");
    encodeParameters = !"false".equals(AccessController.doPrivileged(a));
  }

  /*
   * Names to values.
   */
  private Map<String,String> list = new LinkedHashMap<String,String>();

  /*
   * Names to MIME charsets.
   */
  private Map<String,String> charsets = new HashMap<String,String>();

  /**
   * Constructor for an empty parameter list.
   */
  public ParameterList()
  {
  }

  /**
   * Constructor with a parameter-list string.
   * @param s the parameter-list string
   * @exception ParseException if the parse fails
   */
  public ParameterList(String s)
    throws ParseException
  {
    HeaderTokenizer ht = new HeaderTokenizer(s, HeaderTokenizer.MIME);
    HeaderTokenizer.Token token = ht.next();
    int type = token.getType();
    while (type != HeaderTokenizer.Token.EOF)
      {
        if (type != 0x3b) // ';'
          {
            String m = L10N.getString("err.expected_semicolon");
            Object[] args = new Object[] { s };
            throw new ParseException(MessageFormat.format(m, args));
          }
        token = ht.next();
        type = token.getType();
        if (type != HeaderTokenizer.Token.ATOM)
          {
            String m = L10N.getString("err.expected_parameter_name");
            Object[] args = new Object[] { s };
            throw new ParseException(MessageFormat.format(m, args));
          }
        String key = token.getValue().toLowerCase();
        token = ht.next();
        type = token.getType();
        if (type != 0x3d) // '='
          {
            String m = L10N.getString("err.expected_equals");
            Object[] args = new Object[] { s };
            throw new ParseException(MessageFormat.format(m, args));
          }
        token = ht.next();
        type = token.getType();
        if (type != HeaderTokenizer.Token.ATOM &&
            type != HeaderTokenizer.Token.QUOTEDSTRING)
          {
            String m = L10N.getString("err.expected_parameter_value");
            Object[] args = new Object[] { s };
            throw new ParseException(MessageFormat.format(m, args));
          }
        list.put(key, token.getValue());
        token = ht.next();
        type = token.getType();
      }
    doCombineSegments(true);
  }

  /**
   * Combines individual segments of multi-segment names.
   * @see RFC 2231
   * @since JavaMail 1.5
   */
  public void combineSegments()
  {
    try
      {
        doCombineSegments(false);
      }
    catch (ParseException e)
      {
        // NOOP
      }
  }

  private void doCombineSegments(boolean fussy)
    throws ParseException
  {
    if (!decodeParameters)
      {
        return;
      }
    List<String> names = new ArrayList<String>(list.keySet());
    Collections.sort(names); // To get segments in ascending order
    int len = names.size();
    Map<String,List<String>> segments = new HashMap<String,List<String>>();
    try
      {
        for (int i = 0; i < len; i++)
          {
            String key = names.get(i);
            String value = unquote(list.get(key));
            int si = key.lastIndexOf('*');
            if (si == key.length() - 1)
              {
                // Parameter is encoded, replace with decoded version
                int ai = value.indexOf('\'');
                if (ai == -1)
                  {
                    if (fussy)
                      {
                        String m = L10N.getString("err.no_charset");
                        Object[] args = new Object[] { key, value };
                        m = MessageFormat.format(m, args);
                        throw new ParseException(m);
                      }
                  }
                else
                  {
                    String charset = value.substring(0, ai);
                    ai = value.indexOf('\'', ai + 1); // skip language
                    if (ai == -1)
                      {
                        if (fussy)
                          {
                            String m = L10N.getString("err.no_language");
                            Object[] args = new Object[] { key, value };
                            m = MessageFormat.format(m, args);
                            throw new ParseException(m);
                          }
                      }
                    else
                      {
                        value = decode(value.substring(ai + 1), charset);
                        list.remove(key); // Remove encoded version
                        String newkey = key.substring(0, si);
                        list.put(newkey, value); // Put decoded version
                        charsets.put(newkey, charset);
                      }
                  }
                key = key.substring(0, si);
                si = key.lastIndexOf('*');
              }
            if (si > 0 && si < key.length() - 1)
              {
                // This is a segment
                int segmentIndex = Integer.parseInt(key.substring(si + 1));
                list.remove(key); // Remove segment from list
                String charset = charsets.get(key);
                key = key.substring(0, si);
                charsets.put(key, charset); // Won't handle multiple charsets
                List<String> values = segments.get(key);
                if (values == null)
                  {
                    values = new ArrayList<String>();
                    segments.put(key, values);
                  }
                values.add(unquote(value));
              }
          }
      }
    finally
      {
        // Concatenate the segments and put into list
        Map<String,String> lookup = new HashMap<String,String>();
        for (Iterator<String> i = segments.keySet().iterator(); i.hasNext(); )
          {
            String key = i.next();
            List<String> values = segments.get(key);
            int vlen = values.size();
            StringBuilder buf = new StringBuilder();
            for (int j = 0; j < vlen; j++)
              {
                buf.append(values.get(j));
              }
            list.put(key, buf.toString());
          }
      }
  }

  private static String unquote(String value)
  {
    int len = value.length();
    if (len > 1 &&
        value.charAt(0) == '"' &&
        value.charAt(len - 1) == '"')
      {
        value = value.substring(1, len - 1);
      }
    return value;
  }

  private String decode(String text, String charset)
    throws ParseException
  {
    char[] schars = text.toCharArray();
    int slen = schars.length;
    byte[] dchars = new byte[slen];
    int dlen = 0;
    for (int i = 0; i < slen; i++)
      {
        char c = schars[i];
        if (c == '%')
          {
            if (i + 3 > slen)
              {
                String m = L10N.getString("err.bad_rfc2231_encoding");
                Object[] args = new Object[] { text };
                m = MessageFormat.format(m, args);
                throw new ParseException(m);
              }
            int val = Character.digit(schars[i + 1], 16) << 4 +
              Character.digit(schars[i + 2], 16);
            dchars[dlen++] = ((byte) val);
            i += 2;
          }
        else
          {
            dchars[dlen++] = ((byte) c);
          }
      }
    String javaCharset = MimeUtility.javaCharset(charset);
    try
      {
        return new String(dchars, 0, dlen, javaCharset);
      }
    catch (UnsupportedEncodingException e)
      {
        String m = L10N.getString("err.bad_encoding");
        Object[] args = new Object[] { javaCharset };
        m = MessageFormat.format(m, args);
        throw new ParseException(m);
      }
  }

  /**
   * Returns the number of parameters in this list.
   */
  public int size()
  {
    return list.size();
  }

  /**
   * Returns the value of the specified parameter.
   * Parameter names are case insensitive.
   * @param name the parameter name
   */
  public String get(String name)
  {
    return list.get(name.toLowerCase().trim());
  }

  /**
   * Sets the specified parameter.
   * @param name the parameter name
   * @param value the parameter value
   */
  public void set(String name, String value)
  {
    list.put(name.toLowerCase().trim(), value);
  }

  /**
   * Sets the specified parameter.
   * @param name the parameter name
   * @param value the parameter value
   * @param charset the character set to use to encode the value, if
   * <code>mail.mime.encodeparameters</code> is true.
   * @since JavaMail 1.5
   */
  public void set(String name, String value, String charset)
  {
    String key = name.toLowerCase().trim();
    list.put(key, value);
    charsets.put(key, charset);
  }

  /**
   * Removes the specified parameter from this list.
   * @param name the parameter name
   */
  public void remove(String name)
  {
    list.remove(name.toLowerCase().trim());
  }

  /**
   * Returns the names of all parameters in this list.
   * @return an Enumeration of String
   */
  public Enumeration getNames()
  {
    return new ParameterEnumeration(list.keySet().iterator());
  }

  /**
   * Returns the MIME string representation of this parameter list.
   */
  public String toString()
  {
    // Simply calls toString(int) with a used value of 0.
    return toString(0);
  }

  /**
   * Returns the MIME string representation of this parameter list.
   * @param used the number of character positions already used in the
   * field into which the parameter list is to be inserted
   */
  public String toString(int used)
  {
    // NB this is a bloody complicated implementation
    // because we need to wrap the whole line, and we also need to generate
    // multiple segments for individual values that don't fit on the current
    // line.
    // The spec doesn't handle wrapping parameter names so stuff might still
    // flow over the line boundary but we should be compliant.
    StringBuilder buf = new StringBuilder();
    for (Iterator<String> i = list.keySet().iterator(); i.hasNext(); )
      {
        String key = i.next();
        String value = list.get(key);
        String charset = charsets.get(key);

        // delimiter
        buf.append("; ");
        used += 2;

        // handle wrap after delimiter
        if (used > 76)
          {
            buf.append("\r\n\t");
            used = 8;
          }

        boolean handled = false;
        int klen = key.length();
        int segmentIndex = 0;
        if (encodeParameters && needsEncoding(value))
          {
            // RFC 2231 encoding
            try
              {
                if (charset == null || "".equals(charset))
                  {
                    charset = "UTF-8";
                  }
                byte[] b = value.getBytes(MimeUtility.javaCharset(charset));
                StringBuilder vb = new StringBuilder(charset).append("''");
                for (int j = 0; j < b.length; j++)
                  {
                    String segmentIndexString = Integer.toString(segmentIndex);
                    int len = klen + segmentIndexString.length() +
                      vb.length() + 4;
                    char c = (char) (b[j] & 255);
                    boolean needsEncoding =
                      (c <= 32 ||
                       c >= 127 ||
                       c == '%' ||
                       c == '\'' ||
                       c == '*' ||
                       HeaderTokenizer.MIME.indexOf(c) != -1);
                    int clen = needsEncoding ? 3 : 1;
                    if (used + len + clen > 76)
                      {
                        buf.append("\r\n\t");
                        used = 8;
                      }
                    if (used + len + clen > 76)
                      {
                        buf.append(key);
                        buf.append('*');
                        buf.append(segmentIndexString);
                        buf.append('*');
                        buf.append('=');
                        buf.append(vb.toString());
                        buf.append(";\r\n\t");
                        used = 8;
                        segmentIndex++;
                        vb = new StringBuilder(charset).append("''");
                      }
                    if (needsEncoding)
                      {
                        vb.append('%');
                        vb.append(Character.forDigit((c >> 4) & 0xf, 16));
                        vb.append(Character.forDigit(c & 0xf, 16));
                      }
                    else
                      {
                        vb.append(c);
                      }
                  }
                buf.append(key);
                if (segmentIndex > 0)
                  {
                    String segmentIndexString = Integer.toString(segmentIndex);
                    buf.append('*');
                    buf.append(segmentIndexString);
                    used += segmentIndexString.length() + 1;
                  }
                buf.append('*');
                buf.append('=');
                buf.append(vb.toString());
                used += klen + vb.length() + 2;
                handled = true;
              }
            catch (UnsupportedEncodingException e)
              {
                // ignore
              }
          }
        if (!handled)
          {
            int vlen = value.length();
            if (used + klen + 1 + vlen > 76)
              {
                buf.append("\r\n\t");
                used = 8;
              }
            if (used + klen + 1 + vlen > 76)
              {
                StringBuilder vb = new StringBuilder();
                for (int j = 0; j < vlen; j++)
                  {
                    String segmentIndexString = Integer.toString(segmentIndex);
                    int len = klen + segmentIndexString.length() +
                      vb.length() + 3;
                    if (used + len + 1 > 76)
                      {
                        buf.append(key);
                        buf.append('*');
                        buf.append(segmentIndexString);
                        buf.append('=');
                        buf.append(vb.toString());
                        buf.append(";\r\n\t");
                        used = 8;
                        segmentIndex++;
                        vb = new StringBuilder();
                      }
                    char c = value.charAt(j);
                    vb.append(c);
                  }
                buf.append(key);
                if (segmentIndex > 0)
                  {
                    String segmentIndexString = Integer.toString(segmentIndex);
                    buf.append('*');
                    buf.append(segmentIndexString);
                    used += segmentIndexString.length() + 1;
                  }
                buf.append('=');
                buf.append(vb.toString());
                used += klen + vb.length() + 1;
              }
            else
              {
                buf.append(key);
                buf.append('=');
                buf.append(value);
                used += klen + 1 + vlen;
              }
          }
      }
    return buf.toString();
  }

  private static boolean needsEncoding(String value)
  {
    int len = value.length();
    for (int i = 0; i < len; i++)
      {
        char c = value.charAt(i);
        if (c < 32 || c > 126 || c == ';' || c == '"')
          {
            return true;
          }
      }
    return false;
  }

  /*
   * Needed to provide an enumeration interface for the key iterator.
   */
  static class ParameterEnumeration
    implements Enumeration
  {

    private final Iterator source;

    ParameterEnumeration(Iterator source)
    {
      this.source = source;
    }

    public boolean hasMoreElements()
    {
      return source.hasNext();
    }

    public Object nextElement()
    {
      return source.next();
    }

  }

}

