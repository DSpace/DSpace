/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

/**
 * This is a custom JacksonAnnotationIntrospector which allows us to ignore `@JsonProperty(access = Access
 * .WRITE_ONLY)` annotations in our tests.
 * Normally, this annotation allows the property to be written to (during deserialization),
 * but does NOT allow it to be read (during serialization).
 * In some tests, we need to ignore this annotation so that the test can use/verify the property
 * during both serialization & deserialization.
 *
 * In order to use this class in a test, use JsonMapper.builder():
 * JsonMapper mapper = JsonMapper.builder().annotationIntrospector(new IgnoreJacksonWriteOnlyAccess()).build();
 */
public class IgnoreJacksonWriteOnlyAccess extends JacksonAnnotationIntrospector {

    @Override
    public JsonProperty.Access findPropertyAccess(MapperConfig<?> cfg, Annotated m) {
        JsonProperty.Access access = super.findPropertyAccess(cfg, m);
        if (access == JsonProperty.Access.WRITE_ONLY) {
            return JsonProperty.Access.AUTO;
        }
        return access;
    }
}
