package org.dspace.identifier;

import org.apache.log4j.Logger;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.storage.rdbms.TableRow;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

/**
 * User: kevin (kevin at atmire.com)
 * Date: 14-dec-2010
 * Time: 14:51:52
 *
 * The old DSpace handle identifier service, used to create handles or retrieve objects based on their handle
 */
@Component
public class HandleIdentifierProvider extends IdentifierProvider {
    /** log4j category */
    private static Logger log = Logger.getLogger(HandleIdentifierProvider.class);

    private String[] supportedPrefixes = new String[]{"info:hdl/", "hdl:", "http://hdl.handle.net/"};

    public boolean supports(String identifier)
    {
        for(String prefix : supportedPrefixes){
            if(identifier.startsWith(prefix))
                return true;
        }

        if(identifier.startsWith(ConfigurationManager.getProperty("handle.prefix")))
        {
            return true;
        }

        try {
            String outOfUrl = retrieveHandleOutOfUrl(identifier);
            if(outOfUrl != null && outOfUrl.startsWith(ConfigurationManager.getProperty("handle.prefix")))
                return true;

        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

        return false;
    }

    public String register(Context context, DSpaceObject dso) {
        return mint(context, dso);
    }

    /**
     * Creates a new handle in the database.
     *
     * @param context DSpace context
     * @param dso The DSpaceObject to create a handle for
     * @return The newly created handle
     * @exception java.sql.SQLException If a database error occurs
     */
    public String mint(Context context, DSpaceObject dso) {
        if(dso.getHandle() != null)
            return dso.getHandle();

        try{
            TableRow handle = DatabaseManager.create(context, "Handle");
            String handleId = createId(handle.getIntColumn("handle_id"));

            handle.setColumn("handle", handleId);
            handle.setColumn("resource_type_id", dso.getType());
            handle.setColumn("resource_id", dso.getID());
            DatabaseManager.update(context, handle);

            if (log.isDebugEnabled()) {
                log.debug("Created new handle for "
                        + Constants.typeText[dso.getType()] + " " + handleId);
            }
            return handleId;
        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while attempting to create handle", "Item id: " + dso.getID()));
        }
        
        return null;

    }

    public DSpaceObject resolve(Context context, String identifier, String... attributes) {
        // We can do nothing with this, return null
        try{
            if(identifier.startsWith("hdl:"))
            {
                identifier=identifier.replace("hdl:","");
            }
            TableRow dbhandle = findHandleInternal(context, identifier);

            if (dbhandle == null) {
                //Check for an url
                identifier = retrieveHandleOutOfUrl(identifier);
                if(identifier != null)
                    dbhandle = findHandleInternal(context, identifier);
                
                if(dbhandle == null)
                    return null;
                
            }

            if ((dbhandle.isColumnNull("resource_type_id"))
                    || (dbhandle.isColumnNull("resource_id"))) {
                throw new IllegalStateException("No associated resource type");
            }

            // What are we looking at here?
            int handletypeid = dbhandle.getIntColumn("resource_type_id");
            int resourceID = dbhandle.getIntColumn("resource_id");

            if (handletypeid == Constants.ITEM) {
                Item item = Item.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to item "
                            + ((item == null) ? (-1) : item.getID()));
                }

                return item;
            }
            else if (handletypeid == Constants.COLLECTION) {
                Collection collection = Collection.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to collection "
                            + ((collection == null) ? (-1) : collection.getID()));
                }

                return collection;
            }
            else if (handletypeid == Constants.COMMUNITY) {
                Community community = Community.find(context, resourceID);

                if (log.isDebugEnabled()) {
                    log.debug("Resolved handle " + identifier + " to community "
                            + ((community == null) ? (-1) : community.getID()));
                }

                return community;
            }


        }catch (Exception e){
            log.error(LogManager.getHeader(context, "Error while resolving handle to item", "handle: " + identifier));
        }
//        throw new IllegalStateException("Unsupported Handle Type "
//                + Constants.typeText[handletypeid]);
        return null;
    }

    public void delete(Context context, DSpaceObject dso) throws IdentifierException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static String retrieveHandleOutOfUrl(String url)
            throws SQLException {
        // We can do nothing with this, return null
        if (url.indexOf("/") == -1) return null;

        String[] splitUrl = url.split("/");

        return splitUrl[splitUrl.length - 2] + "/" + splitUrl[splitUrl.length - 1];
    }


    /**
     * Find the database row corresponding to handle.
     *
     * @param context DSpace context
     * @param handle The handle to resolve
     * @return The database row corresponding to the handle
     * @exception java.sql.SQLException If a database error occurs
     */
    private static TableRow findHandleInternal(Context context, String handle)
            throws SQLException {
        if (handle == null) {
            throw new IllegalArgumentException("Handle is null");
        }

        return DatabaseManager
                .findByUnique(context, "Handle", "handle", handle);
    }

    /**
     * Create a new handle id. The implementation uses the PK of the RDBMS
     * Handle table.
     * @id
     * @return A new handle id
     * @exception java.sql.SQLException If a database error occurs
     */
    private static String createId(int id) throws SQLException {
        String handlePrefix = ConfigurationManager.getProperty("handle.prefix");

        //TODO: put dryad. in the dspace.cfg !
        String fullHandle = new StringBuffer().append(handlePrefix).append(
                handlePrefix.endsWith("/") ? "" : "/").append("dryad.").append(
                id).toString();

        log.info("Creating new handle " + fullHandle);
        return fullHandle;
    }
}
