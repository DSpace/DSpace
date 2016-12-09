/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.ezid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.util.EntityUtils;
import org.dspace.identifier.IdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoded response data evoked by a request made to EZID.
 */
public class EZIDResponse
{
    private static final Logger log = LoggerFactory.getLogger(EZIDResponse.class);

    private static final String UTF_8 = "UTF-8";

    private final String status;

    private final String statusValue;

    private final Map<String, String> attributes = new HashMap<String, String>();

    private final HttpResponse response;

    public EZIDResponse(HttpResponse response)
            throws IdentifierException
    {
        this.response = response;

        HttpEntity responseBody = response.getEntity();

        // Collect the content of the percent-encoded response.
        String body;
        try
        {
            body = EntityUtils.toString(responseBody, UTF_8);
        } catch (IOException ex)
        {
            log.error(ex.getMessage());
            throw new IdentifierException("EZID response not understood:  "
                    + ex.getMessage());
        } catch (ParseException ex)
        {
            log.error(ex.getMessage());
            throw new IdentifierException("EZID response not understood:  "
                    + ex.getMessage());
        }

        String[] parts;

        String[] lines = body.split("[\\n\\r]");

        // First line is request status and message or value
        parts = lines[0].split(":", 2);
        status = parts[0].trim();
        if (parts.length > 1)
        {
            statusValue = parts[1].trim();
        }
        else
        {
            statusValue = null;
        }

        // Remaining lines are key: value pairs
        for (int i = 1; i < lines.length; i++)
        {
            parts = lines[i].split(":", 2);
            String key = null, value = null;
            try {
                key = URLDecoder.decode(parts[0], UTF_8).trim();
                if (parts.length > 1)
                {
                    value = URLDecoder.decode(parts[1], UTF_8).trim();
                }
                else
                {
                    value = null;
                }
            } catch (UnsupportedEncodingException e) {
                // XXX SNH, we always use UTF-8 which is required by the Java spec.
            }
            attributes.put(key, value);
        }
    }

    /**
     * Did the EZID request succeed?
     *
     * @return returned status was success.
     */
    public boolean isSuccess()
    {
        return status.equalsIgnoreCase("success");
    }

    /**
     * Get the EZID request status.
     *
     * @return should be "success" or "error".
     */
    public String getEZIDStatus()
    {
        return status;
    }

    /**
     * Value associated with the EZID status (identifier, error text, etc.).
     *
     * @return EZID status value
     */
    public String getEZIDStatusValue()
    {
        return statusValue;
    }

    /**
     * Expose the available keys.
     *
     * @return all keys found in the response.
     */
    public List<String> getKeys()
    {
        List<String> keys = new ArrayList<String>();
        for (String key : attributes.keySet())
        {
            keys.add(key);
        }
        return keys;
    }

    /**
     * Look up the value of a given response datum.
     *
     * @param key the datum to look up.
     * @return the value of {@code key}, or null if {@code key} is undefined.
     */
    public String get(String key)
    {
        return attributes.get(key);
    }

    /**
     * @return status of the HTTP request.
     */
    public int getHttpStatusCode()
    {
        return response.getStatusLine().getStatusCode();
    }

    /**
     * @return reason for status of the HTTP request.
     */
    public String getHttpReasonPhrase()
    {
        return response.getStatusLine().getReasonPhrase();
    }
}
