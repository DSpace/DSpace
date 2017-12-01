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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DCValue;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.ItemIterator;
import org.dspace.core.Context;
import org.dspace.identifier.ezid.EZIDRequest;
import org.dspace.identifier.ezid.EZIDRequestFactory;
import org.dspace.identifier.ezid.EZIDResponse;
import org.dspace.identifier.ezid.Transform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Required;

/**
 * Provide service for DOIs through DataCite using the EZID service.
 *
 * <p>Configuration of this class is is in two parts.</p>
 *
 * <p>Installation-specific configuration (credentials and the "shoulder" value
 * which forms a prefix of the site's DOIs) is supplied from property files in
 * [DSpace]/config**.</p>
 *
 * <dl>
 *  <dt>identifier.doi.ezid.shoulder</dt>
 *  <dd>base of the site's DOIs.  Example:  10.5072/FK2</dd>
 *  <dt>identifier.doi.ezid.user</dt>
 *  <dd>EZID username.</dd>
 *  <dt>identifier.doi.ezid.password</dt>
 *  <dd>EZID password.</dd>
 *  <dt>identifier.doi.ezid.publisher</dt>
 *  <dd>A default publisher, for Items not previously published.  EZID requires a publisher.</dd>
 * </dl>
 *
 * <p>Then there are properties injected using Spring:</p>
 * <ul>
 *  <li>There is a Map (with the property name "crosswalk") from EZID
 *  metadata field names into DSpace field names, injected by Spring.  Specify
 *  the fully-qualified names of all metadata fields to be looked up on a DSpace
 *  object and their values set on mapped fully-qualified names in the object's
 *  DataCite metadata.</li>
 *
 *  <li>A second map ("crosswalkTransform") provides Transform instances
 *  mapped from EZID metadata field names.  This allows the crosswalk to rewrite
 *  field values where the form maintained by DSpace is not directly usable in
 *  EZID metadata.</li>
 * </ul>
 *
 * @author mwood
 */
