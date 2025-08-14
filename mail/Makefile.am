## Process this file with automake to produce Makefile.in.

# Makefile.am for GNU Classpathx JavaMail.

JAVAC = @JAVAC@
JAVACFLAGS = @JAVACFLAGS@
JAVADOC = @JAVADOC@
JAVADOCFLAGS = @JAVADOCFLAGS@

src = @srcdir@/source
dst = classes
doc = @srcdir@/docs

gnumail_jar = gnumail.jar

sources = \
$(src)/gnu/mail/handler/Application.java \
$(src)/gnu/mail/handler/ApplicationOctetStream.java \
$(src)/gnu/mail/handler/Message.java \
$(src)/gnu/mail/handler/MessageRFC822.java \
$(src)/gnu/mail/handler/Multipart.java \
$(src)/gnu/mail/handler/MultipartAlternative.java \
$(src)/gnu/mail/handler/MultipartMixed.java \
$(src)/gnu/mail/handler/MultipartRelated.java \
$(src)/gnu/mail/handler/MultipartSigned.java \
$(src)/gnu/mail/handler/Text.java \
$(src)/gnu/mail/handler/TextHtml.java \
$(src)/gnu/mail/handler/TextPlain.java \
$(src)/gnu/mail/handler/TextXml.java \
$(src)/gnu/mail/util/BOutputStream.java \
$(src)/gnu/mail/util/Base64InputStream.java \
$(src)/gnu/mail/util/Base64OutputStream.java \
$(src)/gnu/mail/util/MailboxURLConnection.java \
$(src)/gnu/mail/util/QInputStream.java \
$(src)/gnu/mail/util/QOutputStream.java \
$(src)/gnu/mail/util/QPInputStream.java \
$(src)/gnu/mail/util/QPOutputStream.java \
$(src)/gnu/mail/util/RFC2822OutputStream.java \
$(src)/gnu/mail/util/UUInputStream.java \
$(src)/gnu/mail/util/UUOutputStream.java \
$(src)/gnu/mail/treeutil/StatusEvent.java \
$(src)/gnu/mail/treeutil/StatusListener.java \
$(src)/gnu/mail/treeutil/StatusSource.java \
$(src)/gnu/mail/providers/ReadOnlyBodyPart.java \
$(src)/gnu/mail/providers/ReadOnlyMessage.java \
$(src)/gnu/mail/providers/smtp/SMTPTransport.java\
$(src)/gnu/mail/providers/imap/IMAPBodyPart.java \
$(src)/gnu/mail/providers/imap/IMAPFolder.java \
$(src)/gnu/mail/providers/imap/IMAPMessage.java \
$(src)/gnu/mail/providers/imap/IMAPMultipartDataSource.java \
$(src)/gnu/mail/providers/imap/IMAPStore.java \
$(src)/gnu/mail/providers/pop3/POP3Folder.java \
$(src)/gnu/mail/providers/pop3/POP3Message.java \
$(src)/gnu/mail/providers/pop3/POP3Store.java \
$(src)/gnu/mail/providers/nntp/ListFolderListener.java \
$(src)/gnu/mail/providers/nntp/NNTPFolder.java \
$(src)/gnu/mail/providers/nntp/NNTPMessage.java \
$(src)/gnu/mail/providers/nntp/NNTPRootFolder.java \
$(src)/gnu/mail/providers/nntp/NNTPStore.java \
$(src)/gnu/mail/providers/nntp/NNTPTransport.java \
$(src)/gnu/mail/providers/mbox/MboxFolder.java \
$(src)/gnu/mail/providers/mbox/MboxMessage.java \
$(src)/gnu/mail/providers/mbox/MboxOutputStream.java \
$(src)/gnu/mail/providers/mbox/MboxStore.java \
$(src)/gnu/mail/providers/maildir/MaildirFolder.java \
$(src)/gnu/mail/providers/maildir/MaildirMessage.java \
$(src)/gnu/mail/providers/maildir/MaildirStore.java

# Package names for Javadoc

package_names = \
gnu.mail.util \
gnu.mail.providers.smtp \
gnu.mail.providers.imap \
gnu.mail.providers.pop3 \
gnu.mail.providers.nntp \
gnu.mail.providers.mbox \
gnu.mail.providers.maildir

# The GNU JAF library.
activation_jar = @ACTIVATION_JAR@

# The GNU classpath inetlib library.
inetlib_jar = @INETLIB_JAR@

# The JSSE library.
#jsse_jar = @JSSE_JAR@

# Resources
src_l10n = \
$(src)/gnu/mail/providers/L10N.properties \
$(src)/gnu/mail/util/L10N.properties

# The META stuff
gnumail_meta = \
META-INF/COPYING \
META-INF/mime.types \
META-INF/mailcap \
META-INF/javamail.charset.map \
META-INF/javamail.default.providers \
META-INF/javamail.default.address.map

manifest_mf = @srcdir@/MANIFEST.MF

# Classpaths
classpath = $(dst):$(inetlib_jar):$(activation_jar)

# Targets
BUILT_SOURCES = $(gnumail_jar)

classes = $(patsubst $(src)/%.java, $(dst)/%.class, $(sources))
dst_l10n = $(patsubst $(src)/%, $(dst)/%, $(src_l10n))

META-INF:
	mkdir -p $@

META-INF/COPYING: META-INF @srcdir@/COPYING
	cp @srcdir@/COPYING $@

META-INF/mime.types: META-INF @srcdir@/mime.types
	cp @srcdir@/mime.types $@

META-INF/mailcap: META-INF @srcdir@/mailcap
	cp @srcdir@/mailcap $@

META-INF/javamail.charset.map: META-INF @srcdir@/javamail.charset.map
	cp @srcdir@/javamail.charset.map $@

META-INF/javamail.default.providers: META-INF @srcdir@/javamail.providers
	cp @srcdir@/javamail.providers $@

META-INF/javamail.default.address.map: META-INF @srcdir@/javamail.address.map
	cp @srcdir@/javamail.address.map $@

# Prepare classes location
$(dst):
	mkdir -p $(dst)
	cd $(dst) && $(JAR) xf $(activation_jar) javax gnu
	cd $(dst) && $(JAR) xf $(inetlib_jar) gnu/inet/imap gnu/inet/smtp gnu/inet/nntp gnu/inet/pop3 gnu/inet/util

$(dst_l10n): $(dst)/%: $(src)/% $(dst)
	cp $< $@

# Jars

$(gnumail_jar): $(dst) $(gnumail_meta) $(classes) $(dst_l10n)
	$(JAR) cfm $(gnumail_jar) $(manifest_mf) \
		$(gnumail_meta) \
		-C $(dst) gnu

# Compilation

$(classes): $(dst)/%.class: $(src)/%.java
	$(JAVAC) $(JAVACFLAGS) -classpath $(classpath) -d $(dst) $<

# Javadoc
javadoc:
	mkdir -p $(doc)
	$(JAVADOC) $(JAVADOCFLAGS) -d $(doc) -sourcepath $(src) \
          -windowtitle "GNU JavaMail API documentation" $(package_names)

# Remove jar files and directories
clean-local:
	rm -rf $(gnumail_jar) META-INF $(dst) $(doc)

install-data-local:
	@srcdir@/mkinstalldirs $(DESTDIR)/$(datadir)/java
	$(INSTALL_DATA) $(gnumail_jar) $(DESTDIR)/$(datadir)/java

# End of Makefile.am
