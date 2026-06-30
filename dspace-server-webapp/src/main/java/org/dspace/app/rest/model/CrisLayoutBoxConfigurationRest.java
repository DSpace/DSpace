/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * The CrisLayoutBoxConfiguration interface details. Each box types will have a
 * specific configuration implementation
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @Type(value = CrisLayoutBoxRelationConfigurationRest.class, name = CrisLayoutBoxRelationConfigurationRest.NAME),
    @Type(value = CrisLayoutMetadataConfigurationRest.class, name = CrisLayoutMetadataConfigurationRest.NAME),
})
public interface CrisLayoutBoxConfigurationRest {
}
