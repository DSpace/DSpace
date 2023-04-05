/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import static org.dspace.app.util.AuthorizeUtil.canCollectionAdminManageAccounts;
import static org.dspace.app.util.AuthorizeUtil.canCommunityAdminManageAccounts;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableCollection;
import org.dspace.discovery.indexobject.IndexableCommunity;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.service.GroupService;
import org.dspace.workflow.WorkflowItemService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * AuthorizeManager handles all authorization checks for DSpace. For better
 * security, DSpace assumes that you do not have the right to do something
 * unless that permission is spelled out somewhere. That "somewhere" is the
 * ResourcePolicy table. The AuthorizeManager is given a user, an object, and an
 * action, and it then does a lookup in the ResourcePolicy table to see if there
 * are any policies giving the user permission to do that action.
 * <p>
 * ResourcePolicies now apply to single objects (such as submit (ADD) permission
 * to a collection.)
 * <p>
 * Note: If an eperson is a member of the administrator group (id 1), then they
 * are automatically given permission for all requests another special group is
 * group 0, which is anonymous - all EPeople are members of group 0.
 */
public class AuthorizeServiceImpl implements AuthorizeService {

    private static final Logger log = LogManager.getLogger();

    @Autowired(required = true)
    protected BitstreamService bitstreamService;
    @Autowired(required = true)
    protected ContentServiceFactory serviceFactory;
    @Autowired(required = true)
    protected GroupService groupService;
    @Autowired(required = true)
    protected ResourcePolicyService resourcePolicyService;
    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;
    @Autowired(required = true)
    protected WorkflowItemService workflowItemService;
    @Autowired(required = true)
    private SearchService searchService;


    protected AuthorizeServiceImpl() {

    }

    @Override
    public void authorizeAnyOf(Context c, DSpaceObject o, int[] actions) throws AuthorizeException, SQLException {
        AuthorizeException ex = null;

        for (int action : actions) {
            try {
                authorizeAction(c, o, action);

                return;
            } catch (AuthorizeException e) {
                if (ex == null) {
                    ex = e;
                }
            }
        }

        throw ex;
    }

    @Override
    public void authorizeAction(Context c, DSpaceObject o, int action)
        throws AuthorizeException, SQLException {
        authorizeAction(c, o, action, true);
    }

    @Override
    public void authorizeAction(Context c, DSpaceObject o, int action, boolean useInheritance)
        throws AuthorizeException, SQLException {
        authorizeAction(c, c.getCurrentUser(), o, action, useInheritance);
    }

    @Override
    public void authorizeAction(Context c, EPerson e, DSpaceObject o, int action, boolean useInheritance)
        throws AuthorizeException, SQLException {
        if (o == null) {
            // action can be -1 due to a null entry
            String actionText;

            if (action == -1) {
                actionText = "null";
            } else {
                actionText = Constants.actionText[action];
            }

            UUID userid;

            if (e == null) {
                userid = null;
            } else {
                userid = e.getID();
            }

            throw new AuthorizeException(
                "Authorization attempted on null DSpace object "
                    + actionText + " by user " + userid);
        }

        if (!authorize(c, o, action, e, useInheritance)) {
            // denied, assemble and throw exception
            int otype = o.getType();
            UUID oid = o.getID();
            UUID userid;

            if (e == null) {
                userid = null;
            } else {
                userid = e.getID();
            }

            //            AuthorizeException j = new AuthorizeException("Denied");
            //            j.printStackTrace();
            // action can be -1 due to a null entry
            String actionText;

            if (action == -1) {
                actionText = "null";
            } else {
                actionText = Constants.actionText[action];
            }

            throw new AuthorizeException("Authorization denied for action "
                                             + actionText + " on " + Constants.typeText[otype] + ":"
                                             + oid + " by user " + userid, o, action);
        }
    }

    @Override
    public boolean authorizeActionBoolean(Context c, DSpaceObject o, int a) throws SQLException {
        return authorizeActionBoolean(c, o, a, true);
    }

    @Override
    public boolean authorizeActionBoolean(Context c, DSpaceObject o, int a, boolean useInheritance)
        throws SQLException {
        boolean isAuthorized = true;

        if (o == null) {
            return false;
        }

        try {
            authorizeAction(c, o, a, useInheritance);
        } catch (AuthorizeException e) {
            isAuthorized = false;
        }

        return isAuthorized;
    }

