/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.converter.MetadataValueWrapperConverter;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.MetadataValue;

/**
 * Wrapper for a {@link MetadataValue} for use with {@link MetadataValueWrapperConverter},
 * so it can be invoked properly via calls to {@link ConverterService#toRest(Object, Projection)}.
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class MetadataValueWrapper {

    MetadataValue metadataValue;

    public MetadataValueWrapper() {}

    public MetadataValue getMetadataValue() {
        return metadataValue;
    }

    public void setMetadataValue(MetadataValue metadataValue) {
        this.metadataValue = metadataValue;
    }
}
