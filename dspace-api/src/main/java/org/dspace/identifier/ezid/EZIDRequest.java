/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier.ezid;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import org.dspace.identifier.DOI;
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

    private static final String ID_PATH = "/id/" + DOI.SCHEME;

    private static final String SHOULDER_PATH = "/shoulder/" + DOI.SCHEME;

    private static final String UTF_8 = "UTF-8";

    private static final String MD_KEY_STATUS = "_status";

    private final AbstractHttpClient client;

    private final String scheme;

    private final String host;

    private final String path;

    private final String authority;

    /**
     * Prepare a context for requests concerning a specific identifier or
     * authority prefix.
     *
     * @param scheme URL scheme for access to the EZID service.
     * @param host Host name for access to the EZID service.
     * @param authority DOI authority prefix, e.g. "10.5072/FK2".
     * @param username an EZID user identity.
     * @param password user's password, or {@code null} for none.
     * @throws URISyntaxException if host or authority is bad.
     * @deprecated since 4.1
     */
    @Deprecated
    EZIDRequest(String scheme, String host, String authority, String username, String password)
            throws URISyntaxException
    {
        this.scheme = scheme;

        this.host = host;

        this.path = "ezid";

        if (authority.charAt(authority.length()-1) == '/')
        {
            this.authority = authority.substring(0, authority.length()-1);
        }
        else
        {
            this.authority = authority;
        }

        client = new DefaultHttpClient();
        if (null != username)
        {
            URI uri = new URI(scheme, host, path, null);
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    new UsernamePasswordCredentials(username, password));
        }
    }

    /**
     * Prepare a context for requests concerning a specific identifier or
     * authority prefix.
     *
     * @param scheme URL scheme for access to the EZID service.
     * @param host Host name for access to the EZID service.
     * @param path Local-path to the EZID service.
     * @param authority DOI authority prefix, e.g. "10.5072/FK2".
     * @param username an EZID user identity.
     * @param password user's password, or {@code null} for none.
     * @throws URISyntaxException if host or authority is bad.
     */
    EZIDRequest(String scheme, String host, String path,
            String authority, String username, String password)
            throws URISyntaxException
    {
        this.scheme = scheme;

        this.host = host;

        this.path = path;

        if (authority.charAt(authority.length()-1) == '/')
        {
            this.authority = authority.substring(0, authority.length()-1);
        }
        else
        {
            this.authority = authority;
        }

        client = new DefaultHttpClient();
        if (null != username)
        {
            URI uri = new URI(scheme, host, path, null);
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(uri.getHost(), uri.getPort()),
                    new UsernamePasswordCredentials(username, password));
        }
    }

    /**
     * Fetch the metadata bound to an identifier.
     *
     * @param name
     *     identifier name
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse lookup(String name)
            throws IdentifierException, IOException, URISyntaxException
    {
        // GET path
        HttpGet request;
        URI uri = new URI(scheme, host, path + ID_PATH + authority + name, null);
        log.debug("EZID lookup {}", uri.toASCIIString());
        request = new HttpGet(uri);
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Create an identifier with a given name. The name is the end of the
     * request path. Note: to "reserve" a given identifier, include "_status =
     * reserved" in {@code metadata}.
     *
     * @param name
     *     identifier name
     * @param metadata ANVL-encoded key/value pairs.
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse create(String name, Map<String, String> metadata)
            throws IOException, IdentifierException, URISyntaxException
    {
        // PUT path [+metadata]
        HttpPut request;
        URI uri = new URI(scheme, host, path + ID_PATH + authority + '/' + name, null);
        log.debug("EZID create {}", uri.toASCIIString());
        request = new HttpPut(uri);
        if (null != metadata)
        {
            request.setEntity(new StringEntity(formatMetadata(metadata), UTF_8));
        }
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Ask EZID to create a unique identifier and return its name. NOTE: to
     * "reserve" a unique identifier, include "_status = reserved" in {@code metadata}.
     *
     * @param metadata ANVL-encoded key/value pairs.
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse mint(Map<String, String> metadata)
            throws IOException, IdentifierException, URISyntaxException
    {
        // POST path [+metadata]
        HttpPost request;
        URI uri = new URI(scheme, host, path + SHOULDER_PATH + authority, null);
        log.debug("EZID mint {}", uri.toASCIIString());
        request = new HttpPost(uri);
        if (null != metadata)
        {
            request.setEntity(new StringEntity(formatMetadata(metadata), UTF_8));
        }
        HttpResponse response = client.execute(request);
        EZIDResponse myResponse = new EZIDResponse(response);
        return myResponse;
    }

    /**
     * Alter the metadata bound to an identifier.
     *
     * @param name
     *     identifier name
     * @param metadata
     *     metadata fields to be altered. Leave the value of a field's empty
     *     to delete the field.
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse modify(String name, Map<String, String> metadata)
            throws IOException, IdentifierException, URISyntaxException
    {
        if (null == metadata)
        {
            throw new IllegalArgumentException("metadata must not be null");
        }
        // POST path +metadata
        HttpPost request;
        URI uri = new URI(scheme, host, path + ID_PATH + authority + name, null);
        log.debug("EZID modify {}", uri.toASCIIString());
        request = new HttpPost(uri);
        request.setEntity(new StringEntity(formatMetadata(metadata), UTF_8));
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Destroy a reserved identifier. Fails if ID was ever public.
     *
     * @param name
     *     identifier name
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse delete(String name)
            throws IOException, IdentifierException, URISyntaxException
    {
        // DELETE path
        HttpDelete request;
        URI uri = new URI(scheme, host, path + ID_PATH + authority + name, null);
        log.debug("EZID delete {}", uri.toASCIIString());
        request = new HttpDelete(uri);
        HttpResponse response = client.execute(request);
        return new EZIDResponse(response);
    }

    /**
     * Remove a public identifier from view.
     *
     * @param name
     *     identifier name
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse withdraw(String name)
            throws IOException, IdentifierException, URISyntaxException
    {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put(MD_KEY_STATUS, "unavailable");
        return modify(name, metadata);
    }

    /**
     * Remove a public identifier from view, with a reason.
     *
     * @param name
     *     identifier name
     * @param reason
     *     annotation for the item's unavailability.
     * @return Decoded response data evoked by a request made to EZID.
     * @throws IdentifierException if the response is error or body malformed.
     * @throws IOException if the HTTP request fails.
     * @throws URISyntaxException if host or authority is bad.
     */
    public EZIDResponse withdraw(String name, String reason)
            throws IOException, IdentifierException, URISyntaxException
    {
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put(MD_KEY_STATUS, "unavailable | " + escape(reason));
        return modify(name, metadata);
    }

    /**
     * Create ANVL-formatted name/value pairs from a Map.
     *
     * @param raw
     *     
     */
    private static String formatMetadata(Map<String, String> raw)
    {
        StringBuilder formatted = new StringBuilder();
        for (Entry<String, String> entry : raw.entrySet())
        {
            formatted.append(escape(entry.getKey()))
                    .append(": ")
                    .append(escape(entry.getValue()))
                    .append('\n');
        }

        return formatted.toString();
    }

    /**
     * Percent-encode a few EZID-specific characters.
     *
     * @return null for null input.
     */
    private static String escape(String s)
    {
        if (null == s) { return s; }

        return s.replace("%", "%25")
                .replace("\n", "%0A")
                .replace("\r", "%0D")
                .replace(":", "%3A");
    }
}
