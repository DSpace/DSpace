/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.handle.dao.HandleDAO;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

import java.sql.SQLException;
import java.util.ArrayList;
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
public class HandleServiceImpl implements HandleService
{
    /** log4j category */
    private static Logger log = Logger.getLogger(HandleServiceImpl.class);

    /** Prefix registered to no one */
    static final String EXAMPLE_PREFIX = "123456789";

    @Autowired(required = true)
    protected HandleDAO handleDAO;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired
    protected SiteService siteService;

    /** Public Constructor */
    protected HandleServiceImpl()
    {
    }

    @Override
    public String resolveToURL(Context context, String handle)
            throws SQLException
    {
        Handle dbhandle = findHandleInternal(context, handle);

        if (dbhandle == null)
        {
            return null;
        }

        String url = configurationService.getProperty("dspace.url")
                + "/handle/" + handle;

        if (log.isDebugEnabled())
        {
            log.debug("Resolved " + handle + " to " + url);
        }

        return url;
    }

    @Override
    public String resolveUrlToHandle(Context context, String url)
            throws SQLException
    {
        String dspaceUrl = configurationService.getProperty("dspace.url")
                + "/handle/";
        String handleResolver = configurationService.getProperty("handle.canonical.prefix");

        String handle = null;

        if (url.startsWith(dspaceUrl))
        {
            handle = url.substring(dspaceUrl.length());
        }

        if (url.startsWith(handleResolver))
        {
            handle = url.substring(handleResolver.length());
        }

        if (null == handle)
        {
            return null;
        }

        // remove trailing slashes
        while (handle.startsWith("/"))
        {
            handle = handle.substring(1);
        }
        Handle dbhandle = findHandleInternal(context, handle);

        return (null == dbhandle) ? null : handle;
    }

    @Override
    public String getCanonicalForm(String handle)
    {

        // Let the admin define a new prefix, if not then we'll use the
        // CNRI default. This allows the admin to use "hdl:" if they want to or
        // use a locally branded prefix handle.myuni.edu.
        String handlePrefix = configurationService.getProperty("handle.canonical.prefix");
        if (StringUtils.isBlank(handlePrefix))
        {
            handlePrefix = "http://hdl.handle.net/";
        }

        return handlePrefix + handle;
    }

    @Override
    public String createHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        Handle handle = handleDAO.create(context, new Handle());
        String handleId = createId(context);

        handle.setHandle(handleId);
        handle.setDSpaceObject(dso);
        dso.addHandle(handle);
        handle.setResourceTypeId(dso.getType());
        handleDAO.save(context, handle);

        if (log.isDebugEnabled())
        {
            log.debug("Created new handle for "
                    + Constants.typeText[dso.getType()] + " (ID=" + dso.getID() + ") " + handleId );
        }

