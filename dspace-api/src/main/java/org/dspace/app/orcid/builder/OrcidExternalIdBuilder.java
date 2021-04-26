/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.builder;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;

import java.util.ArrayList;
import java.util.List;

import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;

/**
 * Implementation of {@link OrcidProfileSectionBuilder} that model an
 * external id.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidExternalIdBuilder extends OrcidSimpleValueBuilder {

    private final List<String> externalIdTypes;

    public OrcidExternalIdBuilder(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference, String metadataFields, String externalIdType) {

        super(sectionType, preference, metadataFields);
        this.externalIdTypes = externalIdType != null ? asList(externalIdType.split(",")) : emptyList();

        if (getExternalIdTypes().size() != getMetadataFields().size()) {
            throw new IllegalArgumentException("The external id types configuration is not compliance with "
                + "the metadata fields configuration");
        }

    }

    public List<String> getExternalIdTypes() {
        return externalIdTypes;
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(EXTERNAL_IDS);
    }

    @Override
    public List<Object> buildOrcidObjects(Context context, Item item, OrcidProfileSectionType type) {

        List<Object> objects = new ArrayList<Object>();

        for (int i = 0; i < metadataFields.size(); i++) {
            for (String metadataValue : getMetadataValues(item, metadataFields.get(i))) {
                PersonExternalIdentifier externalId = new PersonExternalIdentifier();
                externalId.setValue(metadataValue);
                externalId.setType(externalIdTypes.get(i));
                externalId.setRelationship(Relationship.SELF);
                externalId.setUrl(new Url(metadataValue));
                objects.add(externalId);
            }
        }

        return objects;
    }

}
