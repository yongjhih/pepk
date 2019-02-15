/*
 * Copyright 2015 The Error Prone Authors.
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
package com.google.errorprone.annotations.concurrent;

import com.google.errorprone.annotations.GoogleInternal;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Tells TSan to suppress reports of racy writes for the annotated field.
 *
 * <p>Use this annotation on any static or field that will be written only, with no synchronization.
 * The only acceptable place to read from it, without synchronization, is during a test or when
 * dumping state for a crash. Otherwise, reads must be synchronized with previous writes.
 *
 * <p>In practice, this is a very rare situation, and accordingly, this annotation should be used
 * very rarely.
 */
@GoogleInternal
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface WriteOnly {}
