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
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.orcid.jaxb.model.common.Relationship.FUNDED_BY;
import static org.orcid.jaxb.model.common.Relationship.SELF;

import java.io.ByteArrayOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.integration.crosswalks.CSLItemDataCrosswalk;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.orcid.model.OrcidEntityType;
import org.dspace.orcid.model.OrcidProductWorkFieldMapping;
import org.dspace.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.orcid.model.factory.OrcidEntityFactory;
import org.dspace.util.UUIDUtils;
import org.orcid.jaxb.model.common.CitationType;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.LanguageCode;
import org.orcid.jaxb.model.common.Relationship;
import org.orcid.jaxb.model.common.WorkType;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.PublicationDate;
import org.orcid.jaxb.model.v3.release.common.Subtitle;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.common.Url;
import org.orcid.jaxb.model.v3.release.record.Activity;
import org.orcid.jaxb.model.v3.release.record.Citation;
import org.orcid.jaxb.model.v3.release.record.ExternalID;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkContributors;
import org.orcid.jaxb.model.v3.release.record.WorkTitle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link OrcidEntityFactory} that creates instances of
 * {@link Work}. Copy of {@link OrcidWorkFactory}
 * Adapted for Product Entity with own mapping in {@link org.dspace.orcid.model.OrcidProductWorkFieldMapping}
 *
 */