    @Override
    public boolean authorizeActionBoolean(Context c, EPerson e, DSpaceObject o, int a, boolean useInheritance)
        throws SQLException {
        boolean isAuthorized = true;

        if (o == null) {
            return false;
        }

        try {
            authorizeAction(c, e, o, a, useInheritance);
        } catch (AuthorizeException ex) {
            isAuthorized = false;
        }

        return isAuthorized;
    }

    /**
     * Check to see if the given user can perform the given action on the given
     * object. Always returns true if the ignore authorization flat is set in
     * the current context.
     *
     * @param c              current context. User is irrelevant; "ignore authorization"
     *                       flag is relevant
     * @param o              object action is being attempted on
     * @param action         ID of action being attempted, from
     *                       <code>org.dspace.core.Constants</code>
     * @param e              user attempting action
     * @param useInheritance flag to say if ADMIN action on the current object or parent
     *                       object can be used
     * @return <code>true</code> if user is authorized to perform the given
     * action, <code>false</code> otherwise
     * @throws SQLException if database error
     */
    protected boolean authorize(Context c, DSpaceObject o, int action, EPerson e, boolean useInheritance)
        throws SQLException {
        // return FALSE if there is no DSpaceObject
        if (o == null) {
            return false;
        }

        // is authorization disabled for this context?
        if (c.ignoreAuthorization()) {
            return true;
        }

        // If authorization was given before and cached
        Boolean cachedResult = c.getCachedAuthorizationResult(o, action, e);
        if (cachedResult != null) {
            return cachedResult;
        }

        // is eperson set? if not, userToCheck = null (anonymous)
        EPerson userToCheck = null;
        if (e != null) {
            userToCheck = e;

            // perform immediately isAdmin check as this is cheap
            if (isAdmin(c, e)) {
                return true;
            }
        }

        // In case the dso is an bundle or bitstream we must ignore custom
        // policies if it does not belong to at least one installed item (see
        // DS-2614).
        // In case the dso is an item and a corresponding workspace or workflow
        // item exist, we have to ignore custom policies (see DS-2614).
        boolean ignoreCustomPolicies = false;
        if (o instanceof Bitstream) {
            Bitstream b = (Bitstream) o;

            // Ensure that this is not a collection or community logo
            DSpaceObject parent = bitstreamService.getParentObject(c, b);
            if (!(parent instanceof Collection) && !(parent instanceof Community)) {
                ignoreCustomPolicies = !isAnyItemInstalled(c, b.getBundles());
            }
        }
        if (o instanceof Bundle) {
            ignoreCustomPolicies = !isAnyItemInstalled(c, Arrays.asList(((Bundle) o)));
        }
        if (o instanceof Item) {
            // the isArchived check is fast and would exclude the possibility that the item
            // is a workspace or workflow without further queries
            if (!((Item) o).isArchived() &&
                    (workspaceItemService.findByItem(c, (Item) o) != null ||
                    workflowItemService.findByItem(c, (Item) o) != null)) {
                ignoreCustomPolicies = true;
            }
        }

        for (ResourcePolicy rp : getPoliciesActionFilter(c, o, action)) {

            if (ignoreCustomPolicies
                && ResourcePolicy.TYPE_CUSTOM.equals(rp.getRpType())) {
                if (c.isReadOnly()) {
                    //When we are in read-only mode, we will cache authorized actions in a different way
                    //So we remove this resource policy from the cache.
                    c.uncacheEntity(rp);
                }
                continue;
            }

            // check policies for date validity
            if (resourcePolicyService.isDateValid(rp)) {
                if (rp.getEPerson() != null && rp.getEPerson().equals(userToCheck)) {
                    c.cacheAuthorizedAction(o, action, e, true, rp);
                    return true; // match
                }

                if ((rp.getGroup() != null)
                    && groupService.isMember(c, e, rp.getGroup())) {
                    // group was set, and eperson is a member
                    // of that group
                    c.cacheAuthorizedAction(o, action, e, true, rp);
                    return true;
                }
            }

            if (c.isReadOnly()) {
                //When we are in read-only mode, we will cache authorized actions in a different way
                //So we remove this resource policy from the cache.
                c.uncacheEntity(rp);
            }
        }

        if (e != null) {
            // if user is an Admin on this object
            DSpaceObject adminObject = useInheritance ? serviceFactory.getDSpaceObjectService(o)
                                                                      .getAdminObject(c, o, action) : null;

            if (isAdmin(c, e, adminObject)) {
                c.cacheAuthorizedAction(o, action, e, true, null);
                return true;
            }
        }
        // default authorization is denial
        c.cacheAuthorizedAction(o, action, e, false, null);
        return false;
    }

