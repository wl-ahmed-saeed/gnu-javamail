/*
 * IMAPMessage.java
 * Copyright (C) 2003, 2013 Chris Burdess <dog@gnu.org>
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import jakarta.activation.DataHandler;
import jakarta.mail.Flags;
import jakarta.mail.Folder;
import jakarta.mail.IllegalWriteException;
import jakarta.mail.MessagingException;
import jakarta.mail.Part;
import jakarta.mail.internet.ContentDisposition;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.InternetHeaders;
import jakarta.mail.internet.MailDateFormat;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.internet.ParameterList;

import gnu.inet.imap.BODY;
import gnu.inet.imap.BODYSTRUCTURE;
import gnu.inet.imap.IMAPAdapter;
import gnu.inet.imap.IMAPCallback;
import gnu.inet.imap.IMAPConnection;
import gnu.inet.imap.IMAPConstants;
import gnu.inet.imap.ENVELOPE;
import gnu.inet.imap.FetchDataItem;
import gnu.inet.imap.FLAGS;
import gnu.inet.imap.INTERNALDATE;
import gnu.inet.imap.Literal;
import gnu.inet.imap.MessageSet;
import gnu.inet.imap.RFC822_SIZE;
import gnu.inet.imap.UID;
import gnu.inet.imap.UIDSet;
import gnu.inet.util.GetSystemPropertyAction;
import gnu.mail.providers.ReadOnlyMessage;

/**
 * The message class implementing the IMAP4 mail protocol.
 *
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 * @version 1.2
 */
