/*
 * NewsAddress.java
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

package jakarta.mail.internet;

import java.util.ArrayList;
import java.util.StringTokenizer;
import jakarta.mail.Address;

/**
 * An RFC 1036 newsgroup address.
 *
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public class NewsAddress
  extends Address
{

  /*
   * The address type.
   */
  private static final String TYPE = "news";

  /**
   * The name of the newsgroup.
   */
  protected String newsgroup;

  /**
   * The hostname of the news server.
   */
  protected String host;

  /**
   * Constructor for an empty news address.
   */
  public NewsAddress()
  {
  }

  /**
   * Constructor with the given newsgroup.
   * @param newsgroup the newsgroup
   */
  public NewsAddress(String newsgroup)
  {
    this(newsgroup, null);
  }

  /**
   * Constructor with the given newsgroup and host.
   * @param newsgroup the newsgroup
   * @param host the host
   */
  public NewsAddress(String newsgroup, String host)
  {
    this.newsgroup = newsgroup;
    this.host = host;
  }

  /**
   * Returns the type of this address.
   * The type of a <code>NewsAddress</code> is "news".
   */
  public String getType()
  {
    return TYPE;
  }

  /**
   * Sets the newsgroup.
   * @param newsgroup the newsgroup
   */
  public void setNewsgroup(String newsgroup)
  {
    this.newsgroup = newsgroup;
  }

  /**
   * Returns the newsgroup.
   */
  public String getNewsgroup()
  {
    return newsgroup;
  }

  /**
   * Sets the hostname of the news server.
   * @param host the host name
   */
  public void setHost(String host)
  {
    this.host = host;
  }

  /**
   * Returns the hostname of the news server.
   */
  public String getHost()
  {
    return host;
  }

  /**
   * Returns an RFC 1036 string representation of this address.
   */
  public String toString()
  {
    return newsgroup;
  }

  public boolean equals(Object a)
  {
    if (a instanceof NewsAddress)
      {
        NewsAddress na = (NewsAddress) a;
        return (newsgroup.equals(na.newsgroup) &&
               ((host == null && na.host == null) ||
                (host != null && na.host != null &&
                  host.equalsIgnoreCase(na.host))));
      }
    return false;
  }

  public int hashCode()
  {
    int hashCode = 0;
    if (newsgroup != null)
      {
        hashCode += newsgroup.hashCode();
      }
    if (host != null)
      {
        hashCode += host.hashCode();
      }
    return hashCode;
  }

  /**
   * Converts the given array of NewsAddresses into a comma-separated
   * sequence of address strings.
   * The resulting string contains only US-ASCII characters, and is
   * therefore mail-safe.
   * @param addresses the NewsAddress objects
   * @exception ClassCastException if any of the specified addresses
   * is not a NewsAddress
   */
  public static String toString(Address[] addresses)
  {
    if (addresses == null || addresses.length == 0)
      {
        return null;
      }
    StringBuffer buffer = new StringBuffer();
    for (int i = 0; i < addresses.length; i++)
      {
        if (i > 0)
          {
            buffer.append(',');
          }
        // possible ClassCastException...
        NewsAddress na = (NewsAddress) addresses[i];
        buffer.append(na.toString());
      }
    return buffer.toString();
  }

  /**
   * Parses the given comma-separated sequence of newsgroups into
   * NewsAddresses.
   * @param newsgroups a comma-separated newsgroup string
   * @exception AddressException if the parse failed
   */
  public static NewsAddress[] parse(String newsgroups)
    throws AddressException
  {
    StringTokenizer st = new StringTokenizer(newsgroups, ",");
    ArrayList acc = new ArrayList();
    while (st.hasMoreTokens())
      {
        acc.add(new NewsAddress(st.nextToken()));
      }
    NewsAddress[] addresses = new NewsAddress[acc.size()];
    acc.toArray(addresses);
    return addresses;
  }

}

