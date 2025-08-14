/*
 * HeaderTokenizer.java
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
 * A lexer for RFC 822 and MIME headers.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class HeaderTokenizer
{

  /**
   * A token returned by the lexer. These tokens are specified in RFC 822
   * and MIME.
   */
  public static class Token
  {

    /**
     * An ATOM.
     */
    public static final int ATOM = -1;

    /**
     * A quoted-string.
     * The value of this token is the string without the quotes.
     */
    public static final int QUOTEDSTRING = -2;

    /**
     * A comment.
     * The value of this token is the comment string without the comment
     * start and end symbols.
     */
    public static final int COMMENT = -3;

    /**
     * The end of the input.
     */
    public static final int EOF = -4;

    /*
     * The token type.
     */
    private int type;

    /*
     * The value of the token if it is of type ATOM, QUOTEDSTRING, or
     * COMMENT.
     */
    private String value;

    /**
     * Constructor.
     * @param type the token type
     * @param value the token value
     */
    public Token(int type, String value)
    {
      this.type = type;
      this.value = value;
    }

    /**
     * Returns the token type.
     * If the token is a delimiter or a control character,
     * the type is the integer value of that character.
     * Otherwise, its value is one of the following:
     * <ul>
     * <li>ATOM: a sequence of ASCII characters delimited by either
     * SPACE, CTL, '(', '"' or the specified SPECIALS
     * <li>QUOTEDSTRING: a sequence of ASCII characters within quotes
     * <li>COMMENT: a sequence of ASCII characters within '(' and ')'
     * <li>EOF: the end of the header
     * </ul>
     */
    public int getType()
    {
      return type;
    }

    /**
     * Returns the value of the token.
     */
    public String getValue()
    {
      return value;
    }

  }

  private static final ResourceBundle L10N
    = ResourceBundle.getBundle("jakarta.mail.internet.L10N");

  /**
   * RFC 822 specials.
   */
  public static final String RFC822 = "()<>@,;:\\\"\t .[]";

  /**
   * MIME specials.
   */
  public static final String MIME = "()<>@,;:\\\"\t []/?=";

  /*
   * The EOF token.
   */
  private static final Token EOF = new Token(Token.EOF, null);

  /*
   * The header string to parse.
   */
  private String header;

  /*
   * The delimiters.
   */
  private String delimiters;

  /*
   * Whather to skip comments.
   */
  private boolean skipComments;

  /*
   * The index of the character identified as current for the token()
   * call.
   */
  private int pos = 0;

  /*
   * The index of the character that will be considered current on a call to
   * next().
   */
  private int next = 0;

  /*
   * The index of the character that will be considered current on a call to
   * peek().
   */
  private int peek = 0;

  private int maxPos;

  /**
   * Constructor.
   * @param header the RFC 822 header to be tokenized
   * @param delimiters the delimiter characters to be used to delimit ATOMs
   * @param skipComments whether to skip comments
   */
  public HeaderTokenizer(String header, String delimiters,
                         boolean skipComments)
  {
    this.header = (header == null) ? "" : header;
    this.delimiters = delimiters;
    this.skipComments = skipComments;
    pos = next = peek = 0;
    maxPos = header.length();
  }

  /**
   * Constructor.
   * Comments are ignored.
   * @param header the RFC 822 header to be tokenized
   * @param delimiters the delimiter characters to be used to delimit ATOMs
   */
  public HeaderTokenizer(String header, String delimiters)
  {
    this(header, delimiters, true);
  }

  /**
   * Constructor.
   * The RFC822-defined delimiters are used to delimit ATOMs.
   * Comments are ignored.
   */
  public HeaderTokenizer(String header)
  {
    this(header, RFC822, true);
  }

  /**
   * Returns the next token.
   * @return the next token
   * @exception ParseException if the parse fails
   */
  public Token next()
    throws ParseException
  {
    return next('\0', false);
  }

  /**
   * Returns the next token.
   * @param endOfAtom if not NUL, character marking the end of an atom
   * @return the next token
   * @exception ParseException if the parse fails
   * @since JavaMail 1.5
   */
  public Token next(char endOfAtom)
    throws ParseException
  {
    return next(endOfAtom, false);
  }

  /**
   * Returns the next token.
   * @param endOfAtom if not NUL, character marking the end of an atom
   * @param keepEscapes if true, keep backslashes
   * @return the next token
   * @exception ParseException if the parse fails
   * @since JavaMail 1.5
   */
  public Token next(char endOfAtom, boolean keepEscapes)
    throws ParseException
  {
    pos = next;
    Token token = token(endOfAtom, keepEscapes);
    next = pos;
    peek = next;
    return token;
  }

  /**
   * Peeks at the next token. The token will still be available to be read
   * by <code>next()</code>.
   * Invoking this method multiple times returns successive tokens,
   * until <code>next()</code> is called.
   * @param ParseException if the parse fails
   */
  public Token peek()
    throws ParseException
  {
    pos = peek;
    Token token = token('\0', false);
    peek = pos;
    return token;
  }

  /**
   * Returns the rest of the header.
   */
  public String getRemainder()
  {
    return header.substring(next);
  }

  /*
   * Returns the next token.
   */
  private Token token(char endOfAtom, boolean keepEscapes)
    throws ParseException
  {
    skipWhitespace();
    if (pos >= maxPos)
      {
        return EOF;
      }

    char c = header.charAt(pos);

    // comment
    while (c == '(')
      {
        pos++;
        Token comment = parseComment(keepEscapes);
        if (!skipComments)
          {
            return comment;
          }
        skipWhitespace();
        if (pos >= maxPos)
          {
            return EOF;
          }
        c = header.charAt(pos);
      }

    // quotedstring
    if (c == '"')
      {
        pos++;
        return parseQuotedString(c, keepEscapes);
      }

    // delimiter
    if (c < ' ' || c >= '\177' || delimiters.indexOf(c) >= 0)
      {
        if (endOfAtom != '\0' && c != endOfAtom)
          {
            return parseQuotedString(endOfAtom, keepEscapes);
          }
        pos++;
        char[] chars = new char[] { c };
        return new Token(c, new String(chars));
      }

    // atom
    int start = pos;
    while (pos < maxPos)
      {
        c = header.charAt(pos);
        if (c < ' ' || c >= '\177' || c == '(' || c == ' ' || c == '"' ||
            delimiters.indexOf(c) >= 0)
          {
            if (endOfAtom != '\0' && c != endOfAtom)
              {
                return parseQuotedString(endOfAtom, keepEscapes);
              }
            break;
          }
        pos++;
      }
    return new Token(Token.ATOM, header.substring(start, pos));
  }

  private Token parseComment(boolean keepEscapes)
    throws ParseException
  {
    int start = pos;
    boolean needsNormalization = false;
    int parenCount = 1;
    while (parenCount > 0 && pos < maxPos)
      {
        char c = header.charAt(pos);
        if (c == '\\')
          {
            pos++;
            needsNormalization = true;
          }
        else if (c == '\r')
          {
            needsNormalization = true;
          }
        else if (c == '(')
          {
            parenCount++;
          }
        else if (c == ')')
          {
            parenCount--;
          }
        pos++;
      }
    if (parenCount != 0)
      {
        String m = L10N.getString("err.bad_comment");
        Object[] args = new Object[] { header };
        throw new ParseException(MessageFormat.format(m, args));
      }
    String ret = needsNormalization ?
      normalize(header, start, pos - 1, keepEscapes) :
      header.substring(start, pos - 1);
    return new Token(Token.COMMENT, ret);
  }

  private Token parseQuotedString(char endOfAtom, boolean keepEscapes)
    throws ParseException
  {
    int start = pos;
    boolean needsNormalization = false;
    for (; pos < maxPos; pos++)
      {
        char c = header.charAt(pos);
        if (c == '\\')
          {
            pos++;
            needsNormalization = true;
          }
        else if (c == '\r')
          {
            needsNormalization = true;
          }
        else if (c == endOfAtom)
          {
            String ret = needsNormalization ?
              normalize(header, start, pos, keepEscapes) :
              header.substring(start, pos);
            pos++;
            return new Token(Token.QUOTEDSTRING, ret);
          }
      }
    if (endOfAtom == '"')
      {
        String m = L10N.getString("err.bad_quoted_string");
        Object[] args = new Object[] { header };
        throw new ParseException(MessageFormat.format(m, args));
      }
    String ret = needsNormalization ?
      normalize(header, start, pos, keepEscapes) :
      header.substring(start, pos);
    pos++;
    return new Token(Token.QUOTEDSTRING, ret);
  }

  /*
   * Advance pos over any whitespace delimiters.
   */
  private void skipWhitespace()
  {
    while (pos < maxPos)
      {
        char c = header.charAt(pos);
        if (c != ' ' && c != '\t' && c != '\r' && c != '\n')
          {
            return;
          }
        pos++;
      }
  }

  /*
   * Process out CR and backslash (line continuation) bytes.
   */
  private String normalize(String s, int start, int end, boolean keepEscapes)
  {
    StringBuilder buf = new StringBuilder();
    char last = '\0';
    for (int i = start; i < end; i++)
      {
        char c = s.charAt(i);
        if (c == '\\' && !keepEscapes)
          {
            last = c;
            continue;
          }
        if (c == '\r' && i + 1 < end && s.charAt(i + 1) == '\n')
          {
            // Coalesce CRLF to LF
            c = '\n';
            i++;
          }
        if (c == '\n' && last == '\\')
          {
            // Line continuation
            continue;
          }
        buf.append(c);
        last = c;
      }
    return buf.toString();
  }

}

