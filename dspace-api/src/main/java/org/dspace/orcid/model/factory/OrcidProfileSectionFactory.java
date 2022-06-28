/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.profile.OrcidProfileSyncPreference;

/**
 * Interface for classes that creates ORCID profile section object.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidProfileSectionFactory {

    /**
     * Creates an instance of an ORCID object starting from the metadata values
     *
     * @param  context        the DSpace Context
     * @param  metadataValues the metadata values
     * @return                the ORCID object
     */
    public Object create(Context context, List<MetadataValue> metadataValues);

    /**
     * Returns the profile section type related to this factory.
     *
     * @return the profile section type
     */
    public OrcidProfileSectionType getProfileSectionType();

    /**
     * Returns the profile synchronization preference related to this factory.
     *
     * @return the synchronization preference
     */
    public OrcidProfileSyncPreference getSynchronizationPreference();

    /**
     * Returns all the metadata fields involved in the profile section
     * configuration.
     *
     * @return the metadataFields
     */
    public List<String> getMetadataFields();

    /**
     * Given the input item's metadata values generate a metadata signature for each
     * metadata field groups handled by this factory or for each metadata fields if
     * the factory is configured with single metadata fields.
     *
     * @param  context the DSpace context
     * @param  item    the item
     * @return         the metadata signatures
     */
    public List<String> getMetadataSignatures(Context context, Item item);

    /**
     * Returns a description of the item's metadata values related to the given
     * signature.
     *
     * @param  context   the DSpace context
     * @param  item      the item
     * @param  signature the metadata signature
     * @return           the metadata values description
     */
    public String getDescription(Context context, Item item, String signature);
}
