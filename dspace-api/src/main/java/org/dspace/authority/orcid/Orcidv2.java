/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.authority.orcid.xml.XMLtoBio;
import org.dspace.authority.rest.RESTConnector;
import org.json.JSONObject;
import org.orcid.jaxb.model.record_v2.Person;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Jonas Van Goolen (jonas at atmire dot com)
 * This class contains all methods for retrieving "Person" objects calling the ORCID (version 2) endpoints.
 * Additionally, this can also create AuthorityValues based on these returned Person objects
 */
public class Orcidv2 implements SolrAuthorityInterface {

    private static Logger log = Logger.getLogger(Orcidv2.class);

    public RESTConnector restConnector;
    private String OAUTHUrl;
    private String clientId;

    private String clientSecret;

    private String accessToken;

    /**
     *  Initialize the accessToken that is required for all subsequent calls to ORCID
     */
    public void init() throws IOException {
        if (StringUtils.isNotBlank(accessToken) && StringUtils.isNotBlank(clientSecret)) {
            String authenticationParameters = "?client_id=" + clientId + "&client_secret=" + clientSecret + "&scope=/read-public&grant_type=client_credentials";
            HttpPost httpPost = new HttpPost(OAUTHUrl + authenticationParameters);
            httpPost.addHeader("Accept", "application/json");
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpResponse getResponse = httpClient.execute(httpPost);

            InputStream is = getResponse.getEntity().getContent();
            BufferedReader streamReader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

            JSONObject responseObject = null;
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

            if (responseObject != null && responseObject.has("access_token")) {
                accessToken = (String) responseObject.get("access_token");
            }
        }
    }

    /**
     * Makes an instance of the Orcidv2 class based on the provided parameters.
     * This constructor is called through the spring bean initialization
     */
    private Orcidv2(String url, String OAUTHUrl, String clientId, String clientSecret) {
        this.restConnector = new RESTConnector(url);
        this.OAUTHUrl = OAUTHUrl;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    /**
     * Makes an instance of the Orcidv2 class based on the provided parameters.
     * This constructor is called through the spring bean initialization
     */
    private Orcidv2(String url) {
        this.restConnector = new RESTConnector(url);
    }

    /**
     * Makes an instance of the AuthorityValue with the given information.
     * @param text search string
     * @return List<AuthorityValue>
     */
    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
        List<Person> bios = queryBio(text, max);
        List<AuthorityValue> result = new ArrayList<>();
        for (Person person : bios) {
            AuthorityValue orcidAuthorityValue = Orcidv2AuthorityValue.create(person);
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
    public AuthorityValue queryAuthorityID(String id) {
        Person person = getBio(id);
        AuthorityValue valueFromPerson = Orcidv2AuthorityValue.create(person);
        return valueFromPerson;
    }

    /**
     * Retrieve a Person object based on a given orcid identifier
     * @param id orcid identifier
     * @return Person
     */
    public Person getBio(String id) {
        log.debug("getBio called with ID=" + id);
        if(!isValid(id)){
            return null;
        }
        InputStream bioDocument = restConnector.get(id + ((id.endsWith("/person")) ? "" : "/person"), accessToken);
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
        if (rows > 100) {
            throw new IllegalArgumentException("The maximum number of results to retrieve cannot exceed 100.");
        }

        String searchPath = "search?q=" + URLEncoder.encode(text) + "&start=" + start + "&rows=" + rows;
        log.debug("queryBio searchPath=" + searchPath + " accessToken=" + accessToken);
        InputStream bioDocument = restConnector.get(searchPath, accessToken);
        XMLtoBio converter = new XMLtoBio();
        List<Person> bios = converter.convert(bioDocument);
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
     * Check to see if the provided text has the correct ORCID syntax.
     * Since only searching on ORCID id is allowed, this way, we filter out any queries that would return a blank result anyway
     */
    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(Orcidv2AuthorityValue.ORCID_ID_SYNTAX);
    }
}
