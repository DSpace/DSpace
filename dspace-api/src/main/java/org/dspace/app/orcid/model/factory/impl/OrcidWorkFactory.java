/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.model.factory.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.io.ByteArrayOutputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.EnumUtils;
import org.dspace.app.orcid.model.OrcidEntityType;
import org.dspace.app.orcid.model.OrcidWorkFieldMapping;
import org.dspace.app.orcid.model.factory.OrcidCommonObjectFactory;
import org.dspace.app.orcid.model.factory.OrcidEntityFactory;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.integration.crosswalks.CSLItemDataCrosswalk;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
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
 * {@link Work}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidWorkFactory implements OrcidEntityFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidWorkFactory.class);

    @Autowired
    private ItemService itemService;

    @Autowired
    private OrcidCommonObjectFactory orcidCommonObjectFactory;

    private OrcidWorkFieldMapping fieldMapping;

    @Override
    public OrcidEntityType getEntityType() {
        return OrcidEntityType.PUBLICATION;
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
            .filter(metadataValue -> isNotBlank(metadataValue.getValue()))
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

    private WorkTitle getWorkTitle(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTitleField())
            .filter(metadataValue -> isNotBlank(metadataValue.getValue()))
            .map(metadataValue -> getWorkTitle(context, item, metadataValue))
            .orElse(null);
    }

    private WorkTitle getWorkTitle(Context context, Item item, MetadataValue metadataValue) {
        WorkTitle workTitle = new WorkTitle();
        workTitle.setTitle(new Title(metadataValue.getValue()));
        workTitle.setSubtitle(getSubTitle(context, item));
        return workTitle;
    }

    private Subtitle getSubTitle(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getSubTitleField())
            .map(MetadataValue::getValue)
            .map(Subtitle::new)
            .orElse(null);
    }

    private PublicationDate getPublicationDate(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getPublicationDateField())
            .flatMap(orcidCommonObjectFactory::createFuzzyDate)
            .map(PublicationDate::new)
            .orElse(null);
    }

    private ExternalIDs getWorkExternalIds(Context context, Item item) {

        Map<String, String> externalIdentifierFields = fieldMapping.getExternalIdentifierFields();

        ExternalIDs externalIdentifiers = new ExternalIDs();

        if (externalIdentifierFields.containsKey(SIMPLE_HANDLE_PLACEHOLDER)) {
            String handleType = externalIdentifierFields.get(SIMPLE_HANDLE_PLACEHOLDER);
            externalIdentifiers.getExternalIdentifier().add(getExternalId(handleType, item.getHandle()));
        }

        getMetadataValues(context, item, externalIdentifierFields.keySet()).stream()
            .map(this::getExternalId)
            .forEach(externalIdentifiers.getExternalIdentifier()::add);

        return externalIdentifiers;
    }

    private ExternalID getExternalId(MetadataValue metadataValue) {
        Map<String, String> externalIdentifierFields = fieldMapping.getExternalIdentifierFields();
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

    private WorkType getWorkType(Context context, Item item) {
        return getMetadataValue(context, item, fieldMapping.getTypeField())
            .map(MetadataValue::getValue)
            .map(type -> fieldMapping.convertType(type))
            .flatMap(this::getWorkType)
            .orElse(null);
    }

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

    private List<MetadataValue> getMetadataValues(Context context, Item item, Collection<String> metadataFields) {
        return metadataFields.stream()
            .flatMap(metadataField -> itemService.getMetadataByMetadataString(item, metadataField).stream())
            .collect(Collectors.toList());
    }

    private Optional<MetadataValue> getMetadataValue(Context context, Item item, String metadataField) {
        return itemService.getMetadataByMetadataString(item, metadataField).stream().findFirst();
    }

    public ItemService getItemService() {
        return itemService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public OrcidCommonObjectFactory getOrcidCommonObjectFactory() {
        return orcidCommonObjectFactory;
    }

    public void setOrcidCommonObjectFactory(OrcidCommonObjectFactory orcidCommonObjectFactory) {
        this.orcidCommonObjectFactory = orcidCommonObjectFactory;
    }

    public void setFieldMapping(OrcidWorkFieldMapping fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

}
