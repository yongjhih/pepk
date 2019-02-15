/*
 * Copyright (C) 2008 The Guava Authors
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
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.lang.ref.SoftReference;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A replacement for {@link Charset#forName(String)} that provides a layer of caching to avoid slow,
 * disk-bound lookups. This is critical for applications that use non-standard character sets
 * provided through the {@link java.nio.charset.spi.CharsetProvider} framework (as of JDK 1.6.0).
 *
 * <p>The default implementation of {@code Charset.forName()} keeps a global cache of the last two
 * charsets that were looked up. A cache miss results in the looking up of the charset through the
 * available charset providers. Unfortunately, for charsets that are not part of the standard JDK
 * but are provided by the application through the {@code CharsetProvider} framework, this involves
 * an iteration over classloader resources every time, resulting in expensive URL connections and
 * decompression of jar files.
 *
 * <p>{@code CharsetCache} minimizes the impact of this by keeping a cache of all charsets that have
 * been looked up, using {@code SoftReference} instances to curtail its memory usage when necessary.
 * It is critical for the performance of applications that both use non-standard charsets and deal
 * with more than two charsets on a regular basis.
 *
 * @author Darick Tong (darick@google.com)
 */
@GoogleInternal // pending deprecation once our JDK is fixed
@GwtIncompatible
public final class CharsetCache {
  /** The global instance that is used in {@link CharsetCache#forName(String)}. */
  private static final CharsetCache INSTANCE = new CharsetCache(new DefaultLookup(), 100);

  /**
   * Returns a charset object for the named charset. Use this instead of {@link
   * Charset#forName(String)}.
   *
   * @param charsetName The name of the requested charset; may be either a canonical name or an
   *     alias
   * @return A charset object for the named charset
   * @throws IllegalCharsetNameException If the given charset name is illegal
   * @throws IllegalArgumentException If the given charsetName is null
   * @throws UnsupportedCharsetException If no support for the named charset is available in this
   *     instance of the Java virtual machine.
   */
  @CanIgnoreReturnValue
  public static Charset forName(@Nullable String charsetName) {
    return INSTANCE.lookup(charsetName);
  }

  /** The default lookup function implementation directly uses {@link Charset#forName(String)}. */
  private static class DefaultLookup implements Function<String, Charset> {
    @Override
    public Charset apply(String charsetName) {
      return Charset.forName(charsetName);
    }
  }

  /**
   * Store all charsets that have been looked up in an in-memory map. The number of values the map
   * can have is bound by the number of {@code Charset} instances that the JVM supports. The number
   * of keys the map can have is bound by the total number of the aliases of the charsets that the
   * JVM supports.
   *
   * <p>In order to prevent prohibitive memory usage, the map keeps {@code SoftReference} instances
   * to all of the {@code Charset} instances so that the JVM can still garbage collect {@code
   * Charset} instances if necessary.
   */
  private final ConcurrentMap<String, SoftReference<Charset>> hitCache;

  /**
   * Lookups for charsets that are not supported by the JVM also result in a full provider lookup,
   * which is expensive because it necessarily causes the JVM to enumerate {@code ClassLoader}
   * resources for custom {@code CharsetProvider} instances. To mitigate this, we also cache the
   * names of unsupported charsets. Because this keyspace is unbounded, we keep this in an LRU cache
   * with a reasonable size.
   */
  private final Map<String, Boolean> missCache;

  /** The internal Charset lookup function that this cache wraps. */
  private final Function<String, Charset> lookupFunction;

  /**
   * Creates a {@code CharsetCache}.
   *
   * @param lookupFn The internal lookup function that provides the results which this object caches
   * @param missCacheSize The maximum number of unsupported charset names that we cache in memory,
   *     using an LRU cache
   */
  CharsetCache(Function<String, Charset> lookupFn, final int missCacheSize) {
    Preconditions.checkNotNull(lookupFn);
    Preconditions.checkArgument(missCacheSize > 1);

    lookupFunction = lookupFn;
    hitCache = new ConcurrentHashMap<>();

    Map<String, Boolean> temp =
        new LinkedHashMap<String, Boolean>(missCacheSize, 0.75F, true) {
          @Override
          protected boolean removeEldestEntry(Entry<String, Boolean> eldest) {
            return size() > missCacheSize;
          }
        };
    missCache = Collections.synchronizedMap(temp);
  }

  /**
   * Instance method that is delegated to by the {@link CharsetCache#forName(String)} static method.
   */
  Charset lookup(String name) {
    Preconditions.checkArgument(name != null, "Charset name may not be null");
    // As specified by RFC 2278, Charset names are case-insensitive. Reduce our cache keyspace by
    // lowercasing the names from the start.
    name = name.toLowerCase();
    SoftReference<Charset> hit = hitCache.get(name);
    if (hit != null) {
      Charset charset = hit.get();
      if (charset != null) {
        return charset;
      }
    } else if (missCache.get(name) != null) {
      throw new UnsupportedCharsetException(name);
    }
    return lookupAndCache(name);
  }

  /**
   * Looks up the {@code Charset} corresponding to the given {@code name} using the internal lookup
   * function, and caches the result.
   */
  private Charset lookupAndCache(String name) {
    try {
      Charset charset = lookupFunction.apply(name);

      // Cache this Charset under the given "name" as well as under all of the Charset's aliases.
      SoftReference<Charset> ref = new SoftReference<>(charset);
      hitCache.put(name, ref);
      for (String alias : charset.aliases()) {
        hitCache.put(alias.toLowerCase(), ref);
      }
      return charset;
    } catch (UnsupportedCharsetException e) {
      missCache.put(name, Boolean.TRUE);
      throw e;
    }
  }
}