    // check whether any bundle belongs to any item that passed submission
    // and workflow process
    protected boolean isAnyItemInstalled(Context ctx, List<Bundle> bundles)
        throws SQLException {
        for (Bundle bundle : bundles) {
            for (Item item : bundle.getItems()) {
                if (workspaceItemService.findByItem(ctx, item) == null
                    && workflowItemService.findByItem(ctx, item) == null) {
                    return true;
                }
            }
        }
        return false;
    }


    ///////////////////////////////////////////////
    // admin check methods
    ///////////////////////////////////////////////

    @Override
    public boolean isAdmin(Context c, DSpaceObject o) throws SQLException {
        return this.isAdmin(c, c.getCurrentUser(), o);
    }

    @Override
    public boolean isAdmin(Context c, EPerson e, DSpaceObject o) throws SQLException {
        // return true if user is an Administrator
        if (isAdmin(c, e)) {
            return true;
        }

        if (o == null) {
            return false;
        }

        Boolean cachedResult = c.getCachedAuthorizationResult(o, Constants.ADMIN, e);
        if (cachedResult != null) {
            return cachedResult;
        }

        //
        // First, check all Resource Policies directly on this object
        //
        List<ResourcePolicy> policies = getPoliciesActionFilter(c, o, Constants.ADMIN);

        for (ResourcePolicy rp : policies) {
            // check policies for date validity
            if (resourcePolicyService.isDateValid(rp)) {
                if (rp.getEPerson() != null && rp.getEPerson().equals(e)) {
                    c.cacheAuthorizedAction(o, Constants.ADMIN, e, true, rp);
                    return true; // match
                }

                if ((rp.getGroup() != null)
                    && groupService.isMember(c, e, rp.getGroup())) {
                    // group was set, and eperson is a member
                    // of that group
                    c.cacheAuthorizedAction(o, Constants.ADMIN, e, true, rp);
                    return true;
                }
            }

            if (c.isReadOnly()) {
                //When we are in read-only mode, we will cache authorized actions in a different way
                //So we remove this resource policy from the cache.
                c.uncacheEntity(rp);
            }
        }

        // If user doesn't have specific Admin permissions on this object,
        // check the *parent* objects of this object.  This allows Admin
        // permissions to be inherited automatically (e.g. Admin on Community
        // is also an Admin of all Collections/Items in that Community)
        DSpaceObject parent = serviceFactory.getDSpaceObjectService(o).getParentObject(c, o);
        if (parent != null) {
            boolean admin = isAdmin(c, e, parent);
            c.cacheAuthorizedAction(o, Constants.ADMIN, e, admin, null);
            return admin;
        }

        c.cacheAuthorizedAction(o, Constants.ADMIN, e, false, null);
        return false;
    }

    @Override
    public boolean isAdmin(Context c) throws SQLException {
        // if we're ignoring authorization, user is member of admin
        if (c.ignoreAuthorization()) {
            return true;
        }

        EPerson e = c.getCurrentUser();

        if (e == null) {
            return false; // anonymous users can't be admins....
        } else {
            return groupService.isMember(c, Group.ADMIN);
        }
    }

    @Override
    public boolean isAdmin(Context c, EPerson e) throws SQLException {
        // if we're ignoring authorization, user is member of admin
        if (c.ignoreAuthorization()) {
            return true;
        }

        if (e == null) {
            return false; // anonymous users can't be admins....
        } else {
            return groupService.isMember(c, e, Group.ADMIN);
        }
    }

    ///////////////////////////////////////////////
    // policy manipulation methods
    ///////////////////////////////////////////////

    @Override
    public void addPolicy(Context c, DSpaceObject o, int actionID,
                          EPerson e) throws SQLException, AuthorizeException {
        addPolicy(c, o, actionID, e, null);
    }

