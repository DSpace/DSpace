/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.AFFILIATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.EDUCATION;
import static org.dspace.app.orcid.model.OrcidProfileSectionType.QUALIFICATION;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
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

    private String organizationField;

    private String roleField;

    private String startDateField;

    private String endDateField;

    public OrcidAffiliationFactory(OrcidProfileSectionType sectionType, OrcidProfileSyncPreference preference) {
        super(sectionType, preference);
    }

    @Override
    public List<String> getMetadataFields() {
        return List.of(organizationField, roleField, startDateField, endDateField).stream()
            .filter(StringUtils::isNotBlank)
            .collect(Collectors.toList());
    }

    @Override
    public List<OrcidProfileSectionType> getSupportedTypes() {
        return List.of(AFFILIATION, QUALIFICATION, EDUCATION);
    }

    @Override
    public Object create(Context context, List<MetadataValue> metadataValues) {

        Affiliation affiliation = buildAffiliation();

        MetadataValue organization = getMetadataValueByField(metadataValues, organizationField);
        MetadataValue role = getMetadataValueByField(metadataValues, roleField);
        MetadataValue startDate = getMetadataValueByField(metadataValues, startDateField);
        MetadataValue endDate = getMetadataValueByField(metadataValues, endDateField);

        orcidCommonObjectFactory.createFuzzyDate(startDate).ifPresent(affiliation::setStartDate);
        orcidCommonObjectFactory.createFuzzyDate(endDate).ifPresent(affiliation::setEndDate);
        affiliation.setRoleTitle(isUnprocessableValue(role) ? null : role.getValue());

        orcidCommonObjectFactory.createOrganization(context, organization).ifPresent(affiliation::setOrganization);

        return affiliation;
    }

    @Override
    public List<String> getMetadataSignatures(Context context, Item item) {
        List<String> signatures = new ArrayList<String>();

        Map<String, List<MetadataValue>> metadataGroups = getMetadataGroups(item);
        int groupSize = metadataGroups.getOrDefault(organizationField, Collections.emptyList()).size();
        for (int currentGroupIndex = 0; currentGroupIndex < groupSize; currentGroupIndex++) {
            List<MetadataValue> metadataValues = getMetadataValueByPlace(metadataGroups, currentGroupIndex);
            signatures.add(metadataSignatureGenerator.generate(context, metadataValues));
        }

        return signatures;
    }

    @Override
    public String getDescription(Context context, Item item, String signature) {
        List<MetadataValue> metadataValues = metadataSignatureGenerator.findBySignature(context, item, signature);
        if (CollectionUtils.isEmpty(metadataValues)) {
            return null;
        }

        MetadataValue organization = getMetadataValueByField(metadataValues, organizationField);
        MetadataValue role = getMetadataValueByField(metadataValues, roleField);
        MetadataValue startDate = getMetadataValueByField(metadataValues, startDateField);
        MetadataValue endDate = getMetadataValueByField(metadataValues, endDateField);

        String description = isUnprocessableValue(role) ? "" : role.getValue() + " at ";
        description += isUnprocessableValue(organization) ? "" : organization.getValue() + " ";
        description += getDateDescription(startDate, endDate);

        return description.trim();
    }

    private String getDateDescription(MetadataValue startDate, MetadataValue endDate) {
        String dateDescription = "( from ";
        dateDescription += isProcessableValue(startDate) ? startDate.getValue() + " to " : "unspecified to ";
        dateDescription += isProcessableValue(endDate) ? endDate.getValue() + " )" : "present )";
        return dateDescription;
    }

    private MetadataValue getMetadataValueByField(List<MetadataValue> metadataValues, String metadataField) {
        return metadataValues.stream()
            .filter(metadataValue -> metadataValue.getMetadataField().toString('.').equals(metadataField))
            .findFirst().orElse(null);
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

    private boolean isProcessableValue(MetadataValue value) {
        return !isUnprocessableValue(value);
    }

    private boolean isUnprocessableValue(MetadataValue value) {
        return value == null || isBlank(value.getValue()) || value.getValue().equals(PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    private Map<String, List<MetadataValue>> getMetadataGroups(Item item) {
        Map<String, List<MetadataValue>> metadataGroups = new HashMap<>();
        metadataGroups.put(organizationField, itemService.getMetadataByMetadataString(item, organizationField));
        metadataGroups.put(roleField, itemService.getMetadataByMetadataString(item, roleField));
        metadataGroups.put(startDateField, itemService.getMetadataByMetadataString(item, startDateField));
        metadataGroups.put(endDateField, itemService.getMetadataByMetadataString(item, endDateField));
        return metadataGroups;
    }

    private List<MetadataValue> getMetadataValueByPlace(Map<String, List<MetadataValue>> metadataGroups, int place) {
        List<MetadataValue> metadataValues = new ArrayList<MetadataValue>();
        for (String metadataField : metadataGroups.keySet()) {
            List<MetadataValue> nestedMetadataValues = metadataGroups.get(metadataField);
            if (nestedMetadataValues.size() > place) {
                metadataValues.add(nestedMetadataValues.get(place));
            }
        }
        return metadataValues;
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

    public void setOrganizationField(String organizationField) {
        this.organizationField = organizationField;
    }

    public void setRoleField(String roleField) {
        this.roleField = roleField;
    }

    public void setStartDateField(String startDateField) {
        this.startDateField = startDateField;
    }

    public void setEndDateField(String endDateField) {
        this.endDateField = endDateField;
    }

}
