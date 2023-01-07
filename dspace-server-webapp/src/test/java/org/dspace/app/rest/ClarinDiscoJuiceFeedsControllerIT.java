/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.repository.ClarinDiscoJuiceFeedsController.APPLICATION_JAVASCRIPT_UTF8;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.services.ConfigurationService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test class for the controller ClarinDiscoJuiceFeedsController
 *
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class ClarinDiscoJuiceFeedsControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Test
    public void getDiscoFeeds() throws Exception {
        String authTokenAdmin = getAuthToken(eperson.getEmail(), password);

        // Expected response created from the test file: `discofeedResponse.json`
        // Wrapped to the `callback` string = `dj_md_1`
        String responseString = "dj_md_1([{\"country\":\"CZ\",\"keywords\":[\"Identity Provider for employees and " +
                "readers of the Archiepiscopal Gymnasium in Kromeriz - Library\",\"Identity Provider pro zamstnance " +
                "a tene knihovny Arcibiskupskho gymnzia v Kromi\",\"Arcibiskupsk gymnzium v Kromi - " +
                "Knihovna\"],\"entityID\":\"https:\\/\\/agkm.cz\\/idp\\/shibboleth\",\"title\":\"Archiepiscopal " +
                "Gymnasium in Kromeriz - Library\"},{\"country\":\"CZ\",\"keywords\":[\"Identity Provider for staff " +
                "of the Institute of Agricultural Economics and Information and patrons of the Antonn vehla " +
                "Library\",\"Identity Provider pro zamstnance ZEI a tene Knihovny Antonna vehly\",\"stav " +
                "zemdlsk ekonomiky a informac\"],\"entityID\":\"https:\\/\\/aleph.uzei.cz\\/idp\\/shibboleth\"," +
                "\"title\":\"Institute of Agricultural Economics and Information\"},{\"country\":\"CZ\",\"" +
                "keywords\":[\"Identity Provider for patrons and staff of the Research Library in Hradec Krlov\"," +
                "\"Identity Provider pro tene a zamstance Studijn a vdeck knihovny v Hradci Krlov\"," +
                "\"Studijn a vdeck knihovna v Hradci Krlov\"],\"entityID\":\"https:\\/\\/aleph.svkhk.cz\\" +
                "/idp\\/shibboleth\",\"title\":\"The Research Library in Hradec Krlov\"}])";


        // Load bitstream from the item.
        // Request with callback
        getClient(authTokenAdmin).perform(get("/api/discojuice/feeds?callback=dj_md_1"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(APPLICATION_JAVASCRIPT_UTF8))
                .andExpect(content().string(responseString));
    }
}
