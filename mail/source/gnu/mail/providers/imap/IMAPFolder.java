/*
 * IMAPFolder.java
 * Copyright (C) 2003, 2004, 2013 Chris Burdess <dog@gnu.org>
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

package gnu.mail.providers.imap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jakarta.mail.Address;
import jakarta.mail.FetchProfile;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.FolderClosedException;
import jakarta.mail.FolderNotFoundException;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.MethodNotSupportedException;
import jakarta.mail.Store;
import jakarta.mail.UIDFolder;
import jakarta.mail.event.ConnectionEvent;
import jakarta.mail.event.FolderEvent;
import jakarta.mail.internet.MimeMessage;

import jakarta.mail.search.AddressTerm;
import jakarta.mail.search.AndTerm;
import jakarta.mail.search.BodyTerm;
import jakarta.mail.search.ComparisonTerm;
import jakarta.mail.search.DateTerm;
import jakarta.mail.search.FlagTerm;
import jakarta.mail.search.FromTerm;
import jakarta.mail.search.HeaderTerm;
import jakarta.mail.search.IntegerComparisonTerm;
import jakarta.mail.search.MessageIDTerm;
import jakarta.mail.search.NotTerm;
import jakarta.mail.search.OrTerm;
import jakarta.mail.search.RecipientTerm;
import jakarta.mail.search.SearchTerm;
import jakarta.mail.search.SentDateTerm;
import jakarta.mail.search.SizeTerm;
import jakarta.mail.search.StringTerm;
import jakarta.mail.search.SubjectTerm;

import gnu.inet.imap.IMAPAdapter;
import gnu.inet.imap.IMAPCallback;
import gnu.inet.imap.IMAPConnection;
import gnu.inet.imap.IMAPConstants;
import gnu.inet.imap.FetchDataItem;
import gnu.inet.imap.MessageSet;
import gnu.inet.imap.UIDSet;

/**
 * The folder class implementing the IMAP4rev1 mail protocol.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
public class IMAPFolder
  extends Folder
  implements UIDFolder
{

  static final Map<String,Flags.Flag> STR2FLAG;
  static final Map<Flags.Flag,String> FLAG2STR;
  static
  {
    STR2FLAG = new TreeMap<String,Flags.Flag>();
    STR2FLAG.put(IMAPConstants.FLAG_ANSWERED, Flags.Flag.ANSWERED);
    STR2FLAG.put(IMAPConstants.FLAG_DELETED, Flags.Flag.DELETED);
    STR2FLAG.put(IMAPConstants.FLAG_DRAFT, Flags.Flag.DRAFT);
    STR2FLAG.put(IMAPConstants.FLAG_FLAGGED, Flags.Flag.FLAGGED);
    STR2FLAG.put(IMAPConstants.FLAG_RECENT, Flags.Flag.RECENT);
    STR2FLAG.put(IMAPConstants.FLAG_SEEN, Flags.Flag.SEEN);
    FLAG2STR = new HashMap<Flags.Flag,String>();
    FLAG2STR.put(Flags.Flag.ANSWERED, IMAPConstants.FLAG_ANSWERED);
    FLAG2STR.put(Flags.Flag.DELETED, IMAPConstants.FLAG_DELETED);
    FLAG2STR.put(Flags.Flag.DRAFT, IMAPConstants.FLAG_DRAFT);
    FLAG2STR.put(Flags.Flag.FLAGGED, IMAPConstants.FLAG_FLAGGED);
    FLAG2STR.put(Flags.Flag.RECENT, IMAPConstants.FLAG_RECENT);
    FLAG2STR.put(Flags.Flag.SEEN, IMAPConstants.FLAG_SEEN);
  }

  /**
   * The folder path.
   */
  protected String path;

  /**
   * The type of this folder (HOLDS_MESSAGES or HOLDS_FOLDERS).
   */
  protected int type;

  protected Flags permanentFlags = new Flags();

  protected char delimiter;

  protected int messageCount = -1;

  protected int newMessageCount = -1;

  protected int unreadMessageCount = -1;

  protected long uidvalidity = -1L;

  protected long uidnext = -1L;

  private boolean seenTryCreate;

  private static DateFormat searchdf = new SimpleDateFormat("d-MMM-yyyy");

  // -- IMAPCallback --

  IMAPCallback callback = new IMAPAdapter()
  {

    public void alert(String message)
    {
      ((IMAPStore) store).processAlert(message);
    }

    public void exists(int messages)
    {
      int oldMessageCount = messageCount;
      messageCount = messages;
      if (messageCount > oldMessageCount)
        {
          Message[] m = new Message[messageCount - oldMessageCount];
          for (int i = oldMessageCount; i < messageCount; i++)
            {
              m[i - oldMessageCount] = new IMAPMessage(IMAPFolder.this, i);
            }
          notifyMessageAddedListeners(m);
        }
      else if (messageCount < oldMessageCount)
        {
          Message[] m = new Message[oldMessageCount - messageCount];
          for (int i = messageCount; i < oldMessageCount; i++)
            {
              m[i - messageCount] = new IMAPMessage(IMAPFolder.this, i);
            }
          notifyMessageRemovedListeners(false, m);
        }
    }

    public void recent(int messages)
    {
      newMessageCount = messages;
    }

    public void permanentflags(List<String> flags)
    {
      permanentFlags = new Flags();
      for (String flag: flags)
        {
          Flags.Flag ff = STR2FLAG.get(flag);
          if (ff != null)
            {
              permanentFlags.add(ff);
            }
          else
            {
              permanentFlags.add(flag);
            }
        }
    }

    public void unseen(int messages)
    {
      unreadMessageCount = messages;
    }

    public void uidvalidity(long val)
    {
      uidvalidity = val;
    }

    public void uidnext(long val)
    {
      uidnext = val;
    }

    public void readWrite()
    {
      mode = Folder.READ_WRITE;
    }

    public void readOnly()
    {
      mode = Folder.READ_ONLY;
    }

    public void tryCreate()
    {
      seenTryCreate = true;
    }

  };

  /**
   * Constructor.
   */
  protected IMAPFolder(Store store, String path)
  {
    this(store, path, -1, '\u0000');
  }

  /**
   * Constructor.
   */
  protected IMAPFolder(Store store, String path, char delimiter)
  {
    this(store, path, -1, delimiter);
  }

  /**
   * Constructor.
   */
  protected IMAPFolder(Store store, String path, int type, char delimiter)
  {
    super(store);
    this.path = path;
    this.type = type;
    this.delimiter = delimiter;
  }

  // -- Folder --

  /**
   * Returns the name of this folder.
   */
  public String getName()
  {
    int di = path.lastIndexOf(delimiter);
    return (di == -1) ? path : path.substring(di + 1);
  }

  /**
   * Returns the full path of this folder.
   */
  public String getFullName()
  {
    return path;
  }

  /**
   * Returns the type of this folder.
   * @exception MessagingException if a messaging error occurred
   */
  public int getType()
    throws MessagingException
  {
    if (type == -1 && !exists())
      {
        throw new FolderNotFoundException(this, path);
      }
    return type;
  }

  /**
   * Indicates whether this folder exists.
   * @exception MessagingException if a messaging error occurred
   */
  public boolean exists()
    throws MessagingException
  {
    int lsi = path.lastIndexOf(delimiter);
    String parent = (lsi == -1) ? "" : path.substring(0, lsi);
    String name = (lsi == -1) ? path : path.substring(lsi+1);
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        type = -1;
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void list(List<String> attributes, String d,
                           String mailbox)
          {
            type = 0;
            if (!attributes.contains(IMAPConstants.LIST_NOINFERIORS))
              {
                type |= Folder.HOLDS_FOLDERS;
              }
            if (!attributes.contains(IMAPConstants.LIST_NOSELECT))
              {
                type |= Folder.HOLDS_MESSAGES;
              }
            delimiter = (d == null) ? '\u0000' : d.charAt(0);
            if (!d.equals(mailbox))
              {
                path = mailbox;
              }
          }
        };
        connection.list(parent, name, adapter);
        return type != -1;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Indicates whether this folder contains new messages.
   * @exception MessagingException if a messaging error occurred
   */
  public boolean hasNewMessages()
    throws MessagingException
  {
    return getNewMessageCount() > 0; // TODO
  }

  /**
   * Opens this folder.
   * @exception MessagingException if a messaging error occurred
   */
  public void open(int mode)
    throws MessagingException
  {
    IMAPStore s = (IMAPStore) store;
    IMAPConnection connection = s.connection;
    try
      {
        boolean selected = false;
        switch (mode)
          {
          case Folder.READ_WRITE:
            selected = connection.select(path, callback);
            break;
          default:
            selected = connection.examine(path, callback);
          }
        if (!selected)
          {
            if (type == -1 && !exists())
              {
                throw new FolderNotFoundException(this, path);
              }
            throw new MessagingException();
          }
        s.setSelected(this);
        notifyConnectionListeners(ConnectionEvent.OPENED);
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Create this folder.
   */
  public boolean create(int type)
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        String newPath = path;
        if ((type & HOLDS_MESSAGES) == 0)
          {
            getSeparator();
            if (delimiter == '\u0000') // this folder cannot be created
              {
                throw new FolderNotFoundException(this, newPath);
              }
            newPath = new StringBuffer(newPath)
              .append(delimiter)
              .toString();
          }
        if (connection.create(newPath, callback))
          {
            type = -1;
            notifyFolderListeners(FolderEvent.CREATED);
            return true;
          }
        return false;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Delete this folder.
   */
  public boolean delete(boolean flag)
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        if (connection.delete(path, callback))
          {
            type = -1;
            notifyFolderListeners(FolderEvent.DELETED);
            return true;
          }
        return false;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Rename this folder.
   */
  public boolean renameTo(Folder folder)
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        if (connection.rename(path, folder.getFullName(), callback))
          {
            type = -1;
            notifyFolderRenamedListeners(folder);
            return true;
          }
        return false;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Closes this folder.
   * @param expunge if the folder is to be expunged before it is closed
   * @exception MessagingException if a messaging error occurred
   */
  public void close(boolean expunge)
    throws MessagingException
  {
    if (mode == -1)
      {
        return;
      }
    IMAPStore s = (IMAPStore) store;
    boolean selected = s.isSelected(this);
    mode = -1;
    if (expunge && mode == READ_WRITE)
      {
        if (!selected)
          {
            throw new FolderClosedException(this);
          }
        IMAPConnection connection = s.connection;
        try
          {
            final List<Message> acc = new ArrayList<Message>();
            IMAPAdapter adapter = new IMAPAdapter(callback)
            {
              public void expunge(int message)
              {
                acc.add(new IMAPMessage(IMAPFolder.this, message));
              }
            };
            if (!connection.close(adapter))
              {
                throw new IllegalWriteException();
              }
            if (!acc.isEmpty())
              {
                IMAPMessage[] ret = new IMAPMessage[acc.size()];
                acc.toArray(ret);
                notifyMessageRemovedListeners(false, ret);
              }
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
    if (selected)
      {
        s.setSelected(null);
      }
    notifyConnectionListeners(ConnectionEvent.CLOSED);
  }

  /**
   * Expunges this folder.
   * This deletes all the messages marked as deleted.
   * @exception MessagingException if a messaging error occurred
   */
  public Message[] expunge()
    throws MessagingException
  {
    if (!isOpen())
      {
        throw new FolderClosedException(this);
      }
    if (mode == Folder.READ_ONLY)
      {
        throw new IllegalWriteException();
      }
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final List<Message> acc = new ArrayList<Message>();
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void expunge(int message)
          {
            acc.add(new IMAPMessage(IMAPFolder.this, message));
          }
        };
        connection.expunge(adapter);
        IMAPMessage[] ret = new IMAPMessage[acc.size()];
        acc.toArray(ret);
        notifyMessageRemovedListeners(true, ret);
        return ret;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Indicates whether this folder is open.
   */
  public boolean isOpen()
  {
    return (mode != -1);
  }

  /**
   * Returns the permanent flags for this folder.
   */
  public Flags getPermanentFlags()
  {
    return permanentFlags;
  }

  /**
   * Returns the number of messages in this folder.
   * @exception MessagingException if a messaging error occurred
   */
  public int getMessageCount()
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        messageCount = -1;
        if (mode != -1)
          {
            connection.noop(callback);
          }
        if (messageCount == -1 || mode == -1)
          {
            List<String> items = new ArrayList<String>();
            items.add(IMAPConstants.MESSAGES);
            connection.status(path, items, callback);
          }
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
    return messageCount;
  }

  /**
   * Returns the number of new messages in this folder.
   * @exception MessagingException if a messaging error occurred
   */
  public int getNewMessageCount()
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        newMessageCount = -1;
        if (mode != -1)
          {
            connection.noop(callback);
          }
        if (newMessageCount == -1 || mode == -1)
          {
            List<String> items = new ArrayList<String>();
            items.add(IMAPConstants.RECENT);
            connection.status(path, items, callback);
          }
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
    return newMessageCount;
  }

  /**
   * Returns the specified message number from this folder.
   * The message is only retrieved once from the server.
   * Subsequent getMessage() calls to the same message are cached.
   * Since POP3 does not provide a mechanism for retrieving only part of
   * the message(headers, etc), the entire message is retrieved.
   * @exception MessagingException if a messaging error occurred
   */
  public Message getMessage(int msgnum)
    throws MessagingException
  {
    if (mode == -1)
      {
        throw new FolderClosedException(this);
      }
    return new IMAPMessage(this, msgnum);
  }

  /**
   * Copies the specified messages into another folder.
   * <p>
   * The destination folder does not have to be open.
   * @param msgs the messages
   * @param folder the folder to copy the messages to
   */
  public void copyMessages(Message[] msgs, Folder folder)
    throws MessagingException
  {
    if (mode == -1)
      {
        throw new FolderClosedException(this);
      }
    if (folder.getStore() == store)
      {
        IMAPConnection connection = ((IMAPStore) store).connection;
        try
          {
            MessageSet messages = new MessageSet();
            seenTryCreate = false;
            for (int i = 0; i < msgs.length; i++)
              {
                messages.add(msgs[i].getMessageNumber());
              }
            if (!connection.copy(messages, folder.getFullName(), callback))
              {
                if (seenTryCreate)
                  {
                    throw new FolderNotFoundException(folder);
                  }
                throw new MessagingException();
              }
          }
        catch (IOException e)
          {
            throw new MessagingException(e.getMessage(), e);
          }
      }
    else
      {
        super.copyMessages(msgs, folder);
      }
  }

  /**
   * Appends the specified set of messages to this folder.
   * Only <code>MimeMessage</code>s are accepted.
   */
  public void appendMessages(Message[] messages)
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        List<Message> acc = new ArrayList<Message>(messages.length);
        for (int i = 0; i < messages.length; i++)
          {
            Date date = messages[i].getReceivedDate();
            List<String> flags = flagsToString(messages[i].getFlags());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            messages[i].writeTo(out);
            byte[] content = out.toByteArray();
            out = null;
            if (connection.append(path, flags, date, content, callback))
              {
                acc.add(messages[i]);
              }
          }
        messages = new Message[acc.size()];
        acc.toArray(messages);
        notifyMessageAddedListeners(messages);
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * IMAP fetch routine.
   * This executes the fetch for the specified message numbers
   * and updates the messages according to the message statuses returned.
   */
  public void fetch(Message[] messages, FetchProfile fp)
    throws MessagingException
  {
    if (!isOpen())
      {
        throw new FolderClosedException(this);
      }
    if (messages.length == 0)
      {
        return;
      }
    // decide which commands to send
    String[] headers = fp.getHeaderNames();
    List<String> cmds = new ArrayList<String>();
    if (fp.contains(FetchProfile.Item.CONTENT_INFO))
      {
        cmds.add(IMAPConstants.BODYSTRUCTURE);
      }
    else if (fp.contains(FetchProfile.Item.ENVELOPE))
      {
        cmds.add(IMAPConstants.ENVELOPE);
      }
    else if (headers.length > 0)
      {
        // specified headers only
        StringBuilder hbuf = new StringBuilder("BODY.PEEK[HEADER.FIELDS(");
        for (int i = 0; i < headers.length; i++)
          {
            if (i > 0)
              {
                hbuf.append(' ');
              }
            hbuf.append(headers[i]);
          }
        hbuf.append(')');
        hbuf.append(']');
        cmds.add(hbuf.toString());
    }
    if (fp.contains(FetchProfile.Item.FLAGS))
      {
        cmds.add(IMAPConstants.FLAGS);
      }
    cmds.add(IMAPConstants.INTERNALDATE); // for received date
    // get message numbers
    MessageSet messageSet = new MessageSet();
    final Map<Integer,IMAPMessage> num2msg =
      new HashMap<Integer,IMAPMessage>();
    for (int i = 0; i < messages.length; i++)
      {
        int num = messages[i].getMessageNumber();
        messageSet.add(num);
        num2msg.put(num, (IMAPMessage) messages[i]);
      }
    // execute
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void fetch(int message, List<FetchDataItem> data)
          {
            IMAPMessage msg = num2msg.get(message);
            msg.callback.fetch(message, data);
          }
        };
        connection.fetch(messageSet, cmds, adapter);
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * IMAP search function.
   */
  public Message[] search(SearchTerm term)
    throws MessagingException
  {
    return search(term, null);
  }

  /**
   * IMAP search function.
   */
  public Message[] search(SearchTerm term, Message[] msgs)
    throws MessagingException
  {
    List<String> criteria = new ArrayList<String>();
    if (msgs != null && msgs.length > 0)
      {
        // <message set>
        MessageSet msgset = new MessageSet();
        for (int i = 0; i < msgs.length; i++)
          {
            msgset.add(msgs[i].getMessageNumber());
          }
        criteria.add(msgset.toString());
      }
    boolean isIMAPSearch = addTerm(term, criteria);
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        Message[] messages;
        if (isIMAPSearch && !criteria.isEmpty())
          {
            final List<Integer> acc = new ArrayList<Integer>();
            IMAPAdapter adapter = new IMAPAdapter(callback)
            {
              public void search(List<Integer> results)
              {
                acc.addAll(results);
              }
            };
            connection.search(null, criteria, adapter);
            messages = new Message[acc.size()];
            for (int i = 0; i < messages.length; i++)
              {
                int num = acc.get(i);
                messages[i] = new IMAPMessage(IMAPFolder.this, num);
              }
          }
        else
          {
            messages = (msgs != null) ? msgs : getMessages();
          }
        // Enforce final constraints
        messages = super.search(term, messages);
        return messages;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Possibly recursive search term add function.
   * Note that this is not sufficient to enforce all the constraints imposed
   * by the SearchTerm structures - this is why we finally call
   * <code>super.search()</code> in the search method.
   * @return true if all the terms can be represented in IMAP
   */
  private boolean addTerm(SearchTerm term, List<String> list)
  {
    if (term instanceof AndTerm)
      {
        SearchTerm[] terms = ((AndTerm) term).getTerms();
        for (int i = 0; i < terms.length; i++)
          {
            if (!addTerm(terms[i], list))
              return false;
          }
      }
    else if (term instanceof OrTerm)
      {
        list.add(IMAPConstants.SEARCH_OR);
        SearchTerm[] terms = ((OrTerm) term).getTerms();
        for (int i = 0; i < terms.length; i++)
          {
            if (!addTerm(terms[i], list))
              return false;
          }
      }
    else if (term instanceof NotTerm)
      {
        list.add(IMAPConstants.SEARCH_NOT);
        if (!addTerm(((NotTerm) term).getTerm(), list))
          return false;
      }
    else if (term instanceof FlagTerm)
      {
        FlagTerm ft = (FlagTerm) term;
        Flags f = ft.getFlags();
        boolean set = ft.getTestSet();
        // System flags
        Flags.Flag[] sf = f.getSystemFlags();
        for (int i = 0; i < sf.length; i++)
          {
            Flags.Flag ff = sf[i];
            if (ff == Flags.Flag.ANSWERED)
              {
                list.add(set ? IMAPConstants.SEARCH_ANSWERED :
                          IMAPConstants.SEARCH_UNANSWERED);
              }
            else if (ff == Flags.Flag.DELETED)
              {
                list.add(set ? IMAPConstants.SEARCH_DELETED :
                          IMAPConstants.SEARCH_UNDELETED);
              }
            else if (ff == Flags.Flag.DRAFT)
              {
                list.add(set ? IMAPConstants.SEARCH_DRAFT :
                          IMAPConstants.SEARCH_UNDRAFT);
              }
            else if (ff == Flags.Flag.FLAGGED)
              {
                list.add(set ? IMAPConstants.SEARCH_FLAGGED :
                          IMAPConstants.SEARCH_UNFLAGGED);
              }
            else if (ff == Flags.Flag.RECENT)
              {
                list.add(set ? IMAPConstants.SEARCH_RECENT :
                          IMAPConstants.SEARCH_OLD);
              }
            else if (ff == Flags.Flag.SEEN)
              {
                list.add(set ? IMAPConstants.SEARCH_SEEN :
                          IMAPConstants.SEARCH_UNSEEN);
              }
          }
        // Keywords
        String[] uf = f.getUserFlags();
        for (int i = 0; i < uf.length; i++)
          {
            StringBuffer keyword = new StringBuffer();
            keyword.append(set ? IMAPConstants.SEARCH_KEYWORD :
                            IMAPConstants.SEARCH_UNKEYWORD);
            keyword.append('"');
            keyword.append(uf[i]);
            keyword.append('"');
            list.add(keyword.toString());
          }
      }
    else if (term instanceof AddressTerm)
      {
        Address address = ((AddressTerm) term).getAddress();
        StringBuffer criterion = new StringBuffer();
        if (term instanceof FromTerm)
          criterion.append(IMAPConstants.SEARCH_FROM);
        else if (term instanceof RecipientTerm)
          {
            Message.RecipientType rt =
              ((RecipientTerm) term).getRecipientType();
            if (rt == Message.RecipientType.TO)
              criterion.append(IMAPConstants.SEARCH_TO);
            else if (rt == Message.RecipientType.CC)
              criterion.append(IMAPConstants.SEARCH_CC);
            else if (rt == Message.RecipientType.BCC)
              criterion.append(IMAPConstants.SEARCH_BCC);
            else
              criterion = null;
          }
        else
          criterion = null;
        if (criterion != null)
          {
            criterion.append(' ');
            criterion.append('"');
            criterion.append(address.toString());
            criterion.append('"');
            list.add(criterion.toString());
          }
        else
          return false;
      }
    else if (term instanceof ComparisonTerm)
      {
        if (term instanceof DateTerm)
          {
            DateTerm dt = (DateTerm) term;
            Date date = dt.getDate();
            int comparison = dt.getComparison();
            StringBuffer criterion = new StringBuffer();
            switch (comparison)
              {
              case ComparisonTerm.NE:
              case ComparisonTerm.GE:
              case ComparisonTerm.LE:
                criterion.append(IMAPConstants.SEARCH_NOT);
                criterion.append(' ');
              }
            if (term instanceof SentDateTerm)
              criterion.append("SENT");
            switch (comparison)
              {
              case ComparisonTerm.EQ:
              case ComparisonTerm.NE:
                criterion.append(IMAPConstants.SEARCH_ON);
                break;
              case ComparisonTerm.LT:
              case ComparisonTerm.GE:
                criterion.append(IMAPConstants.SEARCH_BEFORE);
                break;
              case ComparisonTerm.GT:
              case ComparisonTerm.LE:
                criterion.append(IMAPConstants.SEARCH_SINCE);
                break;
              }
            criterion.append(' ');
            criterion.append(searchdf.format(date));
            list.add(criterion.toString());
          }
        else if (term instanceof IntegerComparisonTerm)
          {
            IntegerComparisonTerm it = (IntegerComparisonTerm) term;
            int number = it.getNumber();
            int comparison = it.getComparison();
            if (term instanceof SizeTerm)
              {
                StringBuffer criterion = new StringBuffer();
                switch (comparison)
                  {
                  case ComparisonTerm.EQ:
                  case ComparisonTerm.GE:
                  case ComparisonTerm.LE:
                    criterion.append(IMAPConstants.SEARCH_NOT);
                    criterion.append(' ');
                  }
                switch (comparison)
                  {
                  case ComparisonTerm.EQ:
                  case ComparisonTerm.NE:
                    criterion.append(IMAPConstants.SEARCH_OR);
                    criterion.append(' ');
                    criterion.append(IMAPConstants.SEARCH_SMALLER);
                    criterion.append(' ');
                    criterion.append(number);
                    criterion.append(' ');
                    criterion.append(IMAPConstants.SEARCH_LARGER);
                    criterion.append(' ');
                    criterion.append(number);
                    break;
                  case ComparisonTerm.LT:
                  case ComparisonTerm.GE:
                    criterion.append(IMAPConstants.SEARCH_SMALLER);
                    criterion.append(' ');
                    criterion.append(number);
                    break;
                  case ComparisonTerm.GT:
                  case ComparisonTerm.LE:
                    criterion.append(IMAPConstants.SEARCH_LARGER);
                    criterion.append(' ');
                    criterion.append(number);
                    break;
                  }
                list.add(criterion.toString());
              }
            else
              return false;
          }
      }
    else if (term instanceof StringTerm)
      {
        String pattern = ((StringTerm) term).getPattern();
        StringBuffer criterion = new StringBuffer();
        if (term instanceof BodyTerm)
          {
            criterion.append(IMAPConstants.SEARCH_BODY);
          }
        else if (term instanceof HeaderTerm)
          {
            criterion.append(IMAPConstants.SEARCH_HEADER);
            criterion.append(' ');
            criterion.append(((HeaderTerm) term).getHeaderName());
          }
        else if (term instanceof SubjectTerm)
          {
            criterion.append(IMAPConstants.SEARCH_SUBJECT);
          }
        else if (term instanceof MessageIDTerm)
          {
            criterion.append(IMAPConstants.SEARCH_HEADER);
            criterion.append(' ');
            criterion.append("Message-ID");
          }
        else
          {
            criterion = null; // TODO StringAddressTerms?
          }
        if (criterion != null)
          {
            criterion.append(' ');
            criterion.append('"');
            criterion.append(pattern);
            criterion.append('"');
            list.add(criterion.toString());
          }
        else
          return false;
      }
    else
      return false;
    return true;
  }

  public boolean isSubscribed()
  {
    int lsi = path.lastIndexOf(delimiter);
    String parent = (lsi == -1) ? "" : path.substring(0, lsi);
    String name = (lsi == -1) ? path : path.substring(lsi+1);
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final boolean[] subscribed = new boolean[] { false };
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void list(List<String> attributes, String d,
                           String mailbox)
          {
            subscribed[0] = true;
          }
        };
        connection.lsub(parent, name, adapter);
        return subscribed[0];
      }
    catch (IOException e)
      {
        return false;
      }
  }

  public void setSubscribed(boolean flag)
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        if (flag)
          {
            connection.subscribe(path, callback);
          }
        else
          {
            connection.unsubscribe(path, callback);
          }
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Returns the subfolders for this folder.
   */
  public Folder[] list(String pattern)
    throws MessagingException
  {
    getSeparator();
    String spec = ("".equals(path)) ? "%" :
      new StringBuilder(path).append(delimiter).append(pattern).toString();
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final List<Folder> acc = new ArrayList<Folder>();
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void list(List<String> attributes, String d,
                           String mailbox)
          {
            int type = 0;
            if (!attributes.contains(IMAPConstants.LIST_NOINFERIORS))
              {
                type |= Folder.HOLDS_FOLDERS;
              }
            if (!attributes.contains(IMAPConstants.LIST_NOSELECT))
              {
                type |= Folder.HOLDS_MESSAGES;
              }
            char delimiter = (d == null) ? '\u0000' : d.charAt(0);
            acc.add(new IMAPFolder(store, mailbox, type, delimiter));
          }
        };
        connection.list("", spec, adapter);
        Folder[] ret = new Folder[acc.size()];
        acc.toArray(ret);
        return ret;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Returns the subscribed subfolders for this folder.
   */
  public Folder[] listSubscribed(String pattern)
    throws MessagingException
  {
    getSeparator();
    String spec = ("".equals(path)) ? "%" :
      new StringBuffer(path).append(delimiter).append(pattern).toString();
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final List<Folder> acc = new ArrayList<Folder>();
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void list(List<String> attributes, String d,
                           String mailbox)
          {
            int type = 0;
            if (!attributes.contains(IMAPConstants.LIST_NOINFERIORS))
              {
                type |= Folder.HOLDS_FOLDERS;
              }
            if (!attributes.contains(IMAPConstants.LIST_NOSELECT))
              {
                type |= Folder.HOLDS_MESSAGES;
              }
            char delimiter = (d == null) ? '\u0000' : d.charAt(0);
            acc.add(new IMAPFolder(store, mailbox, type, delimiter));
          }
        };
        connection.list("", spec, adapter);
        Folder[] ret = new Folder[acc.size()];
        acc.toArray(ret);
        return ret;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Returns the parent folder of this folder.
   */
  public Folder getParent()
    throws MessagingException
  {
    IMAPStore s = (IMAPStore) store;
    getSeparator();
    int di = path.lastIndexOf(delimiter);
    if (di == -1)
      {
        return s.getDefaultFolder();
      }
    return new IMAPFolder(store, path.substring(0, di), delimiter);
  }

  /**
   * Returns a subfolder with the specified name.
   */
  public Folder getFolder(String name)
    throws MessagingException
  {
    getSeparator();
    StringBuilder buf = new StringBuilder();
    if (path != null && path.length() > 0)
      {
        buf.append(path);
        buf.append(delimiter);
      }
    buf.append(name);
    return new IMAPFolder(store, buf.toString());
  }

  /**
   * Returns the path separator charcter.
   */
  public char getSeparator()
    throws MessagingException
  {
    if (delimiter == '\u0000')
      {
        if (!exists())
          {
            throw new FolderNotFoundException(this);
          }
      }
    return delimiter;
  }

  public boolean equals(Object other)
  {
    if (other instanceof IMAPFolder)
      {
        return ((IMAPFolder) other).path.equals(path);
      }
    return super.equals(other);
  }

  // -- UIDFolder --

  public long getUIDValidity()
    throws MessagingException
  {
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        uidvalidity = -1L;
        if (mode != -1)
          {
            connection.noop(callback);
          }
        if (uidvalidity == -1L || mode == -1)
          {
            List<String> items = new ArrayList<String>();
            items.add(IMAPConstants.UIDVALIDITY);
            connection.status(path, items, callback);
          }
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
    return uidvalidity;
  }

  public Message getMessageByUID(long uid)
    throws MessagingException
  {
    if (mode == -1)
      {
        throw new FolderClosedException(this);
      }
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final List<Message> acc = new ArrayList<Message>();
        List<String> cmds = new ArrayList<String>();
        cmds.add(IMAPConstants.FLAGS);
        cmds.add(IMAPConstants.UID);
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void fetch(int message, List<FetchDataItem> data)
          {
            IMAPMessage msg = new IMAPMessage(IMAPFolder.this, message);
            msg.callback.fetch(message, data);
            acc.add(msg);
          }
        };
        UIDSet uids = new UIDSet();
        uids.add(uid);
        if (connection.uidFetch(uids, cmds, adapter) && !acc.isEmpty())
          {
            return acc.get(0);
          }
        return null;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  public Message[] getMessagesByUID(long start, long end)
    throws MessagingException
  {
    if (mode == -1)
      {
        throw new FolderClosedException(this);
      }
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final List<Message> acc = new ArrayList<Message>();
        List<String> cmds = new ArrayList<String>();
        cmds.add(IMAPConstants.FLAGS);
        cmds.add(IMAPConstants.UID);
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void fetch(int message, List<FetchDataItem> data)
          {
            IMAPMessage msg = new IMAPMessage(IMAPFolder.this, message);
            msg.callback.fetch(message, data);
            acc.add(msg);
          }
        };
        UIDSet uids = new UIDSet();
        for (long i = start; i <= end; i++)
          {
            uids.add(i);
          }
        connection.uidFetch(uids, cmds, adapter);
        Message[] ret = new Message[acc.size()];
        acc.toArray(ret);
        return ret;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  public Message[] getMessagesByUID(long[] uids)
    throws MessagingException
  {
    if (mode == -1)
      {
        throw new FolderClosedException(this);
      }
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        final List<Message> acc = new ArrayList<Message>();
        List<String> cmds = new ArrayList<String>();
        cmds.add(IMAPConstants.FLAGS);
        cmds.add(IMAPConstants.UID);
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void fetch(int message, List<FetchDataItem> data)
          {
            IMAPMessage msg = new IMAPMessage(IMAPFolder.this, message);
            msg.callback.fetch(message, data);
            acc.add(msg);
          }
        };
        UIDSet uidset = new UIDSet();
        for (int i = 0; i < uids.length; i++)
          {
            uidset.add(uids[i]);
          }
        connection.uidFetch(uidset, cmds, adapter);
        Message[] ret = new Message[acc.size()];
        acc.toArray(ret);
        return ret;
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  public long getUID(Message message)
    throws MessagingException
  {
    if (mode == -1)
      {
        throw new FolderClosedException(this);
      }
    if (!(message instanceof IMAPMessage))
      {
        throw new MethodNotSupportedException("not an IMAPMessage");
      }
    IMAPMessage m = (IMAPMessage) message;
    if (m.uid == -1L)
      {
        m.fetchUID();
      }
    return m.uid;
  }

  /**
   * Returns the number of unread messages in this folder.
   * @see jakarta.mail.Folder#getUnreadMessageCount()
   */
  public synchronized int getUnreadMessageCount()
    throws MessagingException
  {
    return getMessageCountByCriteria("NOT SEEN");
  }

  /**
   * Returns the number of deleted messages in this folder.
   * @see jakarta.mail.Folder#getDeletedMessageCount()
   */
  public synchronized int getDeletedMessageCount()
    throws MessagingException
      {
    return getMessageCountByCriteria("DELETED");
  }

  /**
   * Convenience method for returning the number of messages in the
   * current folder that match the single criteria.
   */
  public int getMessageCountByCriteria(String criteria)
    throws MessagingException
  {
    if (!isOpen())
      {
        return -1;
      }
    IMAPConnection connection = ((IMAPStore) store).connection;
    try
      {
        List<String> c = new ArrayList<String>();
        c.add(criteria);
        final List<Integer> acc = new ArrayList<Integer>();
        IMAPAdapter adapter = new IMAPAdapter(callback)
        {
          public void search(List<Integer> results)
          {
            acc.addAll(results);
          }
        };
        connection.search(null, c, adapter);
        return acc.size();
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
  }

  static final List<String> flagsToString(Flags flags)
  {
    List<String> ret = new ArrayList<String>();
    Flags.Flag[] sf = flags.getSystemFlags();
    String[] uf = flags.getUserFlags();
    for (int i = 0; i < sf.length; i++)
      {
        ret.add(FLAG2STR.get(sf[i]));
      }
    for (int i = 0; i < uf.length; i++)
      {
        ret.add(uf[i]);
      }
    return ret;
  }

}
