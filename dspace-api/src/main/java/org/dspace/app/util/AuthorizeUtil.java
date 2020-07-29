/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.apache.logging.log4j.Logger;
import org.dspace.authenticate.factory.AuthenticateServiceFactory;
import org.dspace.authorize.AuthorizeConfiguration;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.dspace.xmlworkflow.factory.XmlWorkflowServiceFactory;
import org.dspace.xmlworkflow.storedcomponents.CollectionRole;
import org.dspace.xmlworkflow.storedcomponents.service.CollectionRoleService;

/**
 * This class is an addition to the AuthorizeManager that perform authorization
 * check on not CRUD (ADD, WRITE, etc.) actions.
 *
 * @author bollini
 */
public class AuthorizeUtil {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(AuthorizeUtil.class);
    /**
     * Default constructor
     */
    private AuthorizeUtil() { }

    /**
     * Is allowed manage (create, remove, edit) bitstream's policies in the
     * current context?
     *
     * @param context   the DSpace Context Object
     * @param bitstream the bitstream that the policy refer to
     * @throws AuthorizeException if authorization error
     *                            if the current context (current user) is not allowed to
     *                            manage the bitstream's policies
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageBitstreamPolicy(Context context,
                                                      Bitstream bitstream) throws AuthorizeException, SQLException {
        Bundle bundle = bitstream.getBundles().get(0);
        authorizeManageBundlePolicy(context, bundle);
    }

    /**
     * Is allowed manage (create, remove, edit) bundle's policies in the
     * current context?
     *
     * @param context the DSpace Context Object
     * @param bundle  the bundle that the policy refer to
     * @throws AuthorizeException if authorization error
     *                            if the current context (current user) is not allowed to
     *                            manage the bundle's policies
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageBundlePolicy(Context context,
                                                   Bundle bundle) throws AuthorizeException, SQLException {
        Item item = bundle.getItems().get(0);
        authorizeManageItemPolicy(context, item);
    }

    /**
     * Is allowed manage (create, remove, edit) item's policies in the
     * current context?
     *
     * @param context the DSpace Context Object
     * @param item    the item that the policy refer to
     * @throws AuthorizeException if authorization error
     *                            if the current context (current user) is not allowed to
     *                            manage the item's policies
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageItemPolicy(Context context, Item item)
        throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canItemAdminManagePolicies()) {
            AuthorizeServiceFactory.getInstance().getAuthorizeService().authorizeAction(context, item, Constants.ADMIN);
        } else if (AuthorizeConfiguration.canCollectionAdminManageItemPolicies()) {
            authorizeService.authorizeAction(context, item
                .getOwningCollection(), Constants.ADMIN);
        } else if (AuthorizeConfiguration.canCommunityAdminManageItemPolicies()) {
            authorizeService
                .authorizeAction(context, item.getOwningCollection()
                                              .getCommunities().get(0), Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage item policies");
        }
    }

    /**
     * Is allowed manage (create, remove, edit) collection's policies in the
     * current context?
     *
     * @param context    the DSpace Context Object
     * @param collection the collection that the policy refer to
     * @throws AuthorizeException if authorization error
     *                            if the current context (current user) is not allowed to
     *                            manage the collection's policies
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageCollectionPolicy(Context context,
                                                       Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCollectionAdminManagePolicies()) {
            authorizeService.authorizeAction(context, collection,
                                             Constants.ADMIN);
        } else if (AuthorizeConfiguration
            .canCommunityAdminManageCollectionPolicies()) {
            authorizeService.authorizeAction(context, collection
                .getCommunities().get(0), Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage collection policies");
        }
    }

    /**
     * Is allowed manage (create, remove, edit) community's policies in the
     * current context?
     *
     * @param context   the DSpace Context Object
     * @param community the community that the policy refer to
     * @throws AuthorizeException if authorization error
     *                            if the current context (current user) is not allowed to
     *                            manage the community's policies
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageCommunityPolicy(Context context,
                                                      Community community) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCommunityAdminManagePolicies()) {
            authorizeService.authorizeAction(context, community,
                                             Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage community policies");
        }
    }

    /**
     * Throw an AuthorizeException if the current user is not a System Admin
     *
     * @param context the DSpace Context Object
     * @throws AuthorizeException if authorization error
     *                            if the current user is not a System Admin
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void requireAdminRole(Context context)
        throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to perform this action");
        }
    }

    /**
     * Is the current user allowed to manage (add, remove, replace) the item's
     * CC License
     *
     * @param context the DSpace Context Object
     * @param item    the item that the CC License refer to
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to
     *                            manage the item's CC License
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageCCLicense(Context context, Item item)
        throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        ItemService itemService = ContentServiceFactory.getInstance().getItemService();
        try {
            authorizeService.authorizeAction(context, item, Constants.ADD, false);
            authorizeService.authorizeAction(context, item, Constants.REMOVE, false);
        } catch (AuthorizeException authex) {
            if (AuthorizeConfiguration.canItemAdminManageCCLicense()) {
                authorizeService
                    .authorizeAction(context, item, Constants.ADMIN);
            } else if (AuthorizeConfiguration.canCollectionAdminManageCCLicense()) {
                authorizeService.authorizeAction(context, itemService
                    .getParentObject(context, item), Constants.ADMIN);
            } else if (AuthorizeConfiguration.canCommunityAdminManageCCLicense()) {
                Collection collection = (Collection) itemService
                    .getParentObject(context, item);
                authorizeService.authorizeAction(context, collectionService.getParentObject(context, collection),
                        Constants.ADMIN);
            } else {
                requireAdminRole(context);
            }
        }
    }

    /**
     * Is the current user allowed to manage (create, remove, edit) the
     * collection's template item?
     *
     * @param context    the DSpace Context Object
     * @param collection the collection
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to manage the collection's
     *                            template item
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageTemplateItem(Context context,
                                                   Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        CollectionService collectionService = ContentServiceFactory.getInstance().getCollectionService();
        boolean isAuthorized = collectionService.canEditBoolean(context, collection, false);

        if (!isAuthorized
            && AuthorizeConfiguration
            .canCollectionAdminManageTemplateItem()) {
            authorizeService.authorizeAction(context, collection,
                                             Constants.ADMIN);
        } else if (!isAuthorized
            && AuthorizeConfiguration
            .canCommunityAdminManageCollectionTemplateItem()) {
            List<Community> communities = collection.getCommunities();
            Community parent = communities != null && communities.size() > 0 ? communities.get(0)
                : null;
            authorizeService.authorizeAction(context, parent, Constants.ADMIN);
        } else if (!isAuthorized && !authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "You are not authorized to create a template item for the collection");
        }
    }

    /**
     * Can the current user manage (create, remove, edit) the submitters group of
     * the collection?
     *
     * @param context    the DSpace Context Object
     * @param collection the collection
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to manage the collection's
     *                            submitters group
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageSubmittersGroup(Context context,
                                                      Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCollectionAdminManageSubmitters()) {
            authorizeService.authorizeAction(context, collection,
                                             Constants.ADMIN);
        } else if (AuthorizeConfiguration
            .canCommunityAdminManageCollectionSubmitters()) {
            authorizeService.authorizeAction(context, collection
                .getCommunities().get(0), Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage collection submitters");
        }
    }

    /**
     * Can the current user manage (create, remove, edit) the workflow groups of
     * the collection?
     *
     * @param context    the DSpace Context Object
     * @param collection the collection
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to manage the collection's
     *                            workflow groups
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManageWorkflowsGroup(Context context,
                                                     Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCollectionAdminManageWorkflows()) {
            authorizeService.authorizeAction(context, collection,
                                             Constants.ADMIN);
        } else if (AuthorizeConfiguration
            .canCommunityAdminManageCollectionWorkflows()) {
            authorizeService.authorizeAction(context, collection
                .getCommunities().get(0), Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage collection workflow");
        }
    }

    /**
     * Can the current user create/edit the admins group of the collection?
     * please note that the remove action need a separate check
     *
     * @param context    the DSpace Context Object
     * @param collection the collection
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to create/edit the
     *                            collection's admins group
     * @throws SQLException       if database error
     *                            if a db error occur
     * @see #authorizeRemoveAdminGroup(Context, Collection)
     */
    public static void authorizeManageAdminGroup(Context context,
                                                 Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCollectionAdminManageAdminGroup()) {
            authorizeService.authorizeAction(context, collection,
                                             Constants.ADMIN);
        } else if (AuthorizeConfiguration
            .canCommunityAdminManageCollectionAdminGroup()) {
            authorizeService.authorizeAction(context, collection
                .getCommunities().get(0), Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage collection admin");
        }
    }

