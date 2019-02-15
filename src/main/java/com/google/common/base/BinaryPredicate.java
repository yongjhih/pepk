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

package com.google.common.base;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtCompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.function.BiPredicate;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Legacy version of {@link java.util.function.BiPredicate}. A two-argument version of {@link
 * Predicate} that determines a true or false value for pairs of inputs.
 *
 * <p>The {@link BinaryPredicates} class provides common binary predicates and related utilities.
 *
 * <p>As this interface extends {@code java.util.function.BiPredicate}, an instance of this type may
 * be used as a {@code BiPredicate} directly. To use a {@code java.util.function.BiPredicate} where
 * a {@code com.google.common.base.BinaryPredicate} is expected, use the method reference {@code
 * binaryPredicate::test}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/FunctionalExplained">the use of functional types</a>.
 *
 * @author Mick Killianey
 */
@GoogleInternal // dead end
@FunctionalInterface
@GwtCompatible
public interface BinaryPredicate<X, Y> extends BiPredicate<X, Y> {
  /**
   * Applies this {@link BinaryPredicate} to the given objects.
   *
   * @return the value of this predicate when applied to inputs {@code x, y}
   */
  @CanIgnoreReturnValue
  boolean apply(@Nullable X x, @Nullable Y y);

  @Override
  default boolean test(@Nullable X x, @Nullable Y y) {
    return apply(x, y);
  }
}
