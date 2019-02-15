// Copyright 2009 Google Inc. All Rights Reserved.

package com.google.common.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies that a program element is not released publicly to open-source
 * libraries, although it lives inside a class or package which is.
 *
 * @author kevinb@google.com (Kevin Bourrillion)
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({
  ElementType.ANNOTATION_TYPE,
  ElementType.CONSTRUCTOR,
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.TYPE
})
@GoogleInternal // by design, of course
@GwtCompatible
public @interface GoogleInternal {}
