/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import static java.util.Collections.emptyList;
import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.Optional.ofNullable;
import static org.apache.commons.collections4.ListUtils.partition;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.orcid.jaxb.model.common.CitationType.FORMATTED_UNSPECIFIED;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.service.ImportService;
import org.dspace.orcid.OrcidToken;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.dspace.orcid.model.OrcidWorkFieldMapping;
import org.dspace.orcid.service.OrcidSynchronizationService;
import org.dspace.orcid.service.OrcidTokenService;
import org.dspace.web.ContextUtil;
import org.orcid.jaxb.model.common.ContributorRole;
import org.orcid.jaxb.model.common.WorkType;
import org.orcid.jaxb.model.v3.release.common.Contributor;
import org.orcid.jaxb.model.v3.release.common.ContributorAttributes;
import org.orcid.jaxb.model.v3.release.common.PublicationDate;
import org.orcid.jaxb.model.v3.release.common.Subtitle;
import org.orcid.jaxb.model.v3.release.common.Title;
import org.orcid.jaxb.model.v3.release.record.Citation;
import org.orcid.jaxb.model.v3.release.record.ExternalIDs;
import org.orcid.jaxb.model.v3.release.record.SourceAware;
import org.orcid.jaxb.model.v3.release.record.Work;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.jaxb.model.v3.release.record.WorkContributors;
import org.orcid.jaxb.model.v3.release.record.WorkTitle;
import org.orcid.jaxb.model.v3.release.record.summary.WorkGroup;
import org.orcid.jaxb.model.v3.release.record.summary.WorkSummary;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ExternalDataProvider} that search for all the works
 * of the profile with the given orcid id that hava a source other than DSpace.
 * The id of the external data objects returned by the methods of this class is
 * the concatenation of the orcid id and the put code associated with the
 * publication, separated by :: (example 0000-0000-0123-4567::123456)
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidPublicationDataProvider extends AbstractExternalDataProvider {

    private final static Logger LOGGER = LoggerFactory.getLogger(OrcidPublicationDataProvider.class);

    /**
     * Examples of valid ORCID IDs:
     * <ul>
     * <li>0000-0002-1825-0097</li>
     * <li>0000-0001-5109-3700</li>
     * <li>0000-0002-1694-233X</li>
     * </ul>
     */
    private final static Pattern ORCID_ID_PATTERN = Pattern.compile("(\\d{4}-){3}\\d{3}(\\d|X)");

    private final static int MAX_PUT_CODES_SIZE = 100;

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    @Autowired
    private OrcidSynchronizationService orcidSynchronizationService;

    @Autowired
    private ImportService importService;

    @Autowired
    private OrcidTokenService orcidTokenService;

    private OrcidWorkFieldMapping fieldMapping;

    private String sourceIdentifier;

    private String readPublicAccessToken;

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {

        if (isInvalidIdentifier(id)) {
            throw new IllegalArgumentException("Invalid identifier '" + id + "', expected <orcid-id>::<put-code>");
        }

        String[] idSections = id.split("::");
        String orcid = idSections[0];
        String putCode = idSections[1];

        validateOrcidId(orcid);

        return getWork(orcid, putCode)
            .filter(work -> hasDifferentSourceClientId(work))
            .filter(work -> work.getPutCode() != null)
            .map(work -> convertToExternalDataObject(orcid, work));
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String orcid, int start, int limit) {

        validateOrcidId(orcid);

        return findWorks(orcid, start, limit).stream()
            .map(work -> convertToExternalDataObject(orcid, work))
            .collect(Collectors.toList());
    }

    private boolean isInvalidIdentifier(String id) {
        return StringUtils.isBlank(id) || id.split("::").length != 2;
    }

    private void validateOrcidId(String orcid) {
        if (!ORCID_ID_PATTERN.matcher(orcid).matches()) {
            throw new IllegalArgumentException("The given ORCID ID is not valid: " + orcid);
        }
    }

    /**
     * Returns all the works related to the given ORCID in the range from start and
     * limit.
     *
     * @param  orcid the ORCID ID of the author to search for works
     * @param  start the start index
     * @param  limit the limit index
     * @return       the list of the works
     */
    private List<Work> findWorks(String orcid, int start, int limit) {
        List<WorkSummary> workSummaries = findWorkSummaries(orcid, start, limit);
        return findWorks(orcid, workSummaries);
    }

    /**
     * Returns all the works summaries related to the given ORCID in the range from
     * start and limit.
     *
     * @param  orcid the ORCID ID of the author to search for works summaries
     * @param  start the start index
     * @param  limit the limit index
     * @return       the list of the works summaries
     */
    private List<WorkSummary> findWorkSummaries(String orcid, int start, int limit) {
        return getWorks(orcid).getWorkGroup().stream()
            .filter(workGroup -> allWorkSummariesHaveDifferentSourceClientId(workGroup))
            .map(workGroup -> getPreferredWorkSummary(workGroup))
            .flatMap(Optional::stream)
            .skip(start)
            .limit(limit > 0 ? limit : Long.MAX_VALUE)
            .collect(Collectors.toList());
    }

    /**
     * Returns all the works related to the given ORCID ID and work summaries (a
     * work has more details than a work summary).
     *
     * @param  orcid         the ORCID id of the author to search for works
     * @param  workSummaries the work summaries used to search the related works
     * @return               the list of the works
     */
    private List<Work> findWorks(String orcid, List<WorkSummary> workSummaries) {

        List<String> workPutCodes = getPutCodes(workSummaries);

        if (CollectionUtils.isEmpty(workPutCodes)) {
            return emptyList();
        }

        if (workPutCodes.size() == 1) {
            return getWork(orcid, workPutCodes.get(0)).stream().collect(Collectors.toList());
        }

        return partition(workPutCodes, MAX_PUT_CODES_SIZE).stream()
            .map(putCodes -> getWorkBulk(orcid, putCodes))
            .flatMap(workBulk -> getWorks(workBulk).stream())
            .collect(Collectors.toList());
    }

    /**
     * Search a work by ORCID id and putcode, using API or PUBLIC urls based on
     * whether the ORCID API keys are configured or not.
     *
     * @param  orcid   the ORCID ID
     * @param  putCode the work's identifier on ORCID
     * @return         the work, if any
     */
    private Optional<Work> getWork(String orcid, String putCode) {
        if (orcidConfiguration.isApiConfigured()) {
            String accessToken = getAccessToken(orcid);
            return orcidClient.getObject(accessToken, orcid, putCode, Work.class);
        } else {
            return orcidClient.getObject(orcid, putCode, Work.class);
        }
    }

    /**
     * Returns all the works related to the given ORCID.
     *
     * @param  orcid the ORCID ID of the author to search for works
     * @return       the list of the works
     */
    private Works getWorks(String orcid) {
        if (orcidConfiguration.isApiConfigured()) {
            String accessToken = getAccessToken(orcid);
            return orcidClient.getWorks(accessToken, orcid);
        } else {
            return orcidClient.getWorks(orcid);
        }
    }

    /**
     * Returns all the works related to the given ORCID by the given putCodes.
     *
     * @param  orcid    the ORCID ID of the author to search for works
     * @param  putCodes the work's put codes to search
     * @return          the list of the works
     */
    private WorkBulk getWorkBulk(String orcid, List<String> putCodes) {
        if (orcidConfiguration.isApiConfigured()) {
            String accessToken = getAccessToken(orcid);
            return orcidClient.getWorkBulk(accessToken, orcid, putCodes);
        } else {
            return orcidClient.getWorkBulk(orcid, putCodes);
        }
    }

    private String getAccessToken(String orcid) {
        List<Item> items = orcidSynchronizationService.findProfilesByOrcid(new Context(), orcid);
        return Optional.ofNullable(items.isEmpty() ? null : items.get(0))
            .flatMap(item -> getAccessToken(item))
            .orElseGet(() -> getReadPublicAccessToken());
    }

    private Optional<String> getAccessToken(Item item) {
        return ofNullable(orcidTokenService.findByProfileItem(getContext(), item))
            .map(OrcidToken::getAccessToken);
    }

    private String getReadPublicAccessToken() {
        if (readPublicAccessToken != null) {
            return readPublicAccessToken;
        }

        OrcidTokenResponseDTO accessTokenResponse = orcidClient.getReadPublicAccessToken();
        readPublicAccessToken = accessTokenResponse.getAccessToken();

        return readPublicAccessToken;
    }

    private List<Work> getWorks(WorkBulk workBulk) {
        return workBulk.getBulk().stream()
            .filter(bulkElement -> (bulkElement instanceof Work))
            .map(bulkElement -> ((Work) bulkElement))
            .collect(Collectors.toList());

    }

    private List<String> getPutCodes(List<WorkSummary> workSummaries) {
        return workSummaries.stream()
            .map(WorkSummary::getPutCode)
            .map(String::valueOf)
            .collect(Collectors.toList());
    }

    private Optional<WorkSummary> getPreferredWorkSummary(WorkGroup workGroup) {
        return workGroup.getWorkSummary().stream()
            .filter(work -> work.getPutCode() != null)
            .filter(work -> NumberUtils.isCreatable(work.getDisplayIndex()))
            .sorted(comparing(work -> Integer.valueOf(work.getDisplayIndex()), reverseOrder()))
            .findFirst();
    }

    private ExternalDataObject convertToExternalDataObject(String orcid, Work work) {
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        externalDataObject.setId(orcid + "::" + work.getPutCode().toString());

        String title = getWorkTitle(work);
        externalDataObject.setDisplayValue(title);
        externalDataObject.setValue(title);

        addMetadataValue(externalDataObject, fieldMapping.getTitleField(), () -> title);
        addMetadataValue(externalDataObject, fieldMapping.getTypeField(), () -> getWorkType(work));
        addMetadataValue(externalDataObject, fieldMapping.getPublicationDateField(), () -> getPublicationDate(work));
        addMetadataValue(externalDataObject, fieldMapping.getJournalTitleField(), () -> getJournalTitle(work));
        addMetadataValue(externalDataObject, fieldMapping.getSubTitleField(), () -> getSubTitleField(work));
        addMetadataValue(externalDataObject, fieldMapping.getShortDescriptionField(), () -> getDescription(work));
        addMetadataValue(externalDataObject, fieldMapping.getLanguageField(), () -> getLanguage(work));

        for (String contributorField : fieldMapping.getContributorFields().keySet()) {
            ContributorRole role = fieldMapping.getContributorFields().get(contributorField);
            addMetadataValues(externalDataObject, contributorField, () -> getContributors(work, role));
        }

        for (String externalIdField : fieldMapping.getExternalIdentifierFields().keySet()) {
            String type = fieldMapping.getExternalIdentifierFields().get(externalIdField);
            addMetadataValues(externalDataObject, externalIdField, () -> getExternalIds(work, type));
        }

        try {
            addMetadataValuesFromCitation(externalDataObject, work.getWorkCitation());
        } catch (Exception e) {
            LOGGER.error("An error occurs reading the following citation: " + work.getWorkCitation().getCitation(), e);
        }

        return externalDataObject;
    }

    private boolean allWorkSummariesHaveDifferentSourceClientId(WorkGroup workGroup) {
        return workGroup.getWorkSummary().stream().allMatch(this::hasDifferentSourceClientId);
    }

    @SuppressWarnings("deprecation")
    private boolean hasDifferentSourceClientId(SourceAware sourceAware) {
        return Optional.ofNullable(sourceAware.getSource())
            .map(source -> source.getSourceClientId())
            .map(sourceClientId -> sourceClientId.getPath())
            .map(clientId -> !StringUtils.equals(orcidConfiguration.getClientId(), clientId))
            .orElse(true);
    }

    private void addMetadataValues(ExternalDataObject externalData, String metadata, Supplier<List<String>> values) {

        if (StringUtils.isBlank(metadata)) {
            return;
        }

        MetadataFieldName field = new MetadataFieldName(metadata);
        for (String value : values.get()) {
            externalData.addMetadata(new MetadataValueDTO(field.schema, field.element, field.qualifier, null, value));
        }
    }

    private void addMetadataValue(ExternalDataObject externalData, String metadata, Supplier<String> valueSupplier) {
        addMetadataValues(externalData, metadata, () -> {
            String value = valueSupplier.get();
            return isNotBlank(value) ? List.of(value) : emptyList();
        });
    }

    private String getWorkTitle(Work work) {
        WorkTitle workTitle = work.getWorkTitle();
        if (workTitle == null) {
            return null;
        }
        Title title = workTitle.getTitle();
        return title != null ? title.getContent() : null;
    }

    private String getWorkType(Work work) {
        WorkType workType = work.getWorkType();
        return workType != null ? fieldMapping.convertType(workType.value()) : null;
    }

    private String getPublicationDate(Work work) {
        PublicationDate publicationDate = work.getPublicationDate();
        if (publicationDate == null) {
            return null;
        }

        StringBuilder builder = new StringBuilder(publicationDate.getYear().getValue());
        if (publicationDate.getMonth() != null) {
            builder.append("-");
            builder.append(publicationDate.getMonth().getValue());
        }

        if (publicationDate.getDay() != null) {
            builder.append("-");
            builder.append(publicationDate.getDay().getValue());
        }

        return builder.toString();
    }

    private String getJournalTitle(Work work) {
        Title journalTitle = work.getJournalTitle();
        return journalTitle != null ? journalTitle.getContent() : null;
    }

    private String getSubTitleField(Work work) {
        WorkTitle workTitle = work.getWorkTitle();
        if (workTitle == null) {
            return null;
        }
        Subtitle subTitle = workTitle.getSubtitle();
        return subTitle != null ? subTitle.getContent() : null;
    }

    private String getDescription(Work work) {
        return work.getShortDescription();
    }

    private String getLanguage(Work work) {
        return work.getLanguageCode() != null ? fieldMapping.convertLanguage(work.getLanguageCode()) : null;
    }

    private List<String> getContributors(Work work, ContributorRole role) {
        WorkContributors workContributors = work.getWorkContributors();
        if (workContributors == null) {
            return emptyList();
        }

        return workContributors.getContributor().stream()
            .filter(contributor -> hasRole(contributor, role))
            .map(contributor -> getContributorName(contributor))
            .flatMap(Optional::stream)
            .collect(Collectors.toList());
    }

    private void addMetadataValuesFromCitation(ExternalDataObject externalDataObject, Citation citation)
        throws Exception {

        if (citation == null || citation.getWorkCitationType() == FORMATTED_UNSPECIFIED) {
            return;
        }

        getImportRecord(citation).ifPresent(importRecord -> enrichExternalDataObject(externalDataObject, importRecord));

    }

    private Optional<ImportRecord> getImportRecord(Citation citation) throws Exception {
        File citationFile = File.createTempFile("temp", "." + citation.getWorkCitationType().value());
        try (FileOutputStream outputStream = new FileOutputStream(citationFile)) {
            IOUtils.write(citation.getCitation(), new FileOutputStream(citationFile), Charset.defaultCharset());
            return Optional.ofNullable(importService.getRecord(citationFile, citationFile.getName()));
        } finally {
            citationFile.delete();
        }
    }

    private void enrichExternalDataObject(ExternalDataObject externalDataObject, ImportRecord importRecord) {
        importRecord.getValueList().stream()
            .filter(metadata -> doesNotContains(externalDataObject, metadata))
            .forEach(metadata -> addMetadata(externalDataObject, metadata));
    }

    private void addMetadata(ExternalDataObject externalDataObject, MetadatumDTO metadata) {
        externalDataObject.addMetadata(new MetadataValueDTO(metadata.getSchema(), metadata.getElement(),
            metadata.getQualifier(), null, metadata.getValue()));
    }

    private boolean doesNotContains(ExternalDataObject externalDataObject, MetadatumDTO metadata) {
        return externalDataObject.getMetadata().stream()
            .filter(metadataValue -> StringUtils.equals(metadataValue.getSchema(), metadata.getSchema()))
            .filter(metadataValue -> StringUtils.equals(metadataValue.getElement(), metadata.getElement()))
            .filter(metadataValue -> StringUtils.equals(metadataValue.getQualifier(), metadata.getQualifier()))
            .findAny().isEmpty();
    }

    private boolean hasRole(Contributor contributor, ContributorRole role) {
        ContributorAttributes attributes = contributor.getContributorAttributes();
        return attributes != null ? role.equals(attributes.getContributorRole()) : false;
    }

    private Optional<String> getContributorName(Contributor contributor) {
        return Optional.ofNullable(contributor.getCreditName())
            .map(creditName -> creditName.getContent());
    }

    private List<String> getExternalIds(Work work, String type) {
        ExternalIDs externalIdentifiers = work.getExternalIdentifiers();
        if (externalIdentifiers == null) {
            return emptyList();
        }

        return externalIdentifiers.getExternalIdentifier().stream()
            .filter(externalId -> type.equals(externalId.getType()))
            .map(externalId -> externalId.getValue())
            .collect(Collectors.toList());
    }

    private Context getContext() {
        Context context = ContextUtil.obtainCurrentRequestContext();
        return context != null ? context : new Context();
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equals(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String orcid) {
        return findWorkSummaries(orcid, 0, -1).size();
    }

    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public void setFieldMapping(OrcidWorkFieldMapping fieldMapping) {
        this.fieldMapping = fieldMapping;
    }

    public void setReadPublicAccessToken(String readPublicAccessToken) {
        this.readPublicAccessToken = readPublicAccessToken;
    }

    public OrcidClient getOrcidClient() {
        return orcidClient;
    }

    public void setOrcidClient(OrcidClient orcidClient) {
        this.orcidClient = orcidClient;
    }

}
