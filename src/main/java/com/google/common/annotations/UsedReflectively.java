package com.google.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Indicates that the annotated element is referenced via reflection. As one example dead code
 * removal will therefore not remove such elements.
 *
 * <p>If this annotates another annotation type, any member annotated with that annotation type
 * will also be assumed to be referenced via reflection.
 *
 * @author ensonic@google.com (Stefan Sauer)
 */
@GoogleInternal // maybe consider it
@GwtCompatible
@Target({
  ElementType.ANNOTATION_TYPE,
  ElementType.CONSTRUCTOR,
  ElementType.FIELD,
  ElementType.METHOD,
  ElementType.TYPE
})
public @interface UsedReflectively {}
