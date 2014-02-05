/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.integration.xoai;

import org.junit.Test;

import java.util.Date;

import static org.dspace.xoai.tests.helpers.SyntacticSugar.and;
import static org.dspace.xoai.tests.helpers.SyntacticSugar.given;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class IdentifyTest extends AbstractDSpaceTest {

    public static final Date EARLIEST_DATE = new Date();

    @Test
    public void requestForIdentifyWithoutRequiredConfigurationAdminEmailSetShouldFail() throws Exception {
        given(theDSpaceConfiguration()
                .withoutProperty("mail.admin"));
        and(given(theConfiguration().withContextConfigurations(aContext("request"))));

        againstTheDataProvider().perform(get("/request?verb=Identify"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    public void requestForIdentifyShouldReturnTheConfiguredValues() throws Exception {
        given(theDSpaceConfiguration()
                .hasProperty("dspace.name", "Test")
                .hasProperty("mail.admin", "test@test.com"));

        and(given(theEarlistEarliestDate().is(EARLIEST_DATE)));
        and(given(theConfiguration().withContextConfigurations(aContext("request"))));

        againstTheDataProvider().perform(get("/request?verb=Identify"))
                .andExpect(status().isOk())
                .andExpect(oaiXPath("//repositoryName").string("Test"))
                .andExpect(oaiXPath("//adminEmail").string("test@test.com"))
                .andExpect(oaiXPath("//earliestDatestamp").string(is(representationOfDate(EARLIEST_DATE))));
    }
}
