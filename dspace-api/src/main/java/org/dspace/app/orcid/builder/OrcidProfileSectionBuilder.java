/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.builder;

import static java.lang.String.format;

import java.util.List;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Abstract class for that handle commons behaviors of all the available orcid
 * profile section builders.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public abstract class OrcidProfileSectionBuilder {

    protected final OrcidProfileSectionType sectionType;

    protected final OrcidProfileSyncPreference preference;

    @Autowired
    protected ItemService itemService;

    public OrcidProfileSectionBuilder(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference) {
        this.sectionType = sectionType;
        this.preference = preference;

        if (!getSupportedTypes().contains(sectionType)) {
            throw new IllegalArgumentException(format("The ORCID configuration does not support "
                + "the section type %s. Supported types are %s", sectionType, getSupportedTypes()));
        }

    }

    /**
     * Returns many instances of ORCID objects starting from the given item.
     *
     * @param  context the DSpace Context
     * @param  item    the item
     * @param  type    the profile section type
     * @return         the ORCID objects
     */
    public abstract List<Object> buildOrcidObjects(Context context, Item item, OrcidProfileSectionType type);

    /**
     * Returns the section type.
     *
     * @return the section type
     */
    public OrcidProfileSectionType getSectionType() {
        return sectionType;
    }

    /**
     * Returns the synchronization preference related to this configuration.
     *
     * @return the section name
     */
    public OrcidProfileSyncPreference getSynchronizationPreference() {
        return preference;
    }

    /**
     * Returns all the supported profile section types.
     *
     * @return the supported sections
     */
    public abstract List<OrcidProfileSectionType> getSupportedTypes();

    /**
     * Returns all the metadata fields involved in the profile section
     * configuration.
     *
     * @return the metadataFields
     */
    public abstract List<String> getMetadataFields();

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    protected List<String> getMetadataValues(Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .map(MetadataValue::getValue)
            .collect(Collectors.toList());
    }

}
