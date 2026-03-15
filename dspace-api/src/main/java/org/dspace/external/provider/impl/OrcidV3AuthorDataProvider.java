/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.exception.OrcidClientException;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedResult;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with the OrcidV3 External
 * Data lookup. It uses the modern {@link OrcidClient} for all ORCID API calls, supporting both
 * public and member API endpoints based on configuration.
 */
public class OrcidV3AuthorDataProvider extends AbstractExternalDataProvider {

    private static final Logger log = LogManager.getLogger(OrcidV3AuthorDataProvider.class);

    @Autowired
    private OrcidClient orcidClient;

    @Autowired
    private OrcidConfiguration orcidConfiguration;

    private String sourceIdentifier;

    private String orcidUrl;

    private Map<String, String> externalIdentifiers;

    public static final String ORCID_ID_SYNTAX = "\\d{4}-\\d{4}-\\d{4}-(\\d{3}X|\\d{4})";
    private static final int MAX_INDEX = 10000;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public OrcidV3AuthorDataProvider() {
    }

    /**
     * Initialization method — no longer needs to retrieve access tokens since
     * {@link OrcidClient} handles authentication internally.
     */
    public void init() {
        // No-op: OrcidClient handles token management
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        Record record = getRecord(id);
        if (record == null) {
            return Optional.empty();
        }
        ExternalDataObject externalDataObject = convertToExternalDataObject(record);
        return Optional.of(externalDataObject);
    }

    /**
     * Convert an ORCID Record to an ExternalDataObject with rich metadata.
     *
     * @param record the ORCID Record
     * @return the ExternalDataObject
     */
    protected ExternalDataObject convertToExternalDataObject(Record record) {
        Person person = record.getPerson();
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        if (person.getName() != null) {
            String lastName = "";
            String firstName = "";
            if (person.getName().getFamilyName() != null) {
                lastName = person.getName().getFamilyName().getContent();
                externalDataObject.addMetadata(new MetadataValueDTO("person", "familyName", null, null,
                        lastName));
            }
            if (person.getName().getGivenNames() != null) {
                firstName = person.getName().getGivenNames().getContent();
                externalDataObject.addMetadata(new MetadataValueDTO("person", "givenName", null, null,
                        firstName));
            }
            if (person.getEmails().getEmails() != null && !person.getEmails().getEmails().isEmpty()) {
                Email email = person.getEmails().getEmails().get(0);
                if (person.getEmails().getEmails().size() > 1) {
                    email = person.getEmails().getEmails().stream().filter(Email::isPrimary).findFirst().orElse(email);
                }
                externalDataObject.addMetadata(new MetadataValueDTO("person", "email", null,
                        null, email.getEmail()));
            }
            externalDataObject.setId(person.getName().getPath());
            externalDataObject
                    .addMetadata(
                            new MetadataValueDTO("person", "identifier", "orcid", null, person.getName().getPath()));
            externalDataObject
                    .addMetadata(new MetadataValueDTO("dc", "identifier", "uri", null,
                            orcidUrl + "/" + person.getName().getPath()));
            appendOtherNames(externalDataObject, person);
            appendResearcherUrls(externalDataObject, person);
            appendExternalIdentifiers(externalDataObject, person);
            appendAffiliations(externalDataObject, record);
            if (!StringUtils.isBlank(lastName) && !StringUtils.isBlank(firstName)) {
                externalDataObject.setDisplayValue(lastName + ", " + firstName);
                externalDataObject.setValue(lastName + ", " + firstName);
            } else if (StringUtils.isBlank(firstName)) {
                externalDataObject.setDisplayValue(lastName);
                externalDataObject.setValue(lastName);
            } else if (StringUtils.isBlank(lastName)) {
                externalDataObject.setDisplayValue(firstName);
                externalDataObject.setValue(firstName);
            }
        } else if (person.getPath() != null) {
            externalDataObject.setId(StringUtils.substringBetween(person.getPath(), "/", "/person"));
        }
        return externalDataObject;
    }

