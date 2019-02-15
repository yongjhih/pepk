/*
 * Copyright (C) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.wireless.android.vending.developer.signing.tools.extern.export;

import java.util.HashMap;
import java.util.Map;

/** Utilities class (to avoid dependencies on other libraries). */
public class Utils {
  /**
   * This is a very very simple flag parser. It assumes that all arguments have a value (separated
   * by whitespace or "=").
   */
  public static Map<String, String> processArgs(String... args) {
    Map<String, String> argMap = new HashMap<>();
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (!arg.startsWith("--")) {
        throw new IllegalArgumentException("Invalid argument: " + arg);
      }
      arg = arg.replaceFirst("--", "");
      if (arg.contains("=")) {
        String[] argPair = arg.split("=", 2);
        argMap.put(argPair[0], argPair[1]);
      } else {
        String key = arg;
        String value;
        if (i + 1 == args.length || args[i + 1].startsWith("--")) {
          value = Boolean.TRUE.toString();
        } else {
          value = args[++i];
        }
        argMap.put(key, value);
      }
    }
    return argMap;
  }

  public static <T> T checkNotNull(T reference, String errorMessage) {
    if (reference == null) {
      throw new NullPointerException(errorMessage);
    }
    return reference;
  }
}
