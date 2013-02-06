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
import java.util.List;
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
 * Provide service for DOIs using DataCite.
 * 
 * <p>This class handles reservation, registration and deletion of DOIs using
 * the direct API from {@link <a href="http://www.datacite.org">DataCite</a>}.
 * Please pay attention that some members of DataCite offers special services
 * and wants their customers to use special APIs. If you are unsure ask your
 * registration agency.</p>
 * 
 * <p>Any identifier a method of this class returns as a string is in the
 * following format: doi:10.123/456.</p>
 * 
 * @author Pascal-Nicolas Becker
 */
public class DOIDataCiteIdentifierProvider
    extends IdentifierProvider
{
    private static final Logger log = LoggerFactory.getLogger(DOIDataCiteIdentifierProvider.class);
        
    // Metadata field name elements
    // TODO: move these to MetadataSchema or some such?
    public static final String MD_SCHEMA = "dc";
    public static final String DOI_ELEMENT = "identifier";
    public static final String DOI_QUALIFIER = null;
    
    private static final String DOI_SCHEME = DOI.SCHEME;
    
    /**
     * This identifier provider supports identifiers of type
     * {@link org.dspace.identifier.DOI}.
     * @param identifier to check if it will be supported by this provider.
     * @return 
     */
    @Override
    public boolean supports(Class<? extends Identifier> identifier)
    {
        return DOI.class.isAssignableFrom(identifier);
    }
    
    /**
     * This identifier provider supports identifiers in the following format:
     * <ul>
     *  <li>doi:10.123/456</li>
     *  <li>10.123/456</li>
     *  <li>http://dx.doi.org/10.123/456</li>
     * </ul>
     * @param identifier to check if it is in a supported format.
     * @return 
     */
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
        String doi = mint(context, dso);
        if (null != doi)
            this.register(context, dso, doi);
        return doi;
    }

    @Override
    public void register(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        String doi = formatIdentifier(identifier);
        if (!isDOIReservedForObject(dso, doi))
        {
            if (isDOIReserved(doi))
                throw new IllegalStateException("Trying to register a DOI that is reserved for another object.");
            if(!reserveDOI(dso, doi))
                throw new IdentifierException("It was impossible to reserve the DOI "
                        + doi + ". Take a look into the logs for further details.");
        }
        
        try
        {
            if (registerDOI(dso, doi))
                saveDOIToObject(context, dso, doi);
            else
                throw new IdentifierException("It was impossible to register the DOI "
                        + doi + ". Take a look into the logs for further details.");
        }
        //FIXME add better exception handling
        catch (Exception e)
        {
            throw new IdentifierException(e);
        }
    }

    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        String doi = formatIdentifier(identifier);
        if (!isDOIReservedForObject(dso, doi))
        {
            if (isDOIReserved(doi))
                throw new IllegalStateException("Trying to register a DOI that is reserved for another object.");
            if(!reserveDOI(dso, doi))
                throw new IdentifierException("It was impossible to reserve the DOI "
                        + doi + ". Take a look into the logs for further details.");
        }
    }
    
    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        //TODO
        return null;
    }

    @Override
    public DSpaceObject resolve(Context context, String identifier,
            String... attributes)
            throws IdentifierNotFoundException, IdentifierNotResolvableException
    {
        // TODO check
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
        // TODO: check
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
        String[] DOIs = getDOIsByObject(dso);
        for (String doi : DOIs) {
            this.delete(context, dso, doi);
        }
    }

    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException
    {
        log.debug("delete {} from {}", identifier, dso);
        String doi = formatIdentifier(identifier);
        log.debug("formated identifier as {}", doi);
        
        if (!deleteDOI(dso, doi))
            throw new IdentifierException("Unable to delete DOI " + doi +
                    ". Was it already registered? Take a look into the logs for further details.");
        
        try
        {
            deleteDOIFromObject(context, dso, doi);
        }
        // TODO: better Exception handling.
        catch (Exception e)
        {
            throw new IdentifierException(e);
        }
        
        log.info("Deleted {}", doi);
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
    public static String formatIdentifier(String identifier) throws IdentifierException
    {
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
    
    protected boolean reserveDOI(DSpaceObject dso, String doi)
    {
        return false;
    }
    
    protected boolean isDOIReserved(String doi)
    {
        return true;
    }
    
    protected boolean isDOIReservedForObject(DSpaceObject dso, String doi)
    {
        return false;
    }
    
    protected boolean registerDOI(DSpaceObject dso, String doi)
    {
        return false;
    }
    
    protected boolean deleteDOI(DSpaceObject dso, String doi)
    {
        return false;
    }
    
    protected String[] getDOIsByObject(DSpaceObject dso)
            throws IdentifierException
    {
        // TODO
        return (new String[0]);
    }
    
    protected void saveDOIToObject(Context context, DSpaceObject dso, String doi)
            throws SQLException, AuthorizeException
    {
        // FIXME
        if (!(dso instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());
        
        Item item = (Item) dso;
        item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, doi);
        try
        {
            item.update();
            context.commit();
            log.info("reserved {}", doi);
        } catch (SQLException ex) {
            throw ex;
        } catch (AuthorizeException ex) {
            throw ex;
        }
    }
    
    protected void deleteDOIFromObject(Context context, DSpaceObject dso, String doi)
            throws AuthorizeException, SQLException
    {
        // FIXME
        if (!(dso instanceof Item))
            throw new IllegalArgumentException("Unsupported type " + dso.getTypeText());

        Item item = (Item)dso;

        DCValue[] metadata = item.getMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        List<String> remainder = new ArrayList<String>();

        for (DCValue id : metadata)
        {
            if (!id.value.equals(doi))
                remainder.add(id.value);
        }

        item.clearMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        item.addMetadata(MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null,
                remainder.toArray(new String[remainder.size()]));
        try {
            item.update();
            context.commit();
        } catch (SQLException e) {
            throw e;
        } catch (AuthorizeException e) {
            throw e;
        }
    }
}
