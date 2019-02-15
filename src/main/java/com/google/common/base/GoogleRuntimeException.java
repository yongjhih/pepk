/*
 * Copyright (C) 2006 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.base;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtIncompatible;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class is exactly the same as GoogleException, except that it extends RuntimeException
 *
 * <p>GoogleRuntimeException allows you to associate two separate messages with an exception - an
 * internalMessage for storing debugging info, and an externalMessage suitable for displaying to a
 * customer.
 *
 * @author peter@google.com (Peter Kappler)
 * @deprecated Please use {@code RuntimeException} (or a custom exception type) in new code.
 */
@Deprecated
@GoogleInternal
@GwtIncompatible
public class GoogleRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  @Nullable private String internalMessage;
  private String externalMessage = "A system error has occurred";

  public GoogleRuntimeException() {}

  /**
   * Creates a new GoogleRuntimeException with the specified internal error message.
   *
   * <p>The external message defaults to <i>A system error has occurred</i>.
   */
  public GoogleRuntimeException(@Nullable String internalMessage) {
    super(internalMessage);
    setInternalMessage(internalMessage);
  }

  /**
   * Wraps a generic Java exception inside a GoogleRuntimeException.
   *
   * <p>The new GoogleRuntimeException uses the stack trace of the Java exception as its internal
   * message.
   *
   * @param externalMessage the error message to show the end-user/customer
   * @param t The {@link Throwable} to wrap
   */
  public GoogleRuntimeException(@Nullable String externalMessage, @Nullable Throwable t) {
    super(externalMessage, t);
    setInternalMessage(Throwables.getStackTraceAsString(t));
    setExternalMessage(externalMessage);
  }

  /**
   * Wraps a GoogleException inside a GoogleRuntimeException.
   *
   * <p>The new GoogleRuntimeException uses the stack trace of the GoogleException and its internal
   * and external messages.
   *
   * <p>(You should have a good reason for doing this; don't just wrap exceptions you don't want to
   * deal with willy nilly.)
   *
   * @param e The {@link GoogleException} to wrap.
   */
  public GoogleRuntimeException(@Nullable GoogleException e) {
    super(e);
    setInternalMessage(e.getInternalMessage());
    setExternalMessage(e.getExternalMessage());
  }

  /**
   * A low-level message that specifically describes the error. This should be a message that might
   * help a developer debug a problem. e.g. "Unable to open database connection. Check the DB
   * server!"
   */
  @Nullable
  public final String getInternalMessage() {
    return internalMessage;
  }

  public final void setInternalMessage(@Nullable String s) {
    internalMessage = s;
  }

  /**
   * A user-friendly message that can be displayed in a web page to an end-user or customer. e.g.
   * "This service is temporarily off-line. Please try again in a few minutes."
   */
  @Nullable
  public final String getExternalMessage() {
    return externalMessage;
  }

  public final void setExternalMessage(@Nullable String s) {
    externalMessage = s;
  }

  /**
   * Same as {@link #getInternalMessage()}. Overrides default method defined in {@code
   * java.lang.Throwable}
   */
  @Nullable
  @Override
  public String getMessage() {
    return getInternalMessage();
  }
}
