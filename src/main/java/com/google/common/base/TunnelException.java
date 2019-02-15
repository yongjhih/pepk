/*
 * Copyright (C) 2016 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.CheckReturnValue;
import java.util.concurrent.Callable;

/**
 * An unchecked exception used to <i>temporarily</i> wrap a checked exception, so that it can be
 * thrown from a method or constructor that doesn't declare that exception type, and then extracted
 * in a surrounding context and handled or propagated as usual.
 *
 * <p>For example, the following code <b>will not compile</b> because the checked {@code
 * IOException} can't pass through the stream methods:
 *
 * <pre>{@code
 * static List<byte[]> readFiles() throws IOException {
 *   return fileStream()
 *       .map(file -> Files.toByteArray(file))
 *       .collect(toList());
 * }
 * }</pre>
 *
 * <p>... but this can be remedied using {@code TunnelException}:
 *
 * <pre>{@code
 * static List<byte[]> readFiles() throws IOException {
 *   try {
 *     return fileStream()
 *         .map(file -> tunnel(() -> Files.toByteArray(file)))
 *         .collect(toList());
 *   } catch (TunnelException e) {
 *     throw e.getCauseAs(IOException.class);
 *   }
 * }
 * }</pre>
 *
 * <p>If multiple types of checked exceptions may be thrown, either as a result of multiple calls to
 * {@code tunnel} or a single call to {@code tunnel} that may throw more than one type of exception,
 * we recommend the following idiom:
 *
 * <pre>{@code
 * static Foo streamToFoo() throws AException, BException {
 *   try {
 *     return someStream()
 *         .map(file -> tunnel(() -> mayThrowAOrBException))
 *         .collect(fooCollector());
 *   } catch (TunnelException e) {
 *     throw e.rethrow(AException.class, BException.class);
 *   }
 * }
 * }</pre>
 *
 * <p>If you wish to handle the exceptions inside your method rather than rethrow them onto a
 * caller, we recommend nested try/catch blocks as follows:
 *
 * <pre>{@code
 * static Foo streamToFoo() {
 *   try {
 *     try {
 *       return someStream()
 *           .map(file -> tunnel(() -> mayThrowAOrBException))
 *           .collect(fooCollector());
 *     } catch (TunnelException e) {
 *       throw e.rethrow(AException.class, BException.class);
 *     }
 *   } catch (AException e) {
 *     ...
 *   } catch (BException e) {
 *     ...
 *   }
 * }
 * }</pre>
 *
 * <p>{@code TunnelException} addresses the issues associated with unwrapping checked exceptions
 * correctly by verifying that the caught exception is of the appropriate type. Users of Error Prone
 * will get <a
 * href="https://g3doc.corp.google.com/java/com/google/devtools/staticanalysis/errorprone/g3doc/TunnelExceptionEnforcement.md">additional
 * compile time checks</a> to statically enforce the correct usage of TunnelException.
 *
 * @author Louis Wasserman
 */
@GoogleInternal
@GwtIncompatible("Class.isInstance")
public final class TunnelException extends RuntimeException {
  /**
   * Evaluate the result of the specified lambda, wrapping any checked exception thrown in a {@code
   * TunnelException}. Unchecked exceptions are rethrown unchanged.
   *
   * <p>To tunnel exceptions from code that has no return value, it is easiest just to add a dummy
   * {@code return null;} to the callback.
   */
  @CanIgnoreReturnValue
  public static <T> T tunnel(Callable<T> callback) {
    checkNotNull(callback);
    try {
      return callback.call();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new TunnelException(e);
    }
  }

  private TunnelException(Exception e) {
    super(
        "TunnelExceptions should always be unwrapped to deal with the checked exception underneath,"
            + " this message should never be seen if TunnelException is used properly.",
        e);
  }

  /**
   * Returns the cause of the {@code TunnelException} without any specific exception type. We
   * recommend using the more specific overload {@link #getCauseAs(Class)} wherever possible.
   */
  @Override
  public synchronized Exception getCause() {
    return (Exception) super.getCause();
  }

  private static ClassCastException exception(
      Throwable cause, String message, Object... formatArgs) {
    ClassCastException result = new ClassCastException(String.format(message, formatArgs));
    result.initCause(cause);
    return result;
  }

  @SafeVarargs
  private static void checkNoRuntimeExceptions(
      String methodName, Class<? extends Exception>... clazzes) {
    for (Class<? extends Exception> clazz : clazzes) {
      checkArgument(
          !RuntimeException.class.isAssignableFrom(clazz),
          "The cause of a TunnelException can never be a RuntimeException, "
              + "but %s argument was %s",
          methodName,
          clazz);
    }
  }

  /**
   * Returns the underlying checked exception of the specified type. This method is intended for
   * <em>only</em> the scenario where the underlying exception is known to be of the specified type.
   *
   * <p>If the exception is <em>not</em> of the specified type, a different exception is thrown; if
   * Error Prone is in use, this will generate a compile-time error.
   */
  public <X extends Exception> X getCauseAs(Class<X> exceptionClazz) {
    checkNotNull(exceptionClazz);
    checkNoRuntimeExceptions("getCause", exceptionClazz);
    if (exceptionClazz.isInstance(getCause())) {
      return exceptionClazz.cast(getCause());
    }
    throw exception(getCause(), "getCause(%s) doesn't match underlying exception", exceptionClazz);
  }

