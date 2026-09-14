/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.matcher.SubmissionCCLicenseMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.hamcrest.Matchers;
import org.junit.Test;

/**
 * Class to the methods from the SubmissionCCLicenseRestRepository
 * CC License now grabs from a set csv and index.rdf for cc bundle license.
 */
public class SubmissionCCLicenseRestRepositoryIT extends AbstractControllerIntegrationTest {


    /**
     * Test the findAll method form the SubmissionCCLicenseRestRepository
     *
     * @throws Exception
     */
    @Test
    public void findAllTest() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses"))
                   .andExpect(status().isOk())
                   .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.submissioncclicenses", Matchers.containsInAnyOrder(
                        SubmissionCCLicenseMatcher.matchLicenseEntry(
                                "license1",
                                "Creative Commons License (3.0)",
                                new int[]{2, 3, 44}
                        ),
                        SubmissionCCLicenseMatcher.matchLicenseEntry(
                                "license265",
                                "Creative Commons License (4.0)",
                                new int[]{2, 3}
                        ),
                        SubmissionCCLicenseMatcher.matchLicenseEntry(
                                "license271",
                                "CC0 1.0 Universal",
                                new int[]{}
                        ),
                        SubmissionCCLicenseMatcher.matchLicenseEntry(
                                "license272",
                                "Public Domain Mark 1.0 Universal",
                                new int[]{}
                        ),
                        SubmissionCCLicenseMatcher.matchLicenseEntry(
                                "license273",
                                "CERTIFICATION 1.0",
                                new int[]{}
                        )
                )));
    }

    @Test
    public void findOneCC40Test() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses/license265"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", Matchers.is("license265")))
                .andExpect(jsonPath("$.name", Matchers.is("Creative Commons License (4.0)")))
                .andExpect(jsonPath("$.fields", Matchers.hasSize(2)))
                // commercial field
                .andExpect(jsonPath("$.fields[0].id", Matchers.is("commercial")))
                .andExpect(jsonPath("$.fields[0].enums", Matchers.hasSize(2)))
                .andExpect(jsonPath("$.fields[0].enums[0].id", Matchers.is("y")))
                .andExpect(jsonPath("$.fields[0].enums[1].id", Matchers.is("n")))
                // derivatives field
                .andExpect(jsonPath("$.fields[1].id", Matchers.is("derivatives")))
                .andExpect(jsonPath("$.fields[1].enums", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.fields[1].enums[0].id", Matchers.is("y")))
                .andExpect(jsonPath("$.fields[1].enums[1].id", Matchers.is("sa")))
                .andExpect(jsonPath("$.fields[1].enums[2].id", Matchers.is("n")));
    }

    @Test
    public void findOneCC30Test() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses/license1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", Matchers.is("license1")))
                .andExpect(jsonPath("$.name", Matchers.is("Creative Commons License (3.0)")))
                .andExpect(jsonPath("$.fields", Matchers.hasSize(3)))
                .andExpect(jsonPath("$.fields[0].id", Matchers.is("commercial")))
                .andExpect(jsonPath("$.fields[1].id", Matchers.is("derivatives")))
                .andExpect(jsonPath("$.fields[2].id", Matchers.is("jurisdiction")))
                // jurisdiction always includes the generic/unported option first
                .andExpect(jsonPath("$.fields[2].enums[0].id", Matchers.is("")));
    }

    @Test
    public void findOneCC0Test() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses/license271"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$.id", Matchers.is("license271")))
                .andExpect(jsonPath("$.name", Matchers.is("CC0 1.0 Universal")))
                .andExpect(jsonPath("$.fields", Matchers.hasSize(0)));
    }

    @Test
    public void findOneTestNonExistingLicense() throws Exception {
        String epersonToken = getAuthToken(eperson.getEmail(), password);

        getClient(epersonToken).perform(get("/api/config/submissioncclicenses/non-existing-license"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void findAllTestUnAuthorized() throws Exception {
        getClient().perform(get("/api/config/submissioncclicenses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneTestUnAuthorized() throws Exception {
        getClient().perform(get("/api/config/submissioncclicenses/license1"))
                .andExpect(status().isUnauthorized());
    }

}
