/*
 * Copyright (C) 2013 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.base;

import static com.google.common.base.Preconditions.checkPositionIndexes;
import static java.lang.Character.MAX_SURROGATE;
import static java.lang.Character.MIN_SURROGATE;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtCompatible;
import com.google.common.annotations.GwtIncompatible;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ReadOnlyBufferException;

/**
 * Low-level, high-performance utility methods related to the {@linkplain Charsets#UTF_8 UTF-8}
 * character encoding. UTF-8 is defined in section D92 of <a
 * href="http://www.unicode.org/versions/Unicode6.2.0/ch03.pdf">The Unicode Standard Core
 * Specification, Chapter 3</a>.
 *
 * <p>The variant of UTF-8 implemented by this class is the restricted definition of UTF-8
 * introduced in Unicode 3.1. One implication of this is that it rejects <a
 * href="http://www.unicode.org/versions/corrigendum1.html">"non-shortest form"</a> byte sequences,
 * even though the JDK decoder may accept them.
 *
 * @author Martin Buchholz
 * @author Clément Roux
 * @since 16.0
 */
@Beta
@GwtCompatible(emulated = true)
public final class Utf8 {
  /**
   * Returns the number of bytes in the UTF-8-encoded form of {@code sequence}. For a string, this
   * method is equivalent to {@code string.getBytes(UTF_8).length}, but is more efficient in both
   * time and space.
   *
   * @throws IllegalArgumentException if {@code sequence} contains ill-formed UTF-16 (unpaired
   *     surrogates)
   */
  public static int encodedLength(CharSequence sequence) {
    // Warning to maintainers: this implementation is highly optimized.
    // See http://go/utf8encodedlength-benchmark MOE:strip_line
    int utf16Length = sequence.length();
    int utf8Length = utf16Length;
    int i = 0;

    // This loop optimizes for pure ASCII.
    while (i < utf16Length && sequence.charAt(i) < 0x80) {
      i++;
    }

    // This loop optimizes for chars less than 0x800.
    for (; i < utf16Length; i++) {
      char c = sequence.charAt(i);
      if (c < 0x800) {
        utf8Length += ((0x7f - c) >>> 31); // branch free!
      } else {
        utf8Length += encodedLengthGeneral(sequence, i);
        break;
      }
    }

    if (utf8Length < utf16Length) {
      // Necessary and sufficient condition for overflow because of maximum 3x expansion
      throw new IllegalArgumentException(
          "UTF-8 length does not fit in int: " + (utf8Length + (1L << 32)));
    }
    return utf8Length;
  }

  private static int encodedLengthGeneral(CharSequence sequence, int start) {
    int utf16Length = sequence.length();
    int utf8Length = 0;
    for (int i = start; i < utf16Length; i++) {
      char c = sequence.charAt(i);
      if (c < 0x800) {
        utf8Length += (0x7f - c) >>> 31; // branch free!
      } else {
        utf8Length += 2;
        // jdk7+: if (Character.isSurrogate(c)) {
        if (MIN_SURROGATE <= c && c <= MAX_SURROGATE) {
          // Check that we have a well-formed surrogate pair.
          if (Character.codePointAt(sequence, i) == c) {
            throw new IllegalArgumentException(unpairedSurrogateMsg(i));
          }
          i++;
        }
      }
    }
    return utf8Length;
  }

