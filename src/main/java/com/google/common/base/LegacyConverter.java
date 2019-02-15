/*
 * Copyright (C) 2013 The Guava Authors
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
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Base class to be extended only by "legacy" converter implementations which <i>cannot</i> adhere
 * to the {@link Converter} contract "convert null to null, non-null to non-null." In normal {@link
 * Converter} implementations, {@code null} inputs are handled automatically by {@link
 * Converter#convert} without ever invoking your {@link Converter#doForward doForward} or {@link
 * Converter#doBackward doBackward} methods at all. When extending this class instead, null inputs
 * will be passed into your "do" methods for you to handle in whatever way necessary.
 *
 * <p><b>Do not:</b>
 *
 * <ul>
 *   <li>Create any <i>new</i> subclasses of this class (this is for legacy compatibility only)
 *   <li>Continue using this class if it is feasible to migrate to the standard null behavior
 *       (extending {@link Converter} directly)
 *   <li>Refer to this type in any way except to create a subclass
 * </ul>
 *
 * <p>See <a href="http://go/converter-and-null">background information</a> on {@link Converter}'s
 * {@code null} behavior.
 *
 * @author Kurt Alfred Kluever
 * @deprecated Use {@link Converter} instead, which has slightly different null behavior. Please see
 *     the class docs for more information.
 */
@Deprecated
@GoogleInternal
@GwtCompatible
public abstract class LegacyConverter<A, B> extends Converter<A, B> {
  /** Constructor for use by subclasses. */
  protected LegacyConverter() {
    super(false);
  }

  /**
   * {@inheritDoc}
   *
   * <p>Note that the {@link Converter} contract specifies that null should be returned <b>if and
   * only if</b> the input is null. If you can satisfy that requirement, please migrate from {@link
   * LegacyConverter} to {@link Converter}, and let it handle null inputs for you automatically.
   *
   * @param a the instance to convert; unlike regular converters, <b>may be null</b>
   * @return the converted instance; unlike regular converters, <b>may be null</b>
   */
  @Nullable
  @Override
  protected abstract B doForward(@Nullable A a);

  /**
   * {@inheritDoc}
   *
   * <p>Note that the {@link Converter} contract specifies that null should be returned <b>if and
   * only if</b> the input is null. If you can satisfy that requirement, please migrate from {@link
   * LegacyConverter} to {@link Converter}, and let it handle null inputs for you automatically.
   *
   * @param b the instance to convert; unlike regular converters, <b>may be null</b>
   * @return the converted instance; unlike regular converters, <b>may be null</b>
   */
  @Nullable
  @Override
  protected abstract A doBackward(@Nullable B b);
}
