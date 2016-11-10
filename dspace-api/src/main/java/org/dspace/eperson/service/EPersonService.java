/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.service;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.service.DSpaceObjectLegacySupportService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.PasswordHash;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Service interface class for the EPerson object.
 * The implementation of this class is responsible for all business logic calls for the EPerson object and is autowired by spring
 *
 * Methods for handling registration by email and forgotten passwords. When
 * someone registers as a user, or forgets their password, the
 * sendRegistrationInfo or sendForgotPasswordInfo methods can be used to send an
 * email to the user. The email contains a special token, a long string which is
 * randomly generated and thus hard to guess. When the user presents the token
 * back to the system, the AccountManager can use the token to determine the
 * identity of the eperson.
 *
 * *NEW* now ignores expiration dates so that tokens never expire
 *
 * @author Peter Breton
 * @author kevinvandevelde at atmire.com
 *
 * @version $Revision$
 */
public interface EPersonService extends DSpaceObjectService<EPerson>, DSpaceObjectLegacySupportService<EPerson>
{

    /**
     * Find the eperson by their email address.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param email
     *     EPerson's email to search by
     * @return EPerson, or {@code null} if none such exists.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public EPerson findByEmail(Context context, String email)
            throws SQLException;

    /**
     * Find the eperson by their netid.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param netId
     *     Network ID
     *
     * @return corresponding EPerson, or <code>null</code>
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public EPerson findByNetid(Context context, String netId)
            throws SQLException;

    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param query
     *     The search string
     *
     * @return array of EPerson objects
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> search(Context context, String query)
            throws SQLException;


    /**
     * Find the epeople that match the search query across firstname, lastname or email.
     * This method also allows offsets and limits for pagination purposes.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param query
     *     The search string
     * @param offset
     *     Inclusive offset
     * @param limit
     *     Maximum number of matches returned
     *
     * @return array of EPerson objects
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> search(Context context, String query, int offset, int limit)
            throws SQLException;

    /**
     * Returns the total number of epeople returned by a specific query, without the overhead
     * of creating the EPerson objects to store the results.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param query
     *     The search string
     *
     * @return the number of epeople matching the query
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public int searchResultCount(Context context, String query)
            throws SQLException;

    /**
     * Find all the epeople that match a particular query
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     *
     * @param context
     *     The relevant DSpace Context.
     * @param sortField
     *     which field to sort EPersons by
     * @return array of EPerson objects
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findAll(Context context, int sortField)
            throws SQLException;

    /**
     * Create a new eperson
     *
     * @param context
     *     The relevant DSpace Context.
     * @return the created EPerson
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws AuthorizeException
     *     Exception indicating the current user of the context does not have permission
     *     to perform a particular action.
     */
    public EPerson create(Context context) throws SQLException,
            AuthorizeException;

    /**
     * Set the EPerson's password.
     *
     * @param ePerson
     *     EPerson whose password we want to set.
     * @param password
     *     the new password.
     */
    public void setPassword(EPerson ePerson, String password);

    /**
     * Set the EPerson's password hash.
     *
     * @param ePerson
     *     EPerson whose password hash we want to set.
     * @param password
     *     hashed password, or null to set row data to NULL.
     */
    public void setPasswordHash(EPerson ePerson, PasswordHash password);

    /**
     * Return the EPerson's password hash.
     *
     * @param ePerson
     *     EPerson whose password hash we want to get.
     * @return hash of the password, or null on failure (such as no password).
     */
    public PasswordHash getPasswordHash(EPerson ePerson);

    /**
     * Check EPerson's password.  Side effect:  original unsalted MD5 hashes are
     * converted using the current algorithm.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param ePerson
     *     EPerson whose password we want to check
     * @param attempt
     *     the password attempt
     * @return boolean successful/unsuccessful
     */
    public boolean checkPassword(Context context, EPerson ePerson, String attempt);

    /**
     * Set a metadata value (in the metadatavalue table) of the metadata field
     * specified by 'field'.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param ePerson
     *     EPerson whose metadata we want to set.
     * @param field
     *     Metadata field we want to set (e.g. "phone").
     * @param value
     *     Metadata value we want to set
     * @throws SQLException
     *     if the requested metadata field doesn't exist
     */
    @Deprecated
    public void setMetadata(Context context, EPerson ePerson, String field, String value) throws SQLException;

    /**
     * Retrieve all accounts which have a password but do not have a digest algorithm
     *
     * @param context
     *     The relevant DSpace Context.
     * @return a list of epeople
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findUnsalted(Context context) throws SQLException;

    /**
     * Retrieve all accounts which have not logged in since the specified date
     *
     * @param context
     *     The relevant DSpace Context.
     * @param date
     *     from which date
     * @return a list of epeople
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findNotActiveSince(Context context, Date date) throws SQLException;

    /**
     * Check for presence of EPerson in tables that have constraints on
     * EPersons. Called by delete() to determine whether the eperson can
     * actually be deleted.
     *
     * An EPerson cannot be deleted if it exists in the item, workflowitem, or
     * tasklistitem tables.
     *
     * @param context
     *     The relevant DSpace Context.
     * @param ePerson
     *     EPerson to find
     * @return List of tables that contain a reference to the eperson.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<String> getDeleteConstraints(Context context, EPerson ePerson) throws SQLException;

    /**
     * Retrieve all accounts which belong to at least one of the specified groups.
     *
     * @param c
     *     The relevant DSpace Context.
     * @param groups
     *     set of eperson groups
     * @return a list of epeople
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public List<EPerson> findByGroups(Context c, Set<Group> groups) throws SQLException;

    /**
     * Retrieve all accounts which are subscribed to receive information about new items.
     *
     * @param context
     *     The relevant DSpace Context.
     * @return a list of epeople
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    List<EPerson> findEPeopleWithSubscription(Context context) throws SQLException;

    /**
     * Count all accounts.
     *
     * @param context
     *     The relevant DSpace Context.
     * @return the total number of epeople
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    int countTotal(Context context) throws SQLException;
}
