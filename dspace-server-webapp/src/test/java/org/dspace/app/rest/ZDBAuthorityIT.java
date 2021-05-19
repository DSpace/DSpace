/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.ItemAuthorityMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * This class handles ZDBAuthority related IT.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science.it)
 */
public class ZDBAuthorityIT extends AbstractControllerIntegrationTest {

    @Test
    public void zdbAuthorityTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/ZDBAuthority/entries")
            .param("filter", "Acta AND Mathematica AND informatica"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$._embedded.entries",
                Matchers.containsInAnyOrder(
                    ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
                        "will be generated::zdb::1447228-4", "Acta mathematica et informatica",
                        "Acta mathematica et informatica", "vocabularyEntry", "data-dc_relation_ispartof",
                        "Acta mathematica et informatica::will be generated::zdb::1447228-4",
                        "data-dc_relation_issn", ""),
                    ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
                        "will be generated::zdb::1194912-0",
                        "Acta mathematica Universitatis Ostraviensis",
                        "Acta mathematica Universitatis Ostraviensis", "vocabularyEntry",
                        "data-dc_relation_ispartof",
                        "Acta mathematica Universitatis Ostraviensis::will be generated::zdb::1194912-0",
                        "data-dc_relation_issn", "1211-4774"),
                    ItemAuthorityMatcher.matchItemAuthorityWithTwoMetadataInOtherInformations(
                        "will be generated::zdb::2618143-5",
                        "Acta mathematica Universitatis Ostraviensis",
                        "Acta mathematica Universitatis Ostraviensis", "vocabularyEntry",
                        "data-dc_relation_ispartof",
                        "Acta mathematica Universitatis Ostraviensis::will be generated::zdb::2618143-5",
                        "data-dc_relation_issn", ""))))
            .andExpect(jsonPath("$.page.totalElements", Matchers.is(3)));
    }

    @Test
    public void zdbAuthorityEmptyQueryTest() throws Exception {
        String token = getAuthToken(eperson.getEmail(), password);
        getClient(token).perform(get("/api/submission/vocabularies/ZDBAuthority/entries")
            .param("filter", ""))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void zdbAuthorityUnauthorizedTest() throws Exception {
        getClient().perform(get("/api/submission/vocabularies/ZDBAuthority/entries")
            .param("filter", "Mathematica AND informatica"))
            .andExpect(status().isUnauthorized());
    }
}