    @Override
    public void addPolicy(Context context, DSpaceObject o, int actionID,
                          EPerson e, String type) throws SQLException, AuthorizeException {
        createResourcePolicy(context, o, null, e, actionID, type);
    }

    @Override
    public void addPolicy(Context c, DSpaceObject o, int actionID,
                          Group g) throws SQLException, AuthorizeException {
        createResourcePolicy(c, o, g, null, actionID, null);
    }

    @Override
    public void addPolicy(Context c, DSpaceObject o, int actionID,
                          Group g, String type) throws SQLException, AuthorizeException {
        createResourcePolicy(c, o, g, null, actionID, type);
    }

    @Override
    public List<ResourcePolicy> getPolicies(Context c, DSpaceObject o)
        throws SQLException {
        return resourcePolicyService.find(c, o);
    }

    @Override
    public List<ResourcePolicy> findPoliciesByDSOAndType(Context c, DSpaceObject o, String type)
        throws SQLException {
        return resourcePolicyService.find(c, o, type);
    }

    @Override
    public List<ResourcePolicy> getPoliciesForGroup(Context c, Group g)
        throws SQLException {
        return resourcePolicyService.find(c, g);
    }

    @Override
    public List<ResourcePolicy> getPoliciesActionFilter(Context c, DSpaceObject o,
                                                        int actionID) throws SQLException {
        return resourcePolicyService.find(c, o, actionID);
    }

    @Override
    public void inheritPolicies(Context c, DSpaceObject src,
                                DSpaceObject dest) throws SQLException, AuthorizeException {
        // find all policies for the source object
        List<ResourcePolicy> policies = getPolicies(c, src);

        //Only inherit non-ADMIN policies (since ADMIN policies are automatically inherited)
        //and non-custom policies as these are manually applied when appropriate
        List<ResourcePolicy> nonAdminPolicies = new ArrayList<>();
        for (ResourcePolicy rp : policies) {
            if (rp.getAction() != Constants.ADMIN && !StringUtils.equals(rp.getRpType(), ResourcePolicy.TYPE_CUSTOM)) {
                nonAdminPolicies.add(rp);
            }
        }
        addPolicies(c, nonAdminPolicies, dest);
    }

    @Override
    public void switchPoliciesAction(Context context, DSpaceObject dso, int fromAction, int toAction)
        throws SQLException, AuthorizeException {
        List<ResourcePolicy> rps = getPoliciesActionFilter(context, dso, fromAction);
        for (ResourcePolicy rp : rps) {
            rp.setAction(toAction);
        }
        resourcePolicyService.update(context, rps);
    }

    @Override
    public void addPolicies(Context c, List<ResourcePolicy> policies, DSpaceObject dest)
        throws SQLException, AuthorizeException {
        // now add them to the destination object
        List<ResourcePolicy> newPolicies = new ArrayList<>(policies.size());

        for (ResourcePolicy srp : policies) {
            ResourcePolicy rp = resourcePolicyService.create(c);

            // copy over values
            rp.setdSpaceObject(dest);
            rp.setAction(srp.getAction());
            rp.setEPerson(srp.getEPerson());
            rp.setGroup(srp.getGroup());
            rp.setStartDate(srp.getStartDate());
            rp.setEndDate(srp.getEndDate());
            rp.setRpName(srp.getRpName());
            rp.setRpDescription(srp.getRpDescription());
            rp.setRpType(srp.getRpType());

            // and add policy to list of new policies
            newPolicies.add(rp);
        }

        resourcePolicyService.update(c, newPolicies);
    }

    @Override
    public void removeAllPolicies(Context c, DSpaceObject o) throws SQLException, AuthorizeException {
        resourcePolicyService.removeAllPolicies(c, o);
    }

    @Override
    public void removeAllPoliciesByDSOAndTypeNotEqualsTo(Context c, DSpaceObject o, String type)
        throws SQLException, AuthorizeException {
        resourcePolicyService.removeDsoAndTypeNotEqualsToPolicies(c, o, type);
    }

    @Override
    public void removeAllPoliciesByDSOAndType(Context c, DSpaceObject o, String type)
        throws SQLException, AuthorizeException {
        resourcePolicyService.removePolicies(c, o, type);
    }

    @Override
    public void removePoliciesActionFilter(Context context, DSpaceObject dso, int actionID)
        throws SQLException, AuthorizeException {
        resourcePolicyService.removePolicies(context, dso, actionID);
    }

