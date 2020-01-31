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
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import org.dspace.external.provider.ExternalDataProvider;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.json.JSONObject;
import org.orcid.jaxb.model.common_v2.OrcidId;
import org.orcid.jaxb.model.record_v2.Person;
import org.orcid.jaxb.model.search_v2.Result;
import org.springframework.beans.factory.annotation.Required;

/**
 * This class is the implementation of the ExternalDataProvider interface that will deal with the OrcidV2 External
 * Data lookup
 */
public class OrcidV2AuthorDataProvider implements ExternalDataProvider {

    private static Logger log = LogManager.getLogger(OrcidV2AuthorDataProvider.class);

    private OrcidRestConnector orcidRestConnector;
    private String OAUTHUrl;
    private String clientId;

    private String clientSecret;

    private String accessToken;

    private String sourceIdentifier;
    private String orcidUrl;

    public static final String ORCID_ID_SYNTAX = "\\d{4}-\\d{4}-\\d{4}-(\\d{3}X|\\d{4})";

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * Initialize the accessToken that is required for all subsequent calls to ORCID.
     *
     * @throws java.io.IOException passed through from HTTPclient.
     */
    public void init() throws IOException {
        if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(clientSecret)) {
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

    /**
     * Makes an instance of the Orcidv2 class based on the provided parameters.
     * This constructor is called through the spring bean initialization
     */
    private OrcidV2AuthorDataProvider(String url) {
        this.orcidRestConnector = new OrcidRestConnector(url);
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        Person person = getBio(id);
        ExternalDataObject externalDataObject = convertToExternalDataObject(person);
        return Optional.of(externalDataObject);
    }

    protected ExternalDataObject convertToExternalDataObject(Person person) {
        ExternalDataObject externalDataObject = new ExternalDataObject(sourceIdentifier);
        String lastName = "";
        String firstName = "";
        if (person.getName().getFamilyName() != null) {
            lastName = person.getName().getFamilyName().getValue();
            externalDataObject.addMetadata(new MetadataValueDTO("person", "familyName", null, null,
                                                                lastName));
        }
        if (person.getName().getGivenNames() != null) {
            firstName = person.getName().getGivenNames().getValue();
            externalDataObject.addMetadata(new MetadataValueDTO("person", "givenName", null, null,
                                                                firstName));

        }
        externalDataObject.setId(person.getName().getPath());
        externalDataObject
            .addMetadata(new MetadataValueDTO("person", "identifier", "orcid", null, person.getName().getPath()));
        externalDataObject
            .addMetadata(new MetadataValueDTO("dc", "identifier", "uri", null, orcidUrl + person.getName().getPath()));
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
        return externalDataObject;
    }

    /**
     * Retrieve a Person object based on a given orcid identifier
     * @param id orcid identifier
     * @return Person
     */
    public Person getBio(String id) {
        log.debug("getBio called with ID=" + id);
        if (!isValid(id)) {
            return null;
        }
        InputStream bioDocument = orcidRestConnector.get(id + ((id.endsWith("/person")) ? "" : "/person"), accessToken);
        XMLtoBio converter = new XMLtoBio();
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

        String searchPath = "search?q=" + URLEncoder.encode(query) + "&start=" + start + "&rows=" + limit;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken);
        XMLtoBio converter = new XMLtoBio();
        List<Result> results = converter.convert(bioDocument);
        List<Person> bios = new LinkedList<>();
        for (Result result : results) {
            OrcidId orcidIdentifier = result.getOrcidIdentifier();
            if (orcidIdentifier != null) {
                log.debug("Found OrcidId=" + orcidIdentifier.toString());
                String orcid = orcidIdentifier.getUriPath();
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
        if (bios == null) {
            return Collections.emptyList();
        } else {
            return bios.stream().map(bio -> convertToExternalDataObject(bio)).collect(Collectors.toList());
        }
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public int getNumberOfResults(String query) {
        String searchPath = "search?q=" + URLEncoder.encode(query) + "&start=" + 0 + "&rows=" + 0;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken);
        XMLtoBio converter = new XMLtoBio();
        return converter.getNumberOfResultsFromXml(bioDocument);
    }


    /**
     * Generic setter for the sourceIdentifier
     * @param sourceIdentifier   The sourceIdentifier to be set on this OrcidV2AuthorDataProvider
     */
    @Required
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    /**
     * Generic getter for the orcidUrl
     * @return the orcidUrl value of this OrcidV2AuthorDataProvider
     */
    public String getOrcidUrl() {
        return orcidUrl;
    }

    /**
     * Generic setter for the orcidUrl
     * @param orcidUrl   The orcidUrl to be set on this OrcidV2AuthorDataProvider
     */
    @Required
    public void setOrcidUrl(String orcidUrl) {
        this.orcidUrl = orcidUrl;
    }

    /**
     * Generic setter for the OAUTHUrl
     * @param OAUTHUrl   The OAUTHUrl to be set on this OrcidV2AuthorDataProvider
     */
    public void setOAUTHUrl(String OAUTHUrl) {
        this.OAUTHUrl = OAUTHUrl;
    }

    /**
     * Generic setter for the clientId
     * @param clientId   The clientId to be set on this OrcidV2AuthorDataProvider
     */
    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    /**
     * Generic setter for the clientSecret
     * @param clientSecret   The clientSecret to be set on this OrcidV2AuthorDataProvider
     */
    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
