/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.VersionHistory;
import org.dspace.versioning.service.VersionHistoryService;
import org.dspace.versioning.service.VersioningService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Required;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Marsa Haoua
 * @author Pascal-Nicolas Becker (dspace at pascal dash becker dot de)
 */
public class VersionedDOIIdentifierProvider extends DOIIdentifierProvider
{
 /** log4j category */
    private static Logger log = Logger.getLogger(VersionedDOIIdentifierProvider.class);
    
    protected DOIConnector connector;

    static final char DOT = '.';
    protected static final String pattern = "\\d+\\" + String.valueOf(DOT) +"\\d+";
    
    @Autowired(required = true)
    protected VersioningService versioningService;
    @Autowired(required = true)
    protected VersionHistoryService versionHistoryService;
    
    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException
    {
        if (!(dso instanceof Item))
        {
            throw new IdentifierException("Currently only Items are supported for DOIs.");
        }
        Item item = (Item) dso;

        VersionHistory history = null;
        try {
            history = versionHistoryService.findByItem(context, item);
        } catch (SQLException ex) {
            throw new RuntimeException("A problem occured while accessing the database.", ex);
        }

        String doi = null;
        try
        {
            doi = getDOIByObject(context, dso);
            if (doi != null)
            {
                return doi;
            }
        }
        catch (SQLException ex)
        {
            log.error("Error while attemping to retrieve information about a DOI for " 
                    + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso)
                    + " with ID " + dso.getID() + ".", ex);
            throw new RuntimeException("Error while attempting to retrieve "
                    + "information about a DOI for "
                    + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso)
                    + " with ID " + dso.getID() + ".", ex);
        }
        
        // check whether we have a DOI in the metadata and if we have to remove it
        String metadataDOI = getDOIOutOfObject(dso);
        if (metadataDOI != null)
        {
            // check whether doi and version number matches
            String bareDOI = getBareDOI(metadataDOI);
            int versionNumber;
            try {
                versionNumber = versionHistoryService.getVersion(context, history, item).getVersionNumber();
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
            String versionedDOI = bareDOI;
            if (versionNumber > 1)
            {
                versionedDOI = bareDOI
                        .concat(String.valueOf(DOT))
                        .concat(String.valueOf(versionNumber));
            }
            if (!metadataDOI.equalsIgnoreCase(versionedDOI))
            {
                log.debug("Will remove DOI " + metadataDOI 
                        + " from item metadata, as it should become " + versionedDOI + ".");
                // remove old versioned DOIs
                try {
                    removePreviousVersionDOIsOutOfObject(context, item, metadataDOI);
                } catch (AuthorizeException ex) {
                    throw new RuntimeException("Trying to remove an old DOI from a versioned item, but wasn't authorized to.", ex);
                }
            } else {
                log.debug("DOI " + doi + " matches version number " + versionNumber + ".");
                // ensure DOI exists in our database as well and return.
                // this also checks that the doi is not assigned to another dso already.
                try {
                    loadOrCreateDOI(context, dso, versionedDOI);
                } catch (SQLException ex) {
                    log.error("A problem with the database connection occurd while processing DOI " + versionedDOI + ".", ex);
                    throw new RuntimeException("A problem with the database connection occured.", ex);
                }
                return versionedDOI;
            }
        }
        
        try{
            if(history != null)
            {
                // versioning is currently supported for items only
                // if we have a history, we have a item
                doi = makeIdentifierBasedOnHistory(context, dso, history);
            } else {
                doi = loadOrCreateDOI(context, dso, null).getDoi();
            }
        } catch(SQLException ex) {
            log.error("SQLException while creating a new DOI: ", ex);
            throw new IdentifierException(ex);
        } catch (AuthorizeException ex) {
            log.error("AuthorizationException while creating a new DOI: ", ex);
            throw new IdentifierException(ex);
        }
        return doi;
    }
    
    @Override
    public void register(Context context, DSpaceObject dso, String identifier) 
            throws IdentifierException
    {
        if (!(dso instanceof Item))
        {
            throw new IdentifierException("Currently only Items are supported for DOIs.");
        }
        Item item = (Item) dso;
        
        if (StringUtils.isEmpty(identifier))
        {
            identifier = mint(context, dso);
        }
        String doiIdentifier = doiService.formatIdentifier(identifier);
        
        DOI doi = null;

        // search DOI in our db
        try
        {
            doi = loadOrCreateDOI(context, dso, doiIdentifier);
        } catch (SQLException ex) {
            log.error("Error in databse connection: " + ex.getMessage(), ex);
            throw new RuntimeException("Error in database conncetion.", ex);
        }

        if (DELETED.equals(doi.getStatus()) ||
                TO_BE_DELETED.equals(doi.getStatus()))
        {
            throw new DOIIdentifierException("You tried to register a DOI that "
                    + "is marked as DELETED.", DOIIdentifierException.DOI_IS_DELETED);
        }

        // Check status of DOI
        if (IS_REGISTERED.equals(doi.getStatus()))
        {
            return;
        }
        
        String metadataDOI = getDOIOutOfObject(dso);
        if (!StringUtils.isEmpty(metadataDOI)
                && !metadataDOI.equalsIgnoreCase(doiIdentifier))
        {
            // remove doi of older version from the metadata
                try {
                    removePreviousVersionDOIsOutOfObject(context, item, metadataDOI);
                } catch (AuthorizeException ex) {
                    throw new RuntimeException("Trying to remove an old DOI from a versioned item, but wasn't authorized to.", ex);
                }
        }
        
        // change status of DOI
        doi.setStatus(TO_BE_REGISTERED);
        try {
            doiService.update(context, doi);
        }
        catch (SQLException ex)
        {
            log.warn("SQLException while changing status of DOI {} to be registered.", ex);
            throw new RuntimeException(ex);
        }
    }
    
    protected String getBareDOI(String identifier)
            throws DOIIdentifierException
    {
        doiService.formatIdentifier(identifier);
        String doiPrefix = DOI.SCHEME.concat(getPrefix())
                                     .concat(String.valueOf(SLASH))
                                     .concat(getNamespaceSeparator());
        String doiPostfix = identifier.substring(doiPrefix.length());
        if (doiPostfix.matches(pattern) && doiPostfix.lastIndexOf(DOT) != -1)
        {
            return doiPrefix.concat(doiPostfix.substring(0, doiPostfix.lastIndexOf(DOT)));
        }
        // if the pattern does not match, we are already working on a bare handle.
        return identifier;
    }
    
    protected String getDOIPostfix(String identifier) 
            throws DOIIdentifierException{
    
        String doiPrefix = DOI.SCHEME.concat(getPrefix()).concat(String.valueOf(SLASH)).concat(getNamespaceSeparator());
        String doiPostfix = null;
        if(null != identifier){
            doiPostfix = identifier.substring(doiPrefix.length());
        }
        return doiPostfix;
    }
    
    // Should never return null!
     protected String makeIdentifierBasedOnHistory(Context context, DSpaceObject dso, VersionHistory history)
             throws AuthorizeException, SQLException, DOIIdentifierException
    {
        // Mint foreach new version an identifier like: 12345/100.versionNumber
        // use the bare handle (g.e. 12345/100) for the first version.

        // currently versioning is supported for items only
        if (!(dso instanceof Item))
        {
            throw new IllegalArgumentException("Cannot create versioned handle for objects other then item: Currently versioning supports items only.");
        }
        Item item = (Item)dso;
        Version version = versionHistoryService.getVersion(context, history, item);

        String previousVersionDOI = null;
        for (Version v : versioningService.getVersionsByHistory(context, history))
        {
            previousVersionDOI = getDOIByObject(context, v.getItem());
            if (null != previousVersionDOI)
            {
                break;
            }
        }

        if (previousVersionDOI == null)
        {
            // We need to generate a new DOI.
            DOI doi = doiService.create(context);
            
            // as we reuse the DOI ID, we do not have to check whether the DOI exists already.
            String identifier = this.getPrefix() + "/" + this.getNamespaceSeparator() +
                    doi.getID().toString();

            if (version.getVersionNumber() > 1)
            {
                identifier.concat(String.valueOf(DOT).concat(String.valueOf(version.getVersionNumber())));
            }

            doi.setDoi(identifier);
            doi.setDSpaceObject(dso);
            doi.setStatus(null);
            doiService.update(context, doi);
            return doi.getDoi();
        }
        assert(previousVersionDOI != null);

        String identifier = getBareDOI(previousVersionDOI);

        if (version.getVersionNumber() > 1)
        {
            identifier = identifier.concat(String.valueOf(DOT)).concat(String.valueOf(versionHistoryService.getVersion(context, history, item).getVersionNumber()));
        }

        loadOrCreateDOI(context, dso, identifier);
        return identifier;
    }
    
    void removePreviousVersionDOIsOutOfObject(Context c, Item item, String oldDoi)
            throws IdentifierException, AuthorizeException
    {
        if (StringUtils.isEmpty(oldDoi))
        {
            throw new IllegalArgumentException("Old DOI must be neither empty nor null!");
        }
        
        String bareDoi = getBareDOI(doiService.formatIdentifier(oldDoi));
        String bareDoiRef = doiService.DOIToExternalForm(bareDoi);        
        
        List<MetadataValue> identifiers = itemService.getMetadata(item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, Item.ANY);
        // We have to remove all DOIs referencing previous versions. To do that,
        // we store all identifiers we do not know in an array list, clear 
        // dc.identifier.uri and add the safed identifiers.
        // The list of identifiers to safe won't get larger then the number of
        // existing identifiers.
        ArrayList<String> newIdentifiers = new ArrayList<String>(identifiers.size());
        boolean changed = false;
        for (MetadataValue identifier : identifiers)
        {
            if (!StringUtils.startsWithIgnoreCase(identifier.getValue(), bareDoiRef))
            {
                newIdentifiers.add(identifier.getValue());
            } else {
                changed = true;
            }
        }
        // reset the metadata if neccessary.
        if (changed)
        {
            try
            {
                itemService.clearMetadata(c, item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, Item.ANY);
                itemService.addMetadata(c, item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null, newIdentifiers);
                itemService.update(c, item);
            } catch (SQLException ex) {
                throw new RuntimeException("A problem with the database connection occured.", ex);
            }
        }
    }

    @Required
    public void setDOIConnector(DOIConnector connector)
    {
        super.setDOIConnector(connector);
        this.connector = connector;
    }
    
    @Required
    public void setConfigurationService(ConfigurationService configurationService) {
        super.setConfigurationService(configurationService);
        this.configurationService = configurationService;
    }

}
