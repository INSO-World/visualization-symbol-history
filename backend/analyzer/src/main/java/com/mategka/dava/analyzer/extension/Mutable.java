package com.mategka.dava.analyzer.extension;

import com.google.common.collect.Iterables;

import java.lang.annotation.*;
import java.util.Collection;
import java.util.Iterator;

/**
 * A marker interface meant to indicate that a given element must be mutable.
 * For container-like classes, this generally means that elements can be added and removed, and that its iterator
 * supports the {@link Iterator#remove() remove()} operation.
 * <p>
 * This can be very helpful when trying to preserve memory; for example, it can mark {@linkplain Collection Collections}
 * as {@linkplain Iterables#consumingIterable(Iterable) consumable}.
 * <p>
 * Note that this annotation does <b>not</b> indicate whether a field, parameter or variable is <i>reassignable</i>.
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.LOCAL_VARIABLE, ElementType.TYPE_USE })
@Retention(RetentionPolicy.SOURCE)
@Documented
public @interface Mutable {

}
