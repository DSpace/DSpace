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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dspace.identifier.IdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A request to EZID concerning a given (or expected) identifier.
 *
 * @author Mark H. Wood
 */
public class EZIDRequest
{
    private static final Logger log = LoggerFactory.getLogger(EZIDRequest.class);

    private URI url;

    private AbstractHttpClient client;

    /**
     * Prepare a context for requests concerning a specific identifier or
     * authority prefix.
     *
     * @param url EZID API service point (and object) for this request.
     * @param username an EZID user identity.
     * @param password user's password, or null for none.
     */
    EZIDRequest(URI url, String username, String password)
            throws URISyntaxException
    {
        this.url = url;
        client = new DefaultHttpClient();
        if (null != username)
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(url.getHost(), url.getPort()),
                    new UsernamePasswordCredentials(username, password));
    }

    /**
     * Fetch an identifier's metadata.
     * 
     * @return
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     */
    public EZIDResponse lookup()
            throws IdentifierException, IOException
    {
        // GET path
        HttpGet request;
        request = new HttpGet(url);
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Create an identifier with a given name. The name is the end of the
     * request path. Note: to "reserve" a given identifier, include "_status =
     * reserved" in {@link metadata}.
     *
     * @param metadata ANVL-encoded key/value pairs.
     * @return
     */
    public EZIDResponse create(Map<String, String> metadata)
            throws IOException, IdentifierException
    {
        // PUT path [+metadata]
        HttpPut request;
        request = new HttpPut(url);
        if (null != metadata)
        {
            try {
                request.setEntity(new StringEntity(formatMetadata(metadata), "UTF-8"));
            } catch (UnsupportedEncodingException ex) { /* SNH */ }
        }
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Ask EZID to create a unique identifier and return its name. NOTE: to
     * "reserve" a unique identifier, include "_status = reserved" in {@link metadata}.
     *
     * @param metadata ANVL-encoded key/value pairs.
     * @return
     */
    public EZIDResponse mint(Map<String, String> metadata)
            throws IOException, IdentifierException
    {
        // POST path [+metadata]
        HttpPost request;
        request = new HttpPost(url);
        if (null != metadata)
        {
            request.setEntity(new StringEntity(formatMetadata(metadata), "UTF-8"));
        }
        HttpResponse response = client.execute(request);
        EZIDResponse myResponse = new EZIDResponse(response);
        // TODO add the identifier to the path for subsequent operations?
        return myResponse;
    }

    /**
     * Alter the identifier's metadata.
     *
     * @param metadata fields to be altered. Leave a field's value empty to
     *                 delete the field.
     * @return
     */
    public EZIDResponse modify(Map<String, String> metadata)
            throws IOException, IdentifierException
    {
        if (null == metadata)
        {
            throw new IllegalArgumentException("metadata must not be null");
        }
        // POST path +metadata
        HttpPost request;
        request = new HttpPost(url);
        request.setEntity(new StringEntity(formatMetadata(metadata), "UTF-8"));
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Destroy a reserved identifier. Fails if ID was ever public.
     */
    public EZIDResponse delete()
            throws IOException, IdentifierException
    {
        // DELETE path
        HttpDelete request;
        request = new HttpDelete(url);
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Remove a public identifier from view.
     */
    public EZIDResponse withdraw()
            throws IOException, IdentifierException
    {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("_status", "unavailable");
        return modify(metadata);
    }

    /**
     * Remove a public identifier from view, with a reason.
     *
     * @param reason annotation for the item's unavailability.
     */
    public EZIDResponse withdraw(String reason)
            throws IOException, IdentifierException
    {
        String reasonEncoded = null;
        try {
            reasonEncoded = URLEncoder.encode(reason, "UTF-8");
        } catch (UnsupportedEncodingException e) { /* XXX SNH */ }
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put("_status", "unavailable | " + reasonEncoded);
        return modify(metadata);
    }

    /**
     * Create ANVL-formatted name/value pairs from a Map.
     */
    private String formatMetadata(Map<String, String> raw)
    {
        StringBuilder formatted = new StringBuilder();
        for (Entry<String, String> entry : raw.entrySet())
            formatted.append(entry.getKey())
                    .append(": ")
                    .append(entry.getValue())
                    .append('\n');

        // Body should be percent-encoded
        String body = null;
        try {
            body = URLEncoder.encode(formatted.toString(), "UTF-8");
        } catch (UnsupportedEncodingException ex) { // XXX SNH
            log.error(ex.getMessage());
        } finally {
            return body;
        }
    }
}