    /**
     * Can the current user remove the admins group of the collection?
     * please note that the create/edit actions need separate check
     *
     * @param context    the DSpace Context Object
     * @param collection the collection
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to remove the
     *                            collection's admins group
     * @throws SQLException       if database error
     *                            if a db error occur
     * @see #authorizeManageAdminGroup(Context, Collection)
     */
    public static void authorizeRemoveAdminGroup(Context context,
                                                 Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        List<Community> parentCommunities = collection.getCommunities();
        if (AuthorizeConfiguration
            .canCommunityAdminManageCollectionAdminGroup()
            && parentCommunities != null && parentCommunities.size() > 0) {
            authorizeService.authorizeAction(context, collection
                .getCommunities().get(0), Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin can remove the admin group of a collection");
        }
    }

    /**
     * Can the current user create/edit the admins group of the community?
     * please note that the remove action need a separate check
     *
     * @param context   the DSpace Context Object
     * @param community the community
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to create/edit the
     *                            community's admins group
     * @throws SQLException       if database error
     *                            if a db error occur
     * @see #authorizeRemoveAdminGroup(Context, Collection)
     */
    public static void authorizeManageAdminGroup(Context context,
                                                 Community community) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup()) {
            authorizeService.authorizeAction(context, community,
                                             Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin are allowed to manage community admin");
        }
    }