    @Override
    public void removeGroupPolicies(Context c, Group group)
        throws SQLException {
        resourcePolicyService.removeGroupPolicies(c, group);
    }

    @Override
    public void removeGroupPolicies(Context c, DSpaceObject o, Group g)
        throws SQLException, AuthorizeException {
        resourcePolicyService.removeDsoGroupPolicies(c, o, g);
    }

    @Override
    public void removeEPersonPolicies(Context c, DSpaceObject o, EPerson e)
        throws SQLException, AuthorizeException {
        resourcePolicyService.removeDsoEPersonPolicies(c, o, e);
    }

    @Override
    public void removeAllEPersonPolicies(Context c, EPerson e)
        throws SQLException, AuthorizeException {
        resourcePolicyService.removeAllEPersonPolicies(c, e);
    }

    @Override
    public List<Group> getAuthorizedGroups(Context c, DSpaceObject o,
                                           int actionID) throws java.sql.SQLException {
        List<ResourcePolicy> policies = getPoliciesActionFilter(c, o, actionID);

        List<Group> groups = new ArrayList<>();
        for (ResourcePolicy resourcePolicy : policies) {
            if (resourcePolicy.getGroup() != null && resourcePolicyService.isDateValid(resourcePolicy)) {
                groups.add(resourcePolicy.getGroup());
            }
        }
        return groups;
    }


    @Override
    public boolean isAnIdenticalPolicyAlreadyInPlace(Context c, DSpaceObject o, ResourcePolicy rp) throws SQLException {
        return isAnIdenticalPolicyAlreadyInPlace(c, o, rp.getGroup(), rp.getAction(), rp.getID());
    }

    @Override
    public boolean isAnIdenticalPolicyAlreadyInPlace(Context c, DSpaceObject dso, Group group, int action, int policyID)
        throws SQLException {
        return !resourcePolicyService.findByTypeGroupActionExceptId(c, dso, group, action, policyID).isEmpty();
    }

    @Override
    public ResourcePolicy findByTypeGroupAction(Context c, DSpaceObject dso, Group group, int action)
        throws SQLException {
        List<ResourcePolicy> policies = resourcePolicyService.find(c, dso, group, action);

        if (CollectionUtils.isNotEmpty(policies)) {
            return policies.iterator().next();
        } else {
            return null;
        }
    }

