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

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtCompatible;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Static methods for creating common {@code BinaryPredicate} objects.
 *
 * @author Mick Killianey
 */
@GoogleInternal // possible future release, but where is this all heading?
@GwtCompatible
public final class BinaryPredicates {
  private BinaryPredicates() {}

  /** Returns a {@link BinaryPredicate} that always evaluates to true. */
  @GwtCompatible(serializable = true)
  public static <X, Y> BinaryPredicate<X, Y> alwaysTrue() {
    return restrict(AlwaysTrue.AlwaysTrue);
  }

  /** Returns a {@link BinaryPredicate} that always evaluates to false. */
  @GwtCompatible(serializable = true)
  public static <X, Y> BinaryPredicate<X, Y> alwaysFalse() {
    return restrict(AlwaysFalse.AlwaysFalse);
  }

  /**
   * Returns a {@link BinaryPredicate} that returns true if its arguments are both null or are
   * equal.
   */
  @GwtCompatible(serializable = true)
  public static <X, Y> BinaryPredicate<X, Y> equality() {
    return restrict(Equality.Equality);
  }

  /**
   * Returns a {@link BinaryPredicate} that returns true if its arguments are both null or refer to
   * the same object.
   */
  @GwtCompatible(serializable = true)
  public static <X, Y> BinaryPredicate<X, Y> identity() {
    return restrict(Identity.Identity);
  }

  /**
   * Returns a {@link BinaryPredicate} that returns true if the application of the given {@link
   * Predicate} to the first argument is true.
   */
  public static <X, Y> BinaryPredicate<X, Y> first(Predicate<? super X> predicate) {
    return new First<>(predicate);
  }

  /**
   * Returns a {@link BinaryPredicate} that returns true if the application of the given {@link
   * Predicate} to the second argument is true.
   */
  public static <X, Y> BinaryPredicate<X, Y> second(Predicate<? super Y> predicate) {
    return new Second<>(predicate);
  }

  /**
   * Returns a {@link BinaryPredicate} that evaluates to {@code true} iff both of the given
   * BinaryPredicates evaluate to true. The components are evaluated in order, and evaluation will
   * be "short-circuited" as soon as the answer is determined.
   *
   * @throws NullPointerException if either of the predicates is null
   */
  public static <X, Y> BinaryPredicate<X, Y> and(
      BinaryPredicate<? super X, ? super Y> binaryPredicate1,
      BinaryPredicate<? super X, ? super Y> binaryPredicate2) {
    BinaryPredicate<X, Y> restricted1 = restrict(binaryPredicate1);
    BinaryPredicate<X, Y> restricted2 = restrict(binaryPredicate2);

    // TODO(kevinb): understand why this gets an "unchecked generic array creation for varargs
    // parameter" warning even though asList is @SafeVarargs
    Iterable<BinaryPredicate<X, Y>> iterable = Arrays.asList(restricted1, restricted2);
    return new And<>(iterable);
  }

  /**
   * Returns a {@link BinaryPredicate} that evaluates to {@code true} iff all of its components
   * evaluate to true. The components are evaluated in order, and evaluation will be
   * "short-circuited" as soon as the answer is determined.
   *
   * <p>{@code and(BinaryPredicate...)} does not defensively copy the array passed in, so future
   * changes to the array will alter the behavior of this {@link BinaryPredicate}. If {@code
   * components} is empty, the returned {@link BinaryPredicate} will evaluate to {@code true}.
   *
   * @throws NullPointerException if any of the components is null
   */
  public static <X, Y> BinaryPredicate<X, Y> and(
      BinaryPredicate<? super X, ? super Y>... components) {
    return new And<X, Y>(Arrays.asList(components));
  }

  /**
   * Returns a {@link BinaryPredicate} that evaluates to {@code true} iff all of its components
   * evaluate to true. The components are evaluated in order, and evaluation will be
   * "short-circuited" as soon as the answer is determined.
   *
   * <p>{@code and(Iterable)} does not defensively copy the {@link Iterable} passed in, so future
   * changes to the {@link Iterable} will alter the behavior of this {@link BinaryPredicate}. If
   * {@code components} is empty, the returned {@link BinaryPredicate} will evaluate to {@code
   * true}.
   *
   * @throws NullPointerException if any of the components is null
   */
  public static <X, Y> BinaryPredicate<X, Y> and(
      Iterable<? extends BinaryPredicate<? super X, ? super Y>> components) {
    return new And<>(components);
  }

  /**
   * Returns a {@link BinaryPredicate} that evaluates to {@code true} iff either of the given
   * BinaryPredicates evaluates to true. The components are evaluated in order, and evaluation will
   * be "short-circuited" as soon as the answer is determined.
   *
   * @throws NullPointerException if either of the predicates is null
   */
  public static <X, Y> BinaryPredicate<X, Y> or(
      BinaryPredicate<? super X, ? super Y> binaryPredicate1,
      BinaryPredicate<? super X, ? super Y> binaryPredicate2) {
    BinaryPredicate<X, Y> restricted1 = restrict(binaryPredicate1);
    BinaryPredicate<X, Y> restricted2 = restrict(binaryPredicate2);

    // TODO(kevinb): understand why this gets an "unchecked generic array creation for varargs
    // parameter" warning even though asList is @SafeVarargs
    Iterable<BinaryPredicate<X, Y>> iterable = Arrays.asList(restricted1, restricted2);
    return new Or<>(iterable);
  }

