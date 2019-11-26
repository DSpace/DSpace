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

import org.dspace.app.rest.model.hateoas.HALResource;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.repository.LinkRestRepository;
import org.dspace.app.rest.utils.Utils;
import org.springframework.stereotype.Component;

/**
 * Class or method-level annotation to control linking/embedding behavior when a {@link RestModel}
 * is wrapped as a {@link HALResource}
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

    /**
     * The class of object returned by invoking the link method. If a list or page is returned, this should
     * specify the inner type.
     *
     * @return the class.
     */
    Class linkClass();

    /**
     * Tells whether embedding the resource indicated by this link is optional.
     * <p>
     * If false (the default), it means the resource will always be embedded unless the {@link LinkRestRepository}
     * forbids it via {@link LinkRestRepository#isEmbeddableRelation(Object, String)}.
     * </p>
     * <p>
     * If true, it means the resource will be embedded normally, unless forbidden by the {@link LinkRestRepository}
     * or the projection, in use, via {@link Projection#allowOptionalEmbed(HALResource, LinkRest)}.
     * </p>
     *
     * @return whether embedding is optional.
     */
    boolean embedOptional() default false;

    /**
     * Tells whether linking the resource indicated by this link is optional.
     * <p>
     * If false (the default), it means the resource will always be linked.
     * </p>
     * <p>
     * If true, it means the resource will only be linked if:
     * <ul>
     *     <li> The resource is embedded, or</li>
     *     <li> The value returned by the link method is not null and linking is not forbidden by the
     *          projection in use, via {@link Projection#allowOptionalLink(HALResource, LinkRest)}</li>
     * </ul>
     * </p>
     *
     * @return whether linking is optional.
     */
    boolean linkOptional() default false;
}
