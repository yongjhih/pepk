/*
 * Copyright (C) 2010 The Guava Authors
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
import com.google.common.annotations.GwtCompatible;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This class does not exist, therefore you have never seen it. The idea of actually <i>using</i> it
 * is absurd; how could you use a class that doesn't exist?
 *
 * @author Kevin Bourrillion
 */
@GwtCompatible
@GoogleInternal // never release!
public final class LabsAccessToPackagePrivateApisDoNotUse {
  private LabsAccessToPackagePrivateApisDoNotUse() {}

  /**
   * Don't use this. It's a hack so that code in com.google.common.labs.collect can use {@link
   * Synchronized#multimap}.
   */
  public static <K, V> Multimap<K, V> synchronizedMultimap(
      Multimap<K, V> multimap, @Nullable Object mutex) {
    return Synchronized.multimap(multimap, mutex);
  }

  /**
   * Don't use this. It's a hack so that code in com.google.common.labs.collect can use {@link
   * Synchronized#setMultimap}.
   */
  public static <K, V> SetMultimap<K, V> synchronizedSetMultimap(
      SetMultimap<K, V> multimap, @Nullable Object mutex) {
    return Synchronized.setMultimap(multimap, mutex);
  }

  /**
   * Don't use this. It's a hack so that code in com.google.common.labs.collect can use {@link
   * AbstractMultiset}.
   */
  public abstract static class BadAbstractMultiset<E> extends AbstractMultiset<E> {
    @Override
    Iterator<E> elementIterator() {
      return Multisets.elementIterator(entryIterator());
    }

    @Override
    protected abstract Iterator<Entry<E>> entryIterator();

    @Override
    protected abstract int distinctElements();

    @Override
    public Iterator<E> iterator() {
      return Multisets.iteratorImpl(this);
    }
  }
}