    private void appendOtherNames(ExternalDataObject externalDataObject, Person person) {
        if (person == null) {
            return;
        }
        var otherNames = person.getOtherNames();
        if (otherNames == null) {
            return;
        }
        var namesList = otherNames.getOtherNames();
        if (namesList == null) {
            return;
        }

        for (var otherName : namesList) {
            if (otherName == null) {
                continue;
            }
            var content = otherName.getContent();
            if (content == null) {
                continue;
            }
            externalDataObject.addMetadata(
                new MetadataValueDTO("crisrp", "name", "variant", null, content)
            );
        }
    }

    private void appendResearcherUrls(ExternalDataObject externalDataObject, Person person) {
        if (person == null) {
            return;
        }
        var researcherUrls = person.getResearcherUrls();
        if (researcherUrls == null) {
            return;
        }
        var urlsList = researcherUrls.getResearcherUrls();
        if (urlsList == null) {
            return;
        }

        for (var researcherUrl : urlsList) {
            if (researcherUrl == null) {
                continue;
            }
            var url = researcherUrl.getUrl();
            if (url == null) {
                continue;
            }
            var value = url.getValue();
            if (value == null) {
                continue;
            }
            externalDataObject.addMetadata(
                new MetadataValueDTO("oairecerif", "identifier", "url", null, value)
            );
        }
    }

    private void appendExternalIdentifiers(ExternalDataObject externalDataObject, Person person) {
        if (getExternalIdentifiers() == null) {
            return;
        }
        if (person == null) {
            return;
        }
        var externalIds = person.getExternalIdentifiers();
        if (externalIds == null) {
            return;
        }
        var idsList = externalIds.getExternalIdentifiers();
        if (idsList == null) {
            return;
        }

        for (var externalIdentifier : idsList) {
            if (externalIdentifier == null) {
                continue;
            }
            var type = externalIdentifier.getType();
            if (type == null) {
                continue;
            }
            String metadataField = externalIdentifiers.get(type);
            if (StringUtils.isEmpty(metadataField)) {
                continue;
            }
            var value = externalIdentifier.getValue();
            if (value == null) {
                continue;
            }

            MetadataFieldName field = new MetadataFieldName(metadataField);
            externalDataObject.addMetadata(
                new MetadataValueDTO(field.schema, field.element, field.qualifier, null, value)
            );
        }
    }

    private void appendAffiliations(
        ExternalDataObject externalDataObject,
        Record record) {

        if (record == null) {
            return;
        }
        var activitiesSummary = record.getActivitiesSummary();
        if (activitiesSummary == null) {
            return;
        }
        var employments = activitiesSummary.getEmployments();
        if (employments == null) {
            return;
        }
        var employmentGroups = employments.getEmploymentGroups();
        if (employmentGroups == null) {
            return;
        }

        for (var affiliationGroup : employmentGroups) {
            if (affiliationGroup == null) {
                continue;
            }
            var activities = affiliationGroup.getActivities();
            if (activities == null) {
                continue;
            }
            for (var employmentSummary : activities) {
                if (employmentSummary == null) {
                    continue;
                }
                var org = employmentSummary.getOrganization();
                if (org == null) {
                    continue;
                }
                var name = org.getName();
                if (name == null) {
                    continue;
                }
                externalDataObject.addMetadata(
                    new MetadataValueDTO("person", "affiliation", "name", null, name)
                );
            }
        }
    }


    /**
     * Retrieve a Record object based on a given orcid identifier.
     *
     * @param id orcid identifier
     * @return Record, or null if invalid or not found
     */
    public Record getRecord(String id) {
        log.debug("getRecord called with ID={}", id);
        if (!isValid(id)) {
            return null;
        }
        try {
            if (orcidConfiguration.isApiConfigured()) {
                return orcidClient.getRecord(getAccessToken(), id);
            } else {
                return orcidClient.getRecord(id);
            }
        } catch (OrcidClientException e) {
            log.error("Error retrieving ORCID record for ID={}", id, e);
            return null;
        }
    }

