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

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks an API in a {@code compatible_with=appengine} library as available only when the library is
 * built for Java 8+. This annotation can be used only from files built with the BUILD macro {@code
 * java_library_with_requires_java_8}, which automatically strips annotated APIs when building for
 * Java 7 (as identified, in most cases, by the developer's passing {@code --config=appengine}).
 *
 * <p>This annotation does <b>not</b> provide automatic stripping when a target is built for
 * <b>Android</b>.
 *
 * <p>For more information, see <a href="http://go/requiresjava8">go/requiresjava8</a>.
 */
@Documented
@GoogleInternal
@Retention(CLASS)
@Target({ANNOTATION_TYPE, CONSTRUCTOR, FIELD, METHOD, TYPE})
public @interface RequiresJava8 {}
