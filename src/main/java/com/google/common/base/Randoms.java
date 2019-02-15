/*
 * Copyright (C) 2009 The Guava Authors
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
import java.security.SecureRandom;
import java.util.Random;

/**
 * Contains static factory methods for creating {@code Random} instances.
 *
 * <p>Using this class is preferred over directly instantiating {@code Random}
 * instances, as it makes the security implications of the selected random
 * implementation more explicit.
 *
 * @author Charles Fry
 */
@GoogleInternal // TODO(kevinb): should we release this?
@GwtIncompatible
public final class Randoms {
  private Randoms() {}

  private static final Random RANDOM = new ReadOnlyRandom();
  private static final SecureRandom SECURE_RANDOM = newDefaultSecureRandom();
  private static final Random THREAD_LOCAL_SECURE_RANDOM = new ThreadLocalSecureRandom();

  /**
   * Returns a {@code SecureRandom} instance (or subclass) that is a
   * cryptographically secure pseudo-random number generator (CSPRNG),
   * and thus fit for use by security-sensitive applications.
   *
   * <p>Note that repeated calls to this method return the same instance.
   *
   * @return a thread-safe, cryptographically strong random number generator
   */
  public static SecureRandom secureRandom() {
    return SECURE_RANDOM;
  }

  /**
   * Returns a {@code SecureRandom} instance (or subclass) that is a cryptographically secure
   * pseudo-random number generator (CSPRNG), and thus fit for use by security-sensitive
   * applications.
   *
   * <p>The supplied seed supplements, but does not replace, the internal seed used by the
   * SecureRandom class. Objects obtained from this method will produce different random numbers,
   * even when the same seed is provided.
   *
   * <p>Since this method can return a subclass of SecureRandom, users of this method should not
   * depend on the exact algorithm returned by this method.
   *
   * @param seed the initial seed
   * @return a thread-safe, cryptographically strong random number generator
   */
  public static SecureRandom secureRandom(byte[] seed) {
    SecureRandom retval = new SecureRandom(seed);
    retval.nextLong(); // force seeding
    return retval;
  }

  /**
   * Returns a {@code Random} instance that is a cryptographically secure pseudo-random number
   * generator (CSPRNG), and thus fit for use by security-sensitive applications.
   *
   * <p>This implementation uses a separate {@link SecureRandom} instance per thread, to reduce
   * synchronization contention.
   *
   * @return a thread-safe, cryptographically strong random number generator
   */
  public static Random threadLocalSecureRandom() {
    return THREAD_LOCAL_SECURE_RANDOM;
  }

  // TODO(martinrb): insecureRandom should return instances with a cycle of
  // at least 2**64.

  /**
   * Returns a {@code Random} instance (or subclass) that is fast but
   * insecure. The returned instance is <i>not</i> cryptographically secure,
   * and should <i>not</i> be used for security-sensitive applications.
   *
   * <p>Note that repeated calls to this method return the same instance.
   *
   * @return a thread-safe, cryptographically weak random number generator
   */
  public static Random insecureRandom() {
    return RANDOM;
  }

  /**
   * Returns a new {@code Random} instance (or subclass) that is fast but
   * insecure. The returned instance is <i>not</i> cryptographically secure,
   * and should <i>not</i> be used for security-sensitive applications.
   *
   * <p>Since this method can return a subclass of Random, users of this
   * method should not depend on the exact algorithm returned by this method.
   * Future versions of this code may substitute a different random number
   * generator which may produce different random numbers from the same seed.
   *
   * @param seed the initial seed
   * @return a thread-safe, cryptographically weak random number generator
   * @see Random#setSeed
   */
  public static Random insecureRandom(long seed) {
    return new Random(seed);
  }

  /**
   * A version of {@link Random} which disallows calls
   * to {@code setSeed(long)}.
   */
  private static class ReadOnlyRandom extends Random {
    private static final long serialVersionUID = 898001275432099254L;
    private boolean initializationComplete = false;

    private ReadOnlyRandom() {
      super();
      initializationComplete = true;
    }

    @Override
    public void setSeed(long seed) {
      if (initializationComplete) {
        throw new UnsupportedOperationException(
            "Setting the seed on the shared Random object is not permitted");
      }
      super.setSeed(seed);
    }
  }

  private static SecureRandom newDefaultSecureRandom() {
    SecureRandom retval = new SecureRandom();
    retval.nextLong(); // force seeding
    return retval;
  }

  private static final ThreadLocal<SecureRandom> localRandom =
      new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
          return newDefaultSecureRandom();
        }
      };

  private static final class ThreadLocalSecureRandom extends Random {
    private boolean initializationComplete = false;

    private ThreadLocalSecureRandom() {
      super();
      initializationComplete = true;
    }

    SecureRandom current() {
      return localRandom.get();
    }

    @Override
    protected int next(int bits) {
      // We cannot delegate to the protected method on the SecureRandom, but since we override all
      // public methods of Random, this method should never actually be called.
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean nextBoolean() {
      return current().nextBoolean();
    }

    @Override
    public void nextBytes(byte[] bytes) {
      current().nextBytes(bytes);
    }

    @Override
    public double nextDouble() {
      return current().nextDouble();
    }

    @Override
    public float nextFloat() {
      return current().nextFloat();
    }

    @Override
    public double nextGaussian() {
      return current().nextGaussian();
    }

    @Override
    public int nextInt() {
      return current().nextInt();
    }

    @Override
    public int nextInt(int n) {
      return current().nextInt(n);
    }

    @Override
    public long nextLong() {
      return current().nextLong();
    }

    @Override
    public void setSeed(long seed) {
      if (initializationComplete) {
        throw new UnsupportedOperationException(
            "Setting the seed on a thread-local Random object is not permitted");
      }
      super.setSeed(seed);
    }
  }
}
