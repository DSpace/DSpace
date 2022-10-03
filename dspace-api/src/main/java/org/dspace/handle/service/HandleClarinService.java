/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.handle.Handle;
import org.dspace.handle.external.HandleRest;

/**
 * Additional service interface class of HandleService for the Handle object in Clarin-DSpace.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 * @author Peter Breton modified for LINDAT/CLARIN
 */
public interface HandleClarinService {

    /**
     * Find all Handles following the sorting options
     * @param context DSpace context object
     * @param sortingColumn sorting option in the specific format e.g. `handle:123456789/111`
     * @param maxResult the max count of the handles
     * @param offset page
     * @return List of Handles
     */
    List<Handle> findAll(Context context, String sortingColumn, int maxResult, int offset) throws SQLException;

    /**
     * Retrieve all handle from the registry
     *
     * @param context       DSpace context object
     * @return              array of handles
     * @throws SQLException if database error
     */
    public List<Handle> findAll(Context context) throws SQLException;

    /**
     * Find the handle corresponding to the given numeric ID.  The ID is
     * a database key internal to DSpace.
     *
     * @param context       DSpace context object
     * @param id            the handle ID
     * @return              the handle object
     * @throws SQLException if database error
     */
    public Handle findByID(Context context, int id) throws SQLException;

    /**
     * Find the handle corresponding to the given string handle.
     *
     * @param context       DSpace context object
     * @param handle        string handle
     * @return              the handle object
     * @throws SQLException if database error
     */
    public Handle findByHandle(Context context, String handle) throws SQLException;

    /**
     * Creates a new external handle.
     * External handle has to have entered URL.
     *
     * @param context             DSpace context object
     * @param handleStr           String
     * @param url                 String
     * @return new Handle
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public Handle createExternalHandle(Context context, String handleStr, String url)
            throws SQLException, AuthorizeException;

    /**
     * Delete the handle.
     *
     * @param context             DSpace context object
     * @param handle              handle
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void delete(Context context, Handle handle) throws SQLException, AuthorizeException;

    /**
     * Save the metadata field in the database.
     *
     * @param context             dspace context
     * @param handle              handle
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void save(Context context, Handle handle)
            throws SQLException, AuthorizeException;

    /**
     * Update handle and url in handle object.
     * It is not possible to update external handle to internal handle or
     * external handle to internal handle.
     *
     * @param context             DSpace context object
     * @param newHandle           new handle
     * @param newUrl              new url
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void update(Context context, Handle handleObject, String newHandle,
                      String newUrl)
            throws SQLException, AuthorizeException;

    /**
     * Set handle prefix.
     *
     * @param context             DSpace context object
     * @param newPrefix           new prefix
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void setPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException, AuthorizeException;

    /* Created for LINDAT/CLARIAH-CZ (UFAL) */
    /**
     * Control, if handle is internal resource.
     *
     * @param handle handle object
     * @return       boolean
     */
    public boolean isInternalResource(Handle handle);

    /**
     * Return the local URL for internal handle,
     * saved url for external handle
     * and null if handle cannot be found.
     *
     * @param context       DSpace context
     * @param handleStr     The handle
     * @return The URL
     * @throws SQLException If a database error occurs
     */
    public String resolveToURL(Context context, String handleStr) throws SQLException;
    /**
     * Return the object which handle maps to (Item, Collection, Community), or null. This is the object
     * itself, not a URL which points to it.
     *
     * @param context DSpace context
     * @param handle The handle to resolve
     * @return The object which handle maps to, or null if handle is not mapped
     *         to any object.
     * @throws IllegalStateException If handle was found but is not bound to an object
     * @throws SQLException If a database error occurs
     */
    public DSpaceObject resolveToObject(Context context, String handle) throws IllegalStateException, SQLException;

    /**
     * Create the external handles from the list of handles with magic URL
     *
     * @param magicHandles handles with `@magicLindat` string in the URL
     * @return List of External Handles
     */
    public List<org.dspace.handle.external.Handle> convertHandleWithMagicToExternalHandle(List<Handle> magicHandles);

    /**
     * Convert external.Handles to the external.HandleRest object
     *
     * @param externalHandles
     * @return List of Handle Rest
     */
    public List<HandleRest> convertExternalHandleToHandleRest(List<org.dspace.handle.external.Handle> externalHandles);

    /**
     * Join the prefix and suffix with the delimiter
     *
     * @param prefix of the handle
     * @param suffix of the handle
     * @return the Handle string which is joined the prefix and suffix with delimiter
     */
    public String completeHandle(String prefix, String suffix);

    /**
     * Returns prefix/suffix or null/null.
     *
     * @param handle Prefix of the handle
     */
    public  String[] splitHandle(String handle);

    /**
     * Retrieve all external handle from the registry. The external handle has `@magicLindat` string in the URL.
     *
     * @param context       DSpace context object
     * @return              array of external handles
     * @throws SQLException if database error
     */
    public List<Handle> findAllExternalHandles(Context context) throws SQLException;

    /**
     * Returns the Handle `dead` column value from the database.
     *
     * @param context       DSpace context object
     * @param handle        handle of Handle object
     * @return              Handle `dead` column value from the database
     * @throws SQLException if database error
     */
    public boolean isDead(Context context, String handle) throws SQLException;

    /**
     * Return the date when was the Handle set as dead
     * @param context DSpace context object
     * @param handle of the Handle object
     * @return Date in the String format
     * @throws SQLException if database error
     */
    public String getDeadSince(Context context, String handle) throws SQLException;
}
