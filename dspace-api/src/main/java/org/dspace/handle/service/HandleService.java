/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.service;

import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.List;

/**
 * Interface to the <a href="http://www.handle.net" target=_new>CNRI Handle
 * System </a>.
 *
 * <p>
 * Currently, this class simply maps handles to local facilities; handles which
 * are owned by other sites (including other DSpaces) are treated as
 * non-existent.
 * </p>
 *
 * @author Peter Breton
 * @version $Revision$
 */
public interface HandleService {

    /**
     * Return the local URL for handle, or null if handle cannot be found.
     *
     * The returned URL is a (non-handle-based) location where a dissemination
     * of the object referred to by handle can be obtained.
     *
     * @param context
     *            DSpace context
     * @param handle
     *            The handle
     * @return The local URL
     * @throws SQLException
     *                If a database error occurs
     */
    public String resolveToURL(Context context, String handle)
            throws SQLException;


    /**
     * Try to detect a handle in a URL.
     * @param context DSpace context
     * @param url The URL
     * @return The handle or null if the handle couldn't be extracted of a URL
     * or if the extracted handle couldn't be found.
     * @throws SQLException  If a database error occurs
     */
    public String resolveUrlToHandle(Context context, String url)
            throws SQLException;

    /**
     * Transforms handle into a URI using http://hdl.handle.net if not 
     * overridden by the configuration property handle.canonical.prefix.
     *
     * No attempt is made to verify that handle is in fact valid.
     *
     * @param handle
     *            The handle
     * @return The canonical form
     */
    public String getCanonicalForm(String handle);

    /**
     * Creates a new handle in the database.
     *
     * @param context
     *            DSpace context
     * @param dso
     *            The DSpaceObject to create a handle for
     * @return The newly created handle
     * @throws SQLException
     *                If a database error occurs
     */
    public String createHandle(Context context, DSpaceObject dso)
            throws SQLException;

    /**
     * Creates a handle entry, but with a handle supplied by the caller (new
     * Handle not generated)
     *
     * @param context
     *            DSpace context
     * @param dso
     *            DSpaceObject
     * @param suppliedHandle
     *            existing handle value
     * @return the Handle
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IllegalStateException if specified handle is already in use by another object
     */
    public String createHandle(Context context, DSpaceObject dso, String suppliedHandle)
            throws SQLException, IllegalStateException;

    /**
     * Creates a handle entry, but with a handle supplied by the caller (new
     * Handle not generated)
     *
     * @param context
     *            DSpace context
     * @param dso
     *            DSpaceObject
     * @param suppliedHandle
     *            existing handle value
     * @param force
     *            FIXME: currently unused
     * @return the Handle
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     * @throws IllegalStateException if specified handle is already in use by another object
     */
    public String createHandle(Context context, DSpaceObject dso, String suppliedHandle, boolean force)
            throws SQLException, IllegalStateException;

    /**
     * Removes binding of Handle to a DSpace object, while leaving the
     * Handle in the table so it doesn't get reallocated.  The AIP
     * implementation also needs it there for foreign key references.
     *
     * @param context DSpace context
     * @param dso DSpaceObject whose Handle to unbind.
     * @throws SQLException
     *     An exception that provides information on a database access error or other errors.
     */
    public void unbindHandle(Context context, DSpaceObject dso)
            throws SQLException;

    /**
     * Return the object which handle maps to, or null. This is the object
     * itself, not a URL which points to it.
     *
     * @param context
     *            DSpace context
     * @param handle
     *            The handle to resolve
     * @return The object which handle maps to, or null if handle is not mapped
     *         to any object.
     * @throws IllegalStateException
     *                If handle was found but is not bound to an object
     * @throws SQLException
     *                If a database error occurs
     */
    public DSpaceObject resolveToObject(Context context, String handle)
            throws IllegalStateException, SQLException;


    /**
     * Return the handle for an Object, or null if the Object has no handle.
     *
     * @param context
     *            DSpace context
     * @param dso
     *            The object to obtain a handle for
     * @return The handle for object, or null if the object has no handle.
     * @throws SQLException
     *                If a database error occurs
     */
    public String findHandle(Context context, DSpaceObject dso)
            throws SQLException;

    /**
     * Return all the handles which start with prefix.
     *
     * @param context
     *            DSpace context
     * @param prefix
     *            The handle prefix
     * @return A list of the handles starting with prefix. The list is
     *         guaranteed to be non-null. Each element of the list is a String.
     * @throws SQLException
     *                If a database error occurs
     */
    public List<String> getHandlesForPrefix(Context context, String prefix)
            throws SQLException;

    /**
     * Get the configured Handle prefix string, or a default
     * @return configured prefix or "123456789"
     */
    public String getPrefix();

    public long countHandlesByPrefix(Context context, String prefix) throws SQLException;

    public int updateHandlesWithNewPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException;

    public void modifyHandleDSpaceObject(Context context, String handle, DSpaceObject newOwner) throws SQLException;

    int countTotal(Context context) throws SQLException;
}