public final class IMAPMessage
  extends ReadOnlyMessage
{

  private static MailDateFormat dateFormat = new MailDateFormat();

  // NB in alphabetical order to use Arrays.binarySearch
  private static final String[] ENVELOPE_HEADERS = new String[]
    { "bcc", "cc", "date", "from", "in-reply-to", "message-id",
      "reply-to", "sender", "subject", "to" };

  static final String PLUS_FLAGS = "+FLAGS";
  static final String MINUS_FLAGS = "-FLAGS";

  /**
   * If set, this contains the received date.
   */
  Date internalDate = null;

  /**
   * The UID associated with this message.
   */
  long uid = -1L;

  /**
   * If set, the current set of headers is complete.
   * If false, and a header is requested but returns null, all headers will
   * be requested from the server.
   */
  boolean headersComplete = false;

  BODYSTRUCTURE.Part part;
  ContentType type;
  ContentDisposition disposition;
  String section;
  int size = -1;
  ENVELOPE envelope;
  Literal literal;

  IMAPCallback callback = new IMAPAdapter()
  {

    public void fetch(int message, List<FetchDataItem> data)
    {
      for (FetchDataItem item : data)
        {
          update(item);
        }
    }

  };

  /**
   * Constructor for a top-level message.
   */
  IMAPMessage(IMAPFolder folder, int msgnum)
  {
    super(folder, msgnum);
    flags = null;
  }

  /**
   * Constructor for a message/rfc866 body part.
   */
  IMAPMessage(IMAPMessage parent, BODYSTRUCTURE.MessagePart part,
              ContentType type, ContentDisposition disposition,
              String section)
  {
    super(parent.getFolder(), parent.msgnum);
    this.part = part;
    this.type = type;
    this.disposition = disposition;
    this.section = section;
    if (part instanceof BODYSTRUCTURE.MessagePart)
      {
        envelope = ((BODYSTRUCTURE.MessagePart) part).getEnvelope();
      }
    flags = null;
  }

  public Date getReceivedDate()
    throws MessagingException
  {
    if (section == null && internalDate == null)
      {
        fetchHeaders();
      }
    return internalDate;
  }

  public int getSize()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getSize();
      }
    if (section == null && size == -1)
      {
        fetchSize();
      }
    return super.getSize();
  }

  public int getLineCount()
    throws MessagingException
  {
    if (part instanceof BODYSTRUCTURE.TextPart)
      {
        return ((BODYSTRUCTURE.TextPart) part).getLines();
      }
    return super.getLineCount();
  }

  public String getContentType()
    throws MessagingException
  {
    if (type == null)
      {
        fetchBodyStructure();
      }
    return type.toString();
  }

  public String getDisposition()
    throws MessagingException
  {
    if (disposition == null && section == null)
      {
        fetchBodyStructure();
      }
    return (disposition == null) ?
      super.getDisposition() :
      disposition.toString();
  }

  public String getEncoding()
    throws MessagingException
  {
    if (part == null)
      {
        fetchBodyStructure();
      }
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getEncoding();
      }
    return super.getEncoding();
  }

  public String getContentID()
    throws MessagingException
  {
    if (part == null)
      {
        fetchBodyStructure();
      }
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        return ((BODYSTRUCTURE.BodyPart) part).getId();
      }
    return super.getContentID();
  }

  public String[] getContentLanguage()
    throws MessagingException
  {
    if (part == null)
      {
        fetchBodyStructure();
      }
    if (part instanceof BODYSTRUCTURE.Multipart)
      {
        List<String> l = ((BODYSTRUCTURE.Multipart) part).getLanguage();
        String[] ret = new String[l.size()];
        l.toArray(ret);
        return ret;
      }
    return super.getContentLanguage();
  }

  public String getDescription()
    throws MessagingException
  {
    if (part == null)
      {
        fetchBodyStructure();
      }
    if (part instanceof BODYSTRUCTURE.BodyPart)
      {
        String description = ((BODYSTRUCTURE.BodyPart) part).getDescription();
        if (description != null)
          {
            try
              {
                description = MimeUtility.decodeText(description);
              }
            catch (UnsupportedEncodingException e)
              {
              }
            return description;
          }
      }
    return super.getDescription();
  }

  public String getMessageID()
    throws MessagingException
  {
    if (section == null && envelope == null)
      {
        fetchEnvelope();
      }
    if (envelope != null)
      {
        String messageId = envelope.getMessageId();
        if (messageId != null)
          {
            return messageId;
          }
      }
    return super.getMessageID();
  }

  public String getFileName()
    throws MessagingException
  {
    String filename = null;
    if (disposition != null)
      {
        filename = disposition.getParameter("filename");
      }
    if (filename == null)
      {
        filename = type.getParameter("name");
      }
    if (filename != null)
      {
        PrivilegedAction a =
          new GetSystemPropertyAction("mail.mime.decodefilename");
        if ("true".equals(AccessController.doPrivileged(a)))
          {
            try
              {
                filename = MimeUtility.decodeText(filename);
              }
            catch (UnsupportedEncodingException e)
              {
                throw new MessagingException(null, e);
              }
          }
      }
    return filename;
  }

  protected InputStream getContentStream()
    throws MessagingException
  {
    if (literal == null)
      {
        fetchContent();
      }
    return literal.getInputStream();
  }

  public DataHandler getDataHandler()
    throws MessagingException
  {
    if (dh == null)
      {
        fetchBodyStructure();
        if (part instanceof BODYSTRUCTURE.Multipart)
          {
            BODYSTRUCTURE.Multipart mp = (BODYSTRUCTURE.Multipart) part;
            dh = new DataHandler(new IMAPMultipartDataSource(this, this,
                                                             mp, section));
          }
        else if (part instanceof BODYSTRUCTURE.MessagePart)
          {
            BODYSTRUCTURE.MessagePart m = (BODYSTRUCTURE.MessagePart) part;
            String ms = (section == null) ? "1" : section + ".1";
            String ct = getContentType();
            dh = new DataHandler(new IMAPMessage(this, m, type, disposition,
                                                 ms), ct);
          }
        // otherwise it will be a MimePartDataHandler
      }
    return super.getDataHandler();
  }

  protected void updateHeaders()
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    super.updateHeaders();
  }

  /**
   * Returns the specified header field.
   */
  public String[] getHeader(String name)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    String[] header = super.getHeader(name);
    if (header == null && !headersComplete)
      {
        if (envelope != null &&
            Arrays.binarySearch(ENVELOPE_HEADERS, name.toLowerCase()) != -1)
          {
            return null;
          }
        fetchHeaders();
        header = super.getHeader(name);
      }
    return header;
  }

  /**
   * Returns the specified header field.
   */
  public String getHeader(String name, String delimiter)
    throws MessagingException
  {
    if (headers == null)
      {
        fetchHeaders();
      }
    String header = super.getHeader(name, delimiter);
    if (header == null && !headersComplete)
      {
        if (envelope != null &&
            Arrays.binarySearch(ENVELOPE_HEADERS, name.toLowerCase()) != -1)
          {
            return null;
          }
        fetchHeaders();
        header = super.getHeader(name, delimiter);
      }
    return header;
  }

  public Enumeration getAllHeaders()
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    return super.getAllHeaders();
  }

  public Enumeration getAllHeaderLines()
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    return super.getAllHeaderLines();
  }

  public Enumeration getMatchingHeaders(String[] names)
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    return super.getMatchingHeaders(names);
  }

  public Enumeration getMatchingHeaderLines(String[] names)
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    return super.getMatchingHeaderLines(names);
  }

  public Enumeration getNonMatchingHeaders(String[] names)
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    return super.getNonMatchingHeaders(names);
  }

  public Enumeration getNonMatchingHeaderLines(String[] names)
    throws MessagingException
  {
    if (!headersComplete)
      {
        fetchHeaders();
      }
    return super.getNonMatchingHeaderLines(names);
  }

  public String getSubject()
    throws MessagingException
  {
    if (envelope == null && section == null && !headersComplete)
      {
        fetchEnvelope();
      }
    if (envelope != null)
      {
        String subject = envelope.getSubject();
        if (subject != null)
          {
            try
              {
                subject = MimeUtility.decodeText(subject);
              }
            catch (UnsupportedEncodingException e)
              {
              }
            return subject;
          }
      }
    return super.getSubject();
  }

  public Date getSentDate()
    throws MessagingException
  {
    if (envelope == null && section == null && !headersComplete)
      {
        fetchEnvelope();
      }
    if (envelope != null)
      {
        String date = envelope.getDate();
        if (date != null)
          {
            return dateFormat.parse(date, new ParsePosition(0));
          }
      }
    return super.getSentDate();
  }

  // -- Flags access --

  public Flags getFlags()
    throws MessagingException
  {
    if (flags == null)
      {
        fetchFlags();
      }
    return super.getFlags();
  }

  public boolean isSet(Flags.Flag flag)
    throws MessagingException
  {
    if (flags == null)
      {
        fetchFlags();
      }
    return super.isSet(flag);
  }

  /**
   * Set the specified flags.
   */
  public void setFlags(Flags flag, boolean set)
    throws MessagingException
  {
    if (section != null)
      {
        throw new IllegalWriteException();
      }
    if (flags == null)
      {
        fetchFlags();
      }
    IMAPConnection connection =
      ((IMAPStore) folder.getStore()).connection;
    try
      {
        if (set)
          {
            flags.add(flag);
          }
        else
          {
            flags.remove(flag);
          }
        List<String> f = IMAPFolder.flagsToString(flags);
        if (uid == -1L)
          {
            MessageSet messages = new MessageSet();
            messages.add(msgnum);
            if (!connection.store(messages, "FLAGS", f, callback))
              {
                flags = null; // re-read
                throw new MessagingException("Could not store flags");
              }
          }
        else
          {
            UIDSet uids = new UIDSet();
            uids.add(uid);
            if (!connection.uidStore(uids, "FLAGS", f, callback))
              {
                flags = null; // re-read
                throw new MessagingException("Could not store flags");
              }
          }
      }
    catch (IOException e)
      {
        flags = null; // will be re-read next time
        throw new MessagingException(e.getMessage(), e);
      }
  }

  /**
   * Fetches the flags fo this message.
   */
  void fetchFlags()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(IMAPConstants.FLAGS);
    fetch(commands);
  }

  /**
   * Fetches the message headers.
   */
  void fetchHeaders()
    throws MessagingException
  {
    new Throwable().printStackTrace();
    List<String> commands = new ArrayList<String>();
    if (section == null)
      {
        commands.add("BODY.PEEK[HEADER]");
        commands.add(IMAPConstants.INTERNALDATE);
      }
    else
      {
        commands.add("BODY.PEEK[" + section + ".HEADER]");
      }
    fetch(commands);
  }

  /**
   * Fetches the message body.
   */
  void fetchContent()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    if (section == null)
      {
        commands.add("BODY.PEEK[]");
        commands.add(IMAPConstants.INTERNALDATE);
      }
    else
      {
        commands.add("BODY.PEEK[" + section + "]");
      }
    fetch(commands);
  }

  /**
   * Fetches the MIME structure of this message.
   */
  void fetchBodyStructure()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(IMAPConstants.BODYSTRUCTURE);
    fetch(commands);
  }

  /**
   * Fetches the envelope of this message.
   */
  void fetchEnvelope()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(IMAPConstants.ENVELOPE);
    fetch(commands);
  }

  /**
   * Fetches the UID.
   */
  void fetchUID()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(IMAPConstants.UID);
    fetch(commands);
  }

  /**
   * Fetches the size.
   */
  void fetchSize()
    throws MessagingException
  {
    List<String> commands = new ArrayList<String>();
    commands.add(IMAPConstants.RFC822_SIZE);
    fetch(commands);
  }

  /**
   * Generic fetch routine.
   */
  private void fetch(List<String> commands)
    throws MessagingException
  {
    IMAPConnection connection =
      ((IMAPStore) folder.getStore()).connection;
    try
      {
        MessageSet msgs = new MessageSet();
        msgs.add(msgnum);
        connection.fetch(msgs, commands, callback);
      }
    catch (IOException e)
      {
        throw new MessagingException(e.getMessage(), e);
      }
    catch (RuntimeException e)
      {
        Throwable cause = e.getCause();
        if (cause instanceof MessagingException)
          {
            throw (MessagingException) cause;
          }
        throw e;
      }
  }

  /**
   * Main fetch data item dispatch.
   */
  void update(FetchDataItem item)
  {
    if (item instanceof FLAGS)
      {
        updateFLAGS((FLAGS) item);
      }
    else if (item instanceof UID)
      {
        updateUID((UID) item);
      }
    else if (item instanceof INTERNALDATE)
      {
        updateINTERNALDATE((INTERNALDATE) item);
      }
    else if (item instanceof RFC822_SIZE)
      {
        updateRFC822_SIZE((RFC822_SIZE) item);
      }
    else if (item instanceof BODYSTRUCTURE)
      {
        updateBODYSTRUCTURE((BODYSTRUCTURE) item);
      }
    else if (item instanceof BODY)
      {
        updateBODY((BODY) item);
      }
    else if (item instanceof ENVELOPE)
      {
        updateENVELOPE((ENVELOPE) item);
      }
  }

  private void updateRFC822_SIZE(RFC822_SIZE size)
  {
    this.size = size.getSize();
  }

  private void updateBODY(BODY body)
  {
    String section = body.getSection();
    if (section != null &&
        (section.equals("HEADER") || section.endsWith(".HEADER")))
      {
        Literal lh = body.getContents();
        InputStream in = lh.getInputStream();
        try
          {
            headers = new InternetHeaders(in);
            headersComplete = true;
          }
        catch (MessagingException e)
          {
            throw (RuntimeException) new RuntimeException().initCause(e);
          }
        finally
          {
            try
              {
                in.close();
              }
            catch (IOException e)
              {
              }
          }
      }
    else
      {
        literal = body.getContents();
      }
  }

  private void updateBODYSTRUCTURE(BODYSTRUCTURE bodystructure)
  {
    part = bodystructure.getPart();
    ParameterList pl = new ParameterList();
    Map<String,String> params = part.getParameters();
    if (params != null)
      {
        for (String key : params.keySet())
          {
            pl.set(key, params.get(key));
          }
      }
    type = new ContentType(part.getPrimaryType(), part.getSubtype(), pl);
    if (part instanceof BODYSTRUCTURE.Multipart)
      {
        BODYSTRUCTURE.Disposition d =
          ((BODYSTRUCTURE.Multipart) part).getDisposition();
        if (d != null)
          {
            pl = new ParameterList();
            params = d.getParameters();
            if (params != null)
              {
                for (String key : params.keySet())
                  {
                    pl.set(key, params.get(key));
                  }
              }
            disposition = new ContentDisposition(d.getType(), pl);
          }
      }
  }

  private void updateENVELOPE(ENVELOPE envelope)
  {
    this.envelope = envelope;
    String date = envelope.getDate();
    String subject = envelope.getSubject();
    InternetAddress[] from = getAddressList(envelope.getFrom());
    InternetAddress[] sender = getAddressList(envelope.getSender());
    InternetAddress[] replyTo = getAddressList(envelope.getReplyTo());
    InternetAddress[] to = getAddressList(envelope.getTo());
    InternetAddress[] cc = getAddressList(envelope.getCc());
    InternetAddress[] bcc = getAddressList(envelope.getBcc());
    String inReplyTo = envelope.getInReplyTo();
    String messageId = envelope.getMessageId();
    if (headers == null)
      {
        headers = new InternetHeaders();
      }
    if (date != null)
      {
        headers.setHeader("Date", date);
      }
    if (subject != null)
      {
        headers.setHeader("Subject", subject);
      }
    if (inReplyTo != null)
      {
        headers.setHeader("In-Reply-To", inReplyTo);
      }
    if (messageId != null)
      {
        headers.setHeader("Message-ID", messageId);
      }
    if (from != null)
      {
        headers.setHeader("From", InternetAddress.toString(from));
      }
    if (sender != null)
      {
        headers.setHeader("Sender",
                          InternetAddress.toString(sender));
      }
    if (replyTo != null)
      {
        headers.setHeader("Reply-To",
                          InternetAddress.toString(replyTo));
      }
    if (to != null)
      {
        headers.setHeader("To", InternetAddress.toString(to));
      }
    if (cc != null)
      {
        headers.setHeader("Cc", InternetAddress.toString(cc));
      }
    if (bcc != null)
      {
        headers.setHeader("Bcc", InternetAddress.toString(bcc));
      }
    // NB headers not complete
  }

  private void updateFLAGS(FLAGS flags)
  {
    this.flags = new Flags();
    for (String flag : flags.getFlags())
      {
        Flags.Flag f = IMAPFolder.STR2FLAG.get(flag);
        if (f != null)
          {
            this.flags.add(f);
          }
        else
          {
            this.flags.add(flag);
          }
      }
  }

  private void updateINTERNALDATE(INTERNALDATE internaldate)
  {
    this.internalDate = internaldate.getInternalDate();
  }

  private void updateUID(UID uid)
  {
    this.uid = uid.getUid();
  }

  private static InternetAddress[] getAddressList(List<ENVELOPE.Address> l)
  {
    if (l == null)
      {
        return null;
      }
    InternetAddress[] ret = new InternetAddress[l.size()];
    for (int i = 0; i < ret.length; i++)
      {
        ENVELOPE.Address a = l.get(i);
        try
          {
            ret[i] = new InternetAddress(a.getAddress(), a.getPersonal());
          }
        catch (UnsupportedEncodingException e)
          {
            throw (RuntimeException) new RuntimeException().initCause(e);
          }
      }
    return ret;
  }

}
