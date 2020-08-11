/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.stereotype.Component;

/**
 * Class or method-level annotation to provide information about linked/embedded subresources of a {@link RestModel}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Target( {ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface LinkRest {

    /**
     * The rel name to use for the link and/or embed.
     * <p>
     * This is optional if the annotation is used at the method level. If unspecified at the method level,
     * the bean name (inferred by the the name of the method) will be used as the name.
     * </p>
     * <p>
     * This is required if the annotation is used at the class level.
     * </p>
     *
     * @return the name, or the empty string if unspecified by the annotation.
     */
    String name() default "";

    /**
     * The name of the method to invoke in the associated link repository.
     * <p>
     * When this is specified, whether at the class or method level, the value of the resource must be provided
     * by a {@link LinkRestRepository}, which is found by its {@link Component} name. See
     * {@link Utils#getResourceRepository(String, String)}} for details.
     * </p>
     *
     * @return the method name, or the empty string if unspecified by the annotation.
     */
    String method() default "";
}