  /**
   * Returns a {@link BinaryPredicate} that evaluates to {@code true} iff any of its components
   * evaluates to true. The components are evaluated in order, and evaluation will be
   * "short-circuited" as soon as the answer is determined.
   *
   * <p>{@code or(BinaryPredicate...)} does not defensively copy the array passed in, so future
   * changes to the array will alter the behavior of this {@link BinaryPredicate}. If {@code
   * components} is empty, the returned {@link BinaryPredicate} will evaluate to {@code false}.
   *
   * @throws NullPointerException if any of the components is null
   */
  public static <X, Y> BinaryPredicate<X, Y> or(
      BinaryPredicate<? super X, ? super Y>... components) {
    return new Or<X, Y>(Arrays.asList(components));
  }

  /**
   * Returns a {@link BinaryPredicate} that evaluates to {@code true} iff any of its components
   * evaluates to true. The components are evaluated in order, and evaluation will be
   * "short-circuited" as soon as the answer is determined.
   *
   * <p>{@code or(Iterable)} does not defensively copy the {@link Iterable} passed in, so future
   * changes to the {@link Iterable} will alter the behavior of this {@link BinaryPredicate}. If
   * {@code components} is empty, the returned {@link BinaryPredicate} will evaluate to {@code
   * true}.
   *
   * @throws NullPointerException if any of the components is null
   */
  public static <X, Y> BinaryPredicate<X, Y> or(
      Iterable<? extends BinaryPredicate<? super X, ? super Y>> components) {
    return new Or<>(components);
  }

  /**
   * Returns a {@code BinaryPredicate} that returns {@code true} if the given {@code
   * BinaryPredicate} returns {@code false}, and vice versa.
   *
   * @throws NullPointerException if {@code binaryPredicate} is null
   */
  public static <X, Y> BinaryPredicate<X, Y> not(
      BinaryPredicate<? super X, ? super Y> binaryPredicate) {
    return new Not<>(binaryPredicate);
  }

  /**
   * Restricts a {@link BinaryPredicate} for methods requiring unnecessarily specific {@code
   * BinaryPredicate} types.
   *
   * <p>If you find that a {@code BinaryPredicate<Foo, Bar>} does not work with your code because
   * you require a {@code BinaryPredicate<SubFoo, SubBar>} to pass to another method, then that
   * method should require a {@code BinaryPredicate<? super SubFoo, ? super SubBar>}. Any {@code
   * BinaryPredicate} that accepts arguments {@code Foo} and {@code Bar} is also capable of taking
   * arguments {@code SubFoo} and {@code SubBar}.
   */
  // TODO(kevinb): don't do this. Things like alwaysTrue() should return
  // BinaryPredicate<Object, Object>.
  @Nullable
  @SuppressWarnings("unchecked") // safe contravariant cast
  private static <X, Y> BinaryPredicate<X, Y> restrict(
      @Nullable BinaryPredicate<? super X, ? super Y> predicate) {
    return (BinaryPredicate<X, Y>) predicate;
  }

  /** @see BinaryPredicates#alwaysTrue */
  // Package private for GWT serialization.
  enum AlwaysTrue implements BinaryPredicate<Object, Object> {
    AlwaysTrue;

    @Override
    public boolean apply(@Nullable Object o1, @Nullable Object o2) {
      return true;
    }
  }

  /** @see BinaryPredicates#alwaysFalse */
  // Package private for GWT serialization.
  enum AlwaysFalse implements BinaryPredicate<Object, Object> {
    AlwaysFalse;

    @Override
    public boolean apply(@Nullable Object o1, @Nullable Object o2) {
      return false;
    }
  }

  /** @see BinaryPredicates#equality */
  // Package private for GWT serialization.
  enum Equality implements BinaryPredicate<Object, Object> {
    Equality;

    @Override
    public boolean apply(@Nullable Object o1, @Nullable Object o2) {
      return Objects.equal(o1, o2);
    }
  }

  /** @see BinaryPredicates#identity */
  // Package private for GWT serialization.
  enum Identity implements BinaryPredicate<Object, Object> {
    Identity;

    @Override
    public boolean apply(@Nullable Object o1, @Nullable Object o2) {
      return o1 == o2;
    }
  }

  /** @see BinaryPredicates#not */
  private static final class Not<X, Y> implements BinaryPredicate<X, Y>, Serializable {
    final BinaryPredicate<? super X, ? super Y> predicate;

    Not(BinaryPredicate<? super X, ? super Y> predicate) {
      this.predicate = Preconditions.checkNotNull(predicate);
    }

