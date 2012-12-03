/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
import org.dspace.identifier.doi.RegistrationAgency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
 * @author Mark H. Wood
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class DOIIdentifierProvider
    extends IdentifierProvider
{
    private static final Logger log = LoggerFactory.getLogger(DOIIdentifierProvider.class);

    // Same as in EZIDRegistrationAgency
    // TODO: Is it realy neccessary here?
    static final String CFG_SHOULDER = "identifier.doi.ezid.shoulder";
    
    // Metadata field name elements
    // XXX move these to MetadataSchema or some such
    public static final String MD_SCHEMA = "dc";
    public static final String DOI_ELEMENT = "identifier";
    public static final String DOI_QUALIFIER = null;

    private static final String DOI_SCHEME = "doi:";
    
    private static RegistrationAgency registrationAgency;
    
    /** Map DataCite metadata into local metadata. */
    private static Map<String, String> crosswalk = new HashMap<String, String>();

    @Autowired
    @Required
    public void setRegistrationAgency(RegistrationAgency registrationAgency) {
        this.registrationAgency = registrationAgency;
    }
    
    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return DOI.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier)
    {
        if (null == identifier)
            return false;
        else
            return identifier.startsWith(DOI_SCHEME); // XXX more thorough test?
    }

    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("register {}", dso);

        if (!(dso instanceof Item))
            throw new IdentifierException("Unsupported object type " + dso.getTypeText());

        Item item = (Item)dso;
        DCValue[] identifiers = item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        for (DCValue identifier : identifiers)
            if ((null != identifier.value) && (identifier.value.startsWith(DOI_SCHEME)))
                return identifier.value;

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
    public void register(Context context, DSpaceObject object, String identifier) throws IdentifierException
    {
        log.debug("register {} as {}", object, identifier);

        if (!(object instanceof Item))
        {
            // TODO throw new IdentifierException("Unsupported object type " + object.getTypeText());
            log.error("Unsupported object type " + object.getTypeText());
            return;
        }

        boolean response = registrationAgency.create(identifier, crosswalkMetadata(object));
        if (response == true)
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
                throw new IdentifierException(ex);
            }
        }
        else
        {
            log.error("Identifier '{}' not registered -- see logs of used registration agency implementation.", identifier);
        }
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("reserve {}", identifier);
        boolean response = registrationAgency.reserve(identifier, crosswalkMetadata(dso));
                    
        if (response == true)
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
            log.error("Identifier '{}' not registered -- See log of registration agency class.", identifier);
        }
    }

    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        log.debug("mint for {}", dso);

        String doi = registrationAgency.mint(crosswalkMetadata(dso));
        
        if (doi == null || doi.isEmpty()) {
            log.error("Error while minting DOI: Registration Agency did not return a DOI.");
            throw new IdentifierException("Error while minting DOI: Registration Agency did not return a DOI.");
        }
        return doi;
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
                    // TODO: idToDOI adds the DOI-Prefix (also called Authority or Shoulder). Sure that it is not already part of identifier?
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
                throw new IdentifierNotFoundException("No object bound to " + identifier);
            Item found1 = found.next();
            if (found.hasNext())
                log.error("More than one object bound to {}!", identifier);
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
            throw new IllegalArgumentException("Unsupported type " + object.getTypeText());

        Item item = (Item)object;
        DCValue found = null;
        for (DCValue candidate : item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null))
            if (candidate.value.startsWith(DOI_SCHEME))
            {
                found = candidate;
                break;
            }
        if (null != found)
        {
            log.debug("Found {}", found.value);
            return found.value;
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
            
            boolean response = false;
            try {
                response = registrationAgency.delete(DOIToId(id.value));
            } catch (IdentifierException e) {
                log.error("Failed to delete DOI -- see logs of used registration agency implementation.");
                log.error("Got following error message: {}", e.getMessage());
                remainder.add(id.value);
                skipped++;
                continue;
            }
            if (!response)
            {
                log.error("Unable to delete {} from DataCite -- see logs of used registration agency implementation.");
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
            throw new IdentifierException(skipped + " identifiers could not be deleted.");
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("delete {} from {}", identifier, dso);

        if (!(dso instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());

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
            boolean response = false;
            try {
                response = registrationAgency.delete(DOIToId(id.value));
            } catch (IdentifierException e) {
                log.error("Failed to delete DOI -- see logs of used registration agency implementation.");
                log.error("Got following error message: {}", e.getMessage());
                remainder.add(id.value);
                skipped++;
                continue;
            }
            if (!response)
            {
                log.error("Unable to delete {} from DataCite -- see logs of used registration agency implementation.");
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
            throw new IdentifierException(identifier + " could not be deleted.");
    }

    /**
     * Format a naked identifier as a DOI with our configured authority prefix.
     * 
     * @throws IdentifierException if authority prefix is not configured.
     */
    // TODO: idToDOI adds DOI-Prefix. Consider refactoring so that a doi id string always cotains the prefix.
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
    // TODO: DOItoID removes DOI-Prefix. Consider refactoring so that a doi id string always cotains the prefix.
    String DOIToId(String DOI)
            throws IdentifierException
    {
        String prefix = "doi:" + loadAuthority();
        if (DOI.startsWith(prefix))
            return DOI.substring(prefix.length());
        else
            return DOI;
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

        // TODO find a way to get a current direct URL to the object and set _target
        // mapped.put("_target", url);

        return mapped;
    }

    @Required
    public void setCrosswalk(Map<String, String> aCrosswalk)
    {
        crosswalk = aCrosswalk;
    }

    /**
     * Get configured value of EZID "shoulder".
     * @throws IdentifierException 
     */
    // Same as in EZIDRegistrationAgency
    // TODO: refactor as far as idToDOI() and DOItoID() gets refactored (see comments there).
    private String loadAuthority()
            throws IdentifierException
    {
        String shoulder = configurationService.getProperty(CFG_SHOULDER);
        if (null != shoulder)
            return shoulder;
        else
            throw new IdentifierException("Unconfigured:  define " + CFG_SHOULDER);
    }
}
