/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.iiif.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.digitalcollections.iiif.model.jackson.IiifModule;
import de.digitalcollections.iiif.model.jackson.IiifObjectMapper;

public class ObjectMapperFactory {

    private ObjectMapperFactory() {}

    /**
     * Gets the jackson ObjectMapper with dbmdz configuration.
     * https://github.com/dbmdz/iiif-apis/blob/main/src/main/java/de/digitalcollections/iiif/model/jackson/IiifObjectMapper.java
     * @return jackson mapper
     */
    public static ObjectMapper getIiifObjectMapper() {
        return new IiifObjectMapper();
    }

    /**
     * Gets the jackson SimpleModule with dbmdz configuration.
     * https://github.com/dbmdz/iiif-apis/blob/main/src/main/java/de/digitalcollections/iiif/model/jackson/IiifModule.java
     * @return model
     */
    public static SimpleModule getIiifModule() {
        return new IiifModule();
    }
}
