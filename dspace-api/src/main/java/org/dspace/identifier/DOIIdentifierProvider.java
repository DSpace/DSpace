/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.identifier;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.logic.Filter;
import org.dspace.content.logic.LogicalStatementException;
import org.dspace.content.logic.TrueFilter;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.identifier.doi.DOIConnector;
import org.dspace.identifier.doi.DOIIdentifierException;
import org.dspace.identifier.doi.DOIIdentifierNotApplicableException;
import org.dspace.identifier.service.DOIService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Provide service for DOIs using DataCite.
 *
 * <p>This class handles reservation, registration and deletion of DOIs using
 * the direct API from <a href="http://www.datacite.org">DataCite</a>.
 * Please pay attention that some members of DataCite offer special services
 * and want their customers to use special APIs. If you are unsure ask your
 * registration agency.</p>
 *
 * <p>Any identifier a method of this class returns is a string in the following format: doi:10.123/456.</p>
 *
 * @author Pascal-Nicolas Becker
 * @author Kim Shepherd
 */
public class DOIIdentifierProvider extends FilteredIdentifierProvider {
    private static final Logger log = LoggerFactory.getLogger(DOIIdentifierProvider.class);

    /**
     * A DOIConnector connects the DOIIdentifierProvider to the API of the DOI
     * registration agency needed to register DOIs. To register DOIs we have to
     * care about two APIs: the <link>IdentifierProvider</link> API of DSpace
     * and the API of the DOI registration agency. The DOIIdentifierProvider
     * manages the DOI database table, generates new DOIs, stores them as
     * metadata in DSpace items and so on. To register DOIs at DOI registration
     * agencies it uses a DOIConnector. A DOI connector has to register and
     * reserve DOIs using the API of the DOI registration agency. If requested
     * by the registration agency it has to convert and send metadata of the
     * DSpace items.
     */
    private DOIConnector connector;

    static final String CFG_PREFIX = "identifier.doi.prefix";
    static final String CFG_NAMESPACE_SEPARATOR = "identifier.doi.namespaceseparator";
    static final char SLASH = '/';

    // Metadata field name elements
    // TODO: move these to MetadataSchema or some such?
    public static final String MD_SCHEMA = "dc";
    public static final String DOI_ELEMENT = "identifier";
    public static final String DOI_QUALIFIER = "uri";
    // The DOI is queued for registered with the service provider
    public static final Integer TO_BE_REGISTERED = 1;
    // The DOI is queued for reservation with the service provider
    public static final Integer TO_BE_RESERVED = 2;
    // The DOI has been registered online
    public static final Integer IS_REGISTERED = 3;
    // The DOI has been reserved online
    public static final Integer IS_RESERVED = 4;
    // The DOI is reserved and requires an updated metadata record to be sent to the service provider
    public static final Integer UPDATE_RESERVED = 5;
    // The DOI is registered and requires an updated metadata record to be sent to the service provider
    public static final Integer UPDATE_REGISTERED = 6;
    // The DOI metadata record should be updated before performing online registration
    public static final Integer UPDATE_BEFORE_REGISTRATION = 7;
    // The DOI will be deleted locally and marked as deleted in the DOI service provider
    public static final Integer TO_BE_DELETED = 8;
    // The DOI has been deleted and is no longer associated with an item
    public static final Integer DELETED = 9;
    // The DOI is created in the database and is waiting for either successful filter check on item install or
    // manual intervention by an administrator to proceed to reservation or registration
    public static final Integer PENDING = 10;
    // The DOI is created in the database, but no more context is known
    public static final Integer MINTED = 11;

    public static final String[] statusText = {
        "UNKNOWN",                      // 0
        "TO_BE_REGISTERED",             // 1
        "TO_BE_RESERVED",               // 2
        "IS_REGISTERED",                // 3
        "IS_RESERVED",                  // 4
        "UPDATE_RESERVED",              // 5
        "UPDATE_REGISTERED",            // 6
        "UPDATE_BEFORE_REGISTRATION",   // 7
        "TO_BE_DELETED",                // 8
        "DELETED",                      // 9
        "PENDING",                      // 10
        "MINTED",                       // 11
    };

    @Autowired(required = true)
    protected DOIService doiService;
    @Autowired(required = true)
    protected ContentServiceFactory contentServiceFactory;
    @Autowired(required = true)
    protected ItemService itemService;

    /**
     * Empty / default constructor for Spring
     */
    protected DOIIdentifierProvider() {
    }

    /**
     * Prefix of DOI namespace. Set in dspace.cfg.
     */
    private String PREFIX;

    /**
     * Part of DOI to separate several applications that generate DOIs.
     * E.g. it could be 'dspace/' if DOIs generated by DSpace should have the form
     * prefix/dspace/uniqueString. Set it to the empty String if DSpace must
     * generate DOIs directly after the DOI Prefix. Set in dspace.cfg.
     */
    private String NAMESPACE_SEPARATOR;

