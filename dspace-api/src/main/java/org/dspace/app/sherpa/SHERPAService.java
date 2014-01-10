/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.sherpa;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.ConfigurationManager;

public class SHERPAService
{
    public SHERPAResponse searchByJournalISSN(String query)
    {
        String endpoint = ConfigurationManager.getProperty("sherpa.romeo.url");
        String apiKey = ConfigurationManager.getProperty("sherpa.romeo.apikey");
        
        GetMethod method = null;
        try
        {
            HttpClient client = new HttpClient();
            method = new GetMethod(endpoint);

            NameValuePair id = new NameValuePair("issn", query);
            NameValuePair versions = new NameValuePair("versions", "all");
            NameValuePair[] params = null;
            if (StringUtils.isNotBlank(apiKey))
            {
                NameValuePair ak = new NameValuePair("ak", apiKey);
                params = new NameValuePair[] { id, versions, ak };
            }
            else
            {
                params = new NameValuePair[] { id, versions };
            }
            method.setQueryString(params);
            // Execute the method.
            int statusCode = client.executeMethod(method);

            if (statusCode != HttpStatus.SC_OK)
            {
                return new SHERPAResponse("SHERPA/RoMEO return not OK status: "
                        + statusCode);
            }

            return new SHERPAResponse(method.getResponseBodyAsStream());
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
