/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.dspace.app.orcid.model.factory.OrcidFactoryUtils.parseConfigurations;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.dspace.app.orcid.model.OrcidEntityType;
import org.dspace.app.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.app.orcid.model.factory.OrcidEntityFactory;
import org.dspace.app.orcid.model.factory.OrcidFactoryUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.FundingType;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.v3.release.common.FuzzyDate;
import org.orcid.jaxb.model.v3.release.common.Organization;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.Activity;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.FundingContributor;
import org.orcid.jaxb.model.v3.release.record.FundingContributors;
import org.orcid.jaxb.model.v3.release.record.FundingTitle;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidEntityFactory} that creates instances of
 * {@link Funding}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidFundingFactory implements OrcidEntityFactory {

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidCommonObjectFactory orcidCommonObjectFactory;

    private Map<String, FundingContributorRole> contributorFields;

    private Map<String, String> externalIdentifierFields;

    private String titleField;

    private String startDateField;

    private String endDateField;

    private String descriptionField;

    private String organizationField;

    @Override
    public OrcidEntityType getEntityType() {
        return OrcidEntityType.PROJECT;
    }

    @Override
    public Activity createOrcidObject(Context context, Item item) {
        Funding funding = new Funding();
        funding.setContributors(getContributors(context, item));
        funding.setDescription(getDescription(context, item));
        funding.setEndDate(getEndDate(context, item));
        funding.setExternalIdentifiers(getExternalIds(context, item));
        funding.setOrganization(getOrganization(context, item));
        funding.setStartDate(getStartDate(context, item));
        funding.setTitle(getTitle(context, item));
        funding.setType(getType(context, item));
        funding.setUrl(getUrl(context, item));
        return funding;
    }

    private FundingContributors getContributors(Context context, Item item) {
        FundingContributors fundingContributors = new FundingContributors();
        getMetadataValues(context, item, contributorFields.keySet()).stream()
            .map(metadataValue -> getFundingContributor(context, metadataValue))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(fundingContributors.getContributor()::add);
        return fundingContributors;
    }

    private Optional<FundingContributor> getFundingContributor(Context context, MetadataValue metadataValue) {
        FundingContributorRole role = contributorFields.get(metadataValue.getMetadataField().toString('.'));
        return orcidCommonObjectFactory.createFundingContributor(context, metadataValue, role);
    }


    private String getDescription(Context context, Item item) {
        return getMetadataValue(context, item, descriptionField)
            .map(MetadataValue::getValue)
            .orElse(null);
    }

    private FuzzyDate getEndDate(Context context, Item item) {
        return getMetadataValue(context, item, endDateField)
            .flatMap(metadataValue -> orcidCommonObjectFactory.createFuzzyDate(metadataValue))
            .orElse(null);
    }

    private ExternalIDs getExternalIds(Context context, Item item) {
        ExternalIDs externalIdentifiers = new ExternalIDs();

        getMetadataValues(context, item, externalIdentifierFields.keySet()).stream()
            .map(this::getExternalId)
            .forEach(externalIdentifiers.getExternalIdentifier()::add);

        return externalIdentifiers;
    }

    private ExternalID getExternalId(MetadataValue metadataValue) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        return getExternalId(externalIdentifierFields.get(metadataField), metadataValue.getValue());
    }

    private ExternalID getExternalId(String type, String value) {
        ExternalID externalID = new ExternalID();
        externalID.setType(type);
        externalID.setValue(value);
        externalID.setRelationship(Relationship.SELF);
        return externalID;
    }

    private Organization getOrganization(Context context, Item item) {
        return getMetadataValue(context, item, organizationField)
            .flatMap(metadataValue -> orcidCommonObjectFactory.createOrganization(context, metadataValue))
            .orElse(null);
    }

    private FuzzyDate getStartDate(Context context, Item item) {
        return getMetadataValue(context, item, startDateField)
            .flatMap(metadataValue -> orcidCommonObjectFactory.createFuzzyDate(metadataValue))
            .orElse(null);
    }

    private FundingTitle getTitle(Context context, Item item) {
        return getMetadataValue(context, item, titleField)
            .filter(metadataValue -> isNotBlank(metadataValue.getValue()))
            .map(metadataValue -> getFundingTitle(context, metadataValue))
            .orElse(null);
    }

    private FundingTitle getFundingTitle(Context context, MetadataValue metadataValue) {
        FundingTitle fundingTitle = new FundingTitle();
        fundingTitle.setTitle(new Title(metadataValue.getValue()));
        return fundingTitle;
    }

    private FundingType getType(Context context, Item item) {
        return FundingType.GRANT;
    }

    private Url getUrl(Context context, Item item) {
        return orcidCommonObjectFactory.createUrl(context, item).orElse(null);
    }

    private List<MetadataValue> getMetadataValues(Context context, Item item, Collection<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> itemService.getMetadataByMetadataString(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private Optional<MetadataValue> getMetadataValue(Context context, Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream().findFirst();
    }

    private Map<String, FundingContributorRole> parseContributors(String contributors) {
        Map<String, String> contributorsMap = parseConfigurations(contributors);
        return contributorsMap.keySet().stream()
            .collect(toMap(identity(), field -> parseContributorRole(contributorsMap.get(field))));
    }

    private FundingContributorRole parseContributorRole(String contributorRole) {
        try {
            return FundingContributorRole.fromValue(contributorRole);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("The funding contributor role " + contributorRole +
                " is invalid, allowed values are " + getAllowedContributorRoles(), ex);
        }
    }

    private List<String> getAllowedContributorRoles() {
        return Arrays.asList(FundingContributorRole.values()).stream()
            .map(FundingContributorRole::value)
            .collect(Collectors.toList());
    }

    public Map<String, String> getExternalIdentifierFields() {
        return externalIdentifierFields;
    }

    public void setExternalIdentifierFields(String externalIdentifierFields) {
        this.externalIdentifierFields = OrcidFactoryUtils.parseConfigurations(externalIdentifierFields);
    }

    public Map<String, FundingContributorRole> getContributorFields() {
        return contributorFields;
    }

    public void setContributorFields(String contributorFields) {
        this.contributorFields = parseContributors(contributorFields);
    }

    public String getTitleField() {
        return titleField;
    }

    public void setTitleField(String titleField) {
        this.titleField = titleField;
    }

    public String getStartDateField() {
        return startDateField;
    }

    public void setStartDateField(String startDateField) {
        this.startDateField = startDateField;
    }

    public String getEndDateField() {
        return endDateField;
    }

    public void setEndDateField(String endDateField) {
        this.endDateField = endDateField;
    }

    public String getDescriptionField() {
        return descriptionField;
    }

    public void setDescriptionField(String descriptionField) {
        this.descriptionField = descriptionField;
    }

    public String getOrganizationField() {
        return organizationField;
    }

    public void setOrganizationField(String organizationField) {
        this.organizationField = organizationField;
    }

}
