/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.io.IOException;
import java.net.*;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.identifier.ezid.EZIDRequest;
import org.dspace.identifier.ezid.EZIDRequestFactory;
import org.dspace.identifier.ezid.EZIDResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Provide service for DOIs through DataCite.
 *
 * <p>Configuration of this class is is in two parts.</p>
 *
 * <p>Installation-specific configuration (credentials and the "shoulder" value
 * which forms a prefix of the site's DOIs) is supplied from property files in
 * [DSpace]/config**.</p>
 *
 * <dl>
 *  <dt>identifier.doi.ezid.shoulder</dt>
 *  <dd>base of the site's DOIs</dd>
 *  <dt>identifier.doi.ezid.user</dt>
 *  <dd>EZID username</dd>
 *  <dt>identifier.doi.ezid.password</dt>
 *  <dd>EZID password</dd>
 * </dl>
 *
 * <p>There is also a Map (with the property name "crosswalk") from EZID
 * metadata field names into DSpace field names, injected by Spring.  Specify
 * the fully-qualified names of all metadata fields to be looked up on a DSpace
 * object and their values set on mapped fully-qualified names in the object's
 * DataCite metadata.</p>
 *
 * @author mwood
 */
public class DataCiteIdentifierProvider
    extends IdentifierProvider
{
    private static final Logger log = LoggerFactory.getLogger(DataCiteIdentifierProvider.class);

    // Configuration property names
    private static final String CFG_SHOULDER = "identifier.doi.ezid.shoulder";
    private static final String CFG_USER = "identifier.doi.ezid.user";
    private static final String CFG_PASSWORD = "identifier.doi.ezid.password";

    // Metadata field name elements
    // XXX move these to MetadataSchema or some such
    public static final String MD_SCHEMA_DSPACE = "dspace";
    public static final String DSPACE_DOI_ELEMENT = "identifier";
    public static final String DSPACE_DOI_QUALIFIER = "doi";

    /** Map DataCite metadata into local metadata. */
    private static Map<String, String> crosswalk = new HashMap<String, String>();

    /** Factory for EZID requests. */
    private static EZIDRequestFactory requestFactory;

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
        log.debug("register {}", dso);

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
        try {
            item.update();
        } catch (SQLException ex) {
            throw new IdentifierException("New identifier not stored", ex);
        } catch (AuthorizeException ex) {
            throw new IdentifierException("New identifier not stored", ex);
        }
        log.info("Registered {}", id);
        return id;
    }

    @Override
    public void register(Context context, DSpaceObject object, String identifier)
    {
        log.debug("register {} as {}", object, identifier);

        if (!(object instanceof Item))
        {
            // TODO throw new IdentifierException("Unsupported object type " + object.getTypeText());
            log.error("Unsupported object type " + object.getTypeText());
            return;
        }

        EZIDResponse response;
        String doi = "unknown"; // In case we can't even build a name
        try {
            doi = getShoulder() + identifier;
            EZIDRequest request = requestFactory.getInstance(doi,
                    getUser(), getPassword());
            response = request.create(crosswalkMetadata(object));
        } catch (IdentifierException e) {
            log.error("doi:{} not registered:  {}", doi, e.getMessage());
            return;
        } catch (IOException e) {
            log.error("doi:{} not registered:  {}", doi, e.getMessage());
            return;
        } catch (URISyntaxException e) {
            log.error("doi:{} not registered:  {}", doi, e.getMessage());
            return;
        }

        if (response.isSuccess())
        {
            Item item = (Item)object;
            item.addMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT,
                    DSPACE_DOI_QUALIFIER, null, identifier);
            try {
                item.update();
                log.info("registered {}", identifier);
            } catch (SQLException ex) {
                // TODO throw new IdentifierException("New identifier not stored", ex);
                log.error("New identifier not stored", ex);
            } catch (AuthorizeException ex) {
                // TODO throw new IdentifierException("New identifier not stored", ex);
                log.error("New identifier not stored", ex);
            }
        }
        else
        {
            log.error("doi:{} not registered -- EZID returned: {}", doi,
                    response.getEZIDStatusValue());
        }
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("reserve {}", identifier);

        EZIDResponse response;
        String doi = "unknown"; // In case we can't even build a name
        try {
            doi = getShoulder() + identifier;
            EZIDRequest request = requestFactory.getInstance(doi,
                    getUser(), getPassword());
            Map<String, String> metadata = crosswalkMetadata(dso);
            metadata.put("_status", "reserved");
            response = request.create(metadata);
        } catch (IOException e) {
            log.error("doi:{} not registered:  {}", doi, e.getMessage());
            return;
        } catch (URISyntaxException e) {
            log.error("doi:{} not registered:  {}", doi, e.getMessage());
            return;
        }

        if (response.isSuccess())
        {
            Item item = (Item)dso;
            item.addMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT,
                    DSPACE_DOI_QUALIFIER, null, identifier);
            try {
                item.update();
                log.info("reserved {}", identifier);
            } catch (SQLException ex) {
                throw new IdentifierException("New identifier not stored", ex);
            } catch (AuthorizeException ex) {
                throw new IdentifierException("New identifier not stored", ex);
            }
        }
        else
        {
            log.error("doi:{} not registered -- EZID returned: {}", doi,
                    response.getEZIDStatusValue());
        }
    }

    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("mint for {}", dso);

        // Compose the request
        EZIDRequest request;
        try {
            request = requestFactory.getInstance(getShoulder(), getUser(), getPassword());
        } catch (URISyntaxException ex) {
            log.error(ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Send the request
        EZIDResponse response;
        try
        {
            response = request.mint(crosswalkMetadata(dso));
        } catch (IOException ex)
        {
            log.error("Failed to send EZID request:  {}", ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Good response?
        if (HttpURLConnection.HTTP_CREATED != response.getHttpStatusCode())
            {
                log.error("EZID server responded:  {} {}", response.getHttpStatusCode(),
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
            String doi = value.substring(0, end).trim();
            log.info("Created {}", doi);
            return doi;
        }
        else
        {
            log.error("EZID responded:  {}", response.getEZIDStatusValue());
            throw new IdentifierException("No DOI returned");
        }
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        log.debug("resolve {}", identifier);

        try
        {
            ItemIterator found = Item.findByMetadataField(context, MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT, DSPACE_DOI_QUALIFIER,
                    identifier);
            if (!found.hasNext())
                throw new IdentifierNotFoundException("No Item bound to DOI " + identifier);
            Item found1 = found.next();
            if (found.hasNext())
                log.error("DOI {} multiply bound!", identifier);
            log.debug("Resolved to {}", found1);
            return found1;
        } catch (SQLException ex)
        {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        } catch (AuthorizeException ex)
        {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        } catch (IOException ex)
        {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        }
    }

    @Override
    public String lookup(Context context, DSpaceObject object)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        log.debug("lookup {}", object);

        Item item;
        if (!(object instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + object.getTypeText());

        item = (Item)object;
        DCValue[] metadata = item.getMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT, DSPACE_DOI_QUALIFIER, null);
        if (metadata.length > 0)
        {
            log.debug("Found {}", metadata[0].value);
            return metadata[0].value;
        }
        else
            throw new IdentifierNotFoundException(object.getTypeText() + " "
                    + object.getID() + " has no DOI");
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("delete {}", dso);

        if (!(dso instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());

        String username = configurationService.getProperty(CFG_USER);
        String password = configurationService.getProperty(CFG_PASSWORD);
        if (null == username || null == password)
            throw new IdentifierException("Unconfigured:  define " + CFG_USER
                    + " and " + CFG_PASSWORD);

        Item item = (Item)dso;

        // delete from EZID
        for (DCValue id : item.getMetadata(MD_SCHEMA_DSPACE, DSPACE_DOI_ELEMENT,
                DSPACE_DOI_QUALIFIER, null))
        {
            EZIDResponse response;
            try {
                EZIDRequest request = requestFactory.getInstance(id.value, username, password);
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
        log.debug("delete {} from {}", identifier, dso);

        throw new UnsupportedOperationException("Not supported yet."); // TODO implement delete(specific)
        // TODO find metadata value == identifier
        // TODO delete from EZID

        // TODO delete from item NOTE!!! can't delete single MD values!
    }

    private String getUser()
            throws IdentifierException
    {
        String user = configurationService.getProperty(CFG_USER);
        if (null != user)
            return user;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_USER);
    }

    private String getPassword()
            throws IdentifierException
    {
        String password = configurationService.getProperty(CFG_PASSWORD);
        if (null != password)
            return password;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_PASSWORD);
    }

    private String getShoulder()
            throws IdentifierException
    {
        String shoulder = configurationService.getProperty(CFG_SHOULDER);
        if (null != shoulder)
            return shoulder;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_SHOULDER);
    }

    /**
     * Map selected DSpace metadata to fields recognized by DataCite.
     */
    static private Map<String, String> crosswalkMetadata(DSpaceObject dso)
    {
        if ((null == dso) || !(dso instanceof Item))
            throw new IllegalArgumentException("Must be an Item");
        Item item = (Item) dso; // TODO generalize to DSO when all DSOs have metadata.

        Map<String, String> mapped = new HashMap<String, String>();
        
        for (Entry<String, String> datum : crosswalk.entrySet())
        {
            DCValue[] values = item.getMetadata(datum.getValue());
            if (null != values)
                for (DCValue value : values)
                    mapped.put(datum.getKey(), value.value);
        }
        return mapped;
    }

    /**
     * @param aCrosswalk the crosswalk to set
     */
    @Required
    public void setCrosswalk(Map<String, String> aCrosswalk)
    {
        crosswalk = aCrosswalk;
    }

    /**
     * @param aRequestFactory the requestFactory to set
     */
    @Required
    public static void setRequestFactory(EZIDRequestFactory aRequestFactory)
    {
        requestFactory = aRequestFactory;
    }
}