    /**
     * Get DOI prefix from configuration
     * @return a String containing the DOI prefix
     */
    protected String getPrefix() {
        if (null == this.PREFIX) {
            this.PREFIX = this.configurationService.getProperty(CFG_PREFIX);
            if (null == this.PREFIX) {
                log.warn("Cannot find DOI prefix in configuration!");
                throw new RuntimeException("Unable to load DOI prefix from "
                                               + "configuration. Cannot find property " +
                                               CFG_PREFIX + ".");
            }
        }
        return this.PREFIX;
    }

    /**
     * Get namespace separator from configuration
     * @return a String containing the namespace separator
     */
    protected String getNamespaceSeparator() {
        if (null == this.NAMESPACE_SEPARATOR) {
            this.NAMESPACE_SEPARATOR = this.configurationService.getProperty(CFG_NAMESPACE_SEPARATOR);
            if (null == this.NAMESPACE_SEPARATOR) {
                this.NAMESPACE_SEPARATOR = "";
            }
        }
        return this.NAMESPACE_SEPARATOR;
    }

    /**
     * Set the DOI connector, which is the component that commuincates with the remote registration service
     * (eg. DataCite, EZID, Crossref)
     * Spring will use this setter to set the DOI connector from the configured property in identifier-services.xml
     *
     * @param connector a DOIConnector
     */
    @Autowired(required = true)
    public void setDOIConnector(DOIConnector connector) {
        this.connector = connector;
    }

    /**
     * This identifier provider supports identifiers of type
     * {@link org.dspace.identifier.DOI}.
     *
     * @param identifier to check if it will be supported by this provider.
     * @return boolean
     */
    @Override
    public boolean supports(Class<? extends Identifier> identifier) {
        return DOI.class.isAssignableFrom(identifier);
    }