    /**
     * Generate Policies policies READ for the date in input adding reason. New policies are assigned automatically
     * at the groups that
     * have right on the collection. E.g., if the anonymous can access the collection policies are assigned to
     * anonymous.
     *
     * @param context          The relevant DSpace Context.
     * @param embargoDate      embargo end date
     * @param reason           embargo reason
     * @param dso              DSpace object
     * @param owningCollection collection to get group policies from
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    @Override
    public void generateAutomaticPolicies(Context context, Date embargoDate,
                                          String reason, DSpaceObject dso, Collection owningCollection)
        throws SQLException, AuthorizeException {

        if (embargoDate != null || (embargoDate == null && dso instanceof Bitstream)) {

            List<Group> authorizedGroups = getAuthorizedGroups(context, owningCollection, Constants.DEFAULT_ITEM_READ);

            removeAllPoliciesByDSOAndType(context, dso, ResourcePolicy.TYPE_CUSTOM);

            // look for anonymous
            boolean isAnonymousInPlace = false;
            for (Group g : authorizedGroups) {
                if (StringUtils.equals(g.getName(), Group.ANONYMOUS)) {
                    isAnonymousInPlace = true;
                }
            }
            if (!isAnonymousInPlace) {
                // add policies for all the groups
                for (Group g : authorizedGroups) {
                    ResourcePolicy rp = createOrModifyPolicy(null, context, null, g, null, embargoDate, Constants.READ,
                                                             reason, dso);
                    if (rp != null) {
                        resourcePolicyService.update(context, rp);
                    }
                }

            } else {
                // add policy just for anonymous
                ResourcePolicy rp = createOrModifyPolicy(null, context, null,
                                                         groupService.findByName(context, Group.ANONYMOUS), null,
                                                         embargoDate, Constants.READ, reason, dso);
                if (rp != null) {
                    resourcePolicyService.update(context, rp);
                }
            }
        }
    }

    @Override
    public ResourcePolicy createResourcePolicy(Context context, DSpaceObject dso, Group group, EPerson eperson,
                                               int type, String rpType) throws SQLException, AuthorizeException {
        return createResourcePolicy(context, dso, group, eperson, type, rpType, null, null, null, null);
    }

    @Override
    public ResourcePolicy createResourcePolicy(Context context, DSpaceObject dso, Group group, EPerson eperson,
                                               int type, String rpType, String rpName, String rpDescription,
                                               Date startDate, Date endDate) throws SQLException, AuthorizeException {
        if (group == null && eperson == null) {
            throw new IllegalArgumentException(
                "We need at least an eperson or a group in order to create a resource policy.");
        }

        ResourcePolicy myPolicy = resourcePolicyService.create(context);
        myPolicy.setdSpaceObject(dso);
        myPolicy.setAction(type);
        myPolicy.setGroup(group);
        myPolicy.setEPerson(eperson);
        myPolicy.setRpType(rpType);
        myPolicy.setRpName(rpName);
        myPolicy.setRpDescription(rpDescription);
        myPolicy.setEndDate(endDate);
        myPolicy.setStartDate(startDate);
        resourcePolicyService.update(context, myPolicy);

        return myPolicy;
    }

    @Override
    public ResourcePolicy createOrModifyPolicy(ResourcePolicy policy, Context context, String name, Group group,
                                               EPerson ePerson,
                                               Date embargoDate, int action, String reason, DSpaceObject dso)
        throws AuthorizeException, SQLException {
        ResourcePolicy policyTemp = null;
        if (policy != null) {
            List<ResourcePolicy> duplicates = resourcePolicyService
                .findByTypeGroupActionExceptId(context, dso, group, action, policy.getID());
            if (!duplicates.isEmpty()) {
                policy = duplicates.get(0);
            }
        } else {
            // if an identical policy (same Action and same Group) is already in place modify it...
            policyTemp = findByTypeGroupAction(context, dso, group, action);
        }

        if (policyTemp != null) {
            policy = policyTemp;
            policy.setRpType(ResourcePolicy.TYPE_CUSTOM);
        }

        if (policy == null) {
            policy = createResourcePolicy(context, dso, group, ePerson, action, ResourcePolicy.TYPE_CUSTOM);
        }
        policy.setGroup(group);
        policy.setEPerson(ePerson);

        if (embargoDate != null) {
            policy.setStartDate(embargoDate);
        } else {
            policy.setStartDate(null);
            policy.setEndDate(null);
        }
        policy.setRpName(name);
        policy.setRpDescription(reason);
        return policy;
    }

    @Override
    public List<ResourcePolicy> getPoliciesActionFilterExceptRpType(Context c, DSpaceObject o, int actionID,
                                                                    String rpType) throws SQLException {
        return resourcePolicyService.findExceptRpType(c, o, actionID, rpType);
    }

    /**
     * Checks that the context's current user is a community admin in the site by querying the solr database.
     *
     * @param context   context with the current user
     * @return          true if the current user is a community admin in the site
     *                  false when this is not the case, or an exception occurred
     * @throws java.sql.SQLException passed through.
     */
    @Override
    public boolean isCommunityAdmin(Context context) throws SQLException {
        return performCheck(context, "search.resourcetype:" + IndexableCommunity.TYPE);
    }

    /**
     * Checks that the context's current user is a collection admin in the site by querying the solr database.
     *
     * @param context   context with the current user
     * @return          true if the current user is a collection admin in the site
     *                  false when this is not the case, or an exception occurred
     * @throws java.sql.SQLException passed through.
     */
    @Override
    public boolean isCollectionAdmin(Context context) throws SQLException {
        return performCheck(context, "search.resourcetype:" + IndexableCollection.TYPE);
    }

    /**
     * Checks that the context's current user is a community or collection admin in the site.
     *
     * @param context   context with the current user
     * @return          true if the current user is a community or collection admin in the site
     *                  false when this is not the case, or an exception occurred
     * @throws java.sql.SQLException passed through.
     */
    @Override
    public boolean isComColAdmin(Context context) throws SQLException {
        return performCheck(context,
            "(search.resourcetype:" + IndexableCommunity.TYPE + " OR search.resourcetype:" +
                IndexableCollection.TYPE + ")");
    }

