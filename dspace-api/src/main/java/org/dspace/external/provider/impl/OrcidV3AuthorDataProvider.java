/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.json.JSONObject;
import org.orcid.jaxb.model.v3.release.common.OrcidIdentifier;
import org.orcid.jaxb.model.v3.release.record.Person;
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
        if (StringUtils.isNotBlank(clientSecret) && StringUtils.isNotBlank(clientId)
            && StringUtils.isNotBlank(OAUTHUrl)) {
            String authenticationParameters = "?client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=/read-public&grant_type=client_credentials";
            HttpPost httpPost = new HttpPost(OAUTHUrl + authenticationParameters);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse getResponse = httpClient.execute(httpPost);

            JSONObject responseObject = null;
            try (InputStream is = getResponse.getEntity().getContent();
                 BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"))) {
                String inputStr;
                while ((inputStr = streamReader.readLine()) != null && responseObject == null) {
                    if (inputStr.startsWith("{") && inputStr.endsWith("}") && inputStr.contains("access_token")) {
                        try {
                            responseObject = new JSONObject(inputStr);
                        } catch (Exception e) {
                            //Not as valid as I'd hoped, move along
                            responseObject = null;
                        }
                    }
                }
            }
            if (responseObject != null && responseObject.has("access_token")) {
                accessToken = (String) responseObject.get("access_token");
            }
        }
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        Person person = getBio(id);
        ExternalDataObject externalDataObject = convertToExternalDataObject(person);
        return Optional.of(externalDataObject);
    }

    protected ExternalDataObject convertToExternalDataObject(Person person) {
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
            externalDataObject.setId(person.getName().getPath());
            externalDataObject
                    .addMetadata(
                            new MetadataValueDTO("person", "identifier", "orcid", null, person.getName().getPath()));
            externalDataObject
                    .addMetadata(new MetadataValueDTO("dc", "identifier", "uri", null,
                                                      orcidUrl + "/" + person.getName().getPath()));
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
        } else if (person.getPath() != null ) {
            externalDataObject.setId(StringUtils.substringBetween(person.getPath(),"/","/person"));
        }
        return externalDataObject;
    }

    /**
     * Retrieve a Person object based on a given orcid identifier.
     * @param id orcid identifier
     * @return Person
     */
    public Person getBio(String id) {
        log.debug("getBio called with ID=" + id);
        if (!isValid(id)) {
            return null;
        }
        InputStream bioDocument = orcidRestConnector.get(id + ((id.endsWith("/person")) ? "" : "/person"), accessToken);
        Person person = converter.convertSinglePerson(bioDocument);
        try {
            bioDocument.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        return person;
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

        String searchPath = "search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&start=" + start
                + "&rows=" + limit;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken);
        List<Result> results = converter.convert(bioDocument);
        List<Person> bios = new LinkedList<>();
        for (Result result : results) {
            OrcidIdentifier orcidIdentifier = result.getOrcidIdentifier();
            if (orcidIdentifier != null) {
                log.debug("Found OrcidId=" + orcidIdentifier.toString());
                String orcid = orcidIdentifier.getPath();
                Person bio = getBio(orcid);
                if (bio != null) {
                    bios.add(bio);
                }
            }
        }
        try {
            bioDocument.close();
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
        if (Objects.isNull(bios)) {
            return Collections.emptyList();
        }
        return bios.stream().map(bio -> convertToExternalDataObject(bio)).collect(Collectors.toList());
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        String searchPath = "search?q=" + URLEncoder.encode(query, StandardCharsets.UTF_8)
                + "&start=" + 0
                + "&rows=" + 0;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken);
        return Math.min(converter.getNumberOfResultsFromXml(bioDocument), MAX_INDEX);
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

}
