/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model;

import static org.dspace.app.orcid.model.OrcidProfileSectionType.AFFILIATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EDUCATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.QUALIFICATION;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.orcid.jaxb.model.v3.release.record.Affiliation;

/**
 * Implementation of {@link OrcidProfileSectionConfiguration} that model the
 * ORCID affiliations (Education, Employment, Qualification etc..). This
 * configuration will be used to build instance of {@link Affiliation}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidAffiliationConfiguration extends OrcidProfileSectionConfiguration {

    private final String departmentField;

    private final String roleTitleField;

    private final String startDateField;

    private final String endDateField;

    public OrcidAffiliationConfiguration(OrcidProfileSectionType sectionType, OrcidProfileSyncPreference preference,
        String departmentField, String roleTitleField, String startDateField, String endDateField) {
        super(sectionType, preference);
        this.departmentField = departmentField;
        this.roleTitleField = roleTitleField;
        this.startDateField = startDateField;
        this.endDateField = endDateField;
    }

    @Override
    public List<String> getMetadataFields() {
        return List.of(departmentField, roleTitleField, startDateField, endDateField).stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    public String getDepartmentField() {
        return departmentField;
    }

    public String getRoleTitleField() {
        return roleTitleField;
    }

    public String getStartDateField() {
        return startDateField;
    }

    public String getEndDateField() {
        return endDateField;
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(AFFILIATION, QUALIFICATION, EDUCATION);
    }

}
