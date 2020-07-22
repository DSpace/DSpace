/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorization;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Class level annotation to document an {@link AuthorizationFeature}
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthorizationFeatureDocumentation {

    /**
     * The name of the authorization feature (required).
     *
     * @return the name.
     */
    String name();

    /**
     * The description of the authorization feature.
     *
     * @return the description of the authorization feature, or the empty string if unspecified by the annotation.
     */
    String description() default "";
}