    /**
     * Can the current user remove the admins group of the community?
     * please note that the create/edit actions need separate check
     *
     * @param context   the DSpace Context Object
     * @param community the community
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to remove the
     *                            collection's admins group
     * @throws SQLException       if database error
     *                            if a db error occur
     * @see #authorizeManageAdminGroup(Context, Community)
     */
    public static void authorizeRemoveAdminGroup(Context context,
                                                 Community community) throws SQLException, AuthorizeException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        List<Community> parentCommunities = community.getParentCommunities();
        Community parentCommunity = null;
        if (0 < parentCommunities.size()) {
            parentCommunity = parentCommunities.get(0);
        }
        if (AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
            && parentCommunity != null) {
            authorizeService.authorizeAction(context, parentCommunity,
                                             Constants.ADMIN);
        } else if (!authorizeService.isAdmin(context)) {
            throw new AuthorizeException(
                "Only system admin can remove the admin group of the community");
        }
    }

    /**
     * Can the current user remove or edit the supplied policy?
     *
     * @param c  the DSpace Context Object
     * @param rp a resource policy
     * @throws AuthorizeException if authorization error
     *                            if the current context (current user) is not allowed to
     *                            remove/edit the policy
     * @throws SQLException       if database error
     *                            if a db error occur
     */
    public static void authorizeManagePolicy(Context c, ResourcePolicy rp)
        throws SQLException, AuthorizeException {
        switch (rp.getdSpaceObject().getType()) {
            case Constants.BITSTREAM:
                authorizeManageBitstreamPolicy(c, (Bitstream) rp.getdSpaceObject());
                break;
            case Constants.BUNDLE:
                authorizeManageBundlePolicy(c, (Bundle) rp.getdSpaceObject());
                break;

            case Constants.ITEM:
                authorizeManageItemPolicy(c, (Item) rp.getdSpaceObject());
                break;
            case Constants.COLLECTION:
                authorizeManageCollectionPolicy(c, (Collection) rp.getdSpaceObject());
                break;
            case Constants.COMMUNITY:
                authorizeManageCommunityPolicy(c, (Community) rp.getdSpaceObject());
                break;

            default:
                requireAdminRole(c);
                break;
        }
    }

    /**
     * Can the current user withdraw the item?
     *
     * @param context the DSpace Context Object
     * @param item    the item
     * @throws SQLException       if database error
     *                            if a db error occur
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to perform the item
     *                            withdraw
     */
    public static void authorizeWithdrawItem(Context context, Item item)
        throws SQLException, AuthorizeException {
        boolean authorized = false;
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        if (AuthorizeConfiguration.canCollectionAdminPerformItemWithdrawn()) {
            authorized = authorizeService.authorizeActionBoolean(context, item
                .getOwningCollection(), Constants.ADMIN);
        } else if (AuthorizeConfiguration.canCommunityAdminPerformItemWithdrawn()) {
            authorized = authorizeService
                .authorizeActionBoolean(context, item.getOwningCollection()
                                                     .getCommunities().get(0), Constants.ADMIN);
        }

        if (!authorized) {
            authorized = authorizeService.authorizeActionBoolean(context, item
                .getOwningCollection(), Constants.REMOVE, false);
        }

        // authorized
        if (!authorized) {
            throw new AuthorizeException(
                "To withdraw item must be COLLECTION_ADMIN or have REMOVE authorization on owning Collection");
        }
    }

    /**
     * Can the current user reinstate the item?
     *
     * @param context the DSpace Context Object
     * @param item    the item
     * @throws SQLException       if database error
     *                            if a db error occur
     * @throws AuthorizeException if authorization error
     *                            if the current user is not allowed to perform the item
     *                            reinstatement
     */
    public static void authorizeReinstateItem(Context context, Item item)
        throws SQLException, AuthorizeException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        List<Collection> colls = item.getCollections();

        for (Collection coll : colls) {
            if (!AuthorizeConfiguration
                .canCollectionAdminPerformItemReinstatiate()) {
                if (AuthorizeConfiguration
                    .canCommunityAdminPerformItemReinstatiate()
                    && authorizeService.authorizeActionBoolean(context,
                                                               coll.getCommunities().get(0), Constants.ADMIN)) {
                    // authorized
                } else {
                    authorizeService.authorizeAction(context, coll,
                                                     Constants.ADD, false);
                }
            } else {
                authorizeService.authorizeAction(context, coll,
                                                 Constants.ADD);
            }
        }
    }

    /**
     * This method will check whether the current user is authorized to manage the default read group
     * @param context       The relevant DSpace context
     * @param collection    The collection for which this will be checked
     * @throws AuthorizeException   If something goes wrong
     * @throws SQLException If something goes wrong
     */
    public static void authorizeManageDefaultReadGroup(Context context,
                                                      Collection collection) throws AuthorizeException, SQLException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        authorizeService.authorizeAction(context, collection, Constants.ADMIN);
    }

    /**
     * This method checks whether the current user has sufficient rights to modify the group.
     * Depending on the kind of group and due to delegated administration, separate checks need to be done to verify
     * whether the user is allowed to modify the group.
     *
     * @param context the context of which the user will be checked
     * @param group   the group to be checked
     * @throws SQLException
     * @throws AuthorizeException
     */
    public static void authorizeManageGroup(Context context, Group group) throws SQLException, AuthorizeException {
        AuthorizeService authorizeService = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
        CollectionRoleService collectionRoleService = XmlWorkflowServiceFactory.getInstance()
                                                                               .getCollectionRoleService();
        if (authorizeService.isAdmin(context)) {
            return;
        }

        DSpaceObject parentObject = groupService.getParentObject(context, group);
        if (parentObject == null) {
            throw new AuthorizeException("not authorized to manage this group");
        }
        if (parentObject.getType() == Constants.COLLECTION) {
            Collection collection = (Collection) parentObject;

            if (group.equals(collection.getSubmitters())) {
                authorizeManageSubmittersGroup(context, collection);
                return;
            }


            List<CollectionRole> collectionRoles = collectionRoleService.findByCollection(context, collection);
            for (CollectionRole role : collectionRoles) {
                if (group.equals(role.getGroup())) {
                    authorizeManageWorkflowsGroup(context, collection);
                    return;
                }
            }

            if (group.equals(collection.getAdministrators())) {
                authorizeManageAdminGroup(context, collection);
                return;
            }
            // if we reach this point, it means that the group is related
            // to a collection but as it is not the submitters, nor the administrators,
            // nor a workflow groups it must be a default item/bitstream groups
            authorizeManageDefaultReadGroup(context, collection);
            return;
        }
        if (parentObject.getType() == Constants.COMMUNITY) {
            Community community = (Community) parentObject;
            authorizeManageAdminGroup(context, community);
            return;
        }

        throw new AuthorizeException("not authorized to manage this group");
    }

    /**
     * This method will return a boolean indicating whether the current user is allowed to register a new
     * account or not
     * @param context   The relevant DSpace context
     * @param request   The current request
     * @return          A boolean indicating whether the current user can register a new account or not
     * @throws SQLException If something goes wrong
     */
    public static boolean authorizeNewAccountRegistration(Context context, HttpServletRequest request)
        throws SQLException {
        if (DSpaceServicesFactory.getInstance().getConfigurationService()
                                 .getBooleanProperty("user.registration", true)) {
            // This allowSetPassword is currently the only mthod that would return true only when it's
            // actually expected to be returning true.
            // For example the LDAP canSelfRegister will return true due to auto-register, while that
            // does not imply a new user can register explicitly
            return AuthenticateServiceFactory.getInstance().getAuthenticationService()
                                             .allowSetPassword(context, request, null);
        }
        return false;
    }

    /**
     * This method will return a boolean indicating whether it's allowed to update the password for the EPerson
     * with the given email and canLogin property
     * @param context   The relevant DSpace context
     * @param email     The email to be checked
     * @return          A boolean indicating if the password can be updated or not
     */
    public static boolean authorizeUpdatePassword(Context context, String email) {
        try {
            EPerson eperson = EPersonServiceFactory.getInstance().getEPersonService().findByEmail(context, email);
            if (eperson != null && eperson.canLogIn()) {
                HttpServletRequest request = new DSpace().getRequestService().getCurrentRequest()
                                                         .getHttpServletRequest();
                return AuthenticateServiceFactory.getInstance().getAuthenticationService()
                                                 .allowSetPassword(context, request, null);
            }
        } catch (SQLException e) {
            log.error("Something went wrong trying to retrieve EPerson for email: " + email, e);
        }
        return false;
    }

    /**
     * This method checks if the community Admin can manage accounts
     *
     * @return true if is able
     */
    public static boolean canCommunityAdminManageAccounts() {
        boolean isAble = false;
        if (AuthorizeConfiguration.canCommunityAdminManagePolicies()
            || AuthorizeConfiguration.canCommunityAdminManageAdminGroup()
            || AuthorizeConfiguration.canCommunityAdminManageCollectionPolicies()
            || AuthorizeConfiguration.canCommunityAdminManageCollectionSubmitters()
            || AuthorizeConfiguration.canCommunityAdminManageCollectionWorkflows()
            || AuthorizeConfiguration.canCommunityAdminManageCollectionAdminGroup()) {
            isAble = true;
        }
        return isAble;
    }

    /**
     * This method checks if the Collection Admin can manage accounts
     *
     * @return true if is able
     */
    public static boolean canCollectionAdminManageAccounts() {
        boolean isAble = false;
        if (AuthorizeConfiguration.canCollectionAdminManagePolicies()
            || AuthorizeConfiguration.canCollectionAdminManageSubmitters()
            || AuthorizeConfiguration.canCollectionAdminManageWorkflows()
            || AuthorizeConfiguration.canCollectionAdminManageAdminGroup()) {
            isAble = true;
        }
        return isAble;
    }
}
