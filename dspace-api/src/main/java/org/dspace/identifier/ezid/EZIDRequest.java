/*
 * Copyright 2012 Indiana University.  All rights reserved.
 *
 * Mark H. Wood, IUPUI University Library, Nov 1, 2012
 */
package org.dspace.identifier.ezid;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.dspace.identifier.IdentifierException;

/**
 * A request to EZID concerning a given (or expected) identifier.
 * @author Mark H. Wood
 */
public class EZIDRequest
{
    private final URIBuilder uri;

    private final URI url;

    private final AbstractHttpClient client;

    /**
     * Prepare a context for requests concerning a specific identifier or
     * authority prefix.
     *
     * @param url an EZID URL for an identifier or authority.
     */
    public EZIDRequest(URIBuilder uri)
            throws URISyntaxException
    {
        this.uri = uri;
        url = this.uri.build();
        client = new DefaultHttpClient();
    }

    /**
     * Prepare a context for requests concerning a specific identifier or
     * authority prefix.
     *
     * @param url an EZID URL for an identifier or authority.
     */
    public EZIDRequest(String uri)
            throws URISyntaxException
    {
        this.uri = new URIBuilder(uri);
        url = this.uri.build();
        client = new DefaultHttpClient();
    }

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
    public EZIDResponse create(String metadata)
            throws IOException, IdentifierException
    {
        // PUT path [+metadata]
        HttpPut request;
        request = new HttpPut(url);
        if (null != metadata)
        {
            try {
                request.setEntity(new StringEntity(metadata, "UTF-8"));
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
    public EZIDResponse mint(String metadata)
            throws IOException, IdentifierException
    {
        // POST path [+metadata]
        HttpPost request;
        request = new HttpPost(url);
        if (null != metadata)
        {
            request.setEntity(new StringEntity(metadata, "UTF-8"));
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
    public EZIDResponse modify(String metadata)
            throws IOException, IdentifierException
    {
        if (null == metadata)
        {
            throw new IllegalArgumentException("metadata must not be null");
        }
        // POST path +metadata
        HttpPost request;
        request = new HttpPost(url);
        request.setEntity(new StringEntity(metadata, "UTF-8"));
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
        return modify("_status = unavailable");
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
        return modify("_status = unavailable | " + reasonEncoded);
    }
}
