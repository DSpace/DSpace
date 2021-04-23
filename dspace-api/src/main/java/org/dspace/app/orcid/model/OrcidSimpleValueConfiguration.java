/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import static org.dspace.app.orcid.model.OrcidProfileSectionType.COUNTRY;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.KEYWORDS;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.OTHER_NAMES;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.RESEARCHER_URLS;

import java.util.List;

import org.dspace.app.profile.OrcidProfileSyncPreference;

/**
 * Implementation of {@OrcidProfileSectionConfiguration} that provides a single
 * value for a particular ORCID profile section.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidSimpleValueConfiguration extends OrcidProfileSectionConfiguration {

    private final String metadataField;

    public OrcidSimpleValueConfiguration(OrcidProfileSectionType sectionType, OrcidProfileSyncPreference preference,
        String metadataField) {
        super(sectionType, preference);
        this.metadataField = metadataField;
    }

    public String getMetadataField() {
        return metadataField;
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(COUNTRY, KEYWORDS, OTHER_NAMES, RESEARCHER_URLS);
    }

    @Override
    public List<String> getMetadataFields() {
        return List.of(getMetadataField());
    }

}
