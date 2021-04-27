/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory;

import java.util.List;

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface for classes that creates ORCID profile section objects.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidProfileSectionFactory {

    /**
     * Creates many instances of ORCID objects starting from the given item.
     *
     * @param  context the DSpace Context
     * @param  item    the item
     * @return         the ORCID objects
     */
    public abstract List<Object> create(Context context, Item item);

    /**
     * Returns all the supported profile section types.
     *
     * @return the supported sections
     */
    public List<OrcidProfileSectionType> getSupportedTypes();

    /**
     * Returns all the metadata fields involved in the profile section
     * configuration.
     *
     * @return the metadataFields
     */
    public List<String> getMetadataFields();
}
