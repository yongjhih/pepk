// Copyright 2006 Google Inc. All Rights Reserved.

package com.google.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that an action method does not change any state (on a session,
 * database, remote service, or otherwise).  This annotation is retained at
 * runtime to serve as a hint to session replication and caching systems.
 *
 * @author crazybob@google.com (Bob Lee)
 */
@GoogleInternal // maybe consider it
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
@GwtCompatible
public @interface ReadOnly {}
