/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import static org.dspace.content.MetadataSchemaEnum.EPERSON;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldName;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.PasswordHash;

/**
 * Service interface class for the EPerson object.
 * The implementation of this class is responsible for all business logic calls
 * for the EPerson object and is autowired by Spring.
 *
 * <p>
 * Methods for handling registration by email and forgotten passwords. When
 * someone registers as a user, or forgets their password, the
 * sendRegistrationInfo or sendForgotPasswordInfo methods can be used to send an
 * email to the user. The email contains a special token, a long string which is
 * randomly generated and thus hard to guess. When the user presents the token
 * back to the system, the AccountManager can use the token to determine the
 * identity of the eperson.
 *
 * <p>
 * *NEW* now ignores expiration dates so that tokens never expire.
 *
 * @author Peter Breton
 * @author kevinvandevelde at atmire.com
 */
public interface EPersonService extends DSpaceObjectService<EPerson>, DSpaceObjectLegacySupportService<EPerson> {

    // Common metadata fields which must be defined.

    public static final MetadataFieldName MD_FIRSTNAME
            = new MetadataFieldName(EPERSON, "firstname");
    public static final MetadataFieldName MD_LASTNAME
            = new MetadataFieldName(EPERSON, "lastname");
    public static final MetadataFieldName MD_PHONE
            = new MetadataFieldName(EPERSON, "phone");
    public static final MetadataFieldName MD_LANGUAGE
            = new MetadataFieldName(EPERSON, "language");

    /**
     * Find the eperson by their email address.
     *
     * @param context The relevant DSpace Context.
     * @param email   EPerson's email to search by
     * @return EPerson, or {@code null} if none such exists.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public EPerson findByEmail(Context context, String email)
        throws SQLException;

    /**
     * Find the eperson by their netid.
     *
     * @param context The relevant DSpace Context.
     * @param netId   Network ID
     * @return corresponding EPerson, or <code>null</code>
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public EPerson findByNetid(Context context, String netId)
        throws SQLException;

    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     *
     * @param context The relevant DSpace Context.
     * @param query   The search string
     * @return array of EPerson objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> search(Context context, String query)
        throws SQLException;


    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     * This method also allows offsets and limits for pagination purposes.
     *
     * @param context The relevant DSpace Context.
     * @param query   The search string
     * @param offset  Inclusive offset
     * @param limit   Maximum number of matches returned
     * @return array of EPerson objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> search(Context context, String query, int offset, int limit)
        throws SQLException;

    /**
     * Returns the total number of epeople returned by a specific query, without the overhead
     * of creating the EPerson objects to store the results.
     *
     * @param context The relevant DSpace Context.
     * @param query   The search string
     * @return the number of epeople matching the query
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public int searchResultCount(Context context, String query)
        throws SQLException;

    /**
     * Find all the {@code EPerson}s in a specific order by field.
     * The sortable fields are:
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     *
     * @param context   The relevant DSpace Context.
     * @param sortField which field to sort EPersons by
     * @return list of EPerson objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     * @deprecated use the paginated method {@link findAll(Context, int)}.
     */
    @Deprecated
    public List<EPerson> findAll(Context context, int sortField)
        throws SQLException;

    /**
     * Find all the {@code EPerson}s in a specific order by field.
     * The sortable fields are:
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     *
     * @param context   The relevant DSpace Context.
     * @param sortField which field to sort EPersons by
     * @param pageSize  how many results return
     * @param offset    the position of the first result to return
     * @return list of EPerson objects
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findAll(Context context, int sortField, int pageSize, int offset)
        throws SQLException;

    /**
     * Create a new eperson
     *
     * @param context The relevant DSpace Context.
     * @return the created EPerson
     * @throws SQLException       An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException Exception indicating the current user of the context does not have permission
     *                            to perform a particular action.
     */
    public EPerson create(Context context) throws SQLException,
        AuthorizeException;

    /**
     * Set the EPerson's password.
     *
     * @param ePerson  EPerson whose password we want to set.
     * @param password the new password.
     */
    public void setPassword(EPerson ePerson, String password);

    /**
     * Set the EPerson's password hash.
     *
     * @param ePerson  EPerson whose password hash we want to set.
     * @param password hashed password, or null to set row data to NULL.
     */
    public void setPasswordHash(EPerson ePerson, PasswordHash password);

    /**
     * Return the EPerson's password hash.
     *
     * @param ePerson EPerson whose password hash we want to get.
     * @return hash of the password, or null on failure (such as no password).
     */
    public PasswordHash getPasswordHash(EPerson ePerson);

    /**
     * Check EPerson's password.  Side effect:  original unsalted MD5 hashes are
     * converted using the current algorithm.
     *
     * @param context The relevant DSpace Context.
     * @param ePerson EPerson whose password we want to check
     * @param attempt the password attempt
     * @return boolean successful/unsuccessful
     */
    public boolean checkPassword(Context context, EPerson ePerson, String attempt);

    /**
     * Retrieve all accounts which have a password but do not have a digest algorithm
     *
     * @param context The relevant DSpace Context.
     * @return a list of epeople
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findUnsalted(Context context) throws SQLException;

    /**
     * Retrieve all accounts which have not logged in since the specified date
     *
     * @param context The relevant DSpace Context.
     * @param date    from which date
     * @return a list of epeople
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException;

    /**
     * Check for presence of EPerson in tables that have constraints on
     * EPersons. Called by delete() to determine whether the eperson can
     * actually be deleted.
     *
     * An EPerson cannot be deleted if it exists in the item, resourcepolicy or workflow-related tables.
     *
     * @param context The relevant DSpace Context.
     * @param ePerson EPerson to find
     * @return List of tables that contain a reference to the eperson.
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<String> getDeleteConstraints(Context context, EPerson ePerson) throws SQLException;

    /**
     * Retrieve all accounts which belong to at least one of the specified groups.
     *
     * @param c      The relevant DSpace Context.
     * @param groups set of eperson groups
     * @return a list of epeople
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findByGroups(Context c, Set<Group> groups) throws SQLException;

    /**
     * Retrieve all accounts which are subscribed to receive information about new items.
     *
     * @param context The relevant DSpace Context.
     * @return a list of epeople
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    List<EPerson> findEPeopleWithSubscription(Context context) throws SQLException;

    /**
     * Count all accounts.
     *
     * @param context The relevant DSpace Context.
     * @return the total number of epeople
     * @throws SQLException An exception that provides information on a database access error or other errors.
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Find the EPerson related to the given profile item. If the given item is not
     * a profile item, null is returned.
     *
     * @param  context      The relevant DSpace Context.
     * @param  profile      the profile item to search for
     * @return              the EPerson, if any
     * @throws SQLException An exception that provides information on a database
     *                      access error or other errors.
     */
    EPerson findByProfileItem(Context context, Item profile) throws SQLException;
}
