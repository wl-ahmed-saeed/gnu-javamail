/*
 * MailSessionDefinition.java
 * Copyright (C) 2013 The Free Software Foundation
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

import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

/**
 * J2EE annotation to define a mail session for registration with JNDI.
 * @version 1.5
 * @since 1.5
 * @author <a href='mailto:dog@gnu.org'>Chris Burdess</a>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface MailSessionDefinition
{

  /**
   * Description of this session.
   */
  String description() default "";

  /**
   * JNDI name to register the session.
   */
  String name();

  /**
   * Store protocol.
   */
  String storeProtocol() default "";

  /**
   * Transport protocol.
   */
  String transportProtocol() default "";

  /**
   * Server hostname.
   */
  String host() default "";

  /**
   * Username.
   */
  String user() default "";

  /**
   * Password.
   */
  String password() default "";

  /**
   * From address for the user.
   */
  String from() default "";

  /**
   * Properties to include in the session, in the format name=value.
   */
  String[] properties() default {};

}