        return handleId;
    }

    @Override
    public String createHandle(Context context, DSpaceObject dso,
                               String suppliedHandle) throws SQLException, IllegalStateException
    {
        return createHandle(context, dso, suppliedHandle, false);
    }

    @Override
    public String createHandle(Context context, DSpaceObject dso,
                               String suppliedHandle, boolean force) throws SQLException, IllegalStateException
    {
        //Check if the supplied handle is already in use -- cannot use the same handle twice
        Handle handle = findHandleInternal(context, suppliedHandle);
        if (handle != null && handle.getDSpaceObject() != null)
        {
            //Check if this handle is already linked up to this specified DSpace Object
            if (handle.getDSpaceObject().getID().equals(dso.getID()))
            {
                //This handle already links to this DSpace Object -- so, there's nothing else we need to do
                return suppliedHandle;
            }
            else
            {
                //handle found in DB table & already in use by another existing resource
                throw new IllegalStateException("Attempted to create a handle which is already in use: " + suppliedHandle);
            }
        }
        else if (handle!=null && handle.getResourceTypeId() != null)
        {
            //If there is a 'resource_type_id' (but 'resource_id' is empty), then the object using
            // this handle was previously unbound (see unbindHandle() method) -- likely because object was deleted
            int previousType = handle.getResourceTypeId();

            //Since we are restoring an object to a pre-existing handle, double check we are restoring the same *type* of object
            // (e.g. we will not allow an Item to be restored to a handle previously used by a Collection)
            if (previousType != dso.getType())
            {
                throw new IllegalStateException("Attempted to reuse a handle previously used by a " +
                        Constants.typeText[previousType] + " for a new " +
                        Constants.typeText[dso.getType()]);
            }
        }
        else if (handle==null) //if handle not found, create it
        {
            //handle not found in DB table -- create a new table entry
            handle = handleDAO.create(context, new Handle());
            handle.setHandle(suppliedHandle);
        }

        handle.setResourceTypeId(dso.getType());
        handle.setDSpaceObject(dso);
        dso.addHandle(handle);
        handleDAO.save(context, handle);

        if (log.isDebugEnabled())
        {
            log.debug("Created new handle for "
                    + Constants.typeText[dso.getType()] + " (ID=" + dso.getID() + ") " + suppliedHandle );
        }

        return suppliedHandle;
    }

    @Override
    public void unbindHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        List<Handle> handles = getInternalHandles(context, dso);
        if (CollectionUtils.isNotEmpty(handles))
        {
            for (Handle handle: handles)
            {
                //Only set the "resouce_id" column to null when unbinding a handle.
                // We want to keep around the "resource_type_id" value, so that we
                // can verify during a restore whether the same *type* of resource
                // is reusing this handle!
                handle.setDSpaceObject(null);

                //Also remove the handle from the DSO list to keep a consistent model
                dso.getHandles().remove(handle);
                
                handleDAO.save(context, handle);

                if (log.isDebugEnabled())
                {
                    log.debug("Unbound Handle " + handle.getHandle() + " from object " + Constants.typeText[dso.getType()] + " id=" + dso.getID());
                }
            }
        }
        else
        {
            log.trace("Cannot find Handle entry to unbind for object " + Constants.typeText[dso.getType()] + " id=" + dso.getID() + ". Handle could have been unbinded before.");
        }
    }

    @Override
    public DSpaceObject resolveToObject(Context context, String handle)
            throws IllegalStateException, SQLException
    {
        Handle dbhandle = findHandleInternal(context, handle);
        // check if handle was allocated previously, but is currently not
        // associated with a DSpaceObject
        // (this may occur when 'unbindHandle()' is called for an obj that was removed)
        if (dbhandle == null || (dbhandle.getDSpaceObject() == null)
                || (dbhandle.getResourceTypeId() == null))
        {
            //if handle has been unbound, just return null (as this will result in a PageNotFound)
            return null;
        }

        return dbhandle.getDSpaceObject();
    }

    @Override
    public String findHandle(Context context, DSpaceObject dso)
            throws SQLException
    {
        List<Handle> handles = getInternalHandles(context, dso);
        if (CollectionUtils.isEmpty(handles))
        {
            return null;
        }
        else
        {
            //TODO: Move this code away from the HandleService & into the Identifier provider
            //Attempt to retrieve a handle that does NOT look like {handle.part}/{handle.part}.{version}
            String result = handles.iterator().next().getHandle();
            for (Handle handle: handles)
            {
                //Ensure that the handle doesn't look like this 12346/213.{version}
                //If we find a match that indicates that we have a proper handle
                if (!handle.getHandle().matches(".*/.*\\.\\d+"))
                {
                    result = handle.getHandle();
                }
            }

            return result;
        }
    }

    @Override
    public List<String> getHandlesForPrefix(Context context, String prefix)
            throws SQLException
    {
        List<Handle> handles = handleDAO.findByPrefix(context, prefix);
        List<String> handleStrings = new ArrayList<String>(handles.size());
        for (Handle handle : handles) {
            handleStrings.add(handle.getHandle());
        }
        return handleStrings;
    }

    @Override
    public String getPrefix()
    {
        String prefix = configurationService.getProperty("handle.prefix");
        if (StringUtils.isBlank(prefix))
        {
            prefix = EXAMPLE_PREFIX; // XXX no good way to exit cleanly
            log.error("handle.prefix is not configured; using " + prefix);
        }
        return prefix;
    }

    @Override
    public long countHandlesByPrefix(Context context, String prefix) throws SQLException {
        return handleDAO.countHandlesByPrefix(context, prefix);
    }

    @Override
    public int updateHandlesWithNewPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException {
        return handleDAO.updateHandlesWithNewPrefix(context, newPrefix, oldPrefix);
    }

    @Override
    public void modifyHandleDSpaceObject(Context context, String handle, DSpaceObject newOwner) throws SQLException {
        Handle dbHandle = findHandleInternal(context, handle);
        if (dbHandle != null)
        {
            // Check if we have to remove the handle from the current handle list
            // or if object is alreday deleted.
            if (dbHandle.getDSpaceObject() != null)
            {
                // Remove the old handle from the current handle list
                dbHandle.getDSpaceObject().getHandles().remove(dbHandle);
            }
            // Transfer the current handle to the new object
            dbHandle.setDSpaceObject(newOwner);
            dbHandle.setResourceTypeId(newOwner.getType());
            newOwner.getHandles().add(0, dbHandle);
            handleDAO.save(context, dbHandle);
        }

    }

    ////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////

    /**
     * Return the handle for an Object, or null if the Object has no handle.
     *
     * @param context
     *            DSpace context
     * @param dso
     *            DSpaceObject for which we require our handles
     * @return The handle for object, or null if the object has no handle.
     * @throws SQLException
     *                If a database error occurs
     */
    protected List<Handle> getInternalHandles(Context context, DSpaceObject dso)
            throws SQLException
    {
        return handleDAO.getHandlesByDSpaceObject(context, dso);
    }

    /**
     * Find the database row corresponding to handle.
     *
     * @param context
     *            DSpace context
     * @param handle
     *            The handle to resolve
     * @return The database row corresponding to the handle
     * @throws SQLException
     *                If a database error occurs
     */
    protected Handle findHandleInternal(Context context, String handle)
            throws SQLException
    {
        if (handle == null)
        {
            throw new IllegalArgumentException("Handle is null");
        }

        return handleDAO.findByHandle(context, handle);
    }

    /**
     * Create/mint a new handle id.
     *
     * @param context DSpace Context
     * @return A new handle id
     * @throws SQLException
     *                If a database error occurs
     */
    protected String createId(Context context) throws SQLException
    {
        // Get configured prefix
        String handlePrefix = getPrefix();

        // Get next available suffix (as a Long, since DSpace uses an incrementing sequence)
        Long handleSuffix = handleDAO.getNextHandleSuffix(context);

        return handlePrefix + (handlePrefix.endsWith("/") ? "" : "/") + handleSuffix.toString();
    }

    @Override
    public int countTotal(Context context) throws SQLException {
        return handleDAO.countRows(context);
    }
}
