/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.handle;

import static org.dspace.handle.external.ExternalHandleConstants.MAGIC_BEAN;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataFieldServiceImpl;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SiteService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogHelper;
import org.dspace.handle.dao.HandleDAO;
import org.dspace.handle.external.HandleRest;
import org.dspace.handle.service.HandleClarinService;
import org.dspace.handle.service.HandleService;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Additional service implementation for the Handle object in Clarin-DSpace.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 * @author Milan Majchrak (milan.majchrak at dataquest.sk)
 */
public class HandleClarinServiceImpl implements HandleClarinService {

    /**
     * log4j logger
     */
    private static Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataFieldServiceImpl.class);

    @Autowired(required = true)
    protected SiteService siteService;

    @Autowired(required = true)
    protected HandleDAO handleDAO;

    @Autowired(required = true)
    protected HandleService handleService;

    @Autowired(required = true)
    protected ItemService itemService;

    @Autowired(required = true)
    protected CollectionService collectionService;

    @Autowired(required = true)
    protected CommunityService communityService;

    @Autowired(required = true)
    protected ConfigurationService configurationService;

    @Autowired(required = true)
    protected AuthorizeService authorizeService;

    static final String PREFIX_DELIMITER = "/";
    static final String PART_IDENTIFIER_DELIMITER = "@";

    /**
     * Protected Constructor
     */
    protected HandleClarinServiceImpl() {
    }

    @Override
    public List<Handle> findAll(Context context) throws SQLException {
        return handleDAO.findAll(context, Handle.class);
    }

    @Override
    public Handle findByID(Context context, int id) throws SQLException {
        return handleDAO.findByID(context, Handle.class, id);
    }

    @Override
    public Handle findByHandle(Context context, String handle) throws SQLException {
        return handleDAO.findByHandle(context, handle);
    }

    @Override
    public Handle createExternalHandle(Context context, String handleStr, String url)
            throws SQLException, AuthorizeException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators may modify the handle registry");
        }
        String handleId;
        //Do we want to generate the new handleId or use entered handleStr by user?
        if (Objects.nonNull(handleStr)) {
            //we use handleStr entered by use
            handleId = handleStr;
        } else {
            //we generate new handleId
            handleId = createId(context);
        }

        Handle handle = handleDAO.create(context, new Handle());
        log.debug("Created new external Handle with handle " + handleId);

        //set handle and url in created handle
        setHandleAndUrlOfHandleObject(context, handle, handleStr, url);

        return handle;
    }

    @Override
    public void delete(Context context, Handle handle) throws SQLException, AuthorizeException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators may modify the handle registry");
        }
        //delete handle
        handleDAO.delete(context, handle);
        log.info(LogHelper.getHeader(context, "delete_handle",
                "handle_id=" + handle.getID()));
    }

    @Override
    public void save(Context context, Handle handle) throws SQLException, AuthorizeException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators may modify the handle registry");
        }
        //save handle
        handleDAO.save(context, handle);
        log.info(LogHelper.getHeader(context, "save_handle",
                "handle_id=" + handle.getID()
                        + "handle=" + handle.getHandle()
                        + "resourceTypeID=" + handle.getResourceTypeId()));
    }

    @Override
    public void update(Context context, Handle handleObject, String newHandle,
                       String newUrl)
            throws SQLException, AuthorizeException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators may modify the handle registry");
        }
        //set handle and url in handle
        setHandleAndUrlOfHandleObject(context, handleObject, newHandle, newUrl);
        log.info(LogHelper.getHeader(context, "update_handle",
                "handle_id=" + handleObject.getID()));
    }

    @Override
    public void setPrefix(Context context, String newPrefix, String oldPrefix) throws SQLException,
            AuthorizeException {
        // Check authorisation: Only admins may create DC types
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                    "Only administrators may modify the handle registry");
        }
        //control, if are new and old prefix entered
        if (StringUtils.isBlank(newPrefix) || StringUtils.isBlank(oldPrefix)) {
            throw new NullPointerException("Cannot set prefix. Required fields are empty.");
        }
        //get handle prefix
        String prefix = handleService.getPrefix();
        //set prefix only if not equal to old prefix
        if (prefix.equals(oldPrefix)) {
            //return value says if set prefix was successful
            if (!(configurationService.setProperty("handle.prefix", newPrefix))) {
                //prefix has not changed
                throw new RuntimeException("error while trying to set handle prefix");
            }
        } else {
            throw new RuntimeException("Cannot set prefix. Entered prefix does not match with ");
        }

        log.info(LogHelper.getHeader(context, "set_handle_prefix",
                "old_prefix=" + oldPrefix + " new_prefix=" + newPrefix));
    }

    /* Created for LINDAT/CLARIAH-CZ (UFAL) */
    @Override
    public boolean isInternalResource(Handle handle) {
        //in internal handle is not entered url
        return (Objects.isNull(handle.getUrl()) || handle.getUrl().isEmpty());
    }

    @Override
    public String resolveToURL(Context context, String handleStr) throws SQLException {
        //handle is not entered
        if (Objects.isNull(handleStr)) {
            throw new IllegalArgumentException("Handle is null");
        }

        // <UFAL>
        handleStr = stripPartIdentifier(handleStr);

        //find handle
        Handle handle = handleDAO.findByHandle(context, handleStr);

        //handle was not find
        if (Objects.isNull(handle)) {
            return null;
        }

        String url;
        if (isInternalResource(handle)) {
            //internal handle
            //create url for internal handle
            url = configurationService.getProperty("dspace.ui.url")
                    + "/handle/" + handleStr;
        } else {
            //external handle
            url = handle.getUrl();
        }
        log.debug("Resolved {} to {}", handle, url);

        return url;
    }

    @Override
    public DSpaceObject resolveToObject(Context context, String handle) throws IllegalStateException, SQLException {
        Handle foundHandle = findByHandle(context, handle);

        if (Objects.isNull(foundHandle)) {
            //If this is the Site-wide Handle, return Site object
            if (handle.equals(configurationService.getProperty("handle.prefix") + "/0")) {
                return siteService.findSite(context);
            }
            //Otherwise, return null (i.e. handle not found in DB)
            return null;
        }

        // check if handle was allocated previously, but is currently not
        // associated with a DSpaceObject
        // (this may occur when 'unbindHandle()' is called for an obj that was removed)
        if (Objects.isNull(foundHandle.getResourceTypeId()) || Objects.isNull(foundHandle.getDSpaceObject())) {
            //if handle has been unbound, just return null (as this will result in a PageNotFound)
            return null;
        }

        int handleTypeId = foundHandle.getResourceTypeId();
        UUID resourceID = foundHandle.getDSpaceObject().getID();

        if (handleTypeId == Constants.ITEM) {
            Item item = itemService.find(context, resourceID);
            if (log.isDebugEnabled()) {
                log.debug("Resolved handle " + handle + " to item "
                        + (Objects.isNull(item) ? (-1) : item.getID()));
            }

            return item;
        } else if (handleTypeId == Constants.COLLECTION) {
            Collection collection = collectionService.find(context, resourceID);
            if (log.isDebugEnabled()) {
                log.debug("Resolved handle " + handle + " to collection "
                        + (Objects.isNull(collection) ? (-1) : collection.getID()));
            }

            return collection;
        } else if (handleTypeId == Constants.COMMUNITY) {
            Community community = communityService.find(context, resourceID);
            if (log.isDebugEnabled()) {
                log.debug("Resolved handle " + handle + " to community "
                        + (Objects.isNull(community) ? (-1) : community.getID()));
            }

            return community;
        }

        throw new IllegalStateException("Unsupported Handle Type "
                + Constants.typeText[handleTypeId]);
    }

    /**
     * Create id for handle object.
     *
     * @param context       DSpace context object
     * @return              handle id
     * @throws SQLException if database error
     */
    private String createId(Context context) throws SQLException {
        // Get configured prefix
        String handlePrefix = handleService.getPrefix();
        // Get next available suffix (as a Long, since DSpace uses an incrementing sequence)
        Long handleSuffix = handleDAO.getNextHandleSuffix(context);

        return handlePrefix + (handlePrefix.endsWith("/") ? "" : "/") + handleSuffix.toString();
    }

    /**
     * Set handle and url of handle object.
     * It is not possible to change internal handle to external handle or
     * external handle to internal handle.
     *
     * @param context       DSpace context object
     * @param handleObject       handle object
     * @param newHandle     new string handle
     * @param newUrl     new url
     * @throws SQLException if database error
     * @throws AuthorizeException if authorization error
     */
    private void setHandleAndUrlOfHandleObject(Context context, Handle handleObject, String newHandle,
                                           String newUrl) throws SQLException, AuthorizeException {
        //set handle
        handleObject.setHandle(newHandle);
        //if it is internal handle, do nothing with url
        if (Objects.isNull(newUrl)) {
            throw new RuntimeException("Cannot change handle and url of handle object.");
        }

        //set url only if is not empty
        //when you add null to String, it converts null to "null"
        if (StringUtils.isBlank(newUrl) || StringUtils.equals(newUrl, "null")) {
            throw new RuntimeException("Cannot change handle and url of handle object.");
        }
        handleObject.setUrl(newUrl);

        this.save(context, handleObject);
        log.info(LogHelper.getHeader(context, "Set handle and url of handle object.",
                "handle_id=" + handleObject.getID()));
    }

    @Override
    public List<org.dspace.handle.external.Handle> convertHandleWithMagicToExternalHandle(List<Handle> magicHandles) {
        List<org.dspace.handle.external.Handle> externalHandles = new ArrayList<>();
        for (org.dspace.handle.Handle handleWithMagic: magicHandles) {
            externalHandles.add(new org.dspace.handle.external.Handle(handleWithMagic.getHandle(),
                    handleWithMagic.getUrl()));
        }

        return externalHandles;
    }

    @Override
    public List<HandleRest> convertExternalHandleToHandleRest(List<org.dspace.handle.external.Handle> externalHandles) {
        List<HandleRest> externalHandleRestList = new ArrayList<>();
        for (org.dspace.handle.external.Handle externalHandle: externalHandles) {
            HandleRest externalHandleRest = new HandleRest();

            externalHandleRest.setHandle(externalHandle.getHandle());
            externalHandleRest.setUrl(externalHandle.url);
            externalHandleRest.setTitle(externalHandle.title);
            externalHandleRest.setSubprefix(externalHandle.subprefix);
            externalHandleRest.setReportemail(externalHandle.reportemail);
            externalHandleRest.setRepository(externalHandle.repository);
            externalHandleRest.setSubmitdate(externalHandle.submitdate);

            externalHandleRestList.add(externalHandleRest);
        }

        return externalHandleRestList;
    }

    /**
     * Returns complete handle made from prefix and suffix
     */
    @Override
    public String completeHandle(String prefix, String suffix) {
        return prefix + PREFIX_DELIMITER + suffix;
    }

    @Override
    public String[] splitHandle(String handle) {
        if (Objects.nonNull(handle)) {
            return handle.split(PREFIX_DELIMITER);
        }
        return new String[] { null, null };
    }

    @Override
    public List<Handle> findAllExternalHandles(Context context) throws SQLException {
        // fetch all handles which contains `@magicLindat` string from the DB
        return handleDAO.findAll(context, Handle.class)
                .stream()
                .filter(handle -> Objects.nonNull(handle))
                .filter(handle -> Objects.nonNull(handle.getUrl()))
                .filter(handle -> handle.getUrl().contains(MAGIC_BEAN))
                .collect(Collectors.toList());
    }

    @Override
    public boolean isDead(Context context, String handle) throws SQLException {
        String baseHandle = stripPartIdentifier(handle);
        Handle foundHandle = handleDAO.findByHandle(context, baseHandle);
        return foundHandle.getDead();

    }
    @Override
    public String getDeadSince(Context context, String handle) throws SQLException {
        String baseHandle = stripPartIdentifier(handle);
        Handle foundHandle = handleDAO.findByHandle(context, baseHandle);
        Date timestamptz = foundHandle.getDeadSince();

        return Objects.nonNull(timestamptz) ? DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.
                format(timestamptz) : null;
    }

    /**
     * Strips the part identifier from the handle
     *
     * @param handle The handle with optional part identifier
     * @return The handle without the part identifier
     */
    private static String stripPartIdentifier(String handle) {
        if (Objects.isNull(handle)) {
            return null;
        }

        String baseHandle;
        int pos = handle.indexOf(PART_IDENTIFIER_DELIMITER);
        if (pos >= 0) {
            baseHandle = handle.substring(0, pos);
        } else {
            baseHandle = handle;
        }
        return baseHandle;
    }
}
