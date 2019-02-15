/*
 * Copyright (C) 2009 Google Inc.
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

package com.google.common.annotations;

import static javax.lang.model.element.Modifier.PUBLIC;

import com.google.errorprone.annotations.IncompatibleModifiers;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a default (zero-argument) constructor that is only provided for
 * GWT serialization, and should never be invoked directly.
 *
 * <p>The constructor should be private for final classes, or protected
 * for classes that might be extended.  For example:
 *
 * <pre>
 * {@literal @}GwtCompatible(serializeable = true)
 * final class Foo implements Serializable {
 *
 *   {@literal @}SuppressedWarnings("unused")
 *   {@literal @}DefaultConstructorForGwt
 *   private Foo() {}
 *
 *   ...
 * }
 * </pre>
 *
 * <p>See
 * <a href="http://code.google.com/webtoolkit/doc/1.6/DevGuideServerCommunication.html#DevGuideSerializableTypes">
 * GWT Serializable Types</a>.
 *
 * @author Tom O'Neill
 */
@Documented
@GoogleInternal
@GwtCompatible
@IncompatibleModifiers(PUBLIC)
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.CONSTRUCTOR)
public @interface DefaultConstructorForGwt {}
