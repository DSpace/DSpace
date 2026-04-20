/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
import org.dspace.external.OrcidConnectionException;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.dspace.orcid.model.factory.OrcidFactoryUtils;
import org.orcid.jaxb.model.v3.release.common.OrcidIdentifier;
import org.orcid.jaxb.model.v3.release.record.Email;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.search.Result;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with the OrcidV3 External
 * Data lookup
 */
public class OrcidV3AuthorDataProvider extends AbstractExternalDataProvider {

    private static final Logger log = LogManager.getLogger(OrcidV3AuthorDataProvider.class);

    private OrcidRestConnector orcidRestConnector;
    private String OAUTHUrl;

    private String clientId;
    private String clientSecret;

    private String accessToken;

    private String sourceIdentifier;

    private String orcidUrl;

    private XMLtoBio converter;

    /**
     * Maximum retries to allow for the access token retrieval
     */
    private int maxClientRetries = 3;

    private Map<String, String> externalIdentifiers;

    public static final String ORCID_ID_SYNTAX = "\\d{4}-\\d{4}-\\d{4}-(\\d{3}X|\\d{4})";
    private static final int MAX_INDEX = 10000;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    public OrcidV3AuthorDataProvider() {
        converter = new XMLtoBio();
    }

    /**
     * Initialize the accessToken that is required for all subsequent calls to ORCID.
     *
     * @throws java.io.IOException passed through from HTTPclient.
     */
    public void init() throws IOException {
        // Initialize access token at spring instantiation. If it fails, the access token will be null rather
        // than causing a fatal Spring startup error
        initializeAccessToken();
    }

    /**
     * Initialize access token, logging an error and decrementing remaining retries if an IOException is thrown.
     * If the optional access token result is empty, set to null instead.
     */
    public void initializeAccessToken() {
        // If we have reaches max retries or the access token is already set, return immediately
        if (maxClientRetries <= 0 || StringUtils.isNotBlank(accessToken)) {
            if (maxClientRetries <= 0) {
                log.warn("Maximum retry attempts reached for ORCID token retrieval");
            }
            return;
        }
        try {
            accessToken = OrcidFactoryUtils.retrieveAccessToken(clientId, clientSecret, OAUTHUrl).orElse(null);
        } catch (IOException e) {
            log.error("Error retrieving ORCID access token, {} retries left", --maxClientRetries);
        }
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        initializeAccessToken();
        Record record = getRecord(id);
        ExternalDataObject externalDataObject = convertToExternalDataObject(record);
        return Optional.of(externalDataObject);
    }

    protected ExternalDataObject convertToExternalDataObject(org.orcid.jaxb.model.v3.release.record.Record record) {
        initializeAccessToken();
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
        org.orcid.jaxb.model.v3.release.record.Record record) {

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
     * @param id orcid identifier
     * @return Record
     */
    public org.orcid.jaxb.model.v3.release.record.Record getRecord(String id) {
        log.debug("getRecord called with ID={}", id);
        if (!isValid(id)) {
            return null;
        }
        if (orcidRestConnector == null) {
            log.error("ORCID REST connector is null, returning null ORCID Person Bio");
            return null;
        }
        initializeAccessToken();
        try (InputStream bioDocument = orcidRestConnector.get(id, accessToken)) {
            return converter.convertToRecord(bioDocument);
        } catch (OrcidConnectionException | IOException e) {
            log.error("Error retrieving ORCID bio for ID={}", id, e);
            return null;
        }
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
        initializeAccessToken();
        if (limit > 100) {
            throw new IllegalArgumentException("The maximum number of results to retrieve cannot exceed 100.");
        }
        if (start > MAX_INDEX) {
            throw new IllegalArgumentException("The starting number of results to retrieve cannot exceed 10000.");
        }
        // Check REST connector is initialized
        if (orcidRestConnector == null) {
            log.error("ORCID REST connector is not initialized, returning empty list");
            return Collections.emptyList();
        }

        String searchPath = "search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&start=" + start
                + "&rows=" + limit;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        try (InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken)) {
            List<Result> results = converter.convert(bioDocument);
            List<org.orcid.jaxb.model.v3.release.record.Record> bios = new LinkedList<>();
            for (Result result : results) {
                OrcidIdentifier orcidIdentifier = result.getOrcidIdentifier();
                if (orcidIdentifier != null) {
                    log.debug("Found OrcidId=" + orcidIdentifier.getPath());
                    String orcid = orcidIdentifier.getPath();
                    org.orcid.jaxb.model.v3.release.record.Record bio = getRecord(orcid);
                    if (bio != null) {
                        bios.add(bio);
                    }
                }
            }
            return bios.stream().map(bio -> convertToExternalDataObject(bio)).collect(Collectors.toList());
        } catch (OrcidConnectionException | IOException e) {
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
        if (orcidRestConnector == null) {
            log.error("ORCID REST connector is null, returning 0");
            return 0;
        }
        initializeAccessToken();
        String searchPath = "search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&start=" + 0
                + "&rows=" + 0;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        try (InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken)) {
            return Math.min(converter.getNumberOfResultsFromXml(bioDocument), MAX_INDEX);
        } catch (OrcidConnectionException | IOException e) {
            log.error("Error getting number of results from ORCID for query={}", query, e);
            return 0;
        }
    }


    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this OrcidV3AuthorDataProvider
     */
    @Autowired(required = true)
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic getter for the orcidUrl
     * @return the orcidUrl value of this OrcidV3AuthorDataProvider
     */
    public String getOrcidUrl() {
        return orcidUrl;
    }

    /**
     * Generic setter for the orcidUrl
     * @param orcidUrl   The orcidUrl to be set on this OrcidV3AuthorDataProvider
     */
    @Autowired(required = true)
    public void setOrcidUrl(String orcidUrl) {
        this.orcidUrl = orcidUrl;
    }

    /**
     * Generic setter for the OAUTHUrl
     * @param OAUTHUrl   The OAUTHUrl to be set on this OrcidV3AuthorDataProvider
     */
    public void setOAUTHUrl(String OAUTHUrl) {
        this.OAUTHUrl = OAUTHUrl;
    }

    /**
     * Generic setter for the clientId
     * @param clientId   The clientId to be set on this OrcidV3AuthorDataProvider
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Generic setter for the clientSecret
     * @param clientSecret   The clientSecret to be set on this OrcidV3AuthorDataProvider
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public OrcidRestConnector getOrcidRestConnector() {
        return orcidRestConnector;
    }

    public void setOrcidRestConnector(OrcidRestConnector orcidRestConnector) {
        this.orcidRestConnector = orcidRestConnector;
    }

    public Map<String, String> getExternalIdentifiers() {
        return externalIdentifiers;
    }

    public void setExternalIdentifiers(Map<String, String> externalIdentifiers) {
        this.externalIdentifiers = externalIdentifiers;
    }
}