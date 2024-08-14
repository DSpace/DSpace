/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import org.dspace.app.rest.model.MetadataValueList;
import org.dspace.app.rest.model.PotentialDuplicateRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.virtual.PotentialDuplicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Convert DSpace PotentialDuplicate object to a PotentialDuplicateRest REST resource
 * for use in REST results.
 *
 * @author Kim Shepherd
 */
@Component
public class PotentialDuplicateConverter implements DSpaceConverter<PotentialDuplicate, PotentialDuplicateRest> {
    @Lazy
    @Autowired
    private ConverterService converter;

    /**
     * Convert a PotentialDuplicate model object into its equivalent REST resource, applying
     * a given projection.
     * @see PotentialDuplicate
     * @see PotentialDuplicateRest
     *
     * @param modelObject a PotentialDuplicate object
     * @param projection current projection
     * @return a converted PotentialDuplicateRest REST object
     */
    @Override
    public PotentialDuplicateRest convert(PotentialDuplicate modelObject, Projection projection) {
        if (modelObject == null) {
            return null;
        }
        // Instantiate new REST model object
        PotentialDuplicateRest rest = new PotentialDuplicateRest();
        // Set or otherwise transform things here, then return
        rest.setUuid(modelObject.getUuid());
        rest.setTitle(modelObject.getTitle());
        rest.setOwningCollectionName(modelObject.getOwningCollectionName());
        rest.setWorkflowItemId(modelObject.getWorkflowItemId());
        rest.setWorkspaceItemId(modelObject.getWorkspaceItemId());
        rest.setMetadata(converter.toRest(new MetadataValueList(modelObject.getMetadataValueList()), projection));

        // Return converted object
        return rest;
    }

    /**
     * For what DSpace API model class does this converter convert?
     * @return Class of model objects represented.
     */
    @Override
    public Class<PotentialDuplicate> getModelClass() {
        return PotentialDuplicate.class;
    }

}