public class EZIDIdentifierProvider
    extends IdentifierProvider
{
    private static final Logger log = LoggerFactory.getLogger(EZIDIdentifierProvider.class);

    // Configuration property names
    static final String CFG_SHOULDER = "identifier.doi.ezid.shoulder";
    static final String CFG_USER = "identifier.doi.ezid.user";
    static final String CFG_PASSWORD = "identifier.doi.ezid.password";
    static final String CFG_PUBLISHER = "identifier.doi.ezid.publisher";

    // DataCite metadata field names
    static final String DATACITE_PUBLISHER = "datacite.publisher";
    static final String DATACITE_PUBLICATION_YEAR = "datacite.publicationyear";

    // DSpace metadata field name elements
    // XXX move these to MetadataSchema or some such
    public static final String MD_SCHEMA = "dc";
    public static final String DOI_ELEMENT = "identifier";
    public static final String DOI_QUALIFIER = null;

    private static final String DOI_SCHEME = "doi:";

    /** Map DataCite metadata into local metadata. */
    private static Map<String, String> crosswalk = new HashMap<String, String>();

    /** Converters to be applied to specific fields. */
    private static Map<String, Transform> transforms = new HashMap<String, Transform>();

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
        if (null == identifier)
        {
            return false;
        }
        else
        {
            return identifier.startsWith(DOI_SCHEME);
        } // XXX more thorough test?
    }

    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("register {}", dso);

        if (!(dso instanceof Item))
        {
            throw new IdentifierException("Unsupported object type " + dso.getTypeText());
        }

        Item item = (Item)dso;
        DCValue[] identifiers = item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        for (DCValue identifier : identifiers)
        {
            if ((null != identifier.value) && (identifier.value.startsWith(DOI_SCHEME)))
            {
                return identifier.value;
            }
        }

        String id = mint(context, item);
        item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, id);
        try {
            item.update();
            context.commit();
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
        try {
            EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                    loadUser(), loadPassword());
            response = request.create(identifier, crosswalkMetadata(object));
        } catch (IdentifierException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return;
        } catch (IOException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return;
        } catch (URISyntaxException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return;
        }

        if (response.isSuccess())
        {
            Item item = (Item)object;
            try {
                item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null,
                        idToDOI(identifier));
                item.update();
                context.commit();
                log.info("registered {}", identifier);
            } catch (SQLException ex) {
                // TODO throw new IdentifierException("New identifier not stored", ex);
                log.error("New identifier not stored", ex);
            } catch (AuthorizeException ex) {
                // TODO throw new IdentifierException("New identifier not stored", ex);
                log.error("New identifier not stored", ex);
            } catch (IdentifierException ex) {
                log.error("New identifier not stored", ex);
            }
        }
        else
        {
            log.error("Identifier '{}' not registered -- EZID returned: {}",
                    identifier, response.getEZIDStatusValue());
        }
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("reserve {}", identifier);

        EZIDResponse response;
        try {
            EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                    loadUser(), loadPassword());
            Map<String, String> metadata = crosswalkMetadata(dso);
            metadata.put("_status", "reserved");
            response = request.create(identifier, metadata);
        } catch (IOException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return;
        } catch (URISyntaxException e) {
            log.error("Identifier '{}' not registered:  {}", identifier, e.getMessage());
            return;
        }

        if (response.isSuccess())
        {
            Item item = (Item)dso;
            item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, idToDOI(identifier));
            try {
                item.update();
                context.commit();
                log.info("reserved {}", identifier);
            } catch (SQLException ex) {
                throw new IdentifierException("New identifier not stored", ex);
            } catch (AuthorizeException ex) {
                throw new IdentifierException("New identifier not stored", ex);
            }
        }
        else
        {
            log.error("Identifier '{}' not registered -- EZID returned: {}",
                    identifier, response.getEZIDStatusValue());
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
            request = requestFactory.getInstance(loadAuthority(), loadUser(), loadPassword());
        } catch (URISyntaxException ex) {
            log.error(ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Send the request
        EZIDResponse response;
        try
        {
            response = request.mint(crosswalkMetadata(dso));
        } catch (IOException ex) {
            log.error("Failed to send EZID request:  {}", ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        } catch (URISyntaxException ex) {
            log.error("Failed to send EZID request:  {}", ex.getMessage());
            throw new IdentifierException("DOI request not sent:  " + ex.getMessage());
        }

        // Good response?
        if (HttpURLConnection.HTTP_CREATED != response.getHttpStatusCode())
            {
                log.error("EZID server responded:  {} {}: {}",
                        new String[] {
                            String.valueOf(response.getHttpStatusCode()),
                            response.getHttpReasonPhrase(),
                            response.getEZIDStatusValue()
                                    });
                throw new IdentifierException("DOI not created:  "
                        + response.getHttpReasonPhrase()
                        + ":  "
                        + response.getEZIDStatusValue());
            }

	// Extract the DOI from the content blob
        if (response.isSuccess())
        {
            String value = response.getEZIDStatusValue();
            int end = value.indexOf('|'); // Following pipe is "shadow ARK"
            if (end < 0)
            {
                end = value.length();
            }
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

        ItemIterator found;
        try {
            found = Item.findByMetadataField(context,
                    MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER,
                    idToDOI(identifier));
        } catch (IdentifierException ex) {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        } catch (AuthorizeException ex) {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        } catch (IOException ex) {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        }
        try {
            if (!found.hasNext())
            {
                throw new IdentifierNotFoundException("No object bound to " + identifier);
            }
            Item found1 = found.next();
            if (found.hasNext())
            {
                log.error("More than one object bound to {}!", identifier);
            }
            log.debug("Resolved to {}", found1);
            return found1;
        } catch (SQLException ex) {
            log.error(ex.getMessage());
            throw new IdentifierNotResolvableException(ex);
        }
    }

    @Override
    public String lookup(Context context, DSpaceObject object)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        log.debug("lookup {}", object);

        if (!(object instanceof Item))
        {
            throw new IllegalArgumentException("Unsupported type " + object.getTypeText());
        }

        Item item = (Item)object;
        DCValue found = null;
        for (DCValue candidate : item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null))
        {
            if (candidate.value.startsWith(DOI_SCHEME))
            {
                found = candidate;
                break;
            }
        }
        if (null != found)
        {
            log.debug("Found {}", found.value);
            return found.value;
        }
        else
        {
            throw new IdentifierNotFoundException(object.getTypeText() + " "
                    + object.getID() + " has no DOI");
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("delete {}", dso);

        if (!(dso instanceof Item))
        {
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());
        }

        Item item = (Item)dso;

        // delete from EZID
        DCValue[] metadata = item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        List<String> remainder = new ArrayList<String>();
        int skipped = 0;
        for (DCValue id : metadata)
        {
            if (!id.value.startsWith(DOI_SCHEME))
            {
                remainder.add(id.value);
                continue;
            }

            EZIDResponse response;
            try {
                EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                        loadUser(), loadPassword());
                response = request.delete(DOIToId(id.value));
            } catch (URISyntaxException e) {
                log.error("Bad URI in metadata value:  {}", e.getMessage());
                remainder.add(id.value);
                skipped++;
                continue;
            } catch (IOException e) {
                log.error("Failed request to EZID:  {}", e.getMessage());
                remainder.add(id.value);
                skipped++;
                continue;
            }
            if (!response.isSuccess())
            {
                log.error("Unable to delete {} from DataCite:  {}", id.value,
                        response.getEZIDStatusValue());
                remainder.add(id.value);
                skipped++;
                continue;
            }
            log.info("Deleted {}", id.value);
        }

        // delete from item
        item.clearMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null,
                remainder.toArray(new String[remainder.size()]));
        try {
            item.update();
            context.commit();
        } catch (SQLException e) {
            log.error("Failed to re-add identifiers:  {}", e.getMessage());
        } catch (AuthorizeException e) {
            log.error("Failed to re-add identifiers:  {}", e.getMessage());
        }

        if (skipped > 0)
        {
            throw new IdentifierException(skipped + " identifiers could not be deleted.");
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("delete {} from {}", identifier, dso);

        if (!(dso instanceof Item))
        {
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());
        }

        Item item = (Item)dso;

        DCValue[] metadata = item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        List<String> remainder = new ArrayList<String>();
        int skipped = 0;
        for (DCValue id : metadata)
        {
            if (!id.value.equals(idToDOI(identifier)))
            {
                remainder.add(id.value);
                continue;
            }

            EZIDResponse response;
            try {
                EZIDRequest request = requestFactory.getInstance(loadAuthority(),
                        loadUser(), loadPassword());
                response = request.delete(DOIToId(id.value));
            } catch (URISyntaxException e) {
                log.error("Bad URI in metadata value {}:  {}", id.value, e.getMessage());
                remainder.add(id.value);
                skipped++;
                continue;
            } catch (IOException e) {
                log.error("Failed request to EZID:  {}", e.getMessage());
                remainder.add(id.value);
                skipped++;
                continue;
            }

            if (!response.isSuccess())
            {
                log.error("Unable to delete {} from DataCite:  {}", id.value,
                        response.getEZIDStatusValue());
                remainder.add(id.value);
                skipped++;
                continue;
            }
            log.info("Deleted {}", id.value);
        }

        // delete from item
        item.clearMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null,
                remainder.toArray(new String[remainder.size()]));
        try {
            item.update();
            context.commit();
        } catch (SQLException e) {
            log.error("Failed to re-add identifiers:  {}", e.getMessage());
        } catch (AuthorizeException e) {
            log.error("Failed to re-add identifiers:  {}", e.getMessage());
        }

        if (skipped > 0)
        {
            throw new IdentifierException(identifier + " could not be deleted.");
        }
    }

    /**
     * Format a naked identifier as a DOI with our configured authority prefix.
     * 
     * @throws IdentifierException if authority prefix is not configured.
     */
    String idToDOI(String id)
            throws IdentifierException
    {
        return "doi:" + loadAuthority() + id;
    }

    /**
     * Remove scheme and our configured authority prefix from a doi: URI string.
     * @return naked local identifier.
     * @throws IdentifierException if authority prefix is not configured.
     */
    String DOIToId(String DOI)
            throws IdentifierException
    {
        String prefix = "doi:" + loadAuthority();
        if (DOI.startsWith(prefix))
        {
            return DOI.substring(prefix.length());
        }
        else
        {
            return DOI;
        }
    }

    /**
     * Get configured value of EZID username.
     * @throws IdentifierException
     */
    private String loadUser()
            throws IdentifierException
    {
        String user = configurationService.getProperty(CFG_USER);
        if (null != user)
        {
            return user;
        }
        else
        {
            throw new IdentifierException("Unconfigured:  define " + CFG_USER);
        }
    }

    /**
     * Get configured value of EZID password.
     * @throws IdentifierException
     */
    private String loadPassword()
            throws IdentifierException
    {
        String password = configurationService.getProperty(CFG_PASSWORD);
        if (null != password)
        {
            return password;
        }
        else
        {
            throw new IdentifierException("Unconfigured:  define " + CFG_PASSWORD);
        }
    }

    /**
     * Get configured value of EZID "shoulder".
     * @throws IdentifierException
     */
    private String loadAuthority()
            throws IdentifierException
    {
        String shoulder = configurationService.getProperty(CFG_SHOULDER);
        if (null != shoulder)
        {
            return shoulder;
        }
        else
        {
            throw new IdentifierException("Unconfigured:  define " + CFG_SHOULDER);
        }
    }

    /**
     * Map selected DSpace metadata to fields recognized by DataCite.
     */
    private Map<String, String> crosswalkMetadata(DSpaceObject dso)
    {
        if ((null == dso) || !(dso instanceof Item))
        {
            throw new IllegalArgumentException("Must be an Item");
        }
        Item item = (Item) dso; // TODO generalize to DSO when all DSOs have metadata.

        Map<String, String> mapped = new HashMap<String, String>();

        for (Entry<String, String> datum : crosswalk.entrySet())
        {
            DCValue[] values = item.getMetadata(datum.getValue());
            if (null != values)
            {
                for (DCValue value : values)
                {
                    String key = datum.getKey();
                    String mappedValue;
                    Transform xfrm = transforms.get(key);
                    if (null != xfrm)
                    {
                        try {
                            mappedValue = xfrm.transform(value.value);
                        } catch (Exception ex) {
                            log.error("Unable to transform '{}' from {} to {}:  {}",
                                    new String[] {
                                        value.value,
                                        value.toString(),
                                        key,
                                        ex.getMessage()
                                    });
                            continue;
                        }
                    }
                    else
                    {
                        mappedValue = value.value;
                    }
                    mapped.put(key, mappedValue);
                }
            }
        }

        // Supply a default publisher, if the Item has none.
        if (!mapped.containsKey(DATACITE_PUBLISHER))
        {
            String publisher = configurationService.getPropertyAsType(CFG_PUBLISHER, "unknown");
            log.info("Supplying default publisher:  {}", publisher);
            mapped.put(DATACITE_PUBLISHER, publisher);
        }

        // Supply current year as year of publication, if the Item has none.
        if (!mapped.containsKey(DATACITE_PUBLICATION_YEAR))
        {
            String year = String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
            log.info("Supplying default publication year:  {}", year);
            mapped.put(DATACITE_PUBLICATION_YEAR, year);
        }

        // TODO find a way to get a current direct URL to the object and set _target
        // mapped.put("_target", url);

        return mapped;
    }

    @Required
    public void setCrosswalk(Map<String, String> aCrosswalk)
    {
        crosswalk = aCrosswalk;
    }

    public void setCrosswalkTransform(Map<String, Transform> transformMap)
    {
        transforms = transformMap;
    }

    @Required
    public static void setRequestFactory(EZIDRequestFactory aRequestFactory)
    {
        requestFactory = aRequestFactory;
    }
}
