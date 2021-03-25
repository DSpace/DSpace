/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.iiif.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.digitalcollections.iiif.model.jackson.IiifModule;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;

public class ObjectMapperFactory {

    private ObjectMapperFactory() {}

    public static ObjectMapper getIiifObjectMapper() {
        return new IiifObjectMapper();
    }

    public static SimpleModule getIiifModule() {
        return new IiifModule();
    }
}
