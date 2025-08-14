# GNU Java Mail (Jakarta Edition)
his repository modules are based on modules provided by Classpath Extensions Softwar [(classpathx)](https://savannah.gnu.org/svn/?group=classpathx), which is part of the GNU Savannah Project. The project has been migrated from Javax Mail to Jakarta Mail for modern Java EE compatibility.

# Packages
There are two main packages that provide JavaMail API functionality:
1. **mail:** implements the Jakarta Mail API with additional provider features including:
    * mbox provider (local mbox file support)
    * Maildir Provider (local maildir directory support)
    * NNTP (store and transport) provider
    * IMAP store provider
    * POP3 store provider
    * SMTP transport provider
2. **inetlib:** extension library providing network protocols such as HTTP, IMAP, POP3, and SMTP to client applications

# Version Number Convention
* The **build.xml** files of the modules (inetlib & mail) are modified to append a version number string to the end of the JAR name to easily differentiate between JAR versions after applying internal modifications.
* Since this repository is based on **GNU 1.1.2 release** and has been migrated to Jakarta Mail, we use the version format **jarname-1.1.2.x-jakarta.jar**
* For example:
  1. The original **inetlib.jar** is renamed to be **inetlib-1.1.2.jar**
  2. The **gnumail.jar** is **gnu-mail-jakarta-1.1.2.jar**
  3. The **providers.jar** is **gnu-mail-providers-jakarta-1.1.2.jar**

# Build JARs
The project uses Apache Ant with Ivy dependency management for building. Dependencies are automatically resolved from Maven Central.

## Building the Inetlib Module (Required First Step)
1. Navigate to the `inetlib` directory: `cd inetlib`
2. Run the build: `ant` (builds the JAR) or `ant inetlib.jar`

## Building the Mail Module
1. Navigate to the `mail` directory: `cd mail`
2. Run the build: `ant` (builds both JARs) or `ant gnumail.jar` or `ant providers.jar`

## Generated JARs
- `gnu-mail-jakarta-1.1.2.jar` - Main GNU JavaMail implementation
- `gnu-mail-providers-jakarta-1.1.2.jar` - Protocol providers (SMTP, IMAP, POP3, NNTP, mbox, maildir)

## Dependencies
- **Jakarta Activation API 2.1.3** - Automatically downloaded from Maven Central
- **Jakarta Mail API 2.1.3** - Automatically downloaded from Maven Central  
- **inetlib-1.1.2.1.jar** - Must be present in `../inetlib/` directory

# Jakarta Mail Migration
This project has been migrated from Javax Mail to Jakarta Mail to ensure compatibility with modern Java EE and Jakarta EE specifications. The migration includes:

- Updated package names from `javax.mail.*` to `jakarta.mail.*`
- Updated method signatures to match Jakarta Activation API
- Updated some class implementations to match new ones.

## Protocol Support
The following protocols are supported and can be enabled/disabled via build properties (in `mail/build.xml`):
- **SMTP/SMTPS** - Email transport (can be disabled with `disable-smtp=true`)
- **IMAP/IMAPS** - Email store (can be disabled with `disable-imap=true`)
- **POP3** - Email store (can be disabled with `disable-pop3=true`)
- **NNTP** - News store and transport (can be disabled with `disable-nntp=true`)
- **mbox** - Local mbox file support (always enabled)
- **maildir** - Local maildir directory support (always enabled)

## Purpose of the Migration
This project addresses a gap in modern Jakarta Mail implementations by providing support for local mail storage protocols (mbox and maildir) that are not directly supported by other Jakarta Mail providers like Angus Mail. 

The converted GNU Mail implementation focuses specifically on these local storage protocols, allowing it to work alongside other Jakarta Mail implementations while providing specialized functionality for **mbox files** and **maildir directories**.

This makes GNU Mail work as a complementary solution, enabling the use of both systems (e.g. GNU and Angus) together in applications.

# License
The included packages are distributed under the terms of the **GNU General Public License with Classpath Exception**, see [classpathx license](https://www.gnu.org/software/classpath/license.html).
