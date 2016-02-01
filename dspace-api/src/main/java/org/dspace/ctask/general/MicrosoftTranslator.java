/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

/**
 * MicrosoftTranslator translates metadata fields using Microsoft Translation API v2
 *
 * Requirements: A valid Bing App ID/Key
 *               More information: http://www.bing.com/developers
 *
 *               This key, and other custom configuration, goes in [dspace]/modules/translator.cfg
 *
 * @author Kim Shepherd
 */

public class MicrosoftTranslator extends AbstractTranslator
{

    protected final String PLUGIN_PREFIX = "translator";

    protected final String baseUrl = "http://api.microsofttranslator.com/V2/Http.svc/Translate";

    private static final Logger log = Logger.getLogger(MicrosoftTranslator.class);


    @Override
    protected void initApi() {
        apiKey =  ConfigurationManager.getProperty(PLUGIN_PREFIX, "api.key.microsoft");
    }

    @Override
    protected String translateText(String from, String to, String text) throws IOException {

        log.debug("Performing API call to translate from " + from + " to " + to);

        text = URLEncoder.encode(text, "UTF-8");

        String translatedText = null;

        String url = baseUrl + "?appId=" + apiKey;
        url += "&to=" + to + "&from=" + from + "&text=" + text;

        HttpClient client = new DefaultHttpClient();
        HttpGet hm = new HttpGet(url);
        HttpResponse httpResponse = client.execute(hm);
        log.debug("Response code from API call is " + httpResponse);

        if(httpResponse.getStatusLine().getStatusCode() == 200) {
            String response = IOUtils.toString(httpResponse.getEntity().getContent(), StandardCharsets.ISO_8859_1);
            response = response.replaceAll("<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">","");
            response = response.replaceAll("</string>","");
            translatedText = response;
        }


        return translatedText;
    }

}