    /**
     * This identifier provider supports identifiers in the following format:
     * <ul>
     * <li>doi:10.123/456</li>
     * <li>10.123/456</li>
     * <li>http://dx.doi.org/10.123/456</li>
     * </ul>
     *
     * @param identifier to check if it is in a supported format.
     * @return boolean
     */
    @Override
    public boolean supports(String identifier) {
        try {
            doiService.formatIdentifier(identifier);
        } catch (IdentifierException | IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    /**
     * Register a new identifier for a given DSpaceObject, never skipping or ignoring any configured filter
     * @param context    - DSpace context
     * @param dso        - DSpaceObject to use for identifier registration
     * @return identifier
     * @throws IdentifierException
     */
    @Override
    public String register(Context context, DSpaceObject dso)
            throws IdentifierException {
        return register(context, dso, this.filter);
    }

    /**
     * Register a specified DOI for a given DSpaceObject, never skipping or ignoring any configured filter
     * @param context    - DSpace context
     * @param dso        - DSpaceObject identified by the new DOI
     * @param identifier - String containing the identifier to register
     * @throws IdentifierException
     */
    @Override
    public void register(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException {
        register(context, dso, identifier, this.filter);
    }

    /**
     * Register a new DOI for a given DSpaceObject
     * @param context    - DSpace context
     * @param dso        - DSpaceObject identified by the new DOI
     * @param filter     - Logical item filter to determine whether this identifier should be registered
     * @throws IdentifierException
     */
    @Override
    public String register(Context context, DSpaceObject dso, Filter filter)
        throws IdentifierException {
        if (!(dso instanceof Item)) {
            // DOI are currently assigned only to Item
            return null;
        }

        String doi = mint(context, dso, filter);

        // register tries to reserve doi if it's not already.
        // So we don't have to reserve it here.
        register(context, dso, doi, filter);
        return doi;
    }

    /**
     * Register a specified DOI for a given DSpaceObject
     * @param context    - DSpace context
     * @param dso        - DSpaceObject identified by the new DOI
     * @param identifier - String containing the DOI to register
     * @param filter     - Logical item filter to determine whether this identifier should be registered
     * @throws IdentifierException
     */
    @Override
    public void register(Context context, DSpaceObject dso, String identifier, Filter filter)
        throws IdentifierException {
        if (!(dso instanceof Item)) {
            // DOI are currently assigned only to Item
            return;
        }
        String doi = doiService.formatIdentifier(identifier);
        DOI doiRow = null;

        // search DOI in our db
        try {
            doiRow = loadOrCreateDOI(context, dso, doi, filter);
        } catch (SQLException ex) {
            log.error("Error in databse connection: " + ex.getMessage());
            throw new RuntimeException("Error in database conncetion.", ex);
        }

        if (DELETED.equals(doiRow.getStatus()) ||
            TO_BE_DELETED.equals(doiRow.getStatus())) {
            throw new DOIIdentifierException("You tried to register a DOI that "
                + "is marked as DELETED.", DOIIdentifierException.DOI_IS_DELETED);
        }

        if (IS_REGISTERED.equals(doiRow.getStatus())) {
            return;
        }

        // change status of DOI
        doiRow.setStatus(TO_BE_REGISTERED);
        try {
            doiService.update(context, doiRow);
        } catch (SQLException sqle) {
            log.warn("SQLException while changing status of DOI {} to be registered.", doi);
            throw new RuntimeException(sqle);
        }

    }

    /**
     * @param context    The relevant DSpace Context.
     * @param dso        DSpaceObject the DOI should be reserved for. Some metadata of
     *                   this object will be send to the registration agency.
     * @param identifier DOI to register in a format that
     *                   {@link org.dspace.identifier.service.DOIService#formatIdentifier(String)} accepts.
     * @throws IdentifierException      If the format of {@code identifier} was
     *                                  unrecognized or if it was impossible to
     *                                  reserve the DOI (registration agency denied
     *                                  for some reason, see logs).
     * @throws IllegalArgumentException If {@code identifier} is a DOI already
     *                                  registered for another DSpaceObject then {@code dso}.
     * @see org.dspace.identifier.IdentifierProvider#reserve(Context, DSpaceObject, String)
     */
    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier)
        throws IdentifierException, IllegalArgumentException {
        reserve(context, dso, identifier, this.filter);
    }

    /**
     * Reserve a specified DOI for a given DSpaceObject
     * @param context    - DSpace context
     * @param dso        - DSpaceObject identified by this DOI
     * @param identifier - String containing the DOI to reserve
     * @param filter     - Logical item filter to determine whether this identifier should be reserved
     * @throws IdentifierException
     * @throws IllegalArgumentException
     */
    @Override
    public void reserve(Context context, DSpaceObject dso, String identifier, Filter filter)
        throws IdentifierException, IllegalArgumentException {
        String doi = doiService.formatIdentifier(identifier);
        DOI doiRow = null;

        try {
            doiRow = loadOrCreateDOI(context, dso, doi, filter);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }

        if (doiRow.getStatus() != null) {
            return;
        }

        doiRow.setStatus(TO_BE_RESERVED);
        try {
            doiService.update(context, doiRow);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Perform the actual online / API interaction required to reserve the DOI online
     * always applying filters if they are configured
     * @param context       - DSpace context
     * @param dso           - DSpaceObject identified by this DOI
     * @param identifier    - String containing the DOI to reserve
     * @throws IdentifierException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public void reserveOnline(Context context, DSpaceObject dso, String identifier)
        throws IdentifierException, IllegalArgumentException, SQLException {
        reserveOnline(context, dso, identifier, this.filter);
    }

    /**
     * Perform the actual online / API interaction required to reserve the DOI online
     * @param context       - DSpace context
     * @param dso           - DSpaceObject identified by this DOI
     * @param identifier    - String containing the DOI to reserve
     * @param filter        - Logical item filter to determine whether this identifier should be reserved online
     * @throws IdentifierException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public void reserveOnline(Context context, DSpaceObject dso, String identifier, Filter filter)
            throws IdentifierException, IllegalArgumentException, SQLException {
        String doi = doiService.formatIdentifier(identifier);
        // get TableRow and ensure DOI belongs to dso regarding our db
        DOI doiRow = loadOrCreateDOI(context, dso, doi, filter);

        if (DELETED.equals(doiRow.getStatus()) || TO_BE_DELETED.equals(doiRow.getStatus())) {
            throw new DOIIdentifierException("You tried to reserve a DOI that "
                    + "is marked as DELETED.", DOIIdentifierException.DOI_IS_DELETED);
        }

        connector.reserveDOI(context, dso, doi);

        doiRow.setStatus(IS_RESERVED);
        doiService.update(context, doiRow);
    }

    /**
     * Perform the actual online / API interaction required to register the DOI online
     * always applying filters if they are configured
     * @param context       - DSpace context
     * @param dso           - DSpaceObject identified by this DOI
     * @param identifier    - String containing the DOI to register
     * @throws IdentifierException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public void registerOnline(Context context, DSpaceObject dso, String identifier)
        throws IdentifierException, IllegalArgumentException, SQLException {

        registerOnline(context, dso, identifier, this.filter);

    }

    /**
     * Perform the actual online / API interaction required to register the DOI online
     * @param context       - DSpace context
     * @param dso           - DSpaceObject identified by this DOI
     * @param identifier    - String containing the DOI to register
     * @param filter     - Logical item filter to determine whether this identifier should be registered online
     * @throws IdentifierException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public void registerOnline(Context context, DSpaceObject dso, String identifier, Filter filter)
            throws IdentifierException, IllegalArgumentException, SQLException {

        String doi = doiService.formatIdentifier(identifier);
        // get TableRow and ensure DOI belongs to dso regarding our db
        DOI doiRow = loadOrCreateDOI(context, dso, doi, filter);

        if (DELETED.equals(doiRow.getStatus()) || TO_BE_DELETED.equals(doiRow.getStatus())) {
            throw new DOIIdentifierException("You tried to register a DOI that "
                    + "is marked as DELETED.", DOIIdentifierException.DOI_IS_DELETED);
        }

        // register DOI Online
        try {
            connector.registerDOI(context, dso, doi);
        } catch (DOIIdentifierException die) {
            // do we have to reserve DOI before we can register it?
            if (die.getCode() == DOIIdentifierException.RESERVE_FIRST) {
                this.reserveOnline(context, dso, identifier, filter);
                connector.registerDOI(context, dso, doi);
            } else {
                throw die;
            }
        }

        // safe DOI as metadata of the item
        try {
            saveDOIToObject(context, dso, doi);
        } catch (AuthorizeException ae) {
            throw new IdentifierException("Not authorized to save a DOI as metadata of an dso!", ae);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }

        doiRow.setStatus(IS_REGISTERED);
        doiService.update(context, doiRow);
    }

    /**
     * Update metadata for a registered object
     * If the DOI for hte item already exists, *always* skip the filter since it should only be used for
     * allowing / disallowing reservation and registration, not metadata updates or deletions
     *
     * @param context       - DSpace context
     * @param dso           - DSpaceObject identified by this DOI
     * @param identifier    - String containing the DOI to reserve
     * @throws IdentifierException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public void updateMetadata(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException, IllegalArgumentException, SQLException {

        String doi = doiService.formatIdentifier(identifier);
        // Use the default filter unless we find the object
        Filter updateFilter = this.filter;

        if (doiService.findDOIByDSpaceObject(context, dso) != null) {
            // We can skip the filter here since we know the DOI already exists for the item
            log.debug("updateMetadata: found DOIByDSpaceObject: " +
                doiService.findDOIByDSpaceObject(context, dso).getDoi());
            updateFilter = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                    "always_true_filter", TrueFilter.class);
        }

        DOI doiRow = loadOrCreateDOI(context, dso, doi, updateFilter);

        if (PENDING.equals(doiRow.getStatus()) || MINTED.equals(doiRow.getStatus())) {
            log.info("Not updating metadata for PENDING or MINTED doi: " + doi);
            return;
        }

        if (DELETED.equals(doiRow.getStatus()) || TO_BE_DELETED.equals(doiRow.getStatus())) {
            throw new DOIIdentifierException("You tried to register a DOI that "
                    + "is marked as DELETED.", DOIIdentifierException.DOI_IS_DELETED);
        }

        if (IS_REGISTERED.equals(doiRow.getStatus())) {
            doiRow.setStatus(UPDATE_REGISTERED);
        } else if (TO_BE_REGISTERED.equals(doiRow.getStatus())) {
            doiRow.setStatus(UPDATE_BEFORE_REGISTRATION);
        } else if (IS_RESERVED.equals(doiRow.getStatus())) {
            doiRow.setStatus(UPDATE_RESERVED);
        } else {
            return;
        }

        doiService.update(context, doiRow);
    }

    /**
     * Update metadata for a registered object in the DOI Connector to update the agency records
     * If the DOI for hte item already exists, *always* skip the filter since it should only be used for
     * allowing / disallowing reservation and registration, not metadata updates or deletions
     *
     * @param context       - DSpace context
     * @param dso           - DSpaceObject identified by this DOI
     * @param identifier    - String containing the DOI to reserve
     * @throws IdentifierException
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    public void updateMetadataOnline(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException, SQLException {
        String doi = doiService.formatIdentifier(identifier);

        // ensure DOI belongs to dso regarding our db
        DOI doiRow = null;
        try {
            doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        } catch (SQLException sqle) {
            log.warn("SQLException while searching a DOI in our db.", sqle);
            throw new RuntimeException("Unable to retrieve information about a DOI out of database.", sqle);
        }
        if (null == doiRow) {
            log.error("Cannot update metadata for DOI {}: unable to find it in our db.", doi);
            throw new DOIIdentifierException("Unable to find DOI.",
                    DOIIdentifierException.DOI_DOES_NOT_EXIST);
        }
        if (!Objects.equals(doiRow.getDSpaceObject(), dso)) {
            log.error("Refuse to update metadata of DOI {} with the metadata of "
                    + " an object ({}/{}) the DOI is not dedicated to.",
                      doi,
                      contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso),
                      dso.getID().toString());
            throw new DOIIdentifierException("Cannot update DOI metadata: "
                    + "DOI and DSpaceObject does not match!",
                    DOIIdentifierException.MISMATCH);
        }

        if (DELETED.equals(doiRow.getStatus()) || TO_BE_DELETED.equals(doiRow.getStatus())) {
            throw new DOIIdentifierException("You tried to update the metadata"
                    + " of a DOI that is marked as DELETED.",
                    DOIIdentifierException.DOI_IS_DELETED);
        }

        connector.updateMetadata(context, dso, doi);

        if (UPDATE_REGISTERED.equals(doiRow.getStatus())) {
            doiRow.setStatus(IS_REGISTERED);
        } else if (UPDATE_BEFORE_REGISTRATION.equals(doiRow.getStatus())) {
            doiRow.setStatus(TO_BE_REGISTERED);
        } else if (UPDATE_RESERVED.equals(doiRow.getStatus())) {
            doiRow.setStatus(IS_RESERVED);
        }

        doiService.update(context, doiRow);
    }

    /**
     * Mint a new DOI in DSpace - this is usually the first step of registration
     * Always apply filters if they are configured
     * @param context    - DSpace context
     * @param dso        - DSpaceObject identified by the new identifier
     * @return a String containing the new identifier
     * @throws IdentifierException
     */
    @Override
    public String mint(Context context, DSpaceObject dso)
            throws IdentifierException {
        return mint(context, dso, this.filter);
    }

    /**
     * Mint a new DOI in DSpace - this is usually the first step of registration
     * @param context    - DSpace context
     * @param dso        - DSpaceObject identified by the new identifier
     * @param filter     - Logical item filter to determine whether this identifier should be registered
     * @return a String containing the new identifier
     * @throws IdentifierException
     */
    @Override
    public String mint(Context context, DSpaceObject dso, Filter filter) throws IdentifierException {

        String doi = null;
        try {
            doi = getDOIByObject(context, dso);
        } catch (SQLException e) {
            log.error("Error while attemping to retrieve information about a DOI for "
                + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " with ID " + dso.getID() + ".");
            throw new RuntimeException("Error while attempting to retrieve " +
                "information about a DOI for " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", e);
        }
        if (null == doi) {
            try {
                DOI doiRow = loadOrCreateDOI(context, dso, null, filter);
                doi = DOI.SCHEME + doiRow.getDoi();

            } catch (SQLException e) {
                log.error("Error while creating new DOI for Object of " +
                    "ResourceType {} with id {}.", dso.getType(), dso.getID());
                throw new RuntimeException("Error while attempting to create a " +
                    "new DOI for " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + " with ID " +
                    dso.getID() + ".", e);
            }
        }
        return doi;
    }

    /**
     * Resolve an identifier to a DSpaceObject, if it is registered
     * @param context    - DSpace context
     * @param identifier - to be resolved.
     * @param attributes - additional information for resolving {@code identifier}.
     * @return a DSpaceObject identified by the identifier string
     * @throws IdentifierNotFoundException
     * @throws IdentifierNotResolvableException
     */
    @Override
    public DSpaceObject resolve(Context context, String identifier, String... attributes)
            throws IdentifierNotFoundException, IdentifierNotResolvableException {
        String doi = null;
        try {
            doi = doiService.formatIdentifier(identifier);
        } catch (IdentifierException e) {
            throw new IdentifierNotResolvableException(e);
        }
        try {
            DSpaceObject dso = getObjectByDOI(context, doi);
            if (null == dso) {
                throw new IdentifierNotFoundException();
            }
            return dso;
        } catch (SQLException sqle) {
            log.error("SQLException while searching a DOI in our db.", sqle);
            throw new RuntimeException("Unable to retrieve information about a DOI out of database.", sqle);
        } catch (IdentifierException e) {
            throw new IdentifierNotResolvableException(e);
        }
    }

    /**
     * Look up a DOI identifier for a given DSpaceObject
     * @param context - DSpace context
     * @param dso     - DSpaceObject to look up
     * @return a String containing the DOI
     * @throws IdentifierNotFoundException
     * @throws IdentifierNotResolvableException
     */
    @Override
    public String lookup(Context context, DSpaceObject dso)
            throws IdentifierNotFoundException, IdentifierNotResolvableException {
        String doi = null;
        try {
            doi = getDOIByObject(context, dso);
        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving DOI out of database.", e);
        }

        if (null == doi) {
            throw new IdentifierNotFoundException("No DOI for DSpaceObject of type " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + " found.");
        }

        return doi;
    }

    /**
     * Delete all DOIs for a DSpaceObject
     * @param context   - DSpace context
     * @param dso       - DSpaceObject to have all its DOIs deleted
     * @throws IdentifierException
     */
    @Override
    public void delete(Context context, DSpaceObject dso)
            throws IdentifierException {
        // delete all DOIs for this Item from our database.
        try {
            String doi = getDOIByObject(context, dso);
            while (null != doi) {
                this.delete(context, dso, doi);
                doi = getDOIByObject(context, dso);
            }
        } catch (SQLException ex) {
            log.error("Error while attemping to retrieve information about a DOI for " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", ex);
            throw new RuntimeException("Error while attempting to retrieve " +
                "information about a DOI for " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", ex);
        }

        // delete all DOIs of this item out of its metadata
        try {
            String doi = getDOIOutOfObject(dso);

            while (null != doi) {
                this.removeDOIFromObject(context, dso, doi);
                doi = getDOIOutOfObject(dso);
            }
        } catch (AuthorizeException ex) {
            log.error("Error while removing a DOI out of the metadata of an " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", ex);
            throw new RuntimeException("Error while removing a DOI out of the metadata of an " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", ex);

        } catch (SQLException ex) {
            log.error("Error while removing a DOI out of the metadata of an " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", ex);
            throw new RuntimeException("Error while removing a DOI out of the " +
                "metadata of an " + contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) +
                " with ID " + dso.getID() + ".", ex);
        }
    }

    /**
     * Delete a specific DOI for a given DSpaceObject
     * @param context    - DSpace context
     * @param dso        - DSpaceObject to be de-identified.
     * @param identifier - String containing identifier to delete
     * @throws IdentifierException
     */
    @Override
    public void delete(Context context, DSpaceObject dso, String identifier)
            throws IdentifierException {
        String doi = doiService.formatIdentifier(identifier);
        DOI doiRow = null;

        try {
            doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }

        // check if DOI belongs to dso
        if (null != doiRow) {
            if (!Objects.equals(dso, doiRow.getDSpaceObject())) {
                throw new DOIIdentifierException("Trying to delete a DOI out of "
                        + "an object that is not addressed by the DOI.",
                        DOIIdentifierException.MISMATCH);
            }
        }

        // remove DOI from metadata
        try {
            removeDOIFromObject(context, dso, doi);
        } catch (AuthorizeException ex) {
            log.error("Not authorized to delete a DOI out of an Item.", ex);
            throw new DOIIdentifierException("Not authorized to delete DOI.",
                    ex, DOIIdentifierException.UNAUTHORIZED_METADATA_MANIPULATION);
        } catch (SQLException ex) {
            log.error("SQLException occurred while deleting a DOI out of an item: "
                    + ex.getMessage());
            throw new RuntimeException("Error while deleting a DOI out of the " +
                    "metadata of an Item " + dso.getID(), ex);
        }

        // change doi status in db if necessary.
        if (null != doiRow) {
            doiRow.setDSpaceObject(null);
            if (doiRow.getStatus() == null) {
                doiRow.setStatus(DELETED);
            } else {
                doiRow.setStatus(TO_BE_DELETED);
            }
            try {
                doiService.update(context, doiRow);
            } catch (SQLException sqle) {
                log.warn("SQLException while changing status of DOI {} to be deleted.", doi);
                throw new RuntimeException(sqle);
            }
        }

        // DOI is a permanent identifier. DataCite for example does not delete
        // DOIS. But it is possible to mark a DOI as "inactive".
    }

    /**
     * Delete a specific DOI in the registration agency records via the DOI Connector
     * @param context    - DSpace context
     * @param identifier - String containing identifier to delete
     * @throws DOIIdentifierException
     */
    public void deleteOnline(Context context, String identifier) throws DOIIdentifierException {
        String doi = doiService.formatIdentifier(identifier);
        DOI doiRow = null;

        try {
            doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle);
        }
        if (null == doiRow) {
            throw new DOIIdentifierException("This identifier: " + identifier
                    + " isn't in our database",
                    DOIIdentifierException.DOI_DOES_NOT_EXIST);
        }
        if (!TO_BE_DELETED.equals(doiRow.getStatus())) {
            log.error("This identifier: {} couldn't be deleted. Delete it first from metadata.",
                DOI.SCHEME + doiRow.getDoi());
            throw new IllegalArgumentException("Couldn't delete this identifier:"
                                             + DOI.SCHEME + doiRow.getDoi()
                                             + ". Delete it first from metadata.");
        }
        connector.deleteDOI(context, doi);

        doiRow.setStatus(DELETED);
        try {
            doiService.update(context, doiRow);
        } catch (SQLException sqle) {
            log.warn("SQLException while changing status of DOI {} deleted.", doi);
            throw new RuntimeException(sqle);
        }
    }

    /**
     * Returns a DSpaceObject depending on its DOI.
     * @param context the context
     * @param identifier The DOI in a format that is accepted by
     *                   {@link org.dspace.identifier.service.DOIService#formatIdentifier(String)}.
     * @return Null if the DOI couldn't be found or the associated DSpaceObject.
     * @throws SQLException if database error
     * @throws DOIIdentifierException If {@code identifier} is null or an empty string.
     * @throws IllegalArgumentException If the identifier couldn't be recognized as DOI.
     */
    public DSpaceObject getObjectByDOI(Context context, String identifier)
            throws SQLException, DOIIdentifierException, IllegalArgumentException {
        String doi = doiService.formatIdentifier(identifier);
        DOI doiRow = doiService.findByDoi(context, doi.substring(DOI.SCHEME.length()));

        if (null == doiRow) {
            return null;
        }

        if (doiRow.getDSpaceObject() == null) {
            log.error("Found DOI " + doi + " in database, but no assigned Object could be found.");
            throw new IllegalStateException("Found DOI " + doi +
                " in database, but no assigned Object could be found.");
        }

        return doiRow.getDSpaceObject();
    }

    /**
     * Search the database for a DOI, using the type and id of an DSpaceObject.
     *
     * @param context The relevant DSpace Context.
     * @param dso     DSpaceObject to find doi for. DOIs with status TO_BE_DELETED will be
     *                ignored.
     * @return The DOI as String or null if DOI was not found.
     * @throws SQLException if database error
     */
    public String getDOIByObject(Context context, DSpaceObject dso) throws SQLException {
//        String sql = "SELECT * FROM Doi WHERE resource_type_id = ? " +
//                "AND resource_id = ? AND ((status != ? AND status != ?) OR status IS NULL)";

        DOI doiRow = doiService.findDOIByDSpaceObject(context, dso, Arrays.asList(DELETED, TO_BE_DELETED));
        if (null == doiRow) {
            return null;
        }

        if (doiRow.getDoi() == null) {
            log.error("A DOI with an empty doi column was found in the database. DSO-Type: " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + ", ID: " + dso.getID() + ".");
            throw new IllegalStateException("A DOI with an empty doi column was found in the database. DSO-Type: " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + ", ID: " + dso.getID() + ".");
        }

        return DOI.SCHEME + doiRow.getDoi();
    }

    /**
     * Load a DOI from the database or creates it if it does not exist.
     * This method can be used to ensure that a DOI exists in the database and
     * to load the appropriate TableRow. As protected method we don't check if
     * the DOI is in a decent format, use DOI.formatIdentifier(String) if necessary.
     *
     * @param context       The relevant DSpace Context.
     * @param dso           The DSpaceObject the DOI should be loaded or created for.
     * @param doiIdentifier A DOI or null if a DOI should be generated. The generated DOI
     *                      can be found in the appropriate column for the TableRow.
     * @return The database row of the object.
     * @throws SQLException           In case of an error using the database.
     * @throws DOIIdentifierException If {@code doi} is not part of our prefix or
     *                                DOI is registered for another object already.
     * @throws IdentifierNotApplicableException passed through.
     */
    protected DOI loadOrCreateDOI(Context context, DSpaceObject dso, String doiIdentifier)
            throws SQLException, DOIIdentifierException, IdentifierNotApplicableException {
        return loadOrCreateDOI(context, dso, doiIdentifier, this.filter);
    }

    /**
     * Load DOI from database, or create one if it doesn't yet exist.
     * We need to distinguish several cases.LoadOrCreate can be called with a
     * specified identifier to load or create. It can also be used to create a
     * new unspecified identifier. In the latter case doiIdentifier is set null.
     * If doiIdentifier is set, we know which doi we should try to load or
     * create, but even in such a situation we might be able to find it in the
     * database or might have to create it.
     *
     * @param context       - DSpace context
     * @param dso           - DSpaceObject to identify
     * @param doiIdentifier - DOI to load or create (null to mint a new one)
     * @param filter     - Logical item filter to determine whether this identifier should be registered
     * @return
     * @throws SQLException
     * @throws DOIIdentifierException
     * @throws org.dspace.identifier.IdentifierNotApplicableException passed through.
     */
    protected DOI loadOrCreateDOI(Context context, DSpaceObject dso, String doiIdentifier, Filter filter)
        throws SQLException, DOIIdentifierException, IdentifierNotApplicableException {

        DOI doi = null;

        // Was an identifier specified that we shall try to load or create if it is not existing yet?
        if (null != doiIdentifier) {
            // we expect DOIs to have the DOI-Scheme except inside the doi table:
            doiIdentifier = doiIdentifier.substring(DOI.SCHEME.length());

            // check if DOI is already in Database
            doi = doiService.findByDoi(context, doiIdentifier);
            if (null != doi) {
                if (doi.getDSpaceObject() == null) {
                    // doi was deleted, check resource type
                    if (doi.getResourceTypeId() != null
                        && doi.getResourceTypeId() != dso.getType()) {
                        // doi was assigned to another resource type. Don't
                        // reactivate it
                        throw new DOIIdentifierException("Cannot reassign"
                            + " previously deleted DOI " + doiIdentifier
                            + " as the resource types of the object it was"
                            + " previously assigned to and the object it"
                            + " shall be assigned to now differ (was: "
                            + Constants.typeText[doi.getResourceTypeId()]
                            + ", trying to assign to "
                            + Constants.typeText[dso.getType()] + ").",
                            DOIIdentifierException.DOI_IS_DELETED);
                    } else {
                        // reassign doi
                        // nothing to do here, doi will br reassigned after this
                        // if-else-if-else-...-block
                        // will check if a filter prohibits creation of DOIs after this if-else-block
                    }
                } else {
                    // doi is assigned to a DSO; is it assigned to our specific dso?
                    // check if DOI already belongs to dso
                    if (dso.getID().equals(doi.getDSpaceObject().getID())) {
                        // Before we return this, check the filter
                        checkMintable(context, filter, dso);
                        return doi;
                    } else {
                        throw new DOIIdentifierException("Trying to create a DOI " +
                            "that is already reserved for another object.",
                            DOIIdentifierException.DOI_ALREADY_EXISTS);
                    }
                }
            }

            // Check if this item is eligible for minting. An IdentifierNotApplicableException will be thrown if not.
            checkMintable(context, filter, dso);

            // check prefix
            if (!doiIdentifier.startsWith(this.getPrefix() + "/")) {
                throw new DOIIdentifierException("Trying to create a DOI " +
                    "that's not part of our Namespace!",
                    DOIIdentifierException.FOREIGN_DOI);
            }
            if (doi == null) {
                // prepare new doiRow
                doi = doiService.create(context);
            }
        } else {
            // Check if this item is eligible for minting. An IdentifierNotApplicableException will be thrown if not.
            checkMintable(context, filter, dso);

            doi = doiService.create(context);
            doiIdentifier = this.getPrefix() + "/" + this.getNamespaceSeparator() +
                doi.getID();
        }

        // prepare new doiRow
        doi.setDoi(doiIdentifier);
        doi.setDSpaceObject(dso);
        doi.setStatus(MINTED);
        try {
            doiService.update(context, doi);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot save DOI to database for unknown reason.");
        }

        return doi;
    }

    /**
     * Loads a DOI out of the metadata of an DSpaceObject.
     *
     * @param dso DSpace object to get DOI metadata from
     * @return The DOI or null if no DOI was found.
     * @throws DOIIdentifierException if identifier error
     */
    public String getDOIOutOfObject(DSpaceObject dso) throws DOIIdentifierException {
        // FIXME
        if (!(dso instanceof Item)) {
            throw new IllegalArgumentException("We currently support DOIs for Items only, not for " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + ".");
        }
        Item item = (Item) dso;

        List<MetadataValue> metadata = itemService.getMetadata(item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        String leftPart = doiService.getResolver() + SLASH + getPrefix() + SLASH + getNamespaceSeparator();
        for (MetadataValue id : metadata) {
            if (id.getValue().startsWith(leftPart)) {
                return doiService.DOIFromExternalFormat(id.getValue());
            }
        }
        return null;
    }

    /**
     * Adds a DOI to the metadata of an item.
     *
     * @param context The relevant DSpace Context.
     * @param dso     DSpaceObject the DOI should be added to.
     * @param doi     The DOI that should be added as metadata.
     * @throws SQLException        if database error
     * @throws AuthorizeException  if authorization error
     * @throws IdentifierException if identifier error
     */
    protected void saveDOIToObject(Context context, DSpaceObject dso, String doi)
            throws SQLException, AuthorizeException, IdentifierException {
        // FIXME
        if (!(dso instanceof Item)) {
            throw new IllegalArgumentException("We currently support DOIs for Items only, not for " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + ".");
        }
        Item item = (Item) dso;

        itemService.addMetadata(context, item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null,
            doiService.DOIToExternalForm(doi));
        try {
            itemService.update(context, item);
        } catch (SQLException | AuthorizeException ex) {
            throw ex;
        }
    }

    /**
     * Removes a DOI out of the metadata of a DSpaceObject.
     *
     * @param context The relevant DSpace Context.
     * @param dso     The DSpaceObject the DOI should be removed from.
     * @param doi     The DOI to remove out of the metadata.
     * @throws AuthorizeException  if authorization error
     * @throws SQLException        if database error
     * @throws IdentifierException if identifier error
     */
    protected void removeDOIFromObject(Context context, DSpaceObject dso, String doi)
        throws AuthorizeException, SQLException, IdentifierException {
        // FIXME
        if (!(dso instanceof Item)) {
            throw new IllegalArgumentException("We currently support DOIs for Items only, not for " +
                contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso) + ".");
        }
        Item item = (Item) dso;

        List<MetadataValue> metadata = itemService.getMetadata(item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        List<String> remainder = new ArrayList<>();

        for (MetadataValue id : metadata) {
            if (!id.getValue().equals(doiService.DOIToExternalForm(doi))) {
                remainder.add(id.getValue());
            }
        }

        itemService.clearMetadata(context, item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null);
        itemService.addMetadata(context, item, MD_SCHEMA, DOI_ELEMENT, DOI_QUALIFIER, null,
                remainder);
        itemService.update(context, item);
    }

    /**
     * Checks to see if an item can have a DOI minted, using the configured logical filter
     * @param context
     * @param filter Logical item filter to apply
     * @param dso The item to be evaluated
     * @throws DOIIdentifierNotApplicableException
     */
    @Override
    public void checkMintable(Context context, Filter filter, DSpaceObject dso)
            throws DOIIdentifierNotApplicableException {
        if (filter == null) {
            Filter trueFilter = DSpaceServicesFactory.getInstance().getServiceManager().getServiceByName(
                    "always_true_filter", TrueFilter.class);
            // If a null filter was passed, and we have a good default filter to apply, apply it.
            // Otherwise, set to TrueFilter which means "no filtering"
            if (this.filter != null) {
                filter = this.filter;
            } else {
                filter = trueFilter;
            }
        }
        // If the check fails, an exception will be thrown to be caught by the calling method
        if (contentServiceFactory.getDSpaceObjectService(dso).getTypeText(dso).equals("ITEM")) {
            try {
                boolean result = filter.getResult(context, (Item) dso);
                log.debug("Result of filter for " + dso.getHandle() + " is " + result);
                if (!result) {
                    throw new DOIIdentifierNotApplicableException("Item " + dso.getHandle() +
                            " was evaluated as 'false' by the item filter, not minting");
                }
            } catch (LogicalStatementException e) {
                log.error("Error evaluating item with logical filter: " + e.getLocalizedMessage());
                throw new DOIIdentifierNotApplicableException(e);
            }
        } else {
            log.debug("DOI Identifier Provider: filterService is null (ie. don't prevent DOI minting)");
        }
    }

    /**
     * Checks to see if an item can have a DOI minted, using the configured logical filter
     * @param context
     * @param dso The item to be evaluated
     * @throws DOIIdentifierNotApplicableException
     */
    @Override
    public void checkMintable(Context context, DSpaceObject dso) throws DOIIdentifierNotApplicableException {
        checkMintable(context, this.filter, dso);
    }

}