/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.json.JSONObject;
import org.orcid.jaxb.model.v3.release.common.OrcidIdentifier;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.search.Result;

/**
 * This class contains all methods for retrieving "Person" objects calling the ORCID (version 3) endpoints.
 * Additionally, this can also create AuthorityValues based on these returned Person objects
 *
 * @author Jonas Van Goolen (jonas at atmire dot com)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class Orcidv3SolrAuthorityImpl implements SolrAuthorityInterface {

    private final static Logger log = LogManager.getLogger();

    private OrcidRestConnector orcidRestConnector;
    private String OAUTHUrl;
    private String clientId;
    private String clientSecret;

    private String accessToken;

    public void setOAUTHUrl(String oAUTHUrl) {
        OAUTHUrl = oAUTHUrl;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    /**
     *  Initialize the accessToken that is required for all subsequent calls to ORCID
     */
    public void init() {
        if (StringUtils.isBlank(accessToken)
                && StringUtils.isNotBlank(clientSecret)
                && StringUtils.isNotBlank(clientId)
                && StringUtils.isNotBlank(OAUTHUrl)) {
            String authenticationParameters = "?client_id=" + clientId +
                    "&client_secret=" + clientSecret +
                    "&scope=/read-public&grant_type=client_credentials";
            try {
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
            } catch (Exception e) {
                throw new RuntimeException("Error during initialization of the Orcid connector", e);
            }
        }
    }

    public void setOrcidRestConnector(OrcidRestConnector orcidRestConnector) {
        this.orcidRestConnector = orcidRestConnector;
    }

    /**
     * Makes an instance of the AuthorityValue with the given information.
     * @param text search string
     * @return List<AuthorityValue>
     */
    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
        init();
        List<Person> bios = queryBio(text, max);
        List<AuthorityValue> result = new ArrayList<>();
        for (Person person : bios) {
            AuthorityValue orcidAuthorityValue = Orcidv3AuthorityValue.create(person);
            if (orcidAuthorityValue != null) {
                result.add(orcidAuthorityValue);
            }
        }
        return result;
    }

    /**
     * Create an AuthorityValue from a Person retrieved using the given orcid identifier.
     * @param id orcid identifier
     * @return AuthorityValue
     */
    @Override
    public AuthorityValue queryAuthorityID(String id) {
        init();
        Person person = getBio(id);
        AuthorityValue valueFromPerson = Orcidv3AuthorityValue.create(person);
        return valueFromPerson;
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
        init();
        InputStream bioDocument = orcidRestConnector.get(id + ((id.endsWith("/person")) ? "" : "/person"), accessToken);
        XMLtoBio converter = new XMLtoBio();
        Person person = converter.convertSinglePerson(bioDocument);
        return person;
    }


    /**
     * Retrieve a list of Person objects.
     * @param text search string
     * @param start offset to use
     * @param rows how many rows to return
     * @return List<Person>
     */
    public List<Person> queryBio(String text, int start, int rows) {
        init();
        if (rows > 100) {
            throw new IllegalArgumentException("The maximum number of results to retrieve cannot exceed 100.");
        }

        String searchPath = "search?q=" + URLEncoder.encode(text) + "&start=" + start + "&rows=" + rows;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        InputStream bioDocument = orcidRestConnector.get(searchPath, accessToken);
        XMLtoBio converter = new XMLtoBio();
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
        return bios;
    }

    /**
     * Retrieve a list of Person objects.
     * @param text search string
     * @param max how many rows to return
     * @return List<Person>
     */
    public List<Person> queryBio(String text, int max) {
        return queryBio(text, 0, max);
    }

    /**
     * Check to see if the provided text has the correct ORCID syntax. Since only
     * searching on ORCID id is allowed, this way, we filter out any queries that
     * would return a blank result anyway
     */
    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(Orcidv3AuthorityValue.ORCID_ID_SYNTAX);
    }
}