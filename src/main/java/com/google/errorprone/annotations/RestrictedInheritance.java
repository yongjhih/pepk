/*
 * Copyright 2018 The Error Prone Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.errorprone.annotations;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Restrict subtyping this type to whitelisted types.
 *
 * <p>Subtypes that are not whitelisted will cause a configurable compiler diagnostic. Whitelisting
 * can either allow the subtype outright, or make the compiler emit a warning. Paths matching a
 * regular expression, e.g. unit tests, can also be excluded.
 *
 * <p>The following example shows a hypothetical interface {@code SafeObject}. It is marked with the
 * {@code @RestrictedInheritance} annotation such that all classes {@code @LegacySafeObject} raise a
 * warning, whereas the {@code @ReviewedSafeObject} annotation silently allows the call.
 *
 * <pre>{@code
 * public {@literal @}interface LegacyUnsafeSafeObject{}
 *
 * public {@literal @}interface ReviewedSafeObject{
 *  public string reviewer();
 *  public string comments();
 * }
 *
 * {@literal @}RestrictedInheritance(
 *      explanation="Implementations of SafeObject need to be safe",
 *      link="http://security.google/safe_object_implementations.html",
 *      allowedOnPath="testsuite/.*", // Unsafe behavior in tests is ok.
 *      whitelistAnnotations = {ReviewedSafeObject.class},
 *      whitelistWithWarningAnnotations = {LegacyUnsafeSafeObject.class})
 * public interface SafeObject {
 *   void doNoHarm();
 * }
 *
 * {@literal @}LegacyUnsafeSafeObject // raises a warning
 * class QuestionablySafeObject implements SafeObject {
 *   void doNoHarm {
 *     // ...
 *   }
 * }
 *
 * {@literal @}ReviewedSafeObject(reviewer="bangert", comment="Review in cl/1234")
 * class ActualSafeObject implements SafeObject {
 *   void doNoHarm {
 *     // ...
 *   }
 * }
 * }</pre>
 */
@GoogleInternal
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RestrictedInheritance {
  /** Very short name for the diagnostic message. Used in error-prone. */
  String checkerName() default "RestrictedApi";

  /** Explanation why the API is restricted, to be inserted into the compiler output. */
  String explanation();

  /** Link explaining why the API is restricted */
  String link();

  /**
   * Allow the restricted API on paths matching this regular expression.
   *
   * <p>Leave empty (the default) to enforce the API restrictions on all paths.
   */
  String allowedOnPath() default "";

  /** Allow calls to the restricted API in methods or classes with this annotation. */
  Class<? extends Annotation>[] whitelistAnnotations() default {};

  /**
   * Emit warnings, not errors, on calls to the restricted API.
   *
   * <p>This should only be used while you are rolling out annotations with
   * suggestedWhitelistAnnotation.
   */
  boolean warningOnlyForRefactoring() default false;

  Class<? extends Annotation> suggestedWhitelistAnnotation() default DontSuggestFixes.class;
}
