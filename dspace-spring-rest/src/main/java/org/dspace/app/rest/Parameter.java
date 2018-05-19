/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation provides extra information about method parameters of a
 * SearchRestMethod allowing automatic bind of request parameters
 * 
 * @see SearchRestMethod
 * 
 * @author Terry Brady
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Parameter {
    String value() default "";
    boolean required() default false;
}
