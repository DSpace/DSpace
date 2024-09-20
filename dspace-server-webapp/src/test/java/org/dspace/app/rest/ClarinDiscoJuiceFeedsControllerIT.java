/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.ClarinDiscoJuiceFeedsDownloadService.openURLConnection;
import static org.dspace.app.rest.repository.ClarinDiscoJuiceFeedsController.APPLICATION_JAVASCRIPT_UTF8;
import static org.junit.Assert.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.app.rest.utils.Utils;
import org.dspace.services.ConfigurationService;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Test class for the controller ClarinDiscoJuiceFeedsController
 *
 * @author Milan Majchrak (dspace at dataquest.sk)
 */
public class ClarinDiscoJuiceFeedsControllerIT extends AbstractControllerIntegrationTest {

    @Autowired
    ConfigurationService configurationService;

    @Autowired
    ClarinDiscoJuiceFeedsUpdateScheduler clarinDiscoJuiceFeedsUpdateScheduler;

    // Just to make sure that the DiscoFeed URL is accessible.
    @Ignore
    @Test
    public void testDiscoFeedURL() throws Exception {
        String discoFeedURL = configurationService.getProperty("shibboleth.discofeed.url.test.connection");
        if (StringUtils.isBlank(discoFeedURL)) {
            throw new RuntimeException("The DiscoFeed testing URL is not set in the configuration. Setup the " +
                    "shibboleth.discofeed.url.test.connection property in the configuration.");
        }

        boolean disableSSL = configurationService.getBooleanProperty("disable.ssl.check.specific.requests", false);
        JSONParser parser = new JSONParser();
        try {
            URL url = new URL(discoFeedURL);
            URLConnection conn = openURLConnection(String.valueOf(url));
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);

            // Disable SSL certificate validation
            if (disableSSL && conn instanceof HttpsURLConnection) {
                Utils.disableCertificateValidation((HttpsURLConnection) conn);
            }

            Object obj = parser.parse(new InputStreamReader(conn.getInputStream()));
            assertNotNull(obj);
        } catch (IOException | ParseException e) {
            throw new RuntimeException("Error while reading the DiscoFeed URL: " + discoFeedURL, e);
        }
    }

    @Test
    public void getDiscoFeeds() throws Exception {
        String authTokenAdmin = getAuthToken(eperson.getEmail(), password);

        String configKey = "shibboleth.discofeed.allowed";
        boolean origVal = configurationService.getBooleanProperty(configKey);
        configurationService.setProperty(configKey, true);
        clarinDiscoJuiceFeedsUpdateScheduler.afterPropertiesSet();

        // Expected response created from the test file: `discofeedResponse.json`
        // Wrapped to the `callback` string = `dj_md_1`
        String expStr = "dj_md_1([{\"country\":\"CZ\",\"keywords\":[\"Identity Provider for employees and " +
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
                .andExpect(content().string(expStr));

        configurationService.setProperty(configKey, origVal);
    }
}
