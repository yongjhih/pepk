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
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.util.Map;

/**
 * Trie implementation supporting CharSequences as prefixes.
 *
 * <p>Prefixes are sequences of characters, and the set of allowed characters is
 * specified as a range of sequential characters. By default, any seven-bit
 * character may appear in a prefix, and so the trie is a 128-ary tree.
 *
 * @author Bob Lee
 * @author Matthew Harris
 */
@GoogleInternal // need to consolidate the various trie-related stuff
@GwtIncompatible
public class PrefixTrie<T> implements PrefixMap<T> {
  /*
   * The set of allowed characters in prefixes is given by a range of
   * consecutive characters. rangeOffset denotes the beginning of the range,
   * and rangeSize gives the number of characters in the range, which is used as
   * the number of children of each node.
   */
  private final char rangeOffset;
  private final int rangeSize;

  private final Node<T> root;

  /** Constructs a trie for holding strings of seven-bit characters. */
  public PrefixTrie() {
    this('\0', (char) 127);
  }

  /**
   * Constructs a trie for holding strings of characters. The set of characters
   * allowed in prefixes is given by the range [rangeOffset, lastCharInRange],
   * inclusive.
   */
  public PrefixTrie(char firstCharInRange, char lastCharInRange) {
    Preconditions.checkArgument(
        firstCharInRange <= lastCharInRange, "Char range must include some chars");
    this.rangeOffset = firstCharInRange;
    this.rangeSize = lastCharInRange - firstCharInRange + 1;

    root = new Node<T>("");
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if prefix contains a character outside the
   *     range of legal prefix characters.
   */
  @CanIgnoreReturnValue
  @Override
  public T put(CharSequence prefix, T value) {
    Preconditions.checkNotNull(value);
    return putInternal(prefix, value);
  }

  private T putInternal(CharSequence prefix, T value) {
    Node<T> current = root;
    int prefixLength = prefix.length();
    for (int i = 0;; i++) {
      int commonLength = Math.min(current.prefix.length(), prefixLength - i);
      String commonPrefix =
          Strings.commonPrefix(prefix.subSequence(i, i + commonLength), current.prefix);
      if (commonPrefix.length() != current.prefix.length()) {
        // Need to split the current Node.
        try {
          current.split(
              commonPrefix.length(),  // Where to split prefix
              current.prefix.charAt(commonPrefix.length()) - rangeOffset,
              rangeSize);
        } catch (ArrayIndexOutOfBoundsException e) {
          throw new IllegalArgumentException(
              "'" + current.prefix.charAt(commonPrefix.length())
              + "' is not a legal prefix character.");
        }
      }
      i += commonPrefix.length();
      if (i == prefixLength) {
        // The prefix argument has perfectly matched a node, return.
        break;
      }
      if (current.next == null) {
        // The prefix argument has perfectly match all the way down to a leaf node, but longer.
        @SuppressWarnings("unchecked")
        Node<T>[] next = new Node[rangeSize];
        current.next = next;
      }
      int nodeIndex = prefix.charAt(i) - rangeOffset;
      try {
        Node<T> next = current.next[nodeIndex];
        if (next == null) {
          current = current.next[nodeIndex] =
              new Node<>(prefix.subSequence(i + 1, prefixLength).toString());
          break;
        }
        current = next;
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new IllegalArgumentException(
            "'" + prefix.charAt(i) + "' is not a legal prefix character.");
      }
    }
    T oldValue = current.value;
    current.value = value;
    return oldValue;
  }

  @Override
  public T get(CharSequence s) {
    T deepestValue = null;
    Node<T> current = root;
    int sLength = s.length();
    for (int i = 0; current != null; i++) {
      String prefix = current.prefix;
      if (i + prefix.length() > sLength
          || !prefix.contentEquals(s.subSequence(i, i + prefix.length()))) {
        break;
      }
      i += prefix.length();
      if (current.value != null) {
        deepestValue = current.value;
      }
      if (i == sLength || current.next == null) {
        break;
      }
      int nodeIndex = s.charAt(i) - rangeOffset;
      if (nodeIndex < 0 || rangeSize <= nodeIndex) {
        return null;
      }
      current = current.next[nodeIndex];
    }
    return deepestValue;
  }

  /**
   * {@inheritDoc}
   *
   * @throws IllegalArgumentException if prefix contains a character outside the
   *     range of legal prefix characters.
   */
  @CanIgnoreReturnValue
  @Override
  public T remove(CharSequence prefix) {
    // TODO(skill): merge nodes on delete
    return putInternal(prefix, null);
  }

  /**
   * Returns a Map containing the same data as this structure.
   *
   * <p>This implementation constructs and populates an entirely new map rather
   * than providing a map view on the trie, so this is mostly useful for
   * debugging.
   *
   * @return a Map mapping each prefix to its corresponding value.
   */
  public Map<String, T> toMap() {
    Map<String, T> map = Maps.newLinkedHashMap();
    addEntries(root, new StringBuilder(), map);
    return map;
  }

  /**
   * Adds to the given map all entries at or below the given node.
   *
   * @param builder a StringBuilder containing the prefix for the given node.
   */
  private void addEntries(Node<T> node, StringBuilder builder, Map<String, T> map) {
    builder.append(node.prefix);

    if (node.value != null) {
      map.put(builder.toString(), node.value);
    }

    if (node.next != null) {
      for (int i = 0; i < node.next.length; i++) {
        Node<T> next = node.next[i];
        if (next != null) {
          builder.append((char) (i + rangeOffset));
          addEntries(next, builder, map);
          builder.deleteCharAt(builder.length() - 1);
        }
      }
    }

    builder.delete(builder.length() - node.prefix.length(), builder.length());
  }

  private static class Node<T> {
    String prefix;
    T value;
    Node<T>[] next;

    Node(String prefix) {
      this.prefix = prefix;
    }

    /**
     * Splits the current node at specified position in the prefix.
     *
     * @param atChar position in the prefix where split occurs
     * @param nextPos an index in Node array where the split character goes to
     * @param totalSize total size of node array
     */
    void split(int atChar, int nextPos, int totalSize) throws ArrayIndexOutOfBoundsException {
      @SuppressWarnings("unchecked")
      Node<T>[] newNext = new Node[totalSize];
      Node<T> newNode = newNext[nextPos] = new Node<>(prefix.substring(atChar + 1));
      newNode.value = value;
      newNode.next = next;
      prefix = prefix.substring(0, atChar);
      value = null;
      next = newNext;
    }
  }
}
