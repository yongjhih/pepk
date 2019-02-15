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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field that normally would be final, but for GWT serializability
 * purposes is not.
 *
 * <p>See
 * <a href="http://code.google.com/webtoolkit/doc/1.6/DevGuideServerCommunication.html#DevGuideSerializableTypes">
 * GWT Serializable Types</a> and the <a
 * href="http://code.google.com/p/google-web-toolkit/issues/detail?id=1054">
 * GWT issue</a> about removing the non-final constraint.
 *
 * @author Charles Fry
 */
@Documented
@GoogleInternal
@GwtCompatible
@Retention(RetentionPolicy.CLASS)
@Target(ElementType.FIELD)
public @interface NonFinalForGwt {}
