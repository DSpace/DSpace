/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.model.factory.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.OrcidFundingFieldMapping;
import org.dspace.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.orcid.model.factory.OrcidEntityFactory;
import org.orcid.jaxb.model.common.FundingContributorRole;
import org.orcid.jaxb.model.common.FundingType;
import org.orcid.jaxb.model.v3.release.common.Amount;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidEntityFactory} that creates instances of
 * {@link Funding}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidFundingFactory implements OrcidEntityFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidFundingFactory.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidCommonObjectFactory orcidCommonObjectFactory;

    @Autowired
    private RelationshipTypeService relationshipTypeService;

    @Autowired
    private RelationshipService relationshipService;

    private OrcidFundingFieldMapping fieldMapping;

    @Override
    public OrcidEntityType getEntityType() {
        return OrcidEntityType.FUNDING;
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
        funding.setAmount(getAmount(context, item));
        return funding;
    }

    private FundingContributors getContributors(Context context, Item item) {
        FundingContributors fundingContributors = new FundingContributors();
        getMetadataValues(context, item, fieldMapping.getContributorFields().keySet()).stream()
            .map(metadataValue -> getFundingContributor(context, metadataValue))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .forEach(fundingContributors.getContributor()::add);
        return fundingContributors;
    }

    private Optional<FundingContributor> getFundingContributor(Context context, MetadataValue metadataValue) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        FundingContributorRole role = fieldMapping.getContributorFields().get(metadataField);
        return orcidCommonObjectFactory.createFundingContributor(context, metadataValue, role);
    }


    private String getDescription(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getDescriptionField())
            .map(MetadataValue::getValue)
            .orElse(null);
    }

    private FuzzyDate getEndDate(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getEndDateField())
            .flatMap(metadataValue -> orcidCommonObjectFactory.createFuzzyDate(metadataValue))
            .orElse(null);
    }

    private ExternalIDs getExternalIds(Context context, Item item) {
        ExternalIDs externalIdentifiers = new ExternalIDs();

        getMetadataValues(context, item, fieldMapping.getExternalIdentifierFields().keySet()).stream()
            .map(this::getExternalId)
            .forEach(externalIdentifiers.getExternalIdentifier()::add);

        return externalIdentifiers;
    }

    private ExternalID getExternalId(MetadataValue metadataValue) {
        String metadataField = metadataValue.getMetadataField().toString('.');
        return getExternalId(fieldMapping.getExternalIdentifierFields().get(metadataField), metadataValue.getValue());
    }

    private ExternalID getExternalId(String type, String value) {
        ExternalID externalID = new ExternalID();
        externalID.setType(type);
        externalID.setValue(value);
        externalID.setRelationship(org.orcid.jaxb.model.common.Relationship.SELF);
        return externalID;
    }

    /**
     * Returns an Organization ORCID entity related to the given item. The
     * relationship type configured with
     * orcid.mapping.funding.organization-relationship-type is the relationship used
     * to search the Organization of the given project item.
     */
    private Organization getOrganization(Context context, Item item) {

        try {

            return relationshipTypeService.findByLeftwardOrRightwardTypeName(context,
                fieldMapping.getOrganizationRelationshipType()).stream()
                .flatMap(relationshipType -> getRelationships(context, item, relationshipType))
                .map(relationship -> getRelatedItem(item, relationship))
                .flatMap(orgUnit -> orcidCommonObjectFactory.createOrganization(context, orgUnit).stream())
                .findFirst()
                .orElse(null);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    private Stream<Relationship> getRelationships(Context context, Item item, RelationshipType relationshipType) {
        try {
            return relationshipService.findByItemAndRelationshipType(context, item, relationshipType).stream();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Item getRelatedItem(Item item, Relationship relationship) {
        return item.equals(relationship.getLeftItem()) ? relationship.getRightItem() : relationship.getLeftItem();
    }

    private FuzzyDate getStartDate(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getStartDateField())
            .flatMap(metadataValue -> orcidCommonObjectFactory.createFuzzyDate(metadataValue))
            .orElse(null);
    }

    private FundingTitle getTitle(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTitleField())
            .map(metadataValue -> getFundingTitle(context, metadataValue))
            .orElse(null);
    }

    private FundingTitle getFundingTitle(Context context, MetadataValue metadataValue) {
        FundingTitle fundingTitle = new FundingTitle();
        fundingTitle.setTitle(new Title(metadataValue.getValue()));
        return fundingTitle;
    }

    /**
     * Returns an instance of FundingType taking the type from the given item. The
     * metadata field to be used to retrieve the item's type is related to the
     * configured typeField (orcid.mapping.funding.type).
     */
    private FundingType getType(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTypeField())
            .map(type -> fieldMapping.convertType(type.getValue()))
            .flatMap(this::getFundingType)
            .orElse(FundingType.CONTRACT);
    }

    private Optional<FundingType> getFundingType(String type) {
        try {
            return Optional.ofNullable(FundingType.fromValue(type));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("The type {} is not valid for ORCID fundings", type);
            return Optional.empty();
        }
    }

    private Url getUrl(Context context, Item item) {
        return orcidCommonObjectFactory.createUrl(context, item).orElse(null);
    }

    /**
     * Returns an Amount instance taking the amount and currency value from the
     * configured metadata values of the given item, if any.
     */
    private Amount getAmount(Context context, Item item) {

        Optional<String> amount = getAmountValue(context, item);
        Optional<String> currency = getCurrencyValue(context, item);

        if (amount.isEmpty() || currency.isEmpty()) {
            return null;
        }

        return getAmount(amount.get(), currency.get());
    }

    /**
     * Returns the amount value of the configured metadata field
     * orcid.mapping.funding.amount
     */
    private Optional<String> getAmountValue(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getAmountField())
            .map(MetadataValue::getValue);
    }

    /**
     * Returns the amount value of the configured metadata field
     * orcid.mapping.funding.amount.currency (if configured using the converter
     * orcid.mapping.funding.amount.currency.converter).
     */
    private Optional<String> getCurrencyValue(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getAmountCurrencyField())
            .map(currency -> fieldMapping.convertAmountCurrency(currency.getValue()))
            .filter(currency -> isValidCurrency(currency));
    }

    private boolean isValidCurrency(String currency) {
        try {
            return currency != null && Currency.getInstance(currency) != null;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private Amount getAmount(String amount, String currency) {
        Amount amountObj = new Amount();
        amountObj.setContent(amount);
        amountObj.setCurrencyCode(currency);
        return amountObj;
    }

    private List<MetadataValue> getMetadataValues(Context context, Item item, Collection<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> itemService.getMetadataByMetadataString(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private Optional<MetadataValue> getMetadataValue(Context context, Item item, String metadataField) {
        if (isBlank(metadataField)) {
            return Optional.empty();
        }
        return itemService.getMetadataByMetadataString(item, metadataField).stream().findFirst()
            .filter(metadataValue -> isNotBlank(metadataValue.getValue()));
    }

    public OrcidFundingFieldMapping getFieldMapping() {
        return fieldMapping;
    }

    public void setFieldMapping(OrcidFundingFieldMapping fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

}
