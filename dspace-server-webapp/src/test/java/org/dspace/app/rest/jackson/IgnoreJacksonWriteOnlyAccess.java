/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.jackson;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class IgnoreJacksonWriteOnlyAccess extends JacksonAnnotationIntrospector {

    @Override
    public JsonProperty.Access findPropertyAccess(Annotated m) {
        JsonProperty.Access access = super.findPropertyAccess(m);
        if (access == JsonProperty.Access.WRITE_ONLY) {
            return JsonProperty.Access.AUTO;
        }
        return access;
    }
}