    @Override
    public boolean apply(@Nullable X x, @Nullable Y y) {
      return !predicate.apply(x, y);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof Not) {
        Not<?, ?> other = (Not<?, ?>) obj;
        return predicate.equals(other.predicate);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return predicate.hashCode();
    }

    private static final long serialVersionUID = 7318841078083112007L;
  }

  /** See {@link #and(BinaryPredicate[])}. */
  private static final class And<X, Y> implements BinaryPredicate<X, Y>, Serializable {
    final Iterable<? extends BinaryPredicate<? super X, ? super Y>> predicates;

    And(Iterable<? extends BinaryPredicate<? super X, ? super Y>> predicates) {
      // TODO(kevinb): make a defensive copy
      for (BinaryPredicate<?, ?> predicate : predicates) {
        checkNotNull(predicate);
      }
      this.predicates = predicates;
    }

    @Override
    public boolean apply(@Nullable X x, @Nullable Y y) {
      for (BinaryPredicate<? super X, ? super Y> predicate : predicates) {
        if (!predicate.apply(x, y)) {
          return false;
        }
      }
      return true;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof And) {
        return iterableElementsEqual(predicates, ((And<?, ?>) obj).predicates);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return iterableAsListHashCode(predicates);
    }

    private static final long serialVersionUID = 4814831122225615776L;
  }

  /** See {@link #or(BinaryPredicate[])}. */
  private static final class Or<X, Y> implements BinaryPredicate<X, Y>, Serializable {
    final Iterable<? extends BinaryPredicate<? super X, ? super Y>> predicates;

    Or(Iterable<? extends BinaryPredicate<? super X, ? super Y>> predicates) {
      // TODO(kevinb): make a defensive copy
      for (BinaryPredicate<?, ?> predicate : predicates) {
        checkNotNull(predicate);
      }
      this.predicates = predicates;
    }

    @Override
    public boolean apply(@Nullable X x, @Nullable Y y) {
      for (BinaryPredicate<? super X, ? super Y> predicate : predicates) {
        if (predicate.apply(x, y)) {
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof Or) {
        return iterableElementsEqual(predicates, ((Or<?, ?>) obj).predicates);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return iterableAsListHashCode(predicates);
    }

    private static final long serialVersionUID = -1352468805830701672L;
  }

  /** See {@link #first(Predicate)}. */
  private static final class First<X, Y> implements BinaryPredicate<X, Y>, Serializable {
    final Predicate<? super X> predicate;

    First(Predicate<? super X> predicate) {
      this.predicate = Preconditions.checkNotNull(predicate);
    }

    @Override
    public boolean apply(@Nullable X x, @Nullable Y y) {
      return predicate.apply(x);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof First) {
        First<?, ?> other = (First<?, ?>) obj;
        return predicate.equals(other.predicate);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return predicate.hashCode();
    }

    private static final long serialVersionUID = 5389902773091803723L;
  }

  /** See {@link #second(Predicate)}. */
  private static final class Second<X, Y> implements BinaryPredicate<X, Y>, Serializable {
    final Predicate<? super Y> predicate;

    Second(Predicate<? super Y> predicate) {
      this.predicate = Preconditions.checkNotNull(predicate);
    }

    @Override
    public boolean apply(@Nullable X x, @Nullable Y y) {
      return predicate.apply(y);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
      if (obj instanceof Second) {
        Second<?, ?> other = (Second<?, ?>) obj;
        return predicate.equals(other.predicate);
      }
      return false;
    }

    @Override
    public int hashCode() {
      return predicate.hashCode();
    }

    private static final long serialVersionUID = -7134579481937611424L;
  }

  /**
   * Determines whether the two Iterables contain equal elements. More specifically, this method
   * returns {@code true} if {@code iterable1} and {@code iterable2} contain the same number of
   * elements and every element of {@code iterable1} is equal to the corresponding element of {@code
   * iterable2}.
   *
   * <p>This is not a general-purpose method; it assumes that the iterations contain no {@code null}
   * elements.
   */
  private static boolean iterableElementsEqual(Iterable<?> iterable1, Iterable<?> iterable2) {
    Iterator<?> iterator1 = iterable1.iterator();
    Iterator<?> iterator2 = iterable2.iterator();
    while (iterator1.hasNext()) {
      if (!iterator2.hasNext()) {
        return false;
      }
      if (!iterator1.next().equals(iterator2.next())) {
        return false;
      }
    }
    return !iterator2.hasNext();
  }

  /**
   * Calculates a hashCode for the elements in the given Iterable as per the contract of
   * List.hashCode().
   *
   * <p>This is not a general-purpose method; it assumes that the iteration contains no null
   * elements.
   */
  private static int iterableAsListHashCode(Iterable<?> iterable) {
    Iterator<?> iterator = iterable.iterator();
    int result = 1;
    while (iterator.hasNext()) {
      Object obj = iterator.next();
      result = 31 * result + obj.hashCode();
    }
    return result;
  }
}
