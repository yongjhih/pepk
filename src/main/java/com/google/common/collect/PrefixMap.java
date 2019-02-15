/*
 * Copyright (C) 2007 The Guava Authors
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

package com.google.common.collect;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Maps string prefixes to values. For example, if you {@code put("foo", 1)},
 * {@code get("foobar")} returns {@code 1}. Prohibits null values.
 *
 * <p>Use instead of iterating over a series of string prefixes calling {@code
 * String.startsWith(prefix)}.
 *
 * @author Bob Lee
 */
@GoogleInternal // need to consolidate the various trie-related stuff
@GwtIncompatible
public interface PrefixMap<T> {
  /**
   * Maps prefix to value, which must not be null.
   *
   * @return the previous value stored for this prefix, or {@code null} if none
   * @throws IllegalArgumentException if prefix is an empty string.
   */
  @CanIgnoreReturnValue
  T put(CharSequence prefix, T value);

  /**
   * Finds a prefix that matches {@code s} and returns the mapped value. If
   * multiple prefixes in the map match {@code s}, the longest match wins.
   *
   * @return value for prefix matching {@code s}, or {@code null} if none match
   */
  T get(CharSequence s);

  /**
   * Removes an exact prefix stored in the map, returning the old value,
   * or null if it did not exist. Inverse operation of put(prefix).
   *
   * @return the previous value stored for this prefix, or {@code null} if none
   * @throws IllegalArgumentException if prefix is an empty string.
   */
  @CanIgnoreReturnValue
  T remove(CharSequence prefix);
}
