/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.dspace.core.ConfigurationManager;

import java.io.IOException;
import java.net.URLEncoder;

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

    private static final String PLUGIN_PREFIX = "translator";

    private static final String baseUrl = "http://api.microsofttranslator.com/V2/Http.svc/Translate";
    private static String apiKey = "";

    private static Logger log = Logger.getLogger(MicrosoftTranslator.class);


    @Override
    protected void initApi() {
        apiKey =  ConfigurationManager.getProperty(PLUGIN_PREFIX, "translate.api.key.microsoft");
    }

    @Override
    protected String translateText(String from, String to, String text) throws IOException {

        log.debug("Performing API call to translate from " + from + " to " + to);

        text = URLEncoder.encode(text, "UTF-8");

        String translatedText = null;

        String url = baseUrl + "?appId=" + apiKey;
        url += "&to=" + to + "&from=" + from + "&text=" + text;

        HttpClient client = new HttpClient();
        HttpMethod hm = new GetMethod(url);
        int code = client.executeMethod(hm);
        log.debug("Response code from API call is " + code);

        if(code == 200) {
            String response = hm.getResponseBodyAsString();
            response = response.replaceAll("<string xmlns=\"http://schemas.microsoft.com/2003/10/Serialization/\">","");
            response = response.replaceAll("</string>","");
            translatedText = response;
        }


        return translatedText;
    }

}