    /**
     *  Finds communities for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @param offset    the offset for pagination
     * @param limit     the amount of dso's to return
     * @return          a list of communities for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public List<Community> findAdminAuthorizedCommunity(Context context, String query, int offset, int limit)
        throws SearchServiceException, SQLException {
        List<Community> communities = new ArrayList<>();
        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:" +
                                                              IndexableCommunity.TYPE,
            offset, limit);
        for (IndexableObject solrCollections : discoverResult.getIndexableObjects()) {
            Community community = ((IndexableCommunity) solrCollections).getIndexedObject();
            communities.add(community);
        }
        return communities;
    }

    /**
     *  Finds the amount of communities for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @return          the number of communities for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public long countAdminAuthorizedCommunity(Context context, String query)
        throws SearchServiceException, SQLException {
        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:" +
                                                              IndexableCommunity.TYPE,
            null, null);
        return discoverResult.getTotalSearchResults();
    }

    /**
     *  Finds collections for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @param offset    the offset for pagination
     * @param limit     the amount of dso's to return
     * @return          a list of collections for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public List<Collection> findAdminAuthorizedCollection(Context context, String query, int offset, int limit)
        throws SearchServiceException, SQLException {
        List<Collection> collections = new ArrayList<>();
        if (context.getCurrentUser() == null) {
            return collections;
        }

        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:" +
                                                              IndexableCollection.TYPE,
            offset, limit);
        for (IndexableObject solrCollections : discoverResult.getIndexableObjects()) {
            Collection collection = ((IndexableCollection) solrCollections).getIndexedObject();
            collections.add(collection);
        }
        return collections;
    }

    /**
     *  Finds the amount of collections for which the logged in user has ADMIN rights.
     *
     * @param context   the context whose user is checked against
     * @param query     the optional extra query
     * @return          the number of collections for which the logged in user has ADMIN rights.
     * @throws SearchServiceException
     */
    @Override
    public long countAdminAuthorizedCollection(Context context, String query)
        throws SearchServiceException, SQLException {
        query = formatCustomQuery(query);
        DiscoverResult discoverResult = getDiscoverResult(context, query + "search.resourcetype:" +
                                                              IndexableCollection.TYPE,
            null, null);
        return discoverResult.getTotalSearchResults();
    }

    @Override
    public boolean isAccountManager(Context context) {
        try {
            return (canCommunityAdminManageAccounts() && isCommunityAdmin(context)
                || canCollectionAdminManageAccounts() && isCollectionAdmin(context));
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean performCheck(Context context, String query) throws SQLException {
        if (context.getCurrentUser() == null) {
            return false;
        }

        try {
            DiscoverResult discoverResult = getDiscoverResult(context, query, null, null);
            if (discoverResult.getTotalSearchResults() > 0) {
                return true;
            }
        } catch (SearchServiceException e) {
            log.error("Failed getting getting community/collection admin status for "
                + context.getCurrentUser().getEmail() + " The search error is: " + e.getMessage()
                + " The search resourceType filter was: " + query);
        }
        return false;
    }

    private DiscoverResult getDiscoverResult(Context context, String query, Integer offset, Integer limit)
        throws SearchServiceException, SQLException {
        String groupQuery = getGroupToQuery(groupService.allMemberGroups(context, context.getCurrentUser()));

        DiscoverQuery discoverQuery = new DiscoverQuery();
        if (!this.isAdmin(context)) {
            query = query + " AND (" +
                "admin:e" + context.getCurrentUser().getID() + groupQuery + ")";
        }
        discoverQuery.setQuery(query);
        if (offset != null) {
            discoverQuery.setStart(offset);
        }
        if (limit != null) {
            discoverQuery.setMaxResults(limit);
        }


        return searchService.search(context, discoverQuery);
    }

    private String getGroupToQuery(List<Group> groups) {
        StringBuilder groupQuery = new StringBuilder();

        if (groups != null) {
            for (Group group: groups) {
                groupQuery.append(" OR admin:g");
                groupQuery.append(group.getID());
            }
        }

        return groupQuery.toString();
    }

    private String formatCustomQuery(String query) {
        if (StringUtils.isBlank(query)) {
            return "";
        } else {
            return query + " AND ";
        }
    }
}