  /**
   * Encodes {@code sequence} into UTF-8 bytes in {@code buffer}, starting at {@code buffer}'s
   * current position. For a string, this method is equivalent to {@code
   * buffer.put(string.getBytes(UTF_8))}, but is more efficient in both time and space. This method
   * requires paired surrogates, and therefore does not support chunking.
   *
   * <p>To ensure sufficient space in the output buffer, either call {@link #encodedLength} to
   * compute the exact amount needed, or leave room for {@code 3 * sequence.length()}, which is the
   * largest possible number of bytes that any input can be encoded to.
   *
   * @throws IllegalArgumentException if {@code sequence} contains ill-formed UTF-16 (unpaired
   *     surrogates)
   * @throws BufferOverflowException if {@code sequence} encoded in UTF-8 does not fit in {@code
   *     buffer}'s remaining space.
   * @throws ReadOnlyBufferException if {@code buffer} is a read-only buffer.
   */
  @GwtIncompatible // ByteBuffer
  @GoogleInternal
  public static void encode(CharSequence sequence, ByteBuffer buffer) {
    if (buffer.isReadOnly()) {
      throw new ReadOnlyBufferException();
    }
    if (buffer.hasArray()) {
      try {
        int encoded =
            encode(
                sequence,
                buffer.array(),
                buffer.arrayOffset() + buffer.position(),
                buffer.remaining());
        buffer.position(encoded - buffer.arrayOffset());
      } catch (ArrayIndexOutOfBoundsException e) {
        BufferOverflowException boe = new BufferOverflowException();
        boe.initCause(e);
        throw boe;
      }
    } else {
      encodeDirect(sequence, buffer);
    }
  }

  /**
   * Encodes {@code sequence} into UTF-8, in {@code bytes}. For a string, this method is equivalent
   * to
   *
   * <pre>{@code
   * byte[] a = string.getBytes(UTF_8);
   * System.arraycopy(a, 0, bytes, 0, a.length);
   * return a.length;
   * }</pre>
   *
   * but is more efficient in both time and space. This method requires paired surrogates, and
   * therefore does not support chunking.
   *
   * <p>To ensure sufficient space in the output buffer, either call {@link #encodedLength} to
   * compute the exact amount needed, or leave room for {@code 3 * sequence.length()}, which is the
   * largest possible number of bytes that any input can be encoded to.
   *
   * @throws IllegalArgumentException if {@code sequence} contains ill-formed UTF-16 (unpaired
   *     surrogates)
   * @throws ArrayIndexOutOfBoundsException if {@code sequence} encoded in UTF-8 is longer than
   *     {@code bytes.length}
   * @return the new offset, equivalent to {@code Utf8.encodedLength(sequence)}
   */
  @GoogleInternal
  @CanIgnoreReturnValue
  public static int encode(CharSequence sequence, byte[] bytes) {
    return encode(sequence, bytes, 0, bytes.length);
  }

  /**
   * Encodes {@code sequence} into UTF-8, in {@code bytes}. For a string, this method is equivalent
   * to
   *
   * <pre>{@code
   * byte[] a = string.getBytes(UTF_8);
   * System.arraycopy(a, 0, bytes, offset, a.length);
   * return offset + a.length;
   * }</pre>
   *
   * but is more efficient in both time and space. This method requires paired surrogates, and
   * therefore does not support chunking.
   *
   * <p>To ensure sufficient space in the output buffer, either call {@link #encodedLength} to
   * compute the exact amount needed, or leave room for {@code 3 * sequence.length()}, which is the
   * largest possible number of bytes that any input can be encoded to.
   *
   * @param offset the starting offset in {@code bytes} to start writing at
   * @throws IllegalArgumentException if {@code sequence} contains ill-formed UTF-16 (unpaired
   *     surrogates)
   * @throws ArrayIndexOutOfBoundsException if {@code sequence} encoded in UTF-8 is longer than
   *     {@code bytes.length - offset}
   * @return the new offset, equivalent to {@code offset + Utf8.encodedLength(sequence)}
   */
  @GoogleInternal
  @CanIgnoreReturnValue
  public static int encode(CharSequence sequence, byte[] bytes, int offset) {
    return encode(sequence, bytes, offset, bytes.length - offset);
  }

