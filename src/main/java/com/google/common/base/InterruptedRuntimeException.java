/*
 * Copyright (C) 2003 The Guava Authors
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
 * A <b>deprecated</b> unchecked version of {@link InterruptedException}, with the added property
 * that the thread's interrupted status is set when creating this.
 *
 * <p>This class has been deprecated, and is not considered the correct way to deal with {@link
 * InterruptedException}. The best approach varies by situation, but here are some suggestions:
 *
 * <ul>
 *   <li>Methods that already throw a checked exception should often wrap the InterruptedException
 *       in a new instance of that exception class, {@linkplain Thread#interrupt restore the
 *       interrupt}, and throw the wrapper exception.
 *   <li>In particular, callers of {@code Future.get} can use {@link
 *       com.google.common.util.concurrent.Futures#get} to perform these operations automatically.
 *   <li>Methods that cannot throw a checked exception should often use {@linkplain
 *       com.google.common.util.concurrent.Uninterruptibles the uninterruptible version of their
 *       operations}.
 *   <li>Low-level concurrency utilities should often avoid catching InterruptedException in the
 *       first place, preferring to propagate it if possible.
 *   <li>If a task runs only in a private thread or executor that is known never to be interrupted
 *       or shut down, InterruptedException is impossible, and it is safe to wrap it in an
 *       AssertionError.
 * </ul>
 *
 * @deprecated follow one of the above suggestions instead.
 * @author George Baggott (gbaggott@google.com)
 */
@Deprecated
@GoogleInternal
@GwtIncompatible
public class InterruptedRuntimeException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  /**
   * Create an InterruptedRuntimeException after discovering via {@link Thread#isInterrupted()} that
   * we've been interrupted.
   */
  public InterruptedRuntimeException() {
    Thread.currentThread().interrupt();
  }

  /**
   * Create an InterruptedRuntimeException after discovering via {@link Thread#isInterrupted()} that
   * we've been interrupted.
   *
   * @param message The detail message
   */
  public InterruptedRuntimeException(@Nullable final String message) {
    super(message);
    Thread.currentThread().interrupt();
  }

  /**
   * Create an InterruptedRuntimeException to wrap an InterruptedException.
   *
   * @param cause The InterruptedException to be wrapped
   */
  public InterruptedRuntimeException(@Nullable final InterruptedException cause) {
    super(cause);
    Thread.currentThread().interrupt();
  }

  /**
   * Create an InterruptedRuntimeException to wrap an InterruptedException.
   *
   * @param message The detail message
   * @param cause The InterruptedException to be wrapped
   */
  public InterruptedRuntimeException(
      @Nullable final String message, @Nullable final InterruptedException cause) {
    super(message, cause);
    Thread.currentThread().interrupt();
  }
}
