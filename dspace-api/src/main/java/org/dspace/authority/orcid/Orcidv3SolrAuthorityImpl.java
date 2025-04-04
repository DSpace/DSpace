/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.external.OrcidRestConnector;
import org.dspace.external.provider.orcid.xml.XMLtoBio;
import org.dspace.orcid.model.factory.OrcidFactoryUtils;
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

    /**
     * Maximum retries to allow for the access token retrieval
     */
    private int maxClientRetries = 3;

    public void setOAUTHUrl(String oAUTHUrl) {
        OAUTHUrl = oAUTHUrl;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    /**
     *  Initialize the accessToken that is required for all subsequent calls to ORCID
     */
    public void init() {
        // Initialize access token at spring instantiation. If it fails, the access token will be null rather
        // than causing a fatal Spring startup error
        initializeAccessToken();
    }

    public void initializeAccessToken() {
        // If we have reaches max retries or the access token is already set, return immediately
        if (maxClientRetries <= 0 || org.apache.commons.lang3.StringUtils.isNotBlank(accessToken)) {
            return;
        }
        try {
            accessToken = OrcidFactoryUtils.retrieveAccessToken(clientId, clientSecret, OAUTHUrl).orElse(null);
        } catch (IOException e) {
            log.error("Error retrieving ORCID access token, {} retries left", --maxClientRetries);
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
        initializeAccessToken();
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
        initializeAccessToken();
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
        if (orcidRestConnector == null) {
            log.error("ORCID REST connector is null, returning null Person");
            return null;
        }
        initializeAccessToken();
        InputStream bioDocument = orcidRestConnector.get(id + ((id.endsWith("/person")) ? "" : "/person"), accessToken);
        XMLtoBio converter = new XMLtoBio();
        return converter.convertSinglePerson(bioDocument);
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
        // Check REST connector is initialized
        if (orcidRestConnector == null) {
            log.error("ORCID REST connector is not initialized, returning empty list");
            return Collections.emptyList();
        }
        // Check / init access token
        initializeAccessToken();

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