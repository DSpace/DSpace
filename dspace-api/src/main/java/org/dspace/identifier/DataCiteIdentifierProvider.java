/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Provide service for DOIs through DataCite.
 * 
 * @author mwood
 */
public class DataCiteIdentifierProvider
    extends IdentifierProvider
{
    private static final Logger log = LoggerFactory.getLogger(DataCiteIdentifierProvider.class);

    private static final Pattern DOIpattern = Pattern.compile("doi:[\\S]+");
    private static final ContentType CONTENT_UTF8_TEXT = ContentType.create("text/plain", "UTF-8");

    private static String EZID_SCHEME;
    private static String EZID_HOST;
    private static String EZID_PATH;

    /** Map DataCite metadata into local metadata. */
    private static Map<String, String> crosswalk = new HashMap<String, String>();

    // TODO move these to MetadataSchema or some such
    public static final String MD_SCHEMA_DSPACE = "dspace";
    public static final String DSPACE_DOI_ELEMENT = "identifier";
    public static final String DSPACE_DOI_QUALIFIER = "doi";

    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return DOI.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier)
    {
        return identifier.startsWith("doi:"); // XXX more thorough test?
    }

    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        Item item;

        if (dso instanceof Item)
            item = (Item)dso;
        else
            throw new IdentifierException("Unsupported object type " + dso.getTypeText());

        String id;
        DCValue[] previous = item.getMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT, DSPACE_DOI_QUALIFIER, null);
        if ((previous.length > 0) && (null != previous[0].value))
            return previous[0].value;

        id = mint(context, item);
        item.addMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT, DSPACE_DOI_QUALIFIER, null, id);
        return id;
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        String doi = request(generatePostBody(dso));
        return doi;
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String lookup(Context context, DSpaceObject object)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        Item item;
        if (!(object instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + object.getTypeText());

        item = (Item)object;
        DCValue[] metadata = item.getMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT, DSPACE_DOI_QUALIFIER, null);
        if (metadata.length > 0)
            return metadata[0].value;
        else
            throw new IdentifierNotFoundException(object.getTypeText() + " "
                    + object.getID() + " has no DOI");
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        if (!(dso instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());

        String username, password; // TODO get these from configuration

        Item item = (Item)dso;

        // delete from EZID
        for (DCValue id : item.getMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT,
                DSPACE_DOI_QUALIFIER, null))
        {
            EZIDResponse response;
            try {
                EZIDRequest request = new EZIDRequest(id.value, username, password);
                response = request.delete();
            } catch (URISyntaxException e) {
                throw new IdentifierException("Bad URI in metadata value", e);
            } catch (IOException e) {
                throw new IdentifierException("Failed request to EZID", e);
            }
            if (!response.isSuccess())
                throw new IdentifierException("Unable to delete " + id.value
                        + "from DataCite:  " + response.getEZIDStatusValue());
        }

        // delete from item
        item.clearMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT, DSPACE_DOI_QUALIFIER, null);
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private static final String CFG_SHOULDER = "identifier.doi.ezid.shoulder";
    private static final String CFG_USER = "identifier.doi.ezid.user";
    private static final String CFG_PASSWORD = "identifier.doi.ezid.password";

    /**
     * Submit some object details and request identifiers for the object.
     *
     * @param postBody the details, formatted for EZID.
     */
    private String request(String postBody)
            throws IdentifierException
    {
        // Get configured
        String shoulder = configurationService.getProperty(CFG_SHOULDER);
        if (null == shoulder)
            throw new IdentifierException("Unconfigured:  define " + CFG_SHOULDER);

        String user = configurationService.getProperty(CFG_USER);
        String password = configurationService.getProperty(CFG_PASSWORD);
        if (null == user || null == password)
            throw new IdentifierException("Unconfigured:  define " + CFG_USER + " and " + CFG_PASSWORD);

        // Address the service
	URIBuilder mintURL = new URIBuilder();
	mintURL.setScheme(EZID_SCHEME)
	    .setHost(EZID_HOST)
	    .setPath(EZID_PATH + shoulder);

        // Compose the request
        EZIDRequest ezrequest;
        try {
            ezrequest = new EZIDRequest(mintURL, user, password);
        } catch (URISyntaxException ex) {
            log.error(ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Send the request
        EZIDResponse response;
        try
        {
            response = ezrequest.create(postBody);
        } catch (IOException ex)
        {
            log.error("Failed to send EZID request:  {}", ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Good response?
        if (HttpURLConnection.HTTP_CREATED != response.getHttpStatusCode())
            {
                log.error("EZID responded:  {} {}", response.getHttpStatusCode(),
                        response.getHttpReasonPhrase());
                throw new IdentifierException("DOI not created:  " + response.getHttpReasonPhrase());
            }

	// Extract the DOI from the content blob
        if (response.isSuccess())
        {
            String value = response.getEZIDStatusValue();
            int end = value.indexOf('|'); // Following pipe is "shadow ARK"
            if (end < 0)
                end = value.length();
            return value.substring(0, end).trim();
        }
        else
            throw new IdentifierException("No DOI returned");
    }

    /**
     * Assemble the identifier request document, one field per line.
     * 
     * @param dso the object we want to identify.
     */
    static private String generatePostBody(DSpaceObject dso)
    {
        if ((null == dso) || !(dso instanceof Item))
            throw new IllegalArgumentException("Must be an Item");
        Item item = (Item) dso; // TODO generalize to DSO when all DSOs have metadata.

        StringBuilder bupher = new StringBuilder();

        for (Map.Entry<String, String> datum : crosswalk.entrySet())
        {
            DCValue[] values = item.getMetadata(datum.getValue());
            if (null != values)
            {
                for (DCValue value : values)
                {
                    bupher.append(datum.getKey())
                            .append(": ")
                            .append(value.value)
                            .append('\n');
                }
            }
        }

	return bupher.toString();
    }

    /**
     * @param aEZID_SCHEME the EZID URL scheme to set
     */
    @Required
    public void setEZID_SCHEME(String aEZID_SCHEME)
    {
        EZID_SCHEME = aEZID_SCHEME;
    }

    /**
     * @param aEZID_HOST the EZID host to set
     */
    @Required
    public void setEZID_HOST(String aEZID_HOST)
    {
        EZID_HOST = aEZID_HOST;
    }

    /**
     * @param aEZID_PATH the EZID path to set
     */
    @Required
    public void setEZID_PATH(String aEZID_PATH)
    {
        EZID_PATH = aEZID_PATH;
    }

    /**
     * @param aCrosswalk the crosswalk to set
     */
    @Required
    public void setCrosswalk(Map<String, String> aCrosswalk)
    {
        crosswalk = aCrosswalk;
    }
}
