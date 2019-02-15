package com.google.common.collect;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.CollectPreconditions.checkEntryNotNull;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.errorprone.annotations.Immutable;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;

/**
 * Immutable Trie implementation supporting CharSequences as prefixes.
 *
 * <p>Prefixes are sequences of characters, and the set of allowed characters is specified as a
 * range of sequential characters. By default, any seven-bit character may appear in a prefix, and
 * so the trie is a 128-ary tree.
 *
 * @author Jason Carlson
 */
@GoogleInternal
@GwtIncompatible
@Immutable(containerOf = "T")
public final class ImmutablePrefixTrie<T> implements PrefixMap<T> {

  /**
   * {@link Builder#build()} constructs a unique {@link com.google.common.collect.PrefixTrie}
   * instance for each call which constructs an {@link ImmutablePrefixTrie}, and {@link
   * ImmutablePrefixTrie} has no accessors which can modify the state of {@code internalTrie}, so we
   * can ignore the ErrorProne warning for this field, since it's state cannot change.
   */
  @SuppressWarnings("Immutable")
  private final PrefixTrie<T> internalTrie;

  /**
   * @deprecated Unsupported Operation
   * @throws UnsupportedOperationException since adding new prefixes would break immutability
   */
  @Deprecated
  @CanIgnoreReturnValue
  @Override
  public T put(CharSequence prefix, T value) {
    throw new UnsupportedOperationException(
        "method put(CharSequence, T) not supported by ImmutablePrefixTrie.");
  }

  /**
   * Finds a prefix that matches {@code s} and returns the mapped value. If multiple prefixes in the
   * map match {@code s}, the longest match wins.
   *
   * @return value for prefix matching {@code s}, or {@code null} if none match
   */
  @Override
  public T get(CharSequence s) {
    checkNotNull(s);
    return internalTrie == null ? null : internalTrie.get(s);
  }

  /**
   * @deprecated Unsupported Operation
   * @throws UnsupportedOperationException since removing prefixes would break immutability
   */
  @Deprecated
  @CanIgnoreReturnValue
  @Override
  public T remove(CharSequence prefix) {
    throw new UnsupportedOperationException(
        "method remove(CharSequence) not supported by ImmutablePrefixTrie.");
  }

  /**
   * Returns a new {@link Builder} with an allowed character range of ['\u0000', '\u007F'] (all
   * 7-bit characters).
   */
  public static <T> Builder<T> builder() {
    return new Builder<>('\u0000', '\u007F');
  }

  /**
   * Returns a new {@link Builder} with an allowed character range of [first, last].
   *
   * @param first the first character in the allowed range
   * @param last the last character in the allowed range
   * @throws IllegalArgumentException if {@code first > last}
   */
  public static <T> Builder<T> builderForRange(char first, char last) {
    return new Builder<>(first, last);
  }

  /** Returns a new {@link Builder} with an allowed character range of ['0','9']. */
  public static <T> Builder<T> numericBuilder() {
    return new Builder<>('0', '9');
  }

  /** Returns a new {@link Builder} with an allowed character range of ['A','z']. */
  public static <T> Builder<T> alphaBuilder() {
    return new Builder<>('A', 'z');
  }

  /**
   * Constructs a new {@code ImmutablePrefixTrie} from a {@link Map} of {@link CharSequence}
   * prefixes to objects, with an allowed character range of ['\u0000', '\u007F'] (all 7-bit
   * characters).
   *
   * @throws IllegalArgumentException if {@code source} contains keys with characters outside of the
   *     allowed range
   * @throws NullPointerException if {@code source} is {@code null} or contains null keys or values.
   */
  public static <T> ImmutablePrefixTrie<T> fromMap(
      Map<? extends CharSequence, ? extends T> source) {
    return fromMapForRange(source, '\u0000', '\u007F');
  }

  /**
   * Constructs a new {@code ImmutablePrefixTrie} from a {@code Map} of {@code String} prefixes to
   * objects, with a static allowed character range of [first, last].
   *
   * @throws IllegalArgumentException if {@code source} contains keys with characters outside of the
   *     allowed range
   * @throws NullPointerException if {@code source} is {@code null} or contains null keys or values
   */
  public static <T> ImmutablePrefixTrie<T> fromMapForRange(
      Map<? extends CharSequence, ? extends T> source, char first, char last) {
    checkNotNull(source);
    if (source.isEmpty()) {
      return new ImmutablePrefixTrie<>(null);
    }

    // We could make a Builder here, but it is more efficient to just construct directly, since we
    // don't have to check for duplicate keys.
    PrefixTrie<T> trie = new PrefixTrie<>(first, last);
    for (Entry<? extends CharSequence, ? extends T> e : source.entrySet()) {
      CharSequence prefix = e.getKey();
      T v = e.getValue();

      checkEntryNotNull(prefix, v);
      checkPrefixInRange(prefix, first, last);
      trie.put(prefix, v);
    }

    return new ImmutablePrefixTrie<>(trie);
  }

