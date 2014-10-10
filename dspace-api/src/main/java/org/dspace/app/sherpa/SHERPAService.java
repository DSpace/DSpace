/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import org.apache.http.client.HttpClient;
import org.apache.http.HttpStatus;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.dspace.core.ConfigurationManager;

public class SHERPAService
{
    public SHERPAResponse searchByJournalISSN(String query)
    {
        String endpoint = ConfigurationManager.getProperty("sherpa.romeo.url");
        String apiKey = ConfigurationManager.getProperty("sherpa.romeo.apikey");
        
        HttpGet method = null;
        try
        {
            URIBuilder uriBuilder = new URIBuilder(endpoint);
            uriBuilder.addParameter("issn", query);
            uriBuilder.addParameter("versions", "all");
            if (StringUtils.isNotBlank(apiKey))
                uriBuilder.addParameter("ak", apiKey);

            method = new HttpGet(uriBuilder.build());

            // Execute the method.
            HttpClient client = new DefaultHttpClient();
            HttpResponse response = client.execute(method);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode != HttpStatus.SC_OK)
            {
                return new SHERPAResponse("SHERPA/RoMEO return not OK status: "
                        + statusCode);
            }

            HttpEntity responseBody = response.getEntity();
            if (null != responseBody)
                return new SHERPAResponse(responseBody.getContent());
            else
                return new SHERPAResponse("SHERPA/RoMEO returned no response");
        }
        catch (Exception e)
        {
            return new SHERPAResponse(
                    "Error processing the SHERPA/RoMEO answer");
        }
        finally
        {
            if (method != null)
            {
                method.releaseConnection();
            }
        }
    }
}
