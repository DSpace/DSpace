/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.authorize;

import java.sql.SQLException;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.SiteService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.FindableObject;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowItemService;
import org.dspace.workflow.factory.WorkflowServiceFactory;
import org.springframework.stereotype.Component;

/**
 * Utility class to manipulate the AuthorizationRest object
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
@Component
public class AuthorizationRestUtil {
    /**
     * Extract the feature name from the Authorization business ID. See {@link Authorization#getID()}
     * 
     * @param id
     *            the Authorization business ID
     * @return the feature name
     */
    public String getFeatureName(String id) {
        return splitIdParts(id)[1];
    }

    /**
     * Get the object addressed in the authorization extracting its type and primary key from the authorization business
     * ID ({@link Authorization#getID()}) and using the appropriate service
     * 
     * @param context
     *            the DSpace context
     * @param id
     *            the Authorization business ID. See {@link Authorization#getID()}
     * @return the object addressed in the authorization
     * @throws SQLException
     *             if an error occur retrieving the data from the database
     * @throws IllegalArgumentException
     *             if the specified id doesn't contain syntactically valid object information
     */
    public FindableObject getObject(Context context, String id) throws SQLException {
        String[] parts = splitIdParts(id);
        String objIdStr = parts[3];
        int objTypeId;
        try {
            objTypeId = Integer.parseInt(parts[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "The type " + objIdStr + " is not yet supported, please implement it if needed by a feature");
        }
        switch (objTypeId) {
            case Constants.SITE:
                SiteService siteService = ContentServiceFactory.getInstance().getSiteService();
                UUID siteUuid = UUID.fromString(objIdStr);
                return siteService.find(context, siteUuid);
            case Constants.COMMUNITY:
                CommunityService comService = ContentServiceFactory.getInstance().getCommunityService();
                UUID comUuid = UUID.fromString(objIdStr);
                return comService.find(context, comUuid);
            case Constants.COLLECTION:
                CollectionService colService = ContentServiceFactory.getInstance().getCollectionService();
                UUID colUuid = UUID.fromString(objIdStr);
                return colService.find(context, colUuid);
            case Constants.ITEM:
                ItemService itemService = ContentServiceFactory.getInstance().getItemService();
                UUID itemUuid = UUID.fromString(objIdStr);
                return itemService.find(context, itemUuid);
            case Constants.BUNDLE:
                BundleService bndService = ContentServiceFactory.getInstance().getBundleService();
                UUID bndUuid = UUID.fromString(objIdStr);
                return bndService.find(context, bndUuid);
            case Constants.BITSTREAM:
                BitstreamService bitService = ContentServiceFactory.getInstance().getBitstreamService();
                UUID bitUuid = UUID.fromString(objIdStr);
                return bitService.find(context, bitUuid);
            case Constants.EPERSON:
                EPersonService epService = EPersonServiceFactory.getInstance().getEPersonService();
                UUID epUuid = UUID.fromString(objIdStr);
                return epService.find(context, epUuid);
            case Constants.GROUP:
                GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
                UUID grUuid = UUID.fromString(objIdStr);
                return groupService.find(context, grUuid);
            case Constants.WORKSPACEITEM:
                WorkspaceItemService wsService = ContentServiceFactory.getInstance().getWorkspaceItemService();
                int wsId = Integer.parseInt(objIdStr);
                return wsService.find(context, wsId);
            case Constants.WORKFLOWITEM:
                WorkflowItemService wfService = WorkflowServiceFactory.getInstance().getWorkflowItemService();
                int wfId = Integer.parseInt(objIdStr);
                return wfService.find(context, wfId);
            default:
                throw new IllegalArgumentException(
                        "The type " + objTypeId + " is not yet supported, please implement it if needed by a feature");
        }
    }

    /**
     * Get the eperson in the authorization extracting its uuid from the authorization business ID
     * ({@link Authorization#getID()}) and retrieving the corresponding eperson object with the {@link EPersonService}.
     * Please note that reference to deleted eperson will result in an IllegalArgumentException
     * 
     * @param context
     *            the DSpace context
     * @param id
     *            the Authorization business ID. See {@link Authorization#getID()}
     * @return the eperson addressed in the authorization or null if not specified.
     * @throws SQLException
     *             if an error occur retrieving the data from the database
     * @throws IllegalArgumentException
     *             if the specified id doesn't contain syntactically valid object information
     */
    public EPerson getEperson(Context context, String id) throws SQLException {
        String epersonIdStr = splitIdParts(id)[0];
        if (StringUtils.isBlank(epersonIdStr)) {
            return null;
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(epersonIdStr);
        } catch (Exception e) {
            throw new IllegalArgumentException("The authorization id " + id +
                    " contains a reference to an invalid eperson uuid " + epersonIdStr);
        }
        EPersonService service = EPersonServiceFactory.getInstance().getEPersonService();
        EPerson ep = service.find(context, uuid);
        if (ep == null) {
            throw new IllegalArgumentException("No eperson found with the uuid " + epersonIdStr);
        }
        return ep;
    }

    /**
     * Split the business ID in an array with a fixed length (4) as follow eperson uuid, feature name, object type id,
     * object id
     * 
     * @param id
     *            the Authorization business ID. See {@link Authorization#getID()}
     * @return an array with a fixed length (4) as follow eperson uuid, feature name, object type id, object id
     */
    private String[] splitIdParts(String id) {
        String[] idParts = id.split("_");
        String eperson = null;
        String feature = null;
        String objType = null;
        String objId = null;
        if (idParts.length == 4) {
            eperson = idParts[0];
            feature = idParts[1];
            objType = idParts[2];
            objId = idParts[3];
        } else if (idParts.length == 3) {
            feature = idParts[0];
            objType = idParts[1];
            objId = idParts[2];
        } else {
            throw new IllegalArgumentException(
                    "the authoization id is invalid, it must have the form " +
                    "[eperson-uuid_]feature-id_object-type_object-id");
        }
        return new String[] { eperson, feature, objType, objId };
    }
}
