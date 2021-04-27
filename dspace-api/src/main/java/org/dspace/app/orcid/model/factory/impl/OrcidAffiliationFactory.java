/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.AFFILIATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EDUCATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.QUALIFICATION;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.orcid.model.OrcidProfileSectionType;
import org.dspace.app.profile.OrcidProfileSyncPreference;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.orcid.jaxb.model.v3.release.record.Affiliation;
import org.orcid.jaxb.model.v3.release.record.Education;
import org.orcid.jaxb.model.v3.release.record.Employment;
import org.orcid.jaxb.model.v3.release.record.Qualification;

/**
 * Implementation of {@link OrcidProfileSectionFactory} that creates ORCID
 * affiliations (Education, Employment, Qualification etc..). This factory will
 * be used to create instance of {@link Affiliation}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidAffiliationFactory extends AbstractOrcidProfileSectionFactory {

    private final String organizationField;

    private final String roleField;

    private final String startDateField;

    private final String endDateField;

    public OrcidAffiliationFactory(OrcidProfileSectionType sectionType, OrcidProfileSyncPreference preference,
        String organizationField, String roleField, String startDateField, String endDateField) {
        super(sectionType, preference);
        this.organizationField = organizationField;
        this.roleField = roleField;
        this.startDateField = startDateField;
        this.endDateField = endDateField;
    }

    @Override
    public List<String> getMetadataFields() {
        return List.of(organizationField, roleField, startDateField, endDateField).stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    public String getOrganizationField() {
        return organizationField;
    }

    public String getRoleField() {
        return roleField;
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

    @Override
    public List<Object> create(Context context, Item item) {

        List<Object> objects = new ArrayList<Object>();

        Map<String, List<MetadataValue>> metadataGroups = getMetadataGroups(item);
        int groupSize = metadataGroups.getOrDefault(organizationField, emptyList()).size();
        for (int currentGroupIndex = 0; currentGroupIndex < groupSize; currentGroupIndex++) {
            objects.add(buildAffiliation(context, item, metadataGroups, currentGroupIndex));
        }

        return objects;
    }

    private Map<String, List<MetadataValue>> getMetadataGroups(Item item) {
        Map<String, List<MetadataValue>> metadataGroups = new HashMap<>();
        metadataGroups.put(organizationField, itemService.getMetadataByMetadataString(item, organizationField));
        metadataGroups.put(roleField, itemService.getMetadataByMetadataString(item, roleField));
        metadataGroups.put(startDateField, itemService.getMetadataByMetadataString(item, startDateField));
        metadataGroups.put(endDateField, itemService.getMetadataByMetadataString(item, endDateField));
        return metadataGroups;
    }

    private Affiliation buildAffiliation(Context context, Item item, Map<String, List<MetadataValue>> metadataGroups,
        int currentGroupIndex) {

        Affiliation affiliation = buildAffiliation();

        MetadataValue organization = getCurrentNestedMetadata(metadataGroups, organizationField, currentGroupIndex);
        MetadataValue role = getCurrentNestedMetadata(metadataGroups, roleField, currentGroupIndex);
        MetadataValue startDate = getCurrentNestedMetadata(metadataGroups, startDateField, currentGroupIndex);
        MetadataValue endDate = getCurrentNestedMetadata(metadataGroups, endDateField, currentGroupIndex);

        orcidCommonObjectFactory.createFuzzyDate(startDate).ifPresent(affiliation::setStartDate);
        orcidCommonObjectFactory.createFuzzyDate(endDate).ifPresent(affiliation::setEndDate);
        affiliation.setRoleTitle(isUnprocessableValue(role) ? null : role.getValue());

        affiliation.setOrganization(orcidCommonObjectFactory.createOrganization(context, organization));

        return affiliation;
    }

    private MetadataValue getCurrentNestedMetadata(Map<String, List<MetadataValue>> metadataGroups,
        String metadataField, int currentGroupIndex) {
        List<MetadataValue> metadataValues = metadataGroups.getOrDefault(metadataField, emptyList());
        return metadataValues.size() > currentGroupIndex ? metadataValues.get(currentGroupIndex) : null;
    }

    private Affiliation buildAffiliation() {
        switch (sectionType) {
            case AFFILIATION:
                return new Employment();
            case EDUCATION:
                return new Education();
            case QUALIFICATION:
                return new Qualification();
            default:
                throw new IllegalStateException("Orcid affiliation builder does not supports " + sectionType);
        }
    }

    private boolean isUnprocessableValue(MetadataValue value) {
        return value == null || isBlank(value.getValue()) || value.getValue().equals(PLACEHOLDER_PARENT_METADATA_VALUE);
    }

}
