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
import com.google.common.annotations.GwtCompatible;
import java.util.function.Consumer;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Legacy version of {@code java.util.function.Consumer}.
 *
 * <p>This interface is complementary to the {@link Supplier} interface. Semantically, it can be
 * used as an Output Stream, Sink, Closure or something else entirely. No guarantees are implied by
 * this interface.
 *
 * <p>This interface is now a legacy type. Use {@code java.util.function.Consumer} (or the
 * appropriate primitive specialization such as {@code IntConsumer}) instead whenever possible.
 * Otherwise, at least reduce <i>explicit</i> dependencies on this type by using lambda expressions
 * or method references instead of classes, leaving your code easier to migrate in the future.
 *
 * <p>As this interface extends {@link java.util.function.Consumer}, an instance of this type can be
 * used as a {@code java.util.function.Consumer} directly. To use a {@code
 * java.util.function.Consumer} called {@code consumer} in a context where a {@code
 * com.google.common.base.Receiver} is needed, use {@code consumer::accept}.
 *
 * @author micapolos@google.com (Michal Pociecha-Los)
 */
@GoogleInternal
@FunctionalInterface
@GwtCompatible
public interface Receiver<T> extends Consumer<T> {

  /**
   * Accepts received object.
   *
   * @param object received object
   */
  void accept(@Nullable T object);
}
