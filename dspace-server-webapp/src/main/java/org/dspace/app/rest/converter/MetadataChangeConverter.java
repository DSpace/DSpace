/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import java.util.LinkedList;
import java.util.List;

import org.dspace.app.rest.model.MetadataChangeEntryRest;
import org.dspace.app.rest.model.MetadataChangeRest;
import org.dspace.external.provider.metadata.service.impl.MetadataChange;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

/**
 * The class acts as a converter so that we can transform a list of {@link MetadataChange} objects into a
 * {@link MetadataChangeRest} object
 */
@Component
public class MetadataChangeConverter implements Converter<List<MetadataChange>, MetadataChangeRest> {

    @Override
    public MetadataChangeRest convert(List<MetadataChange> metadataChanges) {

        MetadataChangeRest metadataChangeRest = new MetadataChangeRest();
        List<MetadataChangeEntryRest> metadataChangeEntryRests = new LinkedList<>();
        for (MetadataChange metadataChange : metadataChanges) {

            metadataChangeEntryRests.add(
                new MetadataChangeEntryRest(metadataChange.getOperation(), metadataChange.getMetadataKey(),
                                            metadataChange.getValue()));
        }

        metadataChangeRest.setMetadataChangeEntryRests(metadataChangeEntryRests);
        return metadataChangeRest;
    }
}
