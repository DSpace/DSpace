/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.io.IOException;
import java.lang.reflect.Constructor;
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
 * <p>Different DataCite Members provides different APIs to handle DOIs. This
 * class wraps the DOI handling within the DSpace API. It uses a
 * <link>org.dspace.identifier.doi.RegistrationAgency</link> to perform API
 * calls. It is configurable which <code>RegistrationAgency</code> is used. To
 * do so, change <code>identifier.doi.registrationagency</code> in
 * <code>dspace.cfg</code>. Per default it uses EZID.</p>
 * 
 * <p>Further configuration is dependent to the registration agency. See the
 * particular documentation of the registration Agency
 * (f.e. <link>org.dspace.identifier.doi.EZIDRegistrationAgency</link>).</p>
 *
 * <p>There is also a Map (with the property name "crosswalk") from DataCite
 * metadata field names into DSpace field names, injected by Spring.  Specify
 * the fully-qualified names of all metadata fields to be looked up on a DSpace
 * object and their values set on mapped fully-qualified names in the object's
 * DataCite metadata.</p>
 * 
 * <p>Every String named identifier any method of DOIIdentifierProvider gets can
 * be in one of the following formats:
 * <ul>
 * <li>doi:10.123/456</li>
 * <li>10.123/456</li>
 * <li>http://dx.doi.org/10.123/456</li>
 * </ul>
 * Any identifier a method of this class returns as string is in the following
 * format: doi:10.123/456. Every DOI that is assigned as string to the
 * registrationAgency is in this format. Every DOI we get as string from a
 * registrationAgency should be in the same format.
 * 
 * @author Mark H. Wood
 * @author Pascal-Nicolas Becker (p dot becker at tu hyphen berlin dot de)
 */
public class DOIIdentifierProvider
    extends IdentifierProvider
{
    private static final Logger log = LoggerFactory.getLogger(DOIIdentifierProvider.class);
        
    // Metadata field name elements
    // TODO: move these to MetadataSchema or some such
    public static final String MD_SCHEMA = "dc";
    public static final String DOI_ELEMENT = "identifier";
    public static final String DOI_QUALIFIER = null;
    
    private static final String DOI_SCHEME = DOI.SCHEME;
    
    private RegistrationAgency registrationAgency;
    
    @Required
    @Autowired
    public void setRegistrationAgency(RegistrationAgency ra) {
        this.registrationAgency = ra;
    }
    
    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return DOI.class.isAssignableFrom(identifier);
    }

    @Override
    public boolean supports(String identifier)
    {
        try {
            formatIdentifier(identifier);
        } catch (IdentifierException e) {
            return false;
        }
            return true;
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
        identifier = formatIdentifier(identifier);
        log.debug("formated identifier as {}", identifier);

        if (!(object instanceof Item))
        {
            // TODO throw new IdentifierException("Unsupported object type " + object.getTypeText());
            log.error("Unsupported object type " + object.getTypeText());
            return;
        }

        boolean response = registrationAgency.create(identifier, object);
        if (response == true)
        {
            Item item = (Item)object;
            try {
                item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, identifier);
                item.update();
                context.commit();
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
            log.error("Identifier '{}' not registered -- see logs of used registration agency implementation.", identifier);
        }
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("reserve {}", identifier);
        identifier = formatIdentifier(identifier);
        log.debug("formated identifier as {}", identifier);

        boolean response = registrationAgency.reserve(identifier, dso);
                    
        if (response == true)
        {
            Item item = (Item)dso;
            item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, identifier);
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

        String doi = registrationAgency.mint(dso);
        
        try {
            //ensure format
            doi = formatIdentifier(doi);
        } catch (IdentifierException e) {
            log.error("Got an invalid DOI ({}) from the registration agency: ", doi, e.getMessage());
            throw new RuntimeException("Got an invalid DOI (" + doi + ") from the registration agency.", e);
        }
        
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
        try {
            identifier = formatIdentifier(identifier);
        } catch (IdentifierException e) {
            throw new IdentifierNotFoundException(e.getMessage());
        }
        log.debug("formated identifier as {}", identifier);

        ItemIterator found;
        try {
            found = Item.findByMetadataField(context,
                    MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER,
                    identifier);
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
                response = registrationAgency.delete(id.value);
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
        identifier = formatIdentifier(identifier);
        log.debug("formated identifier as {}", identifier);

        if (!(dso instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());

        Item item = (Item)dso;

        DCValue[] metadata = item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        List<String> remainder = new ArrayList<String>();
        int skipped = 0;
        for (DCValue id : metadata)
        {
            if (!id.value.equals(identifier))
            {
                remainder.add(id.value);
                continue;
            }
            boolean response = false;
            try {
                response = registrationAgency.delete(id.value);
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
     * This method helps to convert a DOI into a URL. It takes DOIs with or
     * without leading DOI scheme (f.e. doi:10.123/456 as well as 10.123/456)
     * and returns it as URL (f.e. http://dx.doi.org/10.123/456).
     * 
     * @param id A DOI that should be returned in external form.
     * @return A String containing a URL to the official DOI resolver.
     * @throws IdentifierException 
     */
    public static String DOIToExternalForm(String id) throws IdentifierException{
        if (id.startsWith("http://dx.doi.org/10.")) {
            return id;
        }
        String doi = formatIdentifier(id);
        return "http://dx.doi.org/" + doi.substring(DOI_SCHEME.length());
    }
    
    /**
     * Format any accepted identifier with DOI scheme.
     * @param identifier Identifier to format, following format are accepted: f.e. 10.123/456, doi:10.123/456, http://dx.doi.org/10.123/456.
     * @return Given Identifier with DOI-Scheme, f.e. doi:10.123/456.
     * @throws IdentifierException 
     */
    public static String formatIdentifier(String identifier) throws IdentifierException {
        if (null == identifier) 
            throw new IdentifierException("Identifier is null.", new NullPointerException());
        if (identifier.isEmpty())
            throw new IdentifierException("Cannot format an empty identifier.");
        if (identifier.startsWith("doi:"))
            return identifier;
        if (identifier.startsWith("10.") && identifier.contains("/"))
            return DOI_SCHEME + identifier;
        if (identifier.startsWith("http://dx.doi.org/10."))
            return DOI_SCHEME + identifier.substring(18);
        throw new IdentifierException(identifier + "does not seem to be a DOI.");
    }
}
