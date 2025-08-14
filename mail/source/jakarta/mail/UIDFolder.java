/*
 * UIDFolder.java
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

package jakarta.mail;

/**
 * A folder that supports permanent references to messages in the form of a
 * long integer (i.e. an IMAP folder). These UIDs survive the closure and
 * reopening of the message store and session context.
 * <p>
 * UIDs are assigned to messages in a folder in a strictly ascending
 * fashion; that is, if the message number of message <i>x</i> is greater
 * than the message number of message <i>y</i>, its UID will also be greater
 * than that of <i>y</i>.
 *
 * @see RFC 2060 http://www.ietf.org/rfc/rfc2060.txt
 * @author <a href="mailto:dog@gnu.org">Chris Burdess</a>
 * @version 1.5
 */
public interface UIDFolder
{

  /**
   * A fetch profile item for fetching UIDs.
   */
  static class FetchProfileItem
    extends FetchProfile.Item
  {

    /**
     * Indicates the the UIDs of the messages should be prefetched.
     */
    public static final FetchProfileItem UID = new FetchProfileItem("UID");

    protected FetchProfileItem(String name)
    {
      super(name);
    }

  }

  /**
   * This special value can be used as the end parameter in
   * <code>getMessages(start, end)</code> to denote the last UID
   * in this folder.
   */
  long LASTUID = -1L;

  /**
   * Returns the UIDValidity value associated with this folder.
   * <p>
   * A client should compare this value against a UIDValidity value
   * saved from a previous session to ensure that any cached UIDs are valid.
   */
  long getUIDValidity()
    throws MessagingException;

  /**
   * Returns the message corresponding to the given UID, or
   * <code>null</code> if no such message exists.
   * @param uid the UID of the desired message
   */
  Message getMessageByUID(long uid)
    throws MessagingException;

  /**
   * Returns the messages in the given range.
   * The special value LASTUID can be used as the <code>end</code> parameter
   * to indicate the last available UID.
   * @param start the start UID
   * @param end the end UID
   */
  Message[] getMessagesByUID(long start, long end)
    throws MessagingException;

  /**
   * Returns the messages specified by the given UIDs.
   * If any UID is invalid, <code>null</code> is returned for that entry.
   * <p>
   * The returned array will be of the same size as the specified UIDs.
   * @param uids the UIDs
   */
  Message[] getMessagesByUID(long[] uids)
    throws MessagingException;

  /**
   * Returns the UID for the specified message.
   * @param message a message in this folder
   * @exception NoSuchElementException if the given message is not in this
   * folder
   */
  long getUID(Message message)
    throws MessagingException;

}