public class OrcidProductWorkFactory implements OrcidEntityFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidProductWorkFactory.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidCommonObjectFactory orcidCommonObjectFactory;

    private OrcidProductWorkFieldMapping fieldMapping;

    @Override
    public OrcidEntityType getEntityType() {
        return OrcidEntityType.PRODUCT;
    }

    @Override
    public Activity createOrcidObject(Context context, Item item) {
        Work work = new Work();
        work.setJournalTitle(getJournalTitle(context, item));
        work.setWorkContributors(getWorkContributors(context, item));
        work.setWorkTitle(getWorkTitle(context, item));
        work.setPublicationDate(getPublicationDate(context, item));
        work.setWorkExternalIdentifiers(getWorkExternalIds(context, item));
        work.setWorkType(getWorkType(context, item));
        work.setWorkCitation(getWorkCitation(context, item));
        work.setShortDescription(getShortDescription(context, item));
        work.setLanguageCode(getLanguageCode(context, item));
        work.setUrl(getUrl(context, item));
        return work;
    }

    private Title getJournalTitle(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getJournalTitleField())
            .map(metadataValue -> new Title(metadataValue.getValue()))
            .orElse(null);
    }

    private WorkContributors getWorkContributors(Context context, Item item) {
        Map<String, ContributorRole> contributorFields = fieldMapping.getContributorFields();
        List<Contributor> contributors = getMetadataValues(context, item, contributorFields.keySet()).stream()
            .map(metadataValue -> getContributor(context, metadataValue))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
        return new WorkContributors(contributors);
    }

    private Optional<Contributor> getContributor(Context context, MetadataValue metadataValue) {
        Map<String, ContributorRole> contributorFields = fieldMapping.getContributorFields();
        ContributorRole role = contributorFields.get(metadataValue.getMetadataField().toString('.'));
        return orcidCommonObjectFactory.createContributor(context, metadataValue, role);
    }

    /**
     * Create an instance of WorkTitle from the given item.
     */
    private WorkTitle getWorkTitle(Context context, Item item) {
        Optional<String> workTitleValue = getWorkTitleValue(context, item);
        if (workTitleValue.isEmpty()) {
            return null;
        }

        WorkTitle workTitle = new WorkTitle();
        workTitle.setTitle(new Title(workTitleValue.get()));
        getSubTitle(context, item).ifPresent(workTitle::setSubtitle);
        return workTitle;
    }

    /**
     * Take the work title from the configured metadata field of the given item
     * (orcid.mapping.work.title), if any.
     */
    private Optional<String> getWorkTitleValue(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTitleField())
            .map(MetadataValue::getValue);
    }

    /**
     * Take the work title from the configured metadata field of the given item
     * (orcid.mapping.work.sub-title), if any.
     */
    private Optional<Subtitle> getSubTitle(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getSubTitleField())
            .map(MetadataValue::getValue)
            .map(Subtitle::new);
    }

    private PublicationDate getPublicationDate(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getPublicationDateField())
            .flatMap(orcidCommonObjectFactory::createFuzzyDate)
            .map(PublicationDate::new)
            .orElse(null);
    }

    /**
     * Creates an instance of ExternalIDs from the metadata values of the given
     * item, using the orcid.mapping.funding.external-ids configuration.
     */
    private ExternalIDs getWorkExternalIds(Context context, Item item) {
        ExternalIDs externalIdentifiers = new ExternalIDs();
        externalIdentifiers.getExternalIdentifier().addAll(getWorkSelfExternalIds(context, item));
        externalIdentifiers.getExternalIdentifier().addAll(getWorkFundedByExternalIds(context, item));
        return externalIdentifiers;
    }

    /**
     * Creates a list of ExternalID, one for orcid.mapping.funding.external-ids
     * value, taking the values from the given item.
     */
    private List<ExternalID> getWorkSelfExternalIds(Context context, Item item) {

        List<ExternalID> selfExternalIds = new ArrayList<ExternalID>();

        Map<String, String> externalIdentifierFields = fieldMapping.getExternalIdentifierFields();

        if (externalIdentifierFields.containsKey(SIMPLE_HANDLE_PLACEHOLDER)) {
            String handleType = externalIdentifierFields.get(SIMPLE_HANDLE_PLACEHOLDER);
            selfExternalIds.add(getExternalId(handleType, item.getHandle(), SELF));
        }

        getMetadataValues(context, item, externalIdentifierFields.keySet()).stream()
            .map(this::getSelfExternalId)
            .forEach(selfExternalIds::add);

        return selfExternalIds;
    }

    /**
     * Creates an instance of ExternalID taking the value from the given
     * metadataValue. The type of the ExternalID is calculated using the
     * orcid.mapping.funding.external-ids configuration. The relationship of the
     * ExternalID is SELF.
     */
    private ExternalID getSelfExternalId(MetadataValue metadataValue) {
        Map<String, String> externalIdentifierFields = fieldMapping.getExternalIdentifierFields();
        String metadataField = metadataValue.getMetadataField().toString('.');
        return getExternalId(externalIdentifierFields.get(metadataField), metadataValue.getValue(), SELF);
    }

    private List<ExternalID> getWorkFundedByExternalIds(Context context, Item item) {

        if (isBlank(fieldMapping.getFundingExternalIdType())) {
            return Collections.emptyList();
        }

        return getMetadataValues(context, item, fieldMapping.getFundingField()).stream()
            .map(metadataValue -> getWorkFundedByExternalId(context, item, metadataValue))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private Optional<ExternalID> getWorkFundedByExternalId(Context context, Item work, MetadataValue fundingMetadata) {
        return getFundedByExternalIdFromFunding(context, fundingMetadata)
            .or(() -> getFundedByExternalIdFromWork(context, work, fundingMetadata.getPlace()));
    }

    private Optional<ExternalID> getFundedByExternalIdFromFunding(Context context, MetadataValue fundingMetadata) {

        if (isAuthoritySet(fundingMetadata.getAuthority())) {
            return findItemById(context, UUIDUtils.fromString(fundingMetadata.getAuthority()))
                .map(funding -> getFundingExternalId(context, funding));
        }

        return Optional.empty();
    }

    private Optional<ExternalID> getFundedByExternalIdFromWork(Context context, Item work, int fundingPlace) {
        List<MetadataValue> externalIdValues = getMetadataValues(context, work, fieldMapping.getFundingExternalId());

        if (externalIdValues.size() > fundingPlace && isNotPlaceholder(externalIdValues.get(fundingPlace))) {
            String value = externalIdValues.get(fundingPlace).getValue();
            return Optional.of(getExternalId(fieldMapping.getFundingExternalIdType(), value, FUNDED_BY));
        }

        return Optional.empty();
    }

    private ExternalID getFundingExternalId(Context context, Item funding) {

        String externalIdValue = getMetadataValue(context, funding, fieldMapping.getFundingEntityExternalId())
            .map(MetadataValue::getValue)
            .orElse(null);

        if (externalIdValue == null) {
            return null;
        }

        Optional<Url> fundingUrl = getMetadataValue(context, funding, fieldMapping.getFundingUrlField())
            .map(fundingUrlMetadata -> new Url(fundingUrlMetadata.getValue()))
            .or(() -> orcidCommonObjectFactory.createUrl(context, funding));

        ExternalID externalId = getExternalId(fieldMapping.getFundingExternalIdType(), externalIdValue, FUNDED_BY);
        fundingUrl.ifPresent(externalId::setUrl);
        return externalId;
    }

    private boolean isAuthoritySet(String authority) {
        return isNotBlank(authority) && !StringUtils.startsWith(authority, AuthorityValueService.REFERENCE);
    }

    /**
     * Creates an instance of ExternalID with the given type, value and
     * relationship.
     */
    private ExternalID getExternalId(String type, String value, Relationship relationship) {
        ExternalID externalID = new ExternalID();
        externalID.setType(type);
        externalID.setValue(value);
        externalID.setRelationship(relationship);
        return externalID;
    }

    /**
     * Creates an instance of WorkType from the given item, taking the value fom the
     * configured metadata field (orcid.mapping.work.type).
     */
    private WorkType getWorkType(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTypeField())
            .map(MetadataValue::getValue)
            .map(type -> fieldMapping.convertType(type))
            .flatMap(this::getWorkType)
            .orElse(WorkType.UNDEFINED);
    }

    /**
     * Creates an instance of WorkType from the given workType value, if valid.
     */
    private Optional<WorkType> getWorkType(String workType) {
        try {
            return Optional.ofNullable(WorkType.fromValue(workType));
        } catch (IllegalArgumentException ex) {
            LOGGER.warn("The type {} is not valid for ORCID works", workType);
            return Optional.empty();
        }
    }

    private Citation getWorkCitation(Context context, Item item) {

        CSLItemDataCrosswalk citationCrosswalk = getCitationCrosswalk();

        if (citationCrosswalk == null || !citationCrosswalk.canDisseminate(context, item)) {
            return null;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            citationCrosswalk.disseminate(context, item, out);
            return new Citation(out.toString(), fieldMapping.getCitationType());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CSLItemDataCrosswalk getCitationCrosswalk() {
        CitationType citationType = fieldMapping.getCitationType();
        return citationType != null ? fieldMapping.getCitationCrosswalks().get(citationType.value()) : null;
    }

    private String getShortDescription(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getShortDescriptionField())
            .map(MetadataValue::getValue)
            .orElse(null);
    }

    private String getLanguageCode(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getLanguageField())
            .map(MetadataValue::getValue)
            .map(language -> fieldMapping.convertLanguage(language))
            .filter(language -> isValidLanguage(language))
            .orElse(null);
    }

    private boolean isValidLanguage(String language) {

        if (isBlank(language)) {
            return false;
        }

        boolean isValid = EnumUtils.isValidEnum(LanguageCode.class, language);
        if (!isValid) {
            LOGGER.warn("The language {} is not a valid language code for ORCID works", language);
        }
        return isValid;
    }

    private Url getUrl(Context context, Item item) {
        return orcidCommonObjectFactory.createUrl(context, item).orElse(null);
    }

    private List<MetadataValue> getMetadataValues(Context context, Item item, String metadataField) {
        if (isBlank(metadataField)) {
            return Collections.emptyList();
        }
        return itemService.getMetadataByMetadataString(item, metadataField);
    }

    private boolean isNotPlaceholder(MetadataValue metadata) {
        return metadata != null && metadata.getValue() != null
            && !metadata.getValue().equals(PLACEHOLDER_PARENT_METADATA_VALUE);
    }

    private List<MetadataValue> getMetadataValues(Context context, Item item, Collection<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> itemService.getMetadataByMetadataString(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private Optional<Item> findItemById(Context context, UUID id) {
        try {
            return Optional.ofNullable(itemService.find(context, id));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Optional<MetadataValue> getMetadataValue(Context context, Item item, String metadataField) {

        if (isBlank(metadataField)) {
            return Optional.empty();
        }

        return itemService.getMetadataByMetadataString(item, metadataField).stream()
            .filter(metadataValue -> isNotBlank(metadataValue.getValue()))
            .findFirst();
    }

    public void setFieldMapping(OrcidProductWorkFieldMapping fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

}
