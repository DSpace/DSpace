/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authority.AuthorityValue;
import org.dspace.authority.SolrAuthorityInterface;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.exception.OrcidClientException;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedResult;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * This class contains all methods for retrieving "Person" objects calling the ORCID (version 3) endpoints.
 * Additionally, this can also create AuthorityValues based on these returned Person objects.
 * Uses the modern {@link OrcidClient} for all ORCID API calls, supporting both
 * public and member API endpoints based on configuration.
 *
 * @author Jonas Van Goolen (jonas at atmire dot com)
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class Orcidv3SolrAuthorityImpl implements SolrAuthorityInterface {

    private static final Logger log = LogManager.getLogger();

    @Autowired
    OrcidClient orcidClient;

    @Autowired
    OrcidConfiguration orcidConfiguration;

    /**
     * No-op initialization — OrcidClient handles token management internally.
     */
    public void init() {
        // No-op: OrcidClient handles token management
    }

    /**
     * Makes an instance of the AuthorityValue with the given information.
     *
     * @param text search string
     * @return List of AuthorityValues
     */
    @Override
    public List<AuthorityValue> queryAuthorities(String text, int max) {
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
     *
     * @param id orcid identifier
     * @return AuthorityValue
     */
    @Override
    public AuthorityValue queryAuthorityID(String id) {
        Person person = getBio(id);
        return Orcidv3AuthorityValue.create(person);
    }

    /**
     * Retrieve a Person object based on a given orcid identifier.
     *
     * @param id orcid identifier
     * @return Person
     */
    public Person getBio(String id) {
        log.debug("getBio called with ID={}", id);
        if (!isValid(id)) {
            return null;
        }
        try {
            if (orcidConfiguration.isApiConfigured()) {
                return orcidClient.getPerson(getAccessToken(), id);
            } else {
                return orcidClient.getPerson(id);
            }
        } catch (OrcidClientException e) {
            log.error("Error retrieving ORCID bio for ID={}", id, e);
            return null;
        }
    }

    /**
     * Retrieve a list of Person objects.
     *
     * @param text  search string
     * @param start offset to use
     * @param rows  how many rows to return
     * @return List of Persons
     */
    public List<Person> queryBio(String text, int start, int rows) {
        if (rows > 100) {
            throw new IllegalArgumentException("The maximum number of results to retrieve cannot exceed 100.");
        }

        log.debug("queryBio text={} start={} rows={}", text, start, rows);
        try {
            ExpandedSearch searchResult;
            if (orcidConfiguration.isApiConfigured()) {
                searchResult = orcidClient.expandedSearch(getAccessToken(), text, start, rows);
            } else {
                searchResult = orcidClient.expandedSearch(text, start, rows);
            }

            List<Person> bios = new LinkedList<>();
            for (ExpandedResult result : searchResult.getResults()) {
                String orcid = result.getOrcidId();
                if (orcid != null) {
                    log.debug("Found OrcidId={}", orcid);
                    Person bio = getBio(orcid);
                    if (bio != null) {
                        bios.add(bio);
                    }
                }
            }
            return bios;
        } catch (OrcidClientException e) {
            log.error("Error searching ORCID for query={}", text, e);
            return Collections.emptyList();
        }
    }

    /**
     * Retrieve a list of Person objects.
     *
     * @param text search string
     * @param max  how many rows to return
     * @return List of Persons
     */
    public List<Person> queryBio(String text, int max) {
        return queryBio(text, 0, max);
    }

    /**
     * Retrieve an access token for API calls.
     *
     * @return the access token
     */
    private String getAccessToken() {
        return orcidClient.getReadPublicAccessToken().getAccessToken();
    }

    /**
     * Check to see if the provided text has the correct ORCID syntax.
     */
    private boolean isValid(String text) {
        return StringUtils.isNotBlank(text) && text.matches(Orcidv3AuthorityValue.ORCID_ID_SYNTAX);
    }
}
