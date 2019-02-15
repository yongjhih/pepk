/*
 * Copyright (C) 2000 The Guava Authors
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

import static com.google.common.base.CharMatcher.whitespace;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.annotations.GoogleInternal;
import com.google.common.annotations.GwtIncompatible;
import com.google.common.annotations.VisibleForTesting;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Legacy utility methods relating to {@code String} or {@code CharSequence} instances.
 *
 * <p>Note that these utilities are old and unmaintained, and all are somewhere in the process of
 * being <a href="http://go/stringutil">moved or deleted</a>. Higher-quality String utilities are
 * found in other classes of this package including:
 *
 * <ul>
 *   <li>{@link Strings}
 *   <li>{@link CharMatcher}
 *   <li>{@link Joiner}
 *   <li>{@link Splitter}
 *   <li>{@link CaseFormat}
 *   <li>{@link Ascii}
 * </ul>
 */
@GoogleInternal // the good bits are being extracted to other files
@GwtIncompatible
public final class StringUtil {
  private StringUtil() {} // COV_NF_LINE

  /**
   * Returns {@code true} if the given string is null, empty, or comprises only whitespace
   * characters, as defined by {@link CharMatcher#whitespace()}.
   *
   * <p><b>Warning:</b> there are many competing definitions of "whitespace"; please see <a
   * href="http://go/white+space">this spreadsheet</a> for details.
   *
   * @param string the string reference to check
   * @return {@code true} if {@code string} is null, empty, or consists of whitespace characters
   *     only
   */
  public static boolean isEmptyOrWhitespace(@Nullable String string) {
    return string == null || whitespace().matchesAllOf(string);
  }

  /**
   * Returns the given string if it is nonempty and contains at least one non-whitespace character;
   * {@code null} otherwise. See comment in {@link #isEmptyOrWhitespace} on the definition of
   * whitespace.
   *
   * @param string the string to test and possibly return
   * @return {@code null} if {@code string} is null, empty, or contains only whitespace characters;
   *     {@code string} itself otherwise
   */
  @Nullable
  public static String toNullIfEmptyOrWhitespace(@Nullable String string) {
    return isEmptyOrWhitespace(string) ? null : string;
  }

  /*
   * -------------------------------------------------------------------
   * This marks the end of the code that has been written or rewritten
   * in 2008 to the quality standards of the Java core libraries group.
   * Code below this point is still awaiting cleanup (you can help!).
   * See http://wiki/Nonconf/JavaCoreLibrariesStandards.
   * -------------------------------------------------------------------
   */

  private static final Splitter NEWLINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();

  /**
   * Reformats the given string to a fixed width by inserting carriage returns and trimming
   * unnecessary whitespace. See {@link #fixedWidth(String[], int)} for details. The {@code str}
   * argument to this method will be split on newline characters ({@code '\n'}) only (regardless of
   * platform). An array of resulting non-empty strings is then passed to {@link
   * #fixedWidth(String[], int)} as the {@code lines} parameter.
   *
   * @param str the string to format
   * @param width the fixed width (in characters)
   */
  public static String fixedWidth(String str, int width) {
    List<String> lines = new ArrayList<>();

    for (String line : NEWLINE_SPLITTER.split(str)) {
      lines.add(line);
    }

    String[] lineArray = lines.toArray(new String[0]);
    return fixedWidth(lineArray, width);
  }

  /**
   * Reformats the given array of lines to a fixed width by inserting newlines and trimming
   * unnecessary whitespace. This uses simple whitespace-based splitting, not sophisticated
   * internationalized line breaking. Newlines within a line are treated like any other whitespace.
   * Lines which are already short enough will be passed through unmodified.
   *
   * <p>Only breaking whitespace characters (those which match {@link
   * CharMatcher#breakingWhitespace()}) are treated as whitespace by this method. Non-breaking
   * whitespace characters will be considered as ordinary characters which are connected to any
   * other adjacent non-whitespace characters, and will therefore appear in the returned string in
   * their original context.
   *
   * @param lines array of lines to format
   * @param width the fixed width (in characters)
   */
  @VisibleForTesting
  static String fixedWidth(String[] lines, int width) {
    List<String> formattedLines = new ArrayList<>();

    for (String line : lines) {
      formattedLines.add(formatLineToFixedWidth(line, width));
    }

    return Joiner.on('\n').join(formattedLines);
  }

  private static final Splitter TO_WORDS =
      Splitter.on(CharMatcher.breakingWhitespace()).omitEmptyStrings();

  /** Helper method for {@link #fixedWidth(String[], int)} */
  private static String formatLineToFixedWidth(String line, int width) {
    if (line.length() <= width) {
      return line;
    }

    StringBuilder builder = new StringBuilder();
    int col = 0;

    for (String word : TO_WORDS.split(line)) {
      if (col == 0) {
        col = word.length();
      } else {
        int newCol = col + word.length() + 1; // +1 for the space

        if (newCol <= width) {
          builder.append(' ');
          col = newCol;
        } else {
          builder.append('\n');
          col = word.length();
        }
      }

      builder.append(word);
    }

    return builder.toString();
  }

  /**
   * Indents the given String per line.
   *
   * @param iString the string to indent
   * @param iIndentDepth the depth of the indentation
   * @return the indented string
   */
  public static String indent(String iString, int iIndentDepth) {
    StringBuilder spacer = new StringBuilder();
    spacer.append("\n");
    for (int i = 0; i < iIndentDepth; i++) {
      spacer.append("  ");
    }
    return iString.replace("\n", spacer.toString());
  }

  /**
   * Give me a string and a potential prefix, and I return the string following the prefix if the
   * prefix matches, else null. Analogous to the c++ functions strprefix and var_strprefix.
   *
   * @param str the string to strip
   * @param prefix the expected prefix
   * @return the stripped string or <code>null</code> if the string does not start with the prefix
   */
  @Nullable
  public static String stripPrefix(String str, String prefix) {
    return str.startsWith(prefix) ? str.substring(prefix.length()) : null;
  }

  /**
   * Case insensitive version of stripPrefix. Strings are compared in the same way as in {@link
   * String#equalsIgnoreCase}. Analogous to the c++ functions strcaseprefix and var_strcaseprefix.
   *
   * @param str the string to strip
   * @param prefix the expected prefix
   * @return the stripped string or <code>null</code> if the string does not start with the prefix
   */
  @Nullable
  public static String stripPrefixIgnoreCase(String str, String prefix) {
    return startsWithIgnoreCase(str, prefix) ? str.substring(prefix.length()) : null;
  }

  /**
   * Give me a string and a potential suffix, and I return the string before the suffix if the
   * suffix matches, else null. Analogous to the c++ function strsuffix.
   *
   * @param str the string to strip
   * @param suffix the expected suffix
   * @return the stripped string or <code>null</code> if the string does not end with the suffix
   */
  @Nullable
  public static String stripSuffix(String str, String suffix) {
    return str.endsWith(suffix) ? str.substring(0, str.length() - suffix.length()) : null;
  }

  /**
   * Case insensitive version of stripSuffix. Strings are compared in the same way as in {@link
   * String#equalsIgnoreCase}. Analogous to the c++ function strcasesuffix.
   *
   * @param str the string to strip
   * @param suffix the expected suffix
   * @return the stripped string or <code>null</code> if the string does not end with the suffix
   */
  @Nullable
  public static String stripSuffixIgnoreCase(String str, String suffix) {
    return endsWithIgnoreCase(str, suffix)
        ? str.substring(0, str.length() - suffix.length())
        : null;
  }

  // See http://www.microsoft.com/typography/unicode/1252.htm
  private static final CharMatcher FANCY_SINGLE_QUOTE =
      CharMatcher.anyOf("\u0091\u0092\u2018\u2019");
  private static final CharMatcher FANCY_DOUBLE_QUOTE =
      CharMatcher.anyOf("\u0093\u0094\u201c\u201d");

  /** Replaces microsoft "smart quotes" (curly " and ') with their ascii counterparts. */
  public static String replaceSmartQuotes(String str) {
    String tmp = FANCY_SINGLE_QUOTE.replaceFrom(str, '\'');
    return FANCY_DOUBLE_QUOTE.replaceFrom(tmp, '"');
  }

  /**
   * Convert a string of hex digits to a byte array, with the first byte in the array being the MSB.
   * The string passed in should be just the raw digits (upper or lower case), with no leading or
   * trailing characters (like '0x' or 'h'). An odd number of characters is supported. If the string
   * is empty, an empty array will be returned.
   *
   * <p>This is significantly faster than using new BigInteger(str, 16).toByteArray(); especially
   * with larger strings. See the micro benchmark included with the tests.
   *
   * @deprecated Use {@link com.google.common.io.BaseEncoding}{@code
   *     .base16().decode(toUpperCase(str))}, but if your input has an odd number of characters, you
   *     will need to append {@code "0"} to it first.
   */
  @Deprecated
  public static byte[] hexToBytes(CharSequence str) {
    byte[] bytes = new byte[(str.length() + 1) / 2];
    if (str.length() == 0) {
      return bytes;
    }
    bytes[0] = 0;
    int nibbleIdx = (str.length() % 2);
    for (int i = 0; i < str.length(); i++) {
      char c = str.charAt(i);
      if ((nibbleIdx % 2) == 0) {
        bytes[nibbleIdx >> 1] = (byte) (hexValue(c) << 4);
      } else {
        bytes[nibbleIdx >> 1] += (byte) hexValue(c);
      }
      nibbleIdx++;
    }
    return bytes;
  }

  /** Converts any instances of "\r" or "\r\n" style EOLs into "\n" (Line Feed). */
  public static String convertEOLToLF(String input) {
    StringBuilder res = new StringBuilder(input.length());
    char[] s = input.toCharArray();
    int from = 0;
    final int end = s.length;
    for (int i = 0; i < end; i++) {
      if (s[i] == '\r') {
        res.append(s, from, i - from);
        res.append('\n');
        if (i + 1 < end && s[i + 1] == '\n') {
          i++;
        }

        from = i + 1;
      }
    }

    if (from == 0) { // no \r!
      return input;
    }

    res.append(s, from, end - from);
    return res.toString();
  }

  /**
   * Returns a string consisting of "s", with each of the first "len" characters replaced by
   * "maskChar" character.
   */
  @Nullable
  public static String maskLeft(String s, int len, char maskChar) {
    if (len <= 0) {
      return s; // TODO(kak): return checkNotNull(s)
    }
    len = Math.min(len, s.length());
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < len; i++) {
      sb.append(maskChar);
    }
    sb.append(s, len, s.length());
    return sb.toString();
  }

  private static boolean isOctal(char c) {
    return (c >= '0') && (c <= '7');
  }

  private static boolean isHex(char c) {
    return ((c >= '0') && (c <= '9')) || ((c >= 'a') && (c <= 'f')) || ((c >= 'A') && (c <= 'F'));
  }

  private static int hexValue(char c) {
    if ((c >= '0') && (c <= '9')) {
      return (c - '0');
    } else if ((c >= 'a') && (c <= 'f')) {
      return (c - 'a') + 10;
    } else if ((c >= 'A') && (c <= 'F')) {
      return (c - 'A') + 10;
    } else {
      throw new IllegalArgumentException("char is not a hex char");
    }
  }

  /** Unescape any C escape sequences (\n, \r, \\, \ooo, etc) and return the resulting string. */
  public static String unescapeCString(String s) {
    if (s.indexOf('\\') < 0) {
      // Fast path: nothing to unescape
      return s;
    }

    StringBuilder sb = new StringBuilder();
    int len = s.length();
    for (int i = 0; i < len; ) {
      char c = s.charAt(i++);
      if (c == '\\' && (i < len)) {
        c = s.charAt(i++);
        switch (c) {
          case 'a':
            c = '\007';
            break;
          case 'b':
            c = '\b';
            break;
          case 'f':
            c = '\f';
            break;
          case 'n':
            c = '\n';
            break;
          case 'r':
            c = '\r';
            break;
          case 't':
            c = '\t';
            break;
          case 'v':
            c = '\013';
            break;
          case '\\':
            c = '\\';
            break;
          case '?':
            c = '?';
            break;
          case '\'':
            c = '\'';
            break;
          case '"':
            c = '\"';
            break;

          default:
            if ((c == 'x') && (i < len) && isHex(s.charAt(i))) {
              // "\xXX"
              int v = hexValue(s.charAt(i++));
              if ((i < len) && isHex(s.charAt(i))) {
                v = v * 16 + hexValue(s.charAt(i++));
              }
              c = (char) v;
            } else if (isOctal(c)) {
              // "\OOO"
              int v = (c - '0');
              if ((i < len) && isOctal(s.charAt(i))) {
                v = v * 8 + (s.charAt(i++) - '0');
              }
              if ((i < len) && isOctal(s.charAt(i))) {
                v = v * 8 + (s.charAt(i++) - '0');
              }
              c = (char) v;
            } else {
              // Propagate unknown escape sequences.
              sb.append('\\');
            }
            break;
        }
      }
      sb.append(c);
    }
    return sb.toString();
  }

  // TODO(pbarry): move all HTML methods to common.html package

  private static final Map<String, Character> ESCAPE_STRINGS;

  static {
    // HTML character entity references as defined in HTML 4
    // see http://www.w3.org/TR/REC-html40/sgml/entities.html
    Map<String, Character> map = new HashMap<>(252);

    map.put("&nbsp", '\u00A0');
    map.put("&iexcl", '\u00A1');
    map.put("&cent", '\u00A2');
    map.put("&pound", '\u00A3');
    map.put("&curren", '\u00A4');
    map.put("&yen", '\u00A5');
    map.put("&brvbar", '\u00A6');
    map.put("&sect", '\u00A7');
    map.put("&uml", '\u00A8');
    map.put("&copy", '\u00A9');
    map.put("&ordf", '\u00AA');
    map.put("&laquo", '\u00AB');
    map.put("&not", '\u00AC');
    map.put("&shy", '\u00AD');
    map.put("&reg", '\u00AE');
    map.put("&macr", '\u00AF');
    map.put("&deg", '\u00B0');
    map.put("&plusmn", '\u00B1');
    map.put("&sup2", '\u00B2');
    map.put("&sup3", '\u00B3');
    map.put("&acute", '\u00B4');
    map.put("&micro", '\u00B5');
    map.put("&para", '\u00B6');
    map.put("&middot", '\u00B7');
    map.put("&cedil", '\u00B8');
    map.put("&sup1", '\u00B9');
    map.put("&ordm", '\u00BA');
    map.put("&raquo", '\u00BB');
    map.put("&frac14", '\u00BC');
    map.put("&frac12", '\u00BD');
    map.put("&frac34", '\u00BE');
    map.put("&iquest", '\u00BF');
    map.put("&Agrave", '\u00C0');
    map.put("&Aacute", '\u00C1');
    map.put("&Acirc", '\u00C2');
    map.put("&Atilde", '\u00C3');
    map.put("&Auml", '\u00C4');
    map.put("&Aring", '\u00C5');
    map.put("&AElig", '\u00C6');
    map.put("&Ccedil", '\u00C7');
    map.put("&Egrave", '\u00C8');
    map.put("&Eacute", '\u00C9');
    map.put("&Ecirc", '\u00CA');
    map.put("&Euml", '\u00CB');
    map.put("&Igrave", '\u00CC');
    map.put("&Iacute", '\u00CD');
    map.put("&Icirc", '\u00CE');
    map.put("&Iuml", '\u00CF');
    map.put("&ETH", '\u00D0');
    map.put("&Ntilde", '\u00D1');
    map.put("&Ograve", '\u00D2');
    map.put("&Oacute", '\u00D3');
    map.put("&Ocirc", '\u00D4');
    map.put("&Otilde", '\u00D5');
    map.put("&Ouml", '\u00D6');
    map.put("&times", '\u00D7');
    map.put("&Oslash", '\u00D8');
    map.put("&Ugrave", '\u00D9');
    map.put("&Uacute", '\u00DA');
    map.put("&Ucirc", '\u00DB');
    map.put("&Uuml", '\u00DC');
    map.put("&Yacute", '\u00DD');
    map.put("&THORN", '\u00DE');
    map.put("&szlig", '\u00DF');
    map.put("&agrave", '\u00E0');
    map.put("&aacute", '\u00E1');
    map.put("&acirc", '\u00E2');
    map.put("&atilde", '\u00E3');
    map.put("&auml", '\u00E4');
    map.put("&aring", '\u00E5');
    map.put("&aelig", '\u00E6');
    map.put("&ccedil", '\u00E7');
    map.put("&egrave", '\u00E8');
    map.put("&eacute", '\u00E9');
    map.put("&ecirc", '\u00EA');
    map.put("&euml", '\u00EB');
    map.put("&igrave", '\u00EC');
    map.put("&iacute", '\u00ED');
    map.put("&icirc", '\u00EE');
    map.put("&iuml", '\u00EF');
    map.put("&eth", '\u00F0');
    map.put("&ntilde", '\u00F1');
    map.put("&ograve", '\u00F2');
    map.put("&oacute", '\u00F3');
    map.put("&ocirc", '\u00F4');
    map.put("&otilde", '\u00F5');
    map.put("&ouml", '\u00F6');
    map.put("&divide", '\u00F7');
    map.put("&oslash", '\u00F8');
    map.put("&ugrave", '\u00F9');
    map.put("&uacute", '\u00FA');
    map.put("&ucirc", '\u00FB');
    map.put("&uuml", '\u00FC');
    map.put("&yacute", '\u00FD');
    map.put("&thorn", '\u00FE');
    map.put("&yuml", '\u00FF');
    map.put("&fnof", '\u0192');
    map.put("&Alpha", '\u0391');
    map.put("&Beta", '\u0392');
    map.put("&Gamma", '\u0393');
    map.put("&Delta", '\u0394');
    map.put("&Epsilon", '\u0395');
    map.put("&Zeta", '\u0396');
    map.put("&Eta", '\u0397');
    map.put("&Theta", '\u0398');
    map.put("&Iota", '\u0399');
    map.put("&Kappa", '\u039A');
    map.put("&Lambda", '\u039B');
    map.put("&Mu", '\u039C');
    map.put("&Nu", '\u039D');
    map.put("&Xi", '\u039E');
    map.put("&Omicron", '\u039F');
    map.put("&Pi", '\u03A0');
    map.put("&Rho", '\u03A1');
    map.put("&Sigma", '\u03A3');
    map.put("&Tau", '\u03A4');
    map.put("&Upsilon", '\u03A5');
    map.put("&Phi", '\u03A6');
    map.put("&Chi", '\u03A7');
    map.put("&Psi", '\u03A8');
    map.put("&Omega", '\u03A9');
    map.put("&alpha", '\u03B1');
    map.put("&beta", '\u03B2');
    map.put("&gamma", '\u03B3');
    map.put("&delta", '\u03B4');
    map.put("&epsilon", '\u03B5');
    map.put("&zeta", '\u03B6');
    map.put("&eta", '\u03B7');
    map.put("&theta", '\u03B8');
    map.put("&iota", '\u03B9');
    map.put("&kappa", '\u03BA');
    map.put("&lambda", '\u03BB');
    map.put("&mu", '\u03BC');
    map.put("&nu", '\u03BD');
    map.put("&xi", '\u03BE');
    map.put("&omicron", '\u03BF');
    map.put("&pi", '\u03C0');
    map.put("&rho", '\u03C1');
    map.put("&sigmaf", '\u03C2');
    map.put("&sigma", '\u03C3');
    map.put("&tau", '\u03C4');
    map.put("&upsilon", '\u03C5');
    map.put("&phi", '\u03C6');
    map.put("&chi", '\u03C7');
    map.put("&psi", '\u03C8');
    map.put("&omega", '\u03C9');
    map.put("&thetasym", '\u03D1');
    map.put("&upsih", '\u03D2');
    map.put("&piv", '\u03D6');
    map.put("&bull", '\u2022');
    map.put("&hellip", '\u2026');
    map.put("&prime", '\u2032');
    map.put("&Prime", '\u2033');
    map.put("&oline", '\u203E');
    map.put("&frasl", '\u2044');
    map.put("&weierp", '\u2118');
    map.put("&image", '\u2111');
    map.put("&real", '\u211C');
    map.put("&trade", '\u2122');
    map.put("&alefsym", '\u2135');
    map.put("&larr", '\u2190');
    map.put("&uarr", '\u2191');
    map.put("&rarr", '\u2192');
    map.put("&darr", '\u2193');
    map.put("&harr", '\u2194');
    map.put("&crarr", '\u21B5');
    map.put("&lArr", '\u21D0');
    map.put("&uArr", '\u21D1');
    map.put("&rArr", '\u21D2');
    map.put("&dArr", '\u21D3');
    map.put("&hArr", '\u21D4');
    map.put("&forall", '\u2200');
    map.put("&part", '\u2202');
    map.put("&exist", '\u2203');
    map.put("&empty", '\u2205');
    map.put("&nabla", '\u2207');
    map.put("&isin", '\u2208');
    map.put("&notin", '\u2209');
    map.put("&ni", '\u220B');
    map.put("&prod", '\u220F');
    map.put("&sum", '\u2211');
    map.put("&minus", '\u2212');
    map.put("&lowast", '\u2217');
    map.put("&radic", '\u221A');
    map.put("&prop", '\u221D');
    map.put("&infin", '\u221E');
    map.put("&ang", '\u2220');
    map.put("&and", '\u2227');
    map.put("&or", '\u2228');
    map.put("&cap", '\u2229');
    map.put("&cup", '\u222A');
    map.put("&int", '\u222B');
    map.put("&there4", '\u2234');
    map.put("&sim", '\u223C');
    map.put("&cong", '\u2245');
    map.put("&asymp", '\u2248');
    map.put("&ne", '\u2260');
    map.put("&equiv", '\u2261');
    map.put("&le", '\u2264');
    map.put("&ge", '\u2265');
    map.put("&sub", '\u2282');
    map.put("&sup", '\u2283');
    map.put("&nsub", '\u2284');
    map.put("&sube", '\u2286');
    map.put("&supe", '\u2287');
    map.put("&oplus", '\u2295');
    map.put("&otimes", '\u2297');
    map.put("&perp", '\u22A5');
    map.put("&sdot", '\u22C5');
    map.put("&lceil", '\u2308');
    map.put("&rceil", '\u2309');
    map.put("&lfloor", '\u230A');
    map.put("&rfloor", '\u230B');
    map.put("&lang", '\u2329');
    map.put("&rang", '\u232A');
    map.put("&loz", '\u25CA');
    map.put("&spades", '\u2660');
    map.put("&clubs", '\u2663');
    map.put("&hearts", '\u2665');
    map.put("&diams", '\u2666');
    map.put("&quot", '\u0022');
    map.put("&amp", '\u0026');
    map.put("&lt", '\u003C');
    map.put("&gt", '\u003E');
    map.put("&apos", '\'');
    map.put("&OElig", '\u0152');
    map.put("&oelig", '\u0153');
    map.put("&Scaron", '\u0160');
    map.put("&scaron", '\u0161');
    map.put("&Yuml", '\u0178');
    map.put("&circ", '\u02C6');
    map.put("&tilde", '\u02DC');
    map.put("&ensp", '\u2002');
    map.put("&emsp", '\u2003');
    map.put("&thinsp", '\u2009');
    map.put("&zwnj", '\u200C');
    map.put("&zwj", '\u200D');
    map.put("&lrm", '\u200E');
    map.put("&rlm", '\u200F');
    map.put("&ndash", '\u2013');
    map.put("&mdash", '\u2014');
    map.put("&lsquo", '\u2018');
    map.put("&rsquo", '\u2019');
    map.put("&sbquo", '\u201A');
    map.put("&ldquo", '\u201C');
    map.put("&rdquo", '\u201D');
    map.put("&bdquo", '\u201E');
    map.put("&dagger", '\u2020');
    map.put("&Dagger", '\u2021');
    map.put("&permil", '\u2030');
    map.put("&lsaquo", '\u2039');
    map.put("&rsaquo", '\u203A');
    map.put("&euro", '\u20AC');

    ESCAPE_STRINGS = Collections.unmodifiableMap(map);
  }

  private static final CharMatcher HEX_LETTER =
      CharMatcher.inRange('A', 'F').or(CharMatcher.inRange('a', 'f'));

  /**
   * Replace all the occurences of HTML escape strings with the respective characters.
   *
   * <p>The default mode is strict (requiring semicolons).
   *
   * @param s a <code>String</code> value
   * @return a <code>String</code> value
   * @throws NullPointerException if the input string is null.
   * @deprecated Use a full HTML parser, noting that its behavior may differ in some cases. See <a
   *     href="https://goto.google.com/stringutil-html-deprecation"
   *     >go/stringutil-html-deprecation</a> for details.
   */
  @Deprecated
  public static String unescapeHTML(String s) {
    return unescapeHTML(s, false);
  }

  /**
   * Replace all the occurences of HTML escape strings with the respective characters.
   *
   * @param s a <code>String</code> value
   * @param emulateBrowsers a <code>Boolean</code> value that tells the method to allow entity refs
   *     not terminated with a semicolon to be unescaped. (a quirk of this feature, and some
   *     browsers, is that an explicit terminating character is needed - e.g., {@code &lt$} would be
   *     unescaped, but not {@code &ltab} - see the tests for a more in-depth description of
   *     browsers)
   * @return a <code>String</code> value
   * @throws NullPointerException if the input string is null.
   * @deprecated Use a full HTML parser, noting that its behavior may differ in some cases. See <a
   *     href="https://goto.google.com/stringutil-html-deprecation"
   *     >go/stringutil-html-deprecation</a> for details.
   */
  @Deprecated
  public static String unescapeHTML(String s, boolean emulateBrowsers) {

    // See if there are any '&' in the string since that is what we look
    // for to escape. If there isn't, then we don't need to escape this string
    // Based on similar technique used in the escape function.
    int index = s.indexOf('&');
    if (index == -1) {
      // Nothing to escape. Return the original string.
      return s;
    }

    // We found an escaped character. Start slow escaping from there.
    char[] chars = s.toCharArray();
    char[] escaped = new char[chars.length];
    System.arraycopy(chars, 0, escaped, 0, index);

    // Note: escaped[pos] = end of the escaped char array.
    int pos = index;

    for (int i = index; i < chars.length; ) {
      if (chars[i] != '&') {
        escaped[pos++] = chars[i++];
        continue;
      }

      // Allow e.g. &#123;
      int j = i + 1;
      boolean isNumericEntity = false;
      if (j < chars.length && chars[j] == '#') {
        j++;
        isNumericEntity = true;
      }

      // if it's numeric, also check for hex
      boolean isHexEntity = false;
      if (j < chars.length && (chars[j] == 'x' || chars[j] == 'X')) {
        j++;
        isHexEntity = true;
      }

      // Scan until we find a char that is not valid for this sequence.
      for (; j < chars.length; j++) {
        char ch = chars[j];
        boolean isDigit = Character.isDigit(ch);
        if (isNumericEntity) {
          // non-hex numeric sequence end condition
          if (!isHexEntity && !isDigit) {
            break;
          }
          // hex sequence end contition
          if (isHexEntity && !isDigit && !HEX_LETTER.matches(ch)) {
            break;
          }
        }
        // anything other than a digit or letter is always an end condition
        if (!isDigit && !Character.isLetter(ch)) {
          break;
        }
      }

      boolean replaced = false;
      if ((j <= chars.length && emulateBrowsers) || (j < chars.length && chars[j] == ';')) {
        // Check for &#D; and &#xD; pattern
        if (i + 2 < chars.length && s.charAt(i + 1) == '#') {
          try {
            long charcode = 0;
            char ch = s.charAt(i + 2);
            if (isHexEntity) {
              charcode = Long.parseLong(new String(chars, i + 3, j - i - 3), 16);
            } else if (Character.isDigit(ch)) {
              charcode = Long.parseLong(new String(chars, i + 2, j - i - 2));
            }
            if (charcode > 0 && charcode < 65536) {
              escaped[pos++] = (char) charcode;
              replaced = true;
            }
          } catch (NumberFormatException ex) {
            // Failed, not replaced.
          }
        } else {
          String key = new String(chars, i, j - i);
          Character repl = ESCAPE_STRINGS.get(key);
          if (repl != null) {
            escaped[pos++] = repl;
            replaced = true;
          }
        }
        // Skip over ';'
        if (j < chars.length && chars[j] == ';') {
          j++;
        }
      }

      if (!replaced) {
        // Not a recognized escape sequence, leave as-is
        System.arraycopy(chars, i, escaped, pos, j - i);
        pos += j - i;
      }
      i = j;
    }
    return new String(escaped, 0, pos);
  }

  // Escaper for < and > only.
  private static final CharEscaper LT_GT_ESCAPE =
      new CharEscaperBuilder().addEscape('<', "&lt;").addEscape('>', "&gt;").toEscaper();

  private static final Pattern HTML_TAG_PATTERN = Pattern.compile("</?[a-zA-Z][^>]*>");

  /**
   * Given a <code>String</code>, returns an equivalent <code>String</code> with all HTML tags
   * stripped. Note that HTML entities, such as "&amp;amp;" will still be preserved.
   *
   * @deprecated Use a full HTML parser, noting that it <b>will</b> decode HTML entities and that
   *     its behavior may differ in additional cases. See <a
   *     href="https://goto.google.com/stringutil-html-deprecation"
   *     >go/stringutil-html-deprecation</a> for details.
   */
  @Deprecated
  @Nullable
  public static String stripHtmlTags(@Nullable String string) {
    /*
     * TODO(cpovirk): remove CharEscaper and CharEscaperBuilder when deleting
     * this
     */
    if ((string == null) || "".equals(string)) {
      return string;
    }
    String stripped = HTML_TAG_PATTERN.matcher(string).replaceAll("");
    /*
     * Certain inputs result in a well-formed HTML:
     * <<X>script>alert(0)<</X>/script> results in <script>alert(0)</script>
     * The following step ensures that no HTML can slip through by replacing all
     * < and > characters with &lt; and &gt; after HTML tags were stripped.
     */
    return LT_GT_ESCAPE.escape(stripped);
  }

  /**
   * We escape some characters in s to be able to insert strings into JavaScript code. Also, make
   * sure that we don't write out {@code -->} or {@code </script>}, which may close a script tag, or
   * any char in {@code ["'>]} which might close a tag or attribute if seen inside an attribute.
   */
  public static String javaScriptEscape(CharSequence s) {
    return javaScriptEscapeHelper(s, false);
  }

  /**
   * We escape some characters in s to be able to insert strings into JavaScript code. Also, make
   * sure that we don't write out {@code -->} or {@code </script>}, which may close a script tag, or
   * any char in {@code ["'>]} which might close a tag or attribute if seen inside an attribute.
   * Turns all non-ascii characters into ASCII javascript escape sequences (eg \\uhhhh).
   */
  public static String javaScriptEscapeToAscii(CharSequence s) {
    return javaScriptEscapeHelper(s, true);
  }

  /**
   * Represents the type of javascript escaping to perform. Each enum below determines whether to
   * use hex escapes and how to handle quotes.
   */
  public static enum JsEscapingMode {
    /** No hex escapes, pass-through ', and escape " as \". */
    JSON,

    /** Hex escapes, escapes ' and " to \42 and \47, respectively. */
    EMBEDDABLE_JS,

    /** Hex escapes, escapes ' and " to \' and \". */
    MINIMAL_JS
  }

  /** Helper for javaScriptEscape and javaScriptEscapeToAscii */
  private static String javaScriptEscapeHelper(CharSequence s, boolean escapeToAscii) {
    StringBuilder sb = new StringBuilder(s.length() * 9 / 8);
    try {
      escapeStringBody(s, escapeToAscii, JsEscapingMode.EMBEDDABLE_JS, sb);
    } catch (IOException ex) {
      // StringBuilder.append does not throw IOExceptions.
      throw new RuntimeException(ex);
    }
    return sb.toString();
  }

  /**
   * Appends the javascript string literal equivalent of plainText to the given out buffer.
   *
   * @param plainText the string to escape.
   * @param escapeToAscii true to encode all characters not in ascii [\x20-\x7e] <br>
   *     Full escaping of unicode entites isn't required but this makes sure that unicode strings
   *     will survive regardless of the content-encoding of the javascript file which is important
   *     when we use this function to autogenerated javascript source files. This is disabled by
   *     default because it makes non-latin strings very long. <br>
   *     If you seem to have trouble with character-encodings, maybe turn this on to see if the
   *     problem goes away. If so, you need to specify a character encoding for your javascript
   *     somewhere.
   * @param jsEscapingMode determines the type of escaping to perform.
   * @param out the buffer to append output to.
   */
  /*
   * To avoid fallthrough, we would have to either use a hybrid switch-case/if
   * approach (which would obscure our special handling for ' and "), duplicate
   * the content of the default case, or pass a half-dozen parameters to a
   * helper method containing the code from the default case.
   */
  public static void escapeStringBody(
      CharSequence plainText, boolean escapeToAscii, JsEscapingMode jsEscapingMode, Appendable out)
      throws IOException {
    int pos = 0; // Index just past the last char in plainText written to out.
    int len = plainText.length();
    checkNotNull(jsEscapingMode);
    for (int codePoint, charCount, i = 0; i < len; i += charCount) {
      codePoint = Character.codePointAt(plainText, i);
      charCount = Character.charCount(codePoint);

      if (!shouldEscapeChar(codePoint, escapeToAscii, jsEscapingMode)) {
        continue;
      }

      out.append(plainText, pos, i);
      pos = i + charCount;
      switch (codePoint) {
        case '\b':
          out.append("\\b");
          break;
        case '\t':
          out.append("\\t");
          break;
        case '\n':
          out.append("\\n");
          break;
        case '\f':
          out.append("\\f");
          break;
        case '\r':
          out.append("\\r");
          break;
        case '\\':
          out.append("\\\\");
          break;
        case '"':
        case '\'':
          if (jsEscapingMode == JsEscapingMode.JSON && '\'' == codePoint) {
            // JSON does not escape a single quote (and it should be surrounded
            // by double quotes).
            out.append((char) codePoint);
            break;
          } else if (jsEscapingMode != JsEscapingMode.EMBEDDABLE_JS) {
            out.append('\\').append((char) codePoint);
            break;
          }
          // fall through
        default:
          if (codePoint >= 0x100 || jsEscapingMode == JsEscapingMode.JSON) {
            appendUnicodeJavaScriptRepresentation(codePoint, out);
          } else {
            // Output the minimal hex encoding.
            appendHexJavaScriptRepresentation((char) codePoint, out);
          }
          break;
      }
    }
    out.append(plainText, pos, len);
  }

  /** Helper for escapeStringBody, which decides whether to escape a character. */
  private static boolean shouldEscapeChar(
      int codePoint, boolean escapeToAscii, JsEscapingMode jsEscapingMode) {
    // If non-ASCII chars should be escaped, identify non-ASCII code points.
    if (escapeToAscii && (codePoint < 0x20 || codePoint > 0x7e)) {
      return true;
    }

    // If in JSON escaping mode, check JSON *and* JS escaping rules. The JS
    // escaping rules will escape more characters than needed for JSON,
    // but it is safe to escape any character in JSON.
    // TODO(bbavar): Remove unnecessary escaping for JSON, as long as it can be
    // shown that this change in legacy behavior is safe.
    if (jsEscapingMode == JsEscapingMode.JSON) {
      return mustEscapeCharInJsonOrJsString(codePoint);
    }

    // Finally, just check the default JS escaping rules.
    return mustEscapeCharInJsString(codePoint);
  }

  /**
   * Returns a javascript representation of the character in a hex escaped format.
   *
   * @param codePoint The codepoint to append.
   * @param out The buffer to which the hex representation should be appended.
   */
  private static void appendHexJavaScriptRepresentation(char ch, Appendable out)
      throws IOException {
    out.append("\\x").append(hexChars[(ch >>> 4) & 0xf]).append(hexChars[ch & 0xf]);
  }

  /**
   * Returns a javascript representation of the character in a hex escaped format.
   *
   * @param codePoint The codepoint to append.
   * @param out The buffer to which the hex representation should be appended.
   */
  private static void appendUnicodeJavaScriptRepresentation(int codePoint, Appendable out)
      throws IOException {
    if (Character.isSupplementaryCodePoint(codePoint)) {
      // Handle supplementary unicode values which are not representable in
      // javascript.  We deal with these by escaping them as two 4B sequences
      // so that they will round-trip properly when sent from java to javascript
      // and back.
      char[] surrogates = Character.toChars(codePoint);
      appendUnicodeJavaScriptRepresentation(surrogates[0], out);
      appendUnicodeJavaScriptRepresentation(surrogates[1], out);
      return;
    }
    out.append("\\u")
        .append(hexChars[(codePoint >>> 12) & 0xf])
        .append(hexChars[(codePoint >>> 8) & 0xf])
        .append(hexChars[(codePoint >>> 4) & 0xf])
        .append(hexChars[codePoint & 0xf]);
  }

  /**
   * Undo escaping as performed in javaScriptEscape(.) Throws an IllegalArgumentException if the
   * string contains bad escaping.
   */
  public static String javaScriptUnescape(String s) {
    StringBuilder sb = new StringBuilder(s.length());
    for (int i = 0; i < s.length(); ) {
      char c = s.charAt(i);
      if (c == '\\') {
        i = javaScriptUnescapeHelper(s, i + 1, sb);
      } else {
        sb.append(c);
        i++;
      }
    }
    return sb.toString();
  }

  /**
   * Looks for an escape code starting at index i of s, and appends it to sb.
   *
   * @return the index of the first character in s after the escape code.
   * @throws IllegalArgumentException if the escape code is invalid
   */
  private static int javaScriptUnescapeHelper(String s, int i, StringBuilder sb) {
    if (i >= s.length()) {
      throw new IllegalArgumentException("End-of-string after escape character in [" + s + "]");
    }

    char c = s.charAt(i++);
    switch (c) {
      case 'n':
        sb.append('\n');
        break;
      case 'r':
        sb.append('\r');
        break;
      case 't':
        sb.append('\t');
        break;
      case 'b':
        sb.append('\b');
        break;
      case 'f':
        sb.append('\f');
        break;
      case '\\':
      case '\"':
      case '\'':
      case '>':
        sb.append(c);
        break;
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
        --i; // backup to first octal digit
        int nOctalDigits = 1;
        int digitLimit = c < '4' ? 3 : 2;
        while (nOctalDigits < digitLimit
            && i + nOctalDigits < s.length()
            && isOctal(s.charAt(i + nOctalDigits))) {
          ++nOctalDigits;
        }
        sb.append((char) Integer.parseInt(s.substring(i, i + nOctalDigits), 8));
        i += nOctalDigits;
        break;
      case 'x':
      case 'u':
        String hexCode;
        int nHexDigits = (c == 'u' ? 4 : 2);
        try {
          hexCode = s.substring(i, i + nHexDigits);
        } catch (IndexOutOfBoundsException ioobe) {
          throw new IllegalArgumentException(
              "Invalid unicode sequence ["
                  + s.substring(i)
                  + "] at index "
                  + i
                  + " in ["
                  + s
                  + "]");
        }
        int unicodeValue;
        try {
          unicodeValue = Integer.parseInt(hexCode, 16);
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException(
              "Invalid unicode sequence [" + hexCode + "] at index " + i + " in [" + s + "]");
        }
        sb.append((char) unicodeValue);
        i += nHexDigits;
        break;
      default:
        throw new IllegalArgumentException(
            "Unknown escape code [" + c + "] at index " + i + " in [" + s + "]");
    }

    return i;
  }

  /**
   * This converts a string to a Map. It will first split the string into entries using delimEntry.
   * Then each entry is split into a key and a value using delimKey. By default we strip the keys.
   * Use doStripEntry to strip also the entries.
   *
   * @param in the string to be processed
   * @param delimEntry delimiter for the entries
   * @param delimKey delimiter between keys and values
   * @param doStripEntry strip entries before inserting in the map
   * @return an (unordered) {@link HashMap}
   * @deprecated Use {@link Splitter}. Please note the following differences between Splitter and
   *     this method:
   *     <ul>
   *       <li>Splitter will throw an exception if any of the delimiters is null,
   *       <li>Splitter will throw an exception if the entries are malformed (i.e. when entries do
   *           not contain delimKey, or contain more than one delimiter),
   *       <li>this method would ignore multiple keys in the resulting map (i.e. last value for a
   *           key would silently overwrite all others); Splitter will throw an exception in this
   *           case.
   *     </ul>
   */
  @Deprecated
  public static HashMap<String, String> splitToMap(
      String in, @Nullable String delimEntry, @Nullable String delimKey, boolean doStripEntry) {
    checkNotNull(in);

    HashMap<String, String> out = new HashMap<>();
    if (Strings.isNullOrEmpty(delimEntry) || Strings.isNullOrEmpty(delimKey)) {
      out.put(whitespace().trimFrom(in), "");
      return out;
    }

    int len = delimKey.length();
    for (String entry : Splitter.on(delimEntry).split(in)) {
      int pos = entry.indexOf(delimKey);
      if (pos > 0) {
        String value = entry.substring(pos + len);
        if (doStripEntry) {
          value = whitespace().trimFrom(value);
        }
        out.put(whitespace().trimFrom(entry.substring(0, pos)), value);
      } else {
        out.put(whitespace().trimFrom(entry), "");
      }
    }

    return out;
  }

  /**
   * Converts a string to a map like {@link #splitToMap} but with passthrough of a null {@code in}.
   *
   * @deprecated See instructions for {@link #splitToMap}, noting that it and its suggested
   *     replacement will throw {@code NullPointerException} if {@code in} is null.
   */
  @Deprecated
  @Nullable
  public static HashMap<String, String> string2Map(
      @Nullable String in,
      @Nullable String delimEntry,
      @Nullable String delimKey,
      boolean doStripEntry) {
    if (in == null) {
      return null;
    }

    return splitToMap(in, delimEntry, delimKey, doStripEntry);
  }

  /**
   * Parse a list of substrings separated by a given delimiter. The delimiter can also appear in
   * substrings (just double them):
   *
   * <ul>
   *   <li>parseDelimitedList("this|is", '|') returns ["this","is"]
   *   <li>parseDelimitedList("this||is", '|') returns ["this|is"]
   * </ul>
   *
   * @param list String containing delimited substrings
   * @param delimiter Delimiter (anything except ' ' is allowed)
   * @return A String array of parsed substrings
   * @deprecated Use {@link Splitter#on(char) Splitter.on}{@code (delimiter).splitToList(list)} for
   *     simple splitting and {@link com.google.common.text.ExcelDelimited.Reader#splitRow
   *     ExcelDelimited.Reader.splitRow}{@code (list, delimiter)} for more features. Note that
   *     {@code parseDelimitedList}'s special treatment of doubled delimiters is not available in
   *     either.
   */
  @Deprecated
  public static String[] parseDelimitedList(String list, char delimiter) {
    checkNotNull(list);
    String delim = "" + delimiter;
    // Append a sentinel of delimiter + space
    // (see comments below for more info)
    StringTokenizer st = new StringTokenizer(list + delim + " ", delim, true);
    ArrayList<String> v = new ArrayList<>();
    String lastToken = "";
    StringBuilder word = new StringBuilder();

    // We keep a sliding window of 2 tokens
    //
    // delimiter : delimiter -> append delimiter to current word
    //                          and clear most recent token
    //                          (so delim : delim : delim will not
    //                          be treated as two escaped delims.)
    //
    // tok : delimiter -> append tok to current word
    //
    // delimiter : tok -> add current word to list, and clear it.
    //                    (We append a sentinel that conforms to this
    //                    pattern to make sure we've pushed every parsed token)
    while (st.hasMoreTokens()) {
      String tok = st.nextToken();
      if (lastToken != null) {
        if (tok.equals(delim)) {
          word.append(lastToken);
          if (lastToken.equals(delim)) {
            tok = null;
          }
        } else {
          if (word.length() != 0) {
            v.add(word.toString());
          }
          word.setLength(0);
        }
      }
      lastToken = tok;
    }

    return v.toArray(new String[0]);
  }

  /** Splits s with delimiters in delimiter and returns the last token */
  public static String lastToken(String s, String delimiter) {
    return s.substring(CharMatcher.anyOf(delimiter).lastIndexIn(s) + 1);
  }

  private static final Pattern CHARACTER_REFERENCE_PATTERN =
      Pattern.compile("&#?[a-zA-Z0-9]{1,8};");

  /**
   * Determines if a string contains what looks like an html character reference. Useful for
   * deciding whether unescaping is necessary.
   *
   * @deprecated There is no universal way to determine how many rounds of escaping or unescaping is
   *     necessary for a given text. APIs should instead specify whether they operate on unencoded
   *     text or HTML, often through the use of types like {@link
   *     com.google.gwt.user.client.ui.HTML}.
   */
  @Deprecated
  public static boolean containsCharRef(String s) {
    return CHARACTER_REFERENCE_PATTERN.matcher(s).find();
  }

  /**
   * Determines if a string is a CJK word. A string is considered to be CJK if {@link #isCjk(char)}
   * is true for any of its characters.
   */
  public static boolean isCjk(String s) {
    int len = s.length();
    for (int i = 0; i < len; ++i) {
      if (isCjk(s.codePointAt(i))) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines if a character is a CJK ideograph or a character typically used only in CJK text.
   *
   * <p>Note: This function cannot handle supplementary characters. To handle all Unicode
   * characters, including supplementary characters, use the function {@link #isCjk(int)}.
   */
  public static boolean isCjk(char ch) {
    return isCjk((int) ch);
  }

  /**
   * Determines if a character is a CJK ideograph or a character typically used only in CJK text.
   */
  public static boolean isCjk(int codePoint) {
    // Time-saving early exit for all Latin-1 characters.
    if ((codePoint & 0xFFFFFF00) == 0) {
      return false;
    }

    return CJK_BLOCKS.contains(Character.UnicodeBlock.of(codePoint));
  }

  /** Unicode code blocks containing CJK characters. */
  private static final Set<Character.UnicodeBlock> CJK_BLOCKS;

  static {
    Set<Character.UnicodeBlock> set = new HashSet<>();
    set.add(Character.UnicodeBlock.HANGUL_JAMO);
    set.add(Character.UnicodeBlock.CJK_RADICALS_SUPPLEMENT);
    set.add(Character.UnicodeBlock.KANGXI_RADICALS);
    set.add(Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION);
    set.add(Character.UnicodeBlock.HIRAGANA);
    set.add(Character.UnicodeBlock.KATAKANA);
    set.add(Character.UnicodeBlock.BOPOMOFO);
    set.add(Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO);
    set.add(Character.UnicodeBlock.KANBUN);
    set.add(Character.UnicodeBlock.BOPOMOFO_EXTENDED);
    set.add(Character.UnicodeBlock.KATAKANA_PHONETIC_EXTENSIONS);
    set.add(Character.UnicodeBlock.ENCLOSED_CJK_LETTERS_AND_MONTHS);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY);
    set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A);
    set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS);
    set.add(Character.UnicodeBlock.HANGUL_SYLLABLES);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_FORMS);
    set.add(Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS);
    set.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B);
    set.add(Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS_SUPPLEMENT);
    CJK_BLOCKS = Collections.unmodifiableSet(set);
  }

  /**
   * Returns the approximate display width of the string, measured in units of ASCII characters.
   *
   * <p><b>Warning:</b> This method ignores <a
   * href="https://groups.google.com/a/google.com/d/msg/java-libraries-discuss/wcmKfTtB0Xk/71nuUftAwqsJ"
   * >many factors</a> that contribute to display width. At best, it is an upper bound for monospace
   * fonts, but even this has not been tested.
   *
   * @see StringUtil#displayWidth(char)
   */
  public static int displayWidth(String s) {
    // TODO(kevinb): could reimplement this as
    // return s.length() * 2 - CharMatcher.singleWidth().countIn(s);
    int width = 0;
    int len = s.length();
    for (int i = 0; i < len; ++i) {
      width += displayWidth(s.charAt(i));
    }
    return width;
  }

  /**
   * Returns the approximate display width of the character, measured in units of ascii characters.
   *
   * <p><b>Warning:</b> This method ignores <a
   * href="https://groups.google.com/a/google.com/d/msg/java-libraries-discuss/wcmKfTtB0Xk/71nuUftAwqsJ"
   * >many factors</a> that contribute to display width. At best, it is an upper bound for monospace
   * fonts, but even this has not been tested.
   *
   * <p>This method should err on the side of caution. By default, characters are assumed to have
   * width 2; this covers CJK ideographs, various symbols and miscellaneous weird scripts. Given
   * below are some Unicode ranges for which it seems safe to assume that no character is
   * substantially wider than an ascii character:
   *
   * <ul>
   *   <li>Latin, extended Latin, even more extended Latin.
   *   <li>Greek, extended Greek, Cyrillic.
   *   <li>Some symbols (including currency symbols) and punctuation.
   *   <li>Half-width Katakana and Hangul.
   *   <li>Hebrew
   *   <li>Arabic
   *   <li>South Asian scripts, including Indic languages and Thai
   * </ul>
   *
   * Characters in these ranges are given a width of 1.
   *
   * <p>IMPORTANT: this function has analogs in C++ (encodingutils.cc, named UnicodeCharWidth) and
   * JavaScript (java/com/google/ads/common/frontend/adwordsbase/resources/CreateAdUtil.js), which
   * need to be updated if you change the implementation here.
   */
  public static int displayWidth(char ch) {
    // LINT.IfChange
    if (ch <= '\u04f9' // CYRILLIC SMALL LETTER YERU WITH DIAERESIS
        || ch == '\u05be' // HEBREW PUNCTUATION MAQAF
        || (ch >= '\u05d0' && ch <= '\u05ea') // HEBREW LETTER ALEF ... TAV
        || ch == '\u05F3' // HEBREW PUNCTUATION GERESH
        || ch == '\u05f4' // HEBREW PUNCTUATION GERSHAYIM
        || (ch >= '\u0600' && ch <= '\u06ff') // Block=Arabic
        || (ch >= '\u0750' && ch <= '\u077f') // Block=Arabic_Supplement
        || (ch >= '\ufb50' && ch <= '\ufdff') // Block=Arabic_Presentation_Forms-A
        || (ch >= '\ufe70' && ch <= '\ufeff') // Block=Arabic_Presentation_Forms-B
        || (ch >= '\u1e00' && ch <= '\u20af') // LATIN CAPITAL A WITH RING BELOW ... DRACHMA SIGN
        || (ch >= '\u2100' && ch <= '\u213a') // ACCOUNT OF ... ROTATED CAPITAL Q
        || (ch >= '\u0900' && ch <= '\u0d7f') // Block=Indic
        || (ch >= '\u0e00' && ch <= '\u0e7f') // Thai
        || (ch >= '\uff61' && ch <= '\uffdc')) { // HALFWIDTH IDEO FULL STOP ... HALFWIDTH HANGUL I
      return 1;
    }
    return 2;
    // LINT.ThenChange(//depot/google3/ads/creatives/util/character_count.cc)
  }

  private static final char[] hexChars = "0123456789abcdef".toCharArray();

  /**
   * Returns a string that is equivalent to the specified string with its first character converted
   * to uppercase as by {@link String#toUpperCase()}. The returned string will have the same value
   * as the specified string if its first character is non-alphabetic, if its first character is
   * already uppercase, or if the specified string is of length 0.
   *
   * <p>For example:
   *
   * <pre>
   *    capitalize("foo bar").equals("Foo bar");
   *    capitalize("2b or not 2b").equals("2b or not 2b")
   *    capitalize("Foo bar").equals("Foo bar");
   *    capitalize("").equals("");
   * </pre>
   *
   * @param s the string whose first character is to be uppercased
   * @return a string equivalent to <tt>s</tt> with its first character converted to uppercase
   * @throws NullPointerException if <tt>s</tt> is null
   * @deprecated For better internationalization and more flexibility over which words are
   *     capitalized, use {@link com.ibm.icu.lang.UCharacter#toTitleCase(com.ibm.icu.util.ULocale,
   *     String, com.ibm.icu.text.BreakIterator) UCharacter.toTitleCase}.
   */
  @Deprecated
  public static String capitalize(String s) {
    if (s.length() == 0) {
      return s;
    }
    char first = s.charAt(0);
    char capitalized = Character.toUpperCase(first);
    return (first == capitalized) ? s : capitalized + s.substring(1);
  }

  /**
   * Examine a string to see if it starts with a given prefix (case insensitive). Just like
   * String.startsWith() except doesn't respect case. Strings are compared in the same way as in
   * {@link String#equalsIgnoreCase}.
   *
   * @param str the string to examine
   * @param prefix the prefix to look for
   * @return a boolean indicating if str starts with prefix (case insensitive)
   */
  public static boolean startsWithIgnoreCase(String str, String prefix) {
    return str.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  /**
   * Examine a string to see if it ends with a given suffix (case insensitive). Just like
   * String.endsWith() except doesn't respect case. Strings are compared in the same way as in
   * {@link String#equalsIgnoreCase}.
   *
   * @param str the string to examine
   * @param suffix the suffix to look for
   * @return a boolean indicating if str ends with suffix (case insensitive)
   */
  public static boolean endsWithIgnoreCase(String str, String suffix) {
    int len = suffix.length();
    return str.regionMatches(true, str.length() - len, suffix, 0, len);
  }

  /**
   * Returns the longest prefix of a string for which the UTF-8 encoding fits into the given number
   * of bytes, with the additional guarantee that the string is not truncated in the middle of a
   * valid surrogate pair.
   *
   * <p>Unpaired surrogates are counted as taking 3 bytes of storage. However, a subsequent attempt
   * to actually encode a string containing unpaired surrogates is likely to be rejected by the
   * UTF-8 implementation.
   *
   * @param str a string
   * @param maxbytes the maximum number of UTF-8 encoded bytes
   * @return the beginning of the string, so that it uses at most maxbytes bytes in UTF-8
   * @throws IndexOutOfBoundsException if maxbytes is negative
   */
  public static String truncateStringForUtf8Storage(String str, int maxbytes) {
    if (maxbytes < 0) {
      throw new IndexOutOfBoundsException();
    }

    int bytes = 0;
    for (int i = 0, len = str.length(); i < len; i++) {
      char c = str.charAt(i);
      if (c < 0x80) {
        bytes += 1;
      } else if (c < 0x800) {
        bytes += 2;
      } else if (c < Character.MIN_SURROGATE
          || c > Character.MAX_SURROGATE
          || str.codePointAt(i) < Character.MIN_SUPPLEMENTARY_CODE_POINT) {
        bytes += 3;
      } else {
        bytes += 4;
        i += (bytes > maxbytes) ? 0 : 1;
      }
      if (bytes > maxbytes) {
        return str.substring(0, i);
      }
    }
    return str;
  }

  /**
   * If the given string is of length {@code maxLength} or less, then it is returned as is. If the
   * string is longer than {@code maxLength}, the returned string is truncated before the last space
   * character on or before {@code source.charAt(maxLength)}. If the string has no spaces, the
   * returned string is truncated to {@code maxLength}.
   *
   * @param source the string to truncate if necessary
   * @return the original string if its length is less than or equal to {@code maxLength}, otherwise
   *     a truncated string as mentioned above
   */
  public static String truncateIfNecessary(String source, int maxLength) {
    if (source.length() <= maxLength) {
      return source;
    }
    String str = unicodePreservingSubstring(source, 0, maxLength);

    String truncated = whitespace().trimTrailingFrom(str);

    // We may have had multiple spaces at maxLength, which were stripped away
    if (truncated.length() < maxLength) {
      return truncated;
    }
    // We have a truncated string of length maxLength. If the next char was a
    // space, we truncated at a word boundary, so we can return immediately
    if (whitespace().matches(source.charAt(maxLength))) {
      return truncated;
    }
    // We truncated in the middle of the word. Try to truncate before
    // the last space, if it exists. Otherwise, return the truncated string
    for (int i = truncated.length() - 1; i >= 0; --i) {
      if (whitespace().matches(truncated.charAt(i))) {
        String substr = truncated.substring(0, i);
        return whitespace().trimTrailingFrom(substr);
      }
    }
    return truncated;
  }

  /**
   * If this given string is of length {@code maxLength} or less, it will be returned as-is.
   * Otherwise it will be trucated to {@code maxLength}, regardless of whether there are any space
   * characters in the String. If an ellipsis is requested to be appended to the truncated String,
   * the String will be truncated so that the ellipsis will also fit within maxLength. If no
   * truncation was necessary, no ellipsis will be added.
   *
   * @param source the String to truncate if necessary
   * @param maxLength the maximum number of characters to keep
   * @param addEllipsis if true, and if the String had to be truncated, add "..." to the end of the
   *     String before returning. Additionally, the ellipsis will only be added if maxLength is
   *     greater than 3.
   * @return the original string if its length is less than or equal to maxLength, otherwise a
   *     truncated string as mentioned above
   */
  public static String truncateAtMaxLength(String source, int maxLength, boolean addEllipsis) {

    if (source.length() <= maxLength) {
      return source;
    }
    if (addEllipsis && maxLength > 3) {
      return unicodePreservingSubstring(source, 0, maxLength - 3) + "...";
    }
    return unicodePreservingSubstring(source, 0, maxLength);
  }

  /**
   * Normalizes {@code index} such that it respects Unicode character boundaries in {@code str}.
   *
   * <p>If {@code index} is the low surrogate of a unicode character, the method returns {@code
   * index - 1}. Otherwise, {@code index} is returned.
   *
   * <p>In the case in which {@code index} falls in an invalid surrogate pair (e.g. consecutive low
   * surrogates, consecutive high surrogates), or if it is not a valid index into {@code str}, the
   * original value of {@code index} is returned.
   *
   * @param str the String
   * @param index the index to be normalized
   * @return a normalized index that does not split a Unicode character
   */
  @VisibleForTesting
  static int unicodePreservingIndex(String str, int index) {
    if (index > 0 && index < str.length()) {
      if (Character.isHighSurrogate(str.charAt(index - 1))
          && Character.isLowSurrogate(str.charAt(index))) {
        return index - 1;
      }
    }
    return index;
  }

  /**
   * Returns a substring of {@code str} that respects Unicode character boundaries.
   *
   * <p>The string will never be split between a [high, low] surrogate pair, as defined by {@link
   * Character#isHighSurrogate} and {@link Character#isLowSurrogate}.
   *
   * <p>If {@code begin} or {@code end} are the low surrogate of a unicode character, it will be
   * offset by -1.
   *
   * <p>This behavior guarantees that {@code str.equals(StringUtil.unicodePreservingSubstring(str,
   * 0, n) + StringUtil.unicodePreservingSubstring(str, n, str.length())) } is true for all {@code
   * n}.
   *
   * <p>This means that unlike {@link String#substring(int, int)}, the length of the returned
   * substring may not necessarily be equivalent to {@code end - begin}.
   *
   * @param str the original String
   * @param begin the beginning index, inclusive
   * @param end the ending index, exclusive
   * @return the specified substring, possibly adjusted in order to not split unicode surrogate
   *     pairs
   * @throws IndexOutOfBoundsException if the {@code begin} is negative, or {@code end} is larger
   *     than the length of {@code str}, or {@code begin} is larger than {@code end}
   */
  public static String unicodePreservingSubstring(String str, int begin, int end) {
    return str.substring(unicodePreservingIndex(str, begin), unicodePreservingIndex(str, end));
  }

  /**
   * Equivalent to:
   *
   * <pre>
   * {@link #unicodePreservingSubstring(String, int, int)}(
   *     str, begin, str.length())
   * </pre>
   */
  public static String unicodePreservingSubstring(String str, int begin) {
    return unicodePreservingSubstring(str, begin, str.length());
  }

  /**
   * True iff the given character needs to be escaped in a javascript string literal.
   *
   * <p>We need to escape the following characters in javascript string literals.
   *
   * <dl>
   *   <dt>\
   *   <dd>the escape character
   *   <dt>', "
   *   <dd>string delimiters. TODO(msamuel): what about backticks (`) which are non-standard but
   *       recognized as attribute delimiters.
   *   <dt>&amp;, &lt;, &gt;, =
   *   <dd>so that a string literal can be embedded in XHTML without further escaping.
   * </dl>
   *
   * TODO(msamuel): If we're being paranoid, should we escape + to avoid UTF-7 attacks?
   *
   * <p>Unicode format control characters (category Cf) must be escaped since they are removed by
   * javascript parser in a pre-lex pass.
   *
   * <p>According to EcmaScript 262 Section 7.1:
   *
   * <blockquote>
   *
   * The format control characters can occur anywhere in the source text of an ECMAScript program.
   * These characters are removed from the source text before applying the lexical grammar.
   *
   * </blockquote>
   *
   * <p>Additionally, line terminators are not allowed to appear inside strings and Section 7.3 says
   *
   * <blockquote>
   *
   * The following characters are considered to be line terminators:
   *
   * <pre>
   *         Code Point Value   Name                  Formal Name
   *         \u000A             Line Feed             [LF]
   *         \u000D             Carriage Return       [CR]
   *         \u2028             Line separator        [LS]
   *         \u2029             Paragraph separator   [PS]
   * </pre>
   *
   * </blockquote>
   *
   * @param codepoint a char instead of an int since the javascript language does not support
   *     extended unicode.
   */
  @VisibleForTesting
  static boolean mustEscapeCharInJsString(int codepoint) {
    return JS_ESCAPE_CHARS.contains(codepoint);
  }

  /**
   * True iff the given character needs to be escaped in a JSON string literal.
   *
   * <p>We need to escape the following characters in JSON string literals.
   *
   * <dl>
   *   <dt>\
   *   <dd>the escape character
   *   <dt>"
   *   <dd>string delimiter
   *   <dt>0x00 - 0x1F
   *   <dd>control characters
   * </dl>
   *
   * <p>See EcmaScript 262 Section 15.12.1 for the full JSON grammar.
   */
  @VisibleForTesting
  static boolean mustEscapeCharInJsonString(int codepoint) {
    return JSON_ESCAPE_CHARS.contains(codepoint);
  }

  /**
   * True iff the given character needs to be escaped in a JSON string literal or a JavaScript
   * string literal.
   *
   * <p>See {@link #mustEscapeCharInJsString} and {@link #mustEscapeCharInJsonString}.
   */
  @VisibleForTesting
  static boolean mustEscapeCharInJsonOrJsString(int codepoint) {
    return JSON_OR_JS_ESCAPE_CHARS.contains(codepoint);
  }

  /**
   * A small set of code points. {@code com.google.common.base} cannot depend on ICU4J, thus
   * avoiding ICU's {@code UnicodeSet}. For all other purposes, please use {@code
   * com.ibm.icu.text.UnicodeSet}.
   */
  private static class CodePointSet {
    final boolean[] fastArray; // Fast membership test for small valuess
    final Set<Integer> elements;

    private CodePointSet(Set<Integer> codePoints) {
      this.elements = codePoints;
      fastArray = new boolean[0x100];
      for (int i = 0; i < fastArray.length; i++) {
        fastArray[i] = elements.contains(i);
      }
    }

    boolean contains(int codePoint) {
      if (codePoint < fastArray.length) {
        return fastArray[codePoint];
      }
      return elements.contains(codePoint);
    }

    CodePointSet or(CodePointSet other) {
      return new Builder().addSet(this).addSet(other).create();
    }

    static class Builder {
      final Set<Integer> codePoints = new HashSet<>();

      Builder addCodePoint(int c) {
        codePoints.add(c);
        return this;
      }

      Builder addRange(int from, int to) {
        for (int i = from; i <= to; i++) {
          codePoints.add(i);
        }
        return this;
      }

      Builder addSet(CodePointSet set) {
        for (int i : set.elements) {
          codePoints.add(i);
        }
        return this;
      }

      CodePointSet create() {
        return new CodePointSet(codePoints);
      }
    }
  }

  private static final CodePointSet JS_ESCAPE_CHARS =
      new CodePointSet.Builder()
          // All characters in the class of format characters, [:Cf:].
          // Source: http://unicode.org/cldr/utility/list-unicodeset.jsp.
          .addCodePoint(0xAD)
          .addRange(0x600, 0x603)
          .addCodePoint(0x6DD)
          .addCodePoint(0x070F)
          .addRange(0x17B4, 0x17B5)
          .addRange(0x200B, 0x200F)
          .addRange(0x202A, 0x202E)
          .addRange(0x2060, 0x2064)
          .addRange(0x206A, 0x206F)
          .addCodePoint(0xFEFF)
          .addRange(0xFFF9, 0xFFFB)
          .addRange(0x0001D173, 0x0001D17A)
          .addCodePoint(0x000E0001)
          .addRange(0x000E0020, 0x000E007F)
          // Plus characters mentioned in the docs of mustEscapeCharInJsString().
          .addCodePoint(0x0000)
          .addCodePoint(0x000A)
          .addCodePoint(0x000D)
          .addRange(0x2028, 0x2029)
          .addCodePoint(0x0085)
          .addCodePoint(Character.codePointAt("'", 0))
          .addCodePoint(Character.codePointAt("\"", 0))
          .addCodePoint(Character.codePointAt("&", 0))
          .addCodePoint(Character.codePointAt("<", 0))
          .addCodePoint(Character.codePointAt(">", 0))
          .addCodePoint(Character.codePointAt("=", 0))
          .addCodePoint(Character.codePointAt("\\", 0))
          .create();

  private static final CodePointSet JSON_ESCAPE_CHARS =
      new CodePointSet.Builder()
          .addCodePoint(Character.codePointAt("\"", 0))
          .addCodePoint(Character.codePointAt("\\", 0))
          .addRange(0x0000, 0x001F)
          .create();

  private static final CodePointSet JSON_OR_JS_ESCAPE_CHARS = JSON_ESCAPE_CHARS.or(JS_ESCAPE_CHARS);

  // TODO(cpovirk): remove these cloned common.escape classes
  private abstract static class CharEscaper {
    /**
     * Returns the escaped form of a given literal string.
     *
     * @param string the literal string to be escaped
     * @return the escaped form of {@code string}
     * @throws NullPointerException if {@code string} is null
     */
    public String escape(String string) {
      checkNotNull(string); // GWT specific check (do not optimize)
      // Inlineable fast-path loop which hands off to escapeSlow() only if needed
      int length = string.length();
      for (int index = 0; index < length; index++) {
        if (escape(string.charAt(index)) != null) {
          return escapeSlow(string, index);
        }
      }
      return string;
    }

    /**
     * Returns the escaped form of the given character, or {@code null} if this character does not
     * need to be escaped. If an empty array is returned, this effectively strips the input
     * character from the resulting text.
     *
     * <p>If the character does not need to be escaped, this method should return {@code null},
     * rather than a one-character array containing the character itself. This enables the escaping
     * algorithm to perform more efficiently.
     *
     * <p>An escaper is expected to be able to deal with any {@code char} value, so this method
     * should not throw any exceptions.
     *
     * @param c the character to escape if necessary
     * @return the replacement characters, or {@code null} if no escaping was needed
     */
    protected abstract char[] escape(char c);

    /**
     * Returns the escaped form of a given literal string, starting at the given index. This method
     * is called by the {@link #escape(String)} method when it discovers that escaping is required.
     * It is protected to allow subclasses to override the fastpath escaping function to inline
     * their escaping test. See {@link CharEscaperBuilder} for an example usage.
     *
     * @param s the literal string to be escaped
     * @param index the index to start escaping from
     * @return the escaped form of {@code string}
     * @throws NullPointerException if {@code string} is null
     */
    protected String escapeSlow(String s, int index) {
      int slen = s.length();

      // Get a destination buffer and setup some loop variables.
      char[] dest = destinationThreadLocalBuffer.get();
      int destSize = dest.length;
      int destIndex = 0;
      int lastEscape = 0;

      // Loop through the rest of the string, replacing when needed into the
      // destination buffer, which gets grown as needed as well.
      for (; index < slen; index++) {

        // Get a replacement for the current character.
        char[] r = escape(s.charAt(index));

        // If no replacement is needed, just continue.
        if (r == null) continue;

        int rlen = r.length;
        int charsSkipped = index - lastEscape;

        // This is the size needed to add the replacement, not the full size
        // needed by the string. We only regrow when we absolutely must.
        int sizeNeeded = destIndex + charsSkipped + rlen;
        if (destSize < sizeNeeded) {
          destSize = sizeNeeded + (slen - index) + DEST_PAD;
          dest = growBuffer(dest, destIndex, destSize);
        }

        // If we have skipped any characters, we need to copy them now.
        if (charsSkipped > 0) {
          s.getChars(lastEscape, index, dest, destIndex);
          destIndex += charsSkipped;
        }

        // Copy the replacement string into the dest buffer as needed.
        if (rlen > 0) {
          System.arraycopy(r, 0, dest, destIndex, rlen);
          destIndex += rlen;
        }
        lastEscape = index + 1;
      }

      // Copy leftover characters if there are any.
      int charsLeft = slen - lastEscape;
      if (charsLeft > 0) {
        int sizeNeeded = destIndex + charsLeft;
        if (destSize < sizeNeeded) {

          // Regrow and copy, expensive! No padding as this is the final copy.
          dest = growBuffer(dest, destIndex, sizeNeeded);
        }
        s.getChars(lastEscape, slen, dest, destIndex);
        destIndex = sizeNeeded;
      }
      return new String(dest, 0, destIndex);
    }

    /**
     * Helper method to grow the character buffer as needed, this only happens once in a while so
     * it's ok if it's in a method call. If the index passed in is 0 then no copying will be done.
     */
    private static char[] growBuffer(char[] dest, int index, int size) {
      char[] copy = new char[size];
      if (index > 0) {
        System.arraycopy(dest, 0, copy, 0, index);
      }
      return copy;
    }

    /** The amount of padding to use when growing the escape buffer. */
    private static final int DEST_PAD = 32;

    /**
     * A thread-local destination buffer to keep us from creating new buffers. The starting size is
     * 1024 characters. If we grow past this we don't put it back in the threadlocal, we just keep
     * going and grow as needed.
     */
    private static final ThreadLocal<char[]> destinationThreadLocalBuffer =
        new ThreadLocal<char[]>() {
          @Override
          protected char[] initialValue() {
            return new char[1024];
          }
        };
  }

  private static final class CharEscaperBuilder {
    /**
     * Simple decorator that turns an array of replacement char[]s into a CharEscaper, this results
     * in a very fast escape method.
     */
    private static class CharArrayDecorator extends CharEscaper {
      private final char[][] replacements;
      private final int replaceLength;

      CharArrayDecorator(char[][] replacements) {
        this.replacements = replacements;
        this.replaceLength = replacements.length;
      }

      /*
       * Overriding escape method to be slightly faster for this decorator. We
       * test the replacements array directly, saving a method call.
       */
      @Override
      public String escape(String s) {
        int slen = s.length();
        for (int index = 0; index < slen; index++) {
          char c = s.charAt(index);
          if (c < replacements.length && replacements[c] != null) {
            return escapeSlow(s, index);
          }
        }
        return s;
      }

      @Override
      protected char @Nullable [] escape(char c) {
        return c < replaceLength ? replacements[c] : null;
      }
    }

    // Replacement mappings.
    private final Map<Character, String> map;

    // The highest index we've seen so far.
    private int max = -1;

    /** Construct a new sparse array builder. */
    public CharEscaperBuilder() {
      this.map = new HashMap<>();
    }

    /** Add a new mapping from an index to an object to the escaping. */
    public CharEscaperBuilder addEscape(char c, String r) {
      map.put(c, r);
      if (c > max) {
        max = c;
      }
      return this;
    }

    /**
     * Convert this builder into an array of char[]s where the maximum index is the value of the
     * highest character that has been seen. The array will be sparse in the sense that any unseen
     * index will default to null.
     *
     * @return a "sparse" array that holds the replacement mappings.
     */
    public char[][] toArray() {
      char[][] result = new char[max + 1][];
      for (Entry<Character, String> entry : map.entrySet()) {
        result[entry.getKey()] = entry.getValue().toCharArray();
      }
      return result;
    }

    /**
     * Convert this builder into a char escaper which is just a decorator around the underlying
     * array of replacement char[]s.
     *
     * @return an escaper that escapes based on the underlying array.
     */
    public CharEscaper toEscaper() {
      return new CharArrayDecorator(toArray());
    }
  }
}
