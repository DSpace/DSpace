/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import static org.dspace.app.orcid.model.OrcidProfileSectionType.EXTERNAL_IDS;

import java.util.List;

import org.dspace.app.profile.OrcidProfileSyncPreference;

/**
 * Implementation of {@link OrcidProfileSectionConfiguration} that model an
 * external id.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidExternalIdConfiguration extends OrcidSimpleValueConfiguration {

    private final String externalIdType;

    public OrcidExternalIdConfiguration(OrcidProfileSectionType sectionType,
        OrcidProfileSyncPreference preference, String metadataField, String externalIdType) {
        super(sectionType, preference, metadataField);
        this.externalIdType = externalIdType;
    }

    public String getExternalIdType() {
        return externalIdType;
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(EXTERNAL_IDS);
    }

}