  private ImmutablePrefixTrie(@Nullable PrefixTrie<T> internalTrie) {
    this.internalTrie = internalTrie;
  }

  private static void checkPrefixInRange(CharSequence prefix, char first, char last) {
    for (int i = 0; i < prefix.length(); i++) {
      char c = prefix.charAt(i);
      if (c < first || c > last) {
        throw new IllegalArgumentException(
            String.format(
                "\"%s\" contains characters outside of range: ['%c':'%c']", prefix, first, last));
      }
    }
  }

  /**
   * A builder for an {@link ImmutablePrefixTrie}. To instantiate, use {@link
   * ImmutablePrefixTrie#builder()}, {@link ImmutablePrefixTrie#numericBuilder()}, {@link
   * ImmutablePrefixTrie#alphaBuilder()}, or {@link ImmutablePrefixTrie#builderForRange(char,
   * char)}.
   *
   * <p><b>Warning:</b> Unlike {@link ImmutablePrefixTrie}, this class is not thread-safe and should
   * not be shared between threads.
   */
  public static final class Builder<T> {
    private final char first;
    private final char last;

    private PrefixTrie<T> internal;
    private boolean needsCopy;

    private Builder(char first, char last) {
      if (first > last) {
        throw new IllegalArgumentException(
            String.format(
                "First char in range ('%c') is greater than last char in range ('%c')",
                first, last));
      }
      this.first = first;
      this.last = last;
      needsCopy = false;

      internal = new PrefixTrie<>(first, last);
    }

    /**
     * Adds a string prefix with associated object to the Trie. Duplicate prefixes are not allowed.
     * Adding {@code null} is not allowed.
     *
     * @param prefix the string prefix to put to the Trie
     * @param value the object to associate with the prefix
     * @return this builder for chaining
     * @throws NullPointerException if {@code prefix} or {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code prefix} contains characters outside of the allowed
     *     range, or if it is already present in the builder
     */
    @CanIgnoreReturnValue
    public Builder<T> put(CharSequence prefix, T value) {
      checkEntryNotNull(prefix, value);
      checkPrefixInRange(prefix, first, last);

      if (needsCopy) {
        copyInternal();
        needsCopy = false;
      }

      T prev = internal.put(prefix, value);

      // throw if this prefix has been mapped already
      if (prev != null) {
        // restore the old value
        internal.put(prefix, prev);
        throw new IllegalArgumentException(
            String.format("Builder already contains prefix: %s", prefix));
      }
      return this;
    }

    /**
     * Adds all the prefixes in {@code prefixMap} and their associated values to the Trie. Duplicate
     * prefixes are not allowed.
     *
     * @param prefixMap a {@link Map} of prefixes to values
     * @return this builder for chaining
     * @throws NullPointerException if {@code prefixMap} is {@code null} or contains null keys or
     *     values.
     * @throws IllegalArgumentException if a key of {@code prefixMap} contains characters outside of
     *     the allowed range, or if a prefix is already present in the builder
     */
    @CanIgnoreReturnValue
    public Builder<T> putAll(Map<? extends CharSequence, ? extends T> prefixMap) {
      checkNotNull(prefixMap);

      for (Entry<? extends CharSequence, ? extends T> e : prefixMap.entrySet()) {
        put(e.getKey(), e.getValue());
      }
      return this;
    }

    /**
     * Adds all the prefixes in {@code immutablePrefixTrie} and their associated values to the Trie.
     * Duplicate prefixes are not allowed.
     *
     * @param immutablePrefixTrie an {@link ImmutablePrefixTrie} of prefixes to values
     * @return this builder for chaining
     * @throws NullPointerException if {@code immutablePrefixTrie} is {@code null}.
     * @throws IllegalArgumentException if a key of {@code immutablePrefixTrie} contains characters
     *     outside of the allowed range, or if a prefix is already present in the builder
     */
    @CanIgnoreReturnValue
    public Builder<T> putAll(ImmutablePrefixTrie<? extends T> immutablePrefixTrie) {
      checkNotNull(immutablePrefixTrie);

      putAll(immutablePrefixTrie.internalTrie.toMap());
      return this;
    }

    /** Builds and returns a new {@link ImmutablePrefixTrie}. */
    public ImmutablePrefixTrie<T> build() {
      needsCopy = true;
      return new ImmutablePrefixTrie<>(internal);
    }

    private void copyInternal() {
      Map<String, T> prefixMap = internal.toMap();

      internal = new PrefixTrie<>(first, last);

      for (Entry<String, T> e : prefixMap.entrySet()) {
        internal.put(e.getKey(), e.getValue());
      }
    }
  }
}