  /**
   * Throws the underlying checked exception, which is assumed to be of the specified type or one of
   * its subtypes. This method is intended to be used in the idiom
   *
   * <pre>{@code
   * throw tunnelException.rethrow(SomeException.class);
   * }</pre>
   *
   * to satisfy the compiler that an exception is always thrown.
   *
   * <p>If the exception is <em>not</em> of the specified type, a different exception is thrown; if
   * Error Prone is in use, this will generate a compile-time error.
   */
  @CheckReturnValue
  public <X extends Exception> RuntimeException rethrow(Class<X> exceptionClazz) throws X {
    checkNotNull(exceptionClazz);
    checkNoRuntimeExceptions("rethrow", exceptionClazz);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz);
    throw exception(getCause(), "rethrow(%s) doesn't match underlying exception", exceptionClazz);
  }

  /**
   * Throws the underlying checked exception, which is assumed to be of the specified types or one
   * of their subtypes. This method is intended to be used in the idiom
   *
   * <pre>{@code
   * throw tunnelException.rethrow(SomeException1.class, SomeException2.class);
   * }</pre>
   *
   * to satisfy the compiler that an exception is always thrown.
   *
   * <p>If the exception is <em>not</em> of the specified types, a different exception is thrown; if
   * Error Prone is in use, this will generate a compile-time error.
   */
  @CheckReturnValue
  public <X1 extends Exception, X2 extends Exception> RuntimeException rethrow(
      Class<X1> exceptionClazz1, Class<X2> exceptionClazz2) throws X1, X2 {
    checkNotNull(exceptionClazz1);
    checkNotNull(exceptionClazz2);
    checkNoRuntimeExceptions("rethrow", exceptionClazz1, exceptionClazz2);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz1);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz2);
    throw exception(
        getCause(),
        "rethrow(%s, %s) doesn't match underlying exception",
        exceptionClazz1,
        exceptionClazz2);
  }

  /**
   * Throws the underlying checked exception, which is assumed to be of the specified types or one
   * of their subtypes. This method is intended to be used in the idiom
   *
   * <pre>{@code
   * throw tunnelException.rethrow(
   *     SomeException1.class, SomeException2.class, SomeException3.class);
   * }</pre>
   *
   * to satisfy the compiler that an exception is always thrown.
   *
   * <p>If the exception is <em>not</em> of the specified types, a different exception is thrown; if
   * Error Prone is in use, this will generate a compile-time error.
   */
  @CheckReturnValue
  public <X1 extends Exception, X2 extends Exception, X3 extends Exception>
      RuntimeException rethrow(
          Class<X1> exceptionClazz1, Class<X2> exceptionClazz2, Class<X3> exceptionClazz3)
          throws X1, X2, X3 {
    checkNotNull(exceptionClazz1);
    checkNotNull(exceptionClazz2);
    checkNotNull(exceptionClazz3);
    checkNoRuntimeExceptions("rethrow", exceptionClazz1, exceptionClazz2, exceptionClazz3);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz1);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz2);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz3);
    throw exception(
        getCause(),
        "rethrow(%s, %s, %s) doesn't match underlying exception",
        exceptionClazz1,
        exceptionClazz2,
        exceptionClazz3);
  }

  /**
   * Throws the underlying checked exception, which is assumed to be of the specified types or one
   * of their subtypes. This method is intended to be used in the idiom
   *
   * <pre>{@code
   * throw tunnelException.rethrow(
   *     SomeException1.class, SomeException2.class, SomeException3.class, SomeException4.class);
   * }</pre>
   *
   * to satisfy the compiler that an exception is always thrown.
   *
   * <p>If the exception is <em>not</em> of the specified types, a different exception is thrown; if
   * Error Prone is in use, this will generate a compile-time error.
   */
  @CheckReturnValue
  public <X1 extends Exception, X2 extends Exception, X3 extends Exception, X4 extends Exception>
      RuntimeException rethrow(
          Class<X1> exceptionClazz1,
          Class<X2> exceptionClazz2,
          Class<X3> exceptionClazz3,
          Class<X4> exceptionClazz4)
          throws X1, X2, X3, X4 {
    checkNotNull(exceptionClazz1);
    checkNotNull(exceptionClazz2);
    checkNotNull(exceptionClazz3);
    checkNotNull(exceptionClazz4);
    checkNoRuntimeExceptions(
        "rethrow", exceptionClazz1, exceptionClazz2, exceptionClazz3, exceptionClazz4);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz1);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz2);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz3);
    Throwables.throwIfInstanceOf(getCause(), exceptionClazz4);
    throw exception(
        getCause(),
        "rethrow(%s, %s, %s, %s) doesn't match underlying exception",
        exceptionClazz1,
        exceptionClazz2,
        exceptionClazz3,
        exceptionClazz4);
  }
}
