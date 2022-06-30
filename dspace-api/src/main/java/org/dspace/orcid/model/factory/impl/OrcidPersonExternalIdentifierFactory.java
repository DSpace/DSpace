/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory.impl;

import static org.dspace.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;
import static org.dspace.orcid.model.factory.OrcidFactoryUtils.parseConfigurations;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidProfileSectionType;
import org.dspace.profile.OrcidProfileSyncPreference;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.PersonExternalIdentifier;

/**
 * Implementation of {@link OrcidProfileSectionFactory} that model an personal
 * external id.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidPersonExternalIdentifierFactory extends OrcidSimpleValueObjectFactory {

    private Map<String, String> externalIds = new HashMap<>();

    public OrcidPersonExternalIdentifierFactory(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference) {
        super(sectionType, preference);
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(EXTERNAL_IDS);
    }

    @Override
    protected Object create(Context context, MetadataValue metadataValue) {

        String currentMetadataField = metadataValue.getMetadataField().toString('.');
        String externalIdType = externalIds.get(currentMetadataField);

        if (externalIdType == null) {
            throw new IllegalArgumentException("Metadata field not supported: " + currentMetadataField);
        }

        PersonExternalIdentifier externalId = new PersonExternalIdentifier();
        externalId.setValue(metadataValue.getValue());
        externalId.setType(externalIdType);
        externalId.setRelationship(Relationship.SELF);
        externalId.setUrl(new Url(metadataValue.getValue()));

        return externalId;
    }

    public Map<String, String> getExternalIds() {
        return externalIds;
    }

    public void setExternalIds(String externalIds) {
        this.externalIds = parseConfigurations(externalIds);
        setMetadataFields(this.externalIds.keySet().stream().collect(Collectors.joining(",")));
    }

}