    /**
     * Retrieve an access token for API calls. Uses a read-public token obtained
     * via client credentials.
     *
     * @return the access token
     */
    private String getAccessToken() {
        return orcidClient.getReadPublicAccessToken().getAccessToken();
    }

    /**
     * Check to see if the provided text has the correct ORCID syntax.
     * Since only searching on ORCID id is allowed, this way, we filter out any queries that would return a
     * blank result anyway
     */
    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(ORCID_ID_SYNTAX);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        if (limit > 100) {
            throw new IllegalArgumentException("The maximum number of results to retrieve cannot exceed 100.");
        }
        if (start > MAX_INDEX) {
            throw new IllegalArgumentException("The starting number of results to retrieve cannot exceed 10000.");
        }

        log.debug("searchExternalDataObjects query={} start={} limit={}", query, start, limit);
        try {
            ExpandedSearch searchResult = performSearch(query, start, limit);
            List<Record> records = new LinkedList<>();
            for (ExpandedResult result : searchResult.getResults()) {
                String orcid = result.getOrcidId();
                if (orcid != null) {
                    log.debug("Found OrcidId={}", orcid);
                    Record record = getRecord(orcid);
                    if (record != null) {
                        records.add(record);
                    }
                }
            }
            return records.stream().map(this::convertToExternalDataObject).collect(Collectors.toList());
        } catch (OrcidClientException e) {
            log.error("Error searching ORCID for query={}", query, e);
            return Collections.emptyList();
        }
    }

    @Override
    public boolean supports(String source) {
        return Strings.CI.equals(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        log.debug("getNumberOfResults query={}", query);
        try {
            ExpandedSearch searchResult = performSearch(query, 0, 0);
            Long numFound = searchResult.getNumFound();
            return Math.min(numFound != null ? numFound.intValue() : 0, MAX_INDEX);
        } catch (OrcidClientException e) {
            log.error("Error getting number of results from ORCID for query={}", query, e);
            return 0;
        }
    }

    /**
     * Perform an ORCID expanded search, using the member API if configured or the public API otherwise.
     *
     * @param query the search query
     * @param start the start index
     * @param rows  the number of rows
     * @return the ExpandedSearch result
     */
    private ExpandedSearch performSearch(String query, int start, int rows) {
        if (orcidConfiguration.isApiConfigured()) {
            return orcidClient.expandedSearch(getAccessToken(), query, start, rows);
        } else {
            return orcidClient.expandedSearch(query, start, rows);
        }
    }

    /**
     * Generic setter for the sourceIdentifier.
     *
     * @param sourceIdentifier The sourceIdentifier to be set on this OrcidV3AuthorDataProvider
     */
    @Autowired(required = true)
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic getter for the orcidUrl.
     *
     * @return the orcidUrl value of this OrcidV3AuthorDataProvider
     */
    public String getOrcidUrl() {
        return orcidUrl;
    }

    /**
     * Generic setter for the orcidUrl.
     *
     * @param orcidUrl The orcidUrl to be set on this OrcidV3AuthorDataProvider
     */
    @Autowired(required = true)
    public void setOrcidUrl(String orcidUrl) {
        this.orcidUrl = orcidUrl;
    }

    /**
     * Getter for the externalIdentifiers map.
     *
     * @return the externalIdentifiers mapping
     */
    public Map<String, String> getExternalIdentifiers() {
        return externalIdentifiers;
    }

    /**
     * Setter for the externalIdentifiers map.
     *
     * @param externalIdentifiers the mapping of ORCID external ID types to DSpace metadata fields
     */
    public void setExternalIdentifiers(Map<String, String> externalIdentifiers) {
        this.externalIdentifiers = externalIdentifiers;
    }
}