  @GoogleInternal
  private static int encode(CharSequence sequence, byte[] bytes, int offset, int length) {
    int utf16Length = sequence.length();
    int j = offset;
    int i = 0;
    int limit = offset + length;
    // Designed to take advantage of
    // https://wikis.oracle.com/display/HotSpotInternals/RangeCheckElimination
    for (char c; i < utf16Length && i + j < limit && (c = sequence.charAt(i)) < 0x80; i++) {
      bytes[j + i] = (byte) c;
    }
    if (i == utf16Length) {
      return j + utf16Length;
    }
    j += i;
    for (char c; i < utf16Length; i++) {
      c = sequence.charAt(i);
      if (c < 0x80 && j < limit) {
        bytes[j++] = (byte) c;
      } else if (c < 0x800 && j <= limit - 2) {
        bytes[j++] = (byte) ((0xF << 6) | (c >>> 6));
        bytes[j++] = (byte) (0x80 | (0x3F & c));
      } else if ((c < MIN_SURROGATE || MAX_SURROGATE < c) && j <= limit - 3) {
        bytes[j++] = (byte) ((0xF << 5) | (c >>> 12));
        bytes[j++] = (byte) (0x80 | (0x3F & (c >>> 6)));
        bytes[j++] = (byte) (0x80 | (0x3F & c));
      } else if (j <= limit - 4) {
        final int codePoint;
        if ((codePoint = Character.codePointAt(sequence, i)) == c) {
          throw new IllegalArgumentException(unpairedSurrogateMsg(i));
        }
        i++;
        bytes[j++] = (byte) ((0xF << 4) | (codePoint >>> 18));
        bytes[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 12)));
        bytes[j++] = (byte) (0x80 | (0x3F & (codePoint >>> 6)));
        bytes[j++] = (byte) (0x80 | (0x3F & codePoint));
      } else {
        // Prefer throwing (more useful) IllegalArgumentException to ArrayIndexOutOfBoundsException.
        if (unpairedSurrogateAt(sequence, i)) {
          throw new IllegalArgumentException(unpairedSurrogateMsg(i));
        }
        throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + j);
      }
    }
    return j;
  }

  @GwtIncompatible // ByteBuffer
  @GoogleInternal
  private static void encodeDirect(CharSequence sequence, ByteBuffer buffer) {
    int utf16Length = sequence.length();
    int i = 0;

    // This loop optimizes for pure ASCII.
    for (char c; i < utf16Length && (c = sequence.charAt(i)) < 0x80; i++) {
      buffer.put((byte) c);
    }

    // This loop optimizes for surrogate-free input.
    for (; i < utf16Length; i++) {
      final char c = sequence.charAt(i);
      if (c < 0x80) {
        buffer.put((byte) c);
      } else if (c < 0x800) {
        buffer.put((byte) ((0xF << 6) | (c >>> 6)));
        buffer.put((byte) (0x80 | (0x3F & c)));
      } else if (c < MIN_SURROGATE || MAX_SURROGATE < c) {
        buffer.put((byte) ((0xF << 5) | (c >>> 12)));
        buffer.put((byte) (0x80 | (0x3F & (c >>> 6))));
        buffer.put((byte) (0x80 | (0x3F & c)));
      } else {
        encodeDirectGeneral(sequence, buffer, i);
        return;
      }
    }
  }

  @GwtIncompatible // ByteBuffer
  @GoogleInternal
  private static void encodeDirectGeneral(CharSequence sequence, ByteBuffer buffer, int i) {
    int utf16Length = sequence.length();
    for (; i < utf16Length; i++) {
      final char c = sequence.charAt(i);
      if (c < 0x80) {
        buffer.put((byte) c);
      } else if (c < 0x800) {
        buffer.put((byte) ((0xF << 6) | (c >>> 6)));
        buffer.put((byte) (0x80 | (0x3F & c)));
      } else if (c < MIN_SURROGATE || MAX_SURROGATE < c) {
        buffer.put((byte) ((0xF << 5) | (c >>> 12)));
        buffer.put((byte) (0x80 | (0x3F & (c >>> 6))));
        buffer.put((byte) (0x80 | (0x3F & c)));
      } else { // c is a surrogate
        final int codePoint;
        if ((codePoint = Character.codePointAt(sequence, i)) == c) {
          throw new IllegalArgumentException(unpairedSurrogateMsg(i));
        }
        i++;
        buffer.put((byte) ((0xF << 4) | (codePoint >>> 18)));
        buffer.put((byte) (0x80 | (0x3F & (codePoint >>> 12))));
        buffer.put((byte) (0x80 | (0x3F & (codePoint >>> 6))));
        buffer.put((byte) (0x80 | (0x3F & codePoint)));
      }
    }
  }

  /**
   * Returns {@code true} if {@code bytes} is a <i>well-formed</i> UTF-8 byte sequence according to
   * Unicode 6.0. Note that this is a stronger criterion than simply whether the bytes can be
   * decoded. For example, some versions of the JDK decoder will accept "non-shortest form" byte
   * sequences, but encoding never reproduces these. Such byte sequences are <i>not</i> considered
   * well-formed.
   *
   * <p>This method returns {@code true} if and only if {@code Arrays.equals(bytes, new
   * String(bytes, UTF_8).getBytes(UTF_8))} does, but is more efficient in both time and space.
   */
  public static boolean isWellFormed(byte[] bytes) {
    return isWellFormed(bytes, 0, bytes.length);
  }

  /**
   * Returns whether the given byte array slice is a well-formed UTF-8 byte sequence, as defined by
   * {@link #isWellFormed(byte[])}. Note that this can be false even when {@code
   * isWellFormed(bytes)} is true.
   *
   * @param bytes the input buffer
   * @param off the offset in the buffer of the first byte to read
   * @param len the number of bytes to read from the buffer
   */
  public static boolean isWellFormed(byte[] bytes, int off, int len) {
    int end = off + len;
    checkPositionIndexes(off, end, bytes.length);
    // Look for the first non-ASCII character.
    for (int i = off; i < end; i++) {
      if (bytes[i] < 0) {
        return isWellFormedSlowPath(bytes, i, end);
      }
    }
    return true;
  }

  private static boolean isWellFormedSlowPath(byte[] bytes, int off, int end) {
    int index = off;
    while (true) {
      int byte1;

      // Optimize for interior runs of ASCII bytes.
      do {
        if (index >= end) {
          return true;
        }
      } while ((byte1 = bytes[index++]) >= 0);

      if (byte1 < (byte) 0xE0) {
        // Two-byte form.
        if (index == end) {
          return false;
        }
        // Simultaneously check for illegal trailing-byte in leading position
        // and overlong 2-byte form.
        if (byte1 < (byte) 0xC2 || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      } else if (byte1 < (byte) 0xF0) {
        // Three-byte form.
        if (index + 1 >= end) {
          return false;
        }
        int byte2 = bytes[index++];
        if (byte2 > (byte) 0xBF
            // Overlong? 5 most significant bits must not all be zero.
            || (byte1 == (byte) 0xE0 && byte2 < (byte) 0xA0)
            // Check for illegal surrogate codepoints.
            || (byte1 == (byte) 0xED && (byte) 0xA0 <= byte2)
            // Third byte trailing-byte test.
            || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      } else {
        // Four-byte form.
        if (index + 2 >= end) {
          return false;
        }
        int byte2 = bytes[index++];
        if (byte2 > (byte) 0xBF
            // Check that 1 <= plane <= 16. Tricky optimized form of:
            // if (byte1 > (byte) 0xF4
            //     || byte1 == (byte) 0xF0 && byte2 < (byte) 0x90
            //     || byte1 == (byte) 0xF4 && byte2 > (byte) 0x8F)
            || (((byte1 << 28) + (byte2 - (byte) 0x90)) >> 30) != 0
            // Third byte trailing-byte test
            || bytes[index++] > (byte) 0xBF
            // Fourth byte trailing-byte test
            || bytes[index++] > (byte) 0xBF) {
          return false;
        }
      }
    }
  }

  /**
   * Returns true if the char at position i is a surrogate that is not the start of a well-formed
   * surrogate pair.
   */
  @GoogleInternal
  private static boolean unpairedSurrogateAt(CharSequence sequence, int i) {
    int codePoint = Character.codePointAt(sequence, i);
    return MIN_SURROGATE <= codePoint && codePoint <= MAX_SURROGATE;
  }

  private static String unpairedSurrogateMsg(int i) {
    return "Unpaired surrogate at index " + i;
  }

  private Utf8() {}
}
