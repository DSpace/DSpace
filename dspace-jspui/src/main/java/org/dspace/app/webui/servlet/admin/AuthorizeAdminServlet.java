/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import org.apache.commons.lang.time.DateUtils;
import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.PolicySet;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.*;
import org.dspace.content.Collection;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.EPersonService;
import org.dspace.eperson.service.GroupService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

/**
 * Servlet for editing permissions
 * 
 * @author dstuve
 * @version $Revision$
 */
public class AuthorizeAdminServlet extends DSpaceServlet
{
	private final transient ItemService itemService
             = ContentServiceFactory.getInstance().getItemService();
	private final transient CollectionService collectionService
             = ContentServiceFactory.getInstance().getCollectionService();
	private final transient CommunityService communityService
             = ContentServiceFactory.getInstance().getCommunityService();
	private final transient BundleService bundleService
             = ContentServiceFactory.getInstance().getBundleService();
	private final transient BitstreamService bitstreamService
             = ContentServiceFactory.getInstance().getBitstreamService();
	private final transient GroupService groupService
             = EPersonServiceFactory.getInstance().getGroupService();
	private final transient EPersonService personService
             = EPersonServiceFactory.getInstance().getEPersonService();
    private final transient HandleService handleService
             = HandleServiceFactory.getInstance().getHandleService();
	private final transient ResourcePolicyService resourcePolicyService
             = AuthorizeServiceFactory.getInstance().getResourcePolicyService();
	
    @Override
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // handle gets and posts with the post method
        doDSPost(c, request, response);

        // show the main page (select communities, collections, items, etc)
        //        showMainPage(c, request, response);
    }

    @Override
    protected void doDSPost(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        String button = UIUtil.getSubmitButton(request, "submit");

        // check authorization!! the authorize servlet is available to all registred users
        // it is need because also item/collection/community admin could be
        // allowed to manage policies
        
        if (button.equals("submit_collection"))
        {
            // select a collection to work on
            List<Collection> collections = collectionService.findAll(c);

            request.setAttribute("collections", collections);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/collection-select.jsp");
        }
        else if (button.equals("submit_community"))
        {
            // select a community to work on
            List<Community> communities = communityService.findAll(c);

            request.setAttribute("communities", communities);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/community-select.jsp");
        }
        else if (button.equals("submit_advanced"))
        {
            // select a collections to work on
            List<Collection> collections = collectionService.findAll(c);
            List<Group> groups = groupService.findAll(c, null);

            request.setAttribute("collections", collections);
            request.setAttribute("groups", groups);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-advanced.jsp");
        }
        else if (button.equals("submit_item"))
        {
            // select an item to work on
            JSPManager.showJSP(request, response,
                    "/dspace-admin/item-select.jsp");
        }
        // ITEMS ////////////////////////////////////////////////////
        else if (button.equals("submit_item_select"))
        {
            Item item = null;

            UUID itemId = UIUtil.getUUIDParameter(request, "item_id");
            String handle = request.getParameter("handle");

            // if id is set, use it
            if (itemId != null)
            {
                item = itemService.find(c, itemId);
            }
            else if ((handle != null) && !handle.equals(""))
            {
                // otherwise, attempt to resolve handle
                DSpaceObject dso = handleService.resolveToObject(c, handle);

                // make sure it's an item
                if ((dso != null) && (dso.getType() == Constants.ITEM))
                {
                    item = (Item) dso;
                }
            }

            // no item set yet, failed ID & handle, ask user to try again
            if (item == null)
            {
                request.setAttribute("invalid.id", Boolean.TRUE);
                JSPManager.showJSP(request, response,
                        "/dspace-admin/item-select.jsp");
            }
            else
            {
                // show edit form!
                prepItemEditForm(c, request, item);

                JSPManager.showJSP(request, response,
                        "/dspace-admin/authorize-item-edit.jsp");
            }
        }
        else if (button.equals("submit_item_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Item item = itemService
                    .find(c, UIUtil.getUUIDParameter(request, "item_id"));

            AuthorizeUtil.authorizeManageItemPolicy(c, item);
			ResourcePolicy policy = authorizeService.createResourcePolicy(c, item,
					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);

            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to item permission page
            request.setAttribute("edit_title", "Item " + item.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "item_id");
            request.setAttribute("id", "" + item.getID());
            request.setAttribute("newpolicy", "true");

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_item_edit_policy"))
        {
            // edit an item's policy - set up and call policy editor
            Item item = itemService
                    .find(c, UIUtil.getUUIDParameter(request, "item_id"));

            AuthorizeUtil.authorizeManageItemPolicy(c, item);
            int policyId = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;

            policy = resourcePolicyService.find(c, policyId);

            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to collection permission page
            request.setAttribute("edit_title", "Item " + item.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "item_id");
            request.setAttribute("id", "" + item.getID());
            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_bundle_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Item item = itemService
                    .find(c, UIUtil.getUUIDParameter(request, "item_id"));
            Bundle bundle = bundleService.find(c, UIUtil.getUUIDParameter(request,
                    "bundle_id"));

            AuthorizeUtil.authorizeManageBundlePolicy(c, bundle);
			ResourcePolicy policy = authorizeService.createResourcePolicy(c, bundle,
					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);

            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to item permission page
            request.setAttribute("edit_title", "(Item, Bundle) = ("
                    + item.getID() + "," + bundle.getID() + ")");
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "item_id");
            request.setAttribute("id", "" + item.getID());
            request.setAttribute("newpolicy", "true");

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_bitstream_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Item item = itemService
                    .find(c, UIUtil.getUUIDParameter(request, "item_id"));
            Bitstream bitstream = bitstreamService.find(c, UIUtil.getUUIDParameter(
                    request, "bitstream_id"));

            AuthorizeUtil.authorizeManageBitstreamPolicy(c, bitstream);
			ResourcePolicy policy = authorizeService.createResourcePolicy(c, bitstream,
					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);

            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to item permission page
            request.setAttribute("edit_title", "(Item,Bitstream) = ("
                    + item.getID() + "," + bitstream.getID() + ")");
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "item_id");
            request.setAttribute("id", "" + item.getID());
            request.setAttribute("newpolicy", "true");

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_item_delete_policy"))
        {
            // delete a permwUtil.getIntParameter(request, "item_id"));
            
            ResourcePolicy policy = resourcePolicyService.find(c, UIUtil
                    .getIntParameter(request, "policy_id"));

            Item item = itemService
                    .find(c, UIUtil.getUUIDParameter(request, "item_id"));

			AuthorizeUtil.authorizeManageItemPolicy(c, item);
            
            // do the remove
            resourcePolicyService.delete(c, policy);

            // show edit form!
            prepItemEditForm(c, request, item);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-item-edit.jsp");
        }
        // COLLECTIONS ////////////////////////////////////////////////////////
        else if (button.equals("submit_collection_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Collection collection = collectionService.find(c, UIUtil.getUUIDParameter(
                    request, "collection_id"));

            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
			ResourcePolicy policy = authorizeService.createResourcePolicy(c, collection,
					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);


            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to collection permission page
            request.setAttribute("edit_title", "Collection "
                    + collection.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "collection_id");
            request.setAttribute("id", "" + collection.getID());
            request.setAttribute("newpolicy", "true");

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_community_select"))
        {
            // edit the collection's permissions
            Community target = communityService.find(c, UIUtil.getUUIDParameter(
                    request, "community_id"));
            List<ResourcePolicy> policies = authorizeService.getPolicies(c, target);

            request.setAttribute("community", target);
            request.setAttribute("policies", policies);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-community-edit.jsp");
        }
        else if (button.equals("submit_collection_delete_policy"))
        {
            // delete a permission from a collection
            Collection collection = collectionService.find(c, UIUtil.getUUIDParameter(
                    request, "collection_id"));
            
            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
            ResourcePolicy policy = resourcePolicyService.find(c, UIUtil
                    .getIntParameter(request, "policy_id"));

            // do the remove
            resourcePolicyService.delete(c, policy);

            // return to collection permission page
            request.setAttribute("collection", collection);

            List<ResourcePolicy> policies = authorizeService.getPolicies(c, collection);
            request.setAttribute("policies", policies);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-collection-edit.jsp");
        }
        else if (button.equals("submit_community_delete_policy"))
        {
            // delete a permission from a community
            Community community = communityService.find(c, UIUtil.getUUIDParameter(
                    request, "community_id"));
            
            AuthorizeUtil.authorizeManageCommunityPolicy(c, community);
            ResourcePolicy policy = resourcePolicyService.find(c, UIUtil
                    .getIntParameter(request, "policy_id"));

            // do the remove
            resourcePolicyService.delete(c, policy);

            // return to collection permission page
            request.setAttribute("community", community);

            List<ResourcePolicy> policies = authorizeService.getPolicies(c, community);
            request.setAttribute("policies", policies);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-community-edit.jsp");
        }
        else if (button.equals("submit_collection_edit_policy"))
        {
            // edit a collection's policy - set up and call policy editor
            Collection collection = collectionService.find(c, UIUtil.getUUIDParameter(
                    request, "collection_id"));

            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
            int policyId = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;

            if (policyId == -1)
            {
                // create new policy
    			policy = authorizeService.createResourcePolicy(c, collection,
    					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);
            }
            else
            {
                policy = resourcePolicyService.find(c, policyId);
            }

            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to collection permission page
            request.setAttribute("edit_title", "Collection "
                    + collection.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "collection_id");
            request.setAttribute("id", "" + collection.getID());
            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_community_edit_policy"))
        {
            // edit a community's policy - set up and call policy editor
            Community community = communityService.find(c, UIUtil.getUUIDParameter(
                    request, "community_id"));
            
            AuthorizeUtil.authorizeManageCommunityPolicy(c, community);

            int policyId = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;

            if (policyId == -1)
            {
                // create new policy
    			policy = authorizeService.createResourcePolicy(c, community,
    					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);

            }
            else
            {
                policy = resourcePolicyService.find(c, policyId);
            }

            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to collection permission page
            request
                    .setAttribute("edit_title", "Community "
                            + community.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "community_id");
            request.setAttribute("id", "" + community.getID());
            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_collection_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Collection collection = collectionService.find(c, UIUtil.getUUIDParameter(
                    request, "collection_id"));

            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
			ResourcePolicy policy = authorizeService.createResourcePolicy(c, collection,
					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);


            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to collection permission page
            request.setAttribute("edit_title", "Collection "
                    + collection.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "collection_id");
            request.setAttribute("id", "" + collection.getID());
            request.setAttribute("newpolicy", "true");

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_community_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Community community = communityService.find(c, UIUtil.getUUIDParameter(
                    request, "community_id"));

            AuthorizeUtil.authorizeManageCommunityPolicy(c, community);
			ResourcePolicy policy = authorizeService.createResourcePolicy(c, community,
					groupService.findByName(c, Group.ANONYMOUS), null, -1, null);


            List<Group> groups = groupService.findAll(c, null);
            List<EPerson> epeople = personService.findAll(c, EPerson.EMAIL);

            // return to collection permission page
            request
                    .setAttribute("edit_title", "Community "
                            + community.getID());
            request.setAttribute("policy", policy);
            request.setAttribute("groups", groups);
            request.setAttribute("epeople", epeople);
            request.setAttribute("id_name", "community_id");
            request.setAttribute("id", "" + community.getID());
            request.setAttribute("newpolicy", "true");

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-policy-edit.jsp");
        }
        else if (button.equals("submit_save_policy"))
        {
            int policyId = UIUtil.getIntParameter(request, "policy_id");
            int actionId = UIUtil.getIntParameter(request, "action_id");
            UUID groupId = UIUtil.getUUIDParameter(request, "group_id");
            UUID collectionId = UIUtil
                    .getUUIDParameter(request, "collection_id");
            UUID communityId = UIUtil.getUUIDParameter(request, "community_id");
            UUID itemId = UIUtil.getUUIDParameter(request, "item_id");
            Date startDate = null;
            try {
                startDate = DateUtils.parseDate(request.getParameter("policy_start_date"),
                        new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (Exception ex) {
                //Ignore start date is already null
            }
            Date endDate = null;
            try {
                endDate = DateUtils.parseDate(request.getParameter("policy_end_date"),
                        new String[]{"yyyy-MM-dd", "yyyy-MM", "yyyy"});
            } catch (Exception ex) {
                //Ignore end date is already null
            }

            Item item = null;
            Collection collection = null;
            Community community = null;
            String displayPage = null;

            ResourcePolicy policy = resourcePolicyService.find(c, policyId);
            AuthorizeUtil.authorizeManagePolicy(c, policy);
            Group group = groupService.find(c, groupId);

            if (collectionId != null)
            {
                collection = collectionService.find(c, collectionId);

                // modify the policy
                policy.setAction(actionId);
                policy.setGroup(group);
                resourcePolicyService.update(c, policy);

                // if it is a read, policy, modify the logo policy to match
                if (actionId == Constants.READ)
                {
                    // first get a list of READ policies from collection
                    List<ResourcePolicy> rps = authorizeService.getPoliciesActionFilter(c,
                            collection, Constants.READ);

                    // remove all bitstream policies, then add READs
                    Bitstream bs = collection.getLogo();

                    if (bs != null)
                    {
                        authorizeService.removeAllPolicies(c, bs);
                        authorizeService.addPolicies(c, rps, bs);
                    }
                }

                // set up page attributes
                request.setAttribute("collection", collection);
                request.setAttribute("policies", authorizeService.getPolicies(
                        c, collection));
                displayPage = "/dspace-admin/authorize-collection-edit.jsp";
            }
            else if (communityId != null)
            {
                community = communityService.find(c, communityId);

                // modify the policy
                policy.setAction(actionId);
                policy.setGroup(group);
                resourcePolicyService.update(c, policy);

                // if it is a read, policy, modify the logo policy to match
                if (actionId == Constants.READ)
                {
                    // first get a list of READ policies from collection
                    List<ResourcePolicy> rps = authorizeService.getPoliciesActionFilter(c,
                            community, Constants.READ);

                    // remove all bitstream policies, then add READs
                    Bitstream bs = community.getLogo();

                    if (bs != null)
                    {
                        authorizeService.removeAllPolicies(c, bs);
                        authorizeService.addPolicies(c, rps, bs);
                    }
                }

                // set up page attributes
                request.setAttribute("community", community);
                request.setAttribute("policies", authorizeService.getPolicies(
                        c, community));
                displayPage = "/dspace-admin/authorize-community-edit.jsp";
            }
            else if (itemId != null)
            {
                item = itemService.find(c, itemId);

                // modify the policy
                policy.setAction(actionId);
                policy.setGroup(group);
                // start and end dates are used for Items and Bitstreams only.
                // Set start and end date even if they are null to be able to 
                // delete previously set dates.
                policy.setStartDate(startDate);
                policy.setEndDate(endDate);
                resourcePolicyService.update(c, policy);

                // show edit form!
                prepItemEditForm(c, request, item);

                displayPage = "/dspace-admin/authorize-item-edit.jsp";
            }

            // now return to previous state
            JSPManager.showJSP(request, response, displayPage);
        }
        else if (button.equals("submit_cancel_policy"))
        {
            // delete the policy that we created if it's a new one
            if ((request.getParameter("newpolicy") != null))
            {
                int policyId = UIUtil.getIntParameter(request, "policy_id");
                ResourcePolicy rp = resourcePolicyService.find(c, policyId);
                AuthorizeUtil.authorizeManagePolicy(c, rp);
                resourcePolicyService.delete(c, rp);
            }

            // return to the previous page
            UUID collectionId = UIUtil.getUUIDParameter(request, "collection_id");
            UUID communityId = UIUtil.getUUIDParameter(request, "community_id");
            UUID itemId = UIUtil.getUUIDParameter(request, "item_id");
            String displayPage = null;

            if (collectionId != null)
            {
                // set up for return to collection edit page
                Collection t = collectionService.find(c, collectionId);

                request.setAttribute("collection", t);
                request.setAttribute("policies", authorizeService.getPolicies(
                        c, t));
                displayPage = "/dspace-admin/authorize-collection-edit.jsp";
            }
            else if (communityId != null)
            {
                // set up for return to community edit page
                Community t = communityService.find(c, communityId);

                request.setAttribute("community", t);
                request.setAttribute("policies", authorizeService.getPolicies(
                        c, t));
                displayPage = "/dspace-admin/authorize-community-edit.jsp";
            }
            else if (itemId != null)
            {
                // set up for return to item edit page
                Item t = itemService.find(c, itemId);

                // show edit form!
                prepItemEditForm(c, request, t);

                displayPage = "/dspace-admin/authorize-item-edit.jsp";
            }

            JSPManager.showJSP(request, response, displayPage);
        }
        else if (button.equals("submit_advanced_clear"))
        {
            AuthorizeUtil.requireAdminRole(c);
            // remove all policies for a set of objects
            UUID collectionId = UIUtil.getUUIDParameter(request, "collection_id");
            int resourceType = UIUtil.getIntParameter(request, "resource_type");

            // if it's to bitstreams, do it to bundles too
            PolicySet.setPolicies(c, Constants.COLLECTION, collectionId,
                    resourceType, 0, groupService.findByName(c, Group.ANONYMOUS).getID(), false, true);

            if (resourceType == Constants.BITSTREAM)
            {
                PolicySet.setPolicies(c, Constants.COLLECTION, collectionId,
                        Constants.BUNDLE, 0, groupService.findByName(c, Group.ANONYMOUS).getID(), false, true);
            }

            // return to the main page
            showMainPage(c, request, response);
        }
        else if (button.equals("submit_advanced_add"))
        {
            AuthorizeUtil.requireAdminRole(c);
            // add a policy to a set of objects
            UUID collectionId = UIUtil.getUUIDParameter(request, "collection_id");
            int resourceType = UIUtil.getIntParameter(request, "resource_type");
            int actionId = UIUtil.getIntParameter(request, "action_id");
            UUID groupId = UIUtil.getUUIDParameter(request, "group_id");

            PolicySet.setPolicies(c, Constants.COLLECTION, collectionId,
                    resourceType, actionId, groupId, false, false);

            // if it's a bitstream, do it to the bundle too
            if (resourceType == Constants.BITSTREAM)
            {
                PolicySet.setPolicies(c, Constants.COLLECTION, collectionId,
                        Constants.BUNDLE, actionId, groupId, false, false);
            }

            // return to the main page
            showMainPage(c, request, response);
        }
        else if (button.equals("submit_collection_select"))
        {
            // edit the collection's permissions
            Collection collection = collectionService.find(c, UIUtil.getUUIDParameter(
                    request, "collection_id"));
            List<ResourcePolicy> policies = authorizeService.getPolicies(c, collection);

            request.setAttribute("collection", collection);
            request.setAttribute("policies", policies);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-collection-edit.jsp");
        }
        else
        {
            // return to the main page
            showMainPage(c, request, response);
        }

        c.complete();
    }

    void showMainPage(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        JSPManager.showJSP(request, response,
                "/dspace-admin/authorize-main.jsp");
    }

    void prepItemEditForm(Context c, HttpServletRequest request, Item item)
            throws SQLException
    {
        List<ResourcePolicy> itemPolicies = authorizeService.getPolicies(c, item);

        // Put bundle and bitstream policies in their own hashes
        Map<UUID, List<ResourcePolicy>> bundlePolicies = new HashMap<>();
        Map<UUID, List<ResourcePolicy>> bitstreamPolicies = new HashMap<>();

        List<Bundle> bundles = item.getBundles();

        for (Bundle myBundle : bundles)
        {
            List<ResourcePolicy> myPolicies = authorizeService.getPolicies(c, myBundle);

            // add bundle's policies to bundle_policies map
            bundlePolicies.put(myBundle.getID(), myPolicies);

            // go through all bundle's bitstreams, add to bitstream map
            List<Bitstream> bitstreams = myBundle.getBitstreams();

            for (Bitstream myBitstream : bitstreams)
            {
                myPolicies = authorizeService.getPolicies(c, myBitstream);
                bitstreamPolicies.put(myBitstream.getID(),
                        myPolicies);
            }
        }

        request.setAttribute("item", item);
        request.setAttribute("item_policies", itemPolicies);
        request.setAttribute("bundles", bundles);
        request.setAttribute("bundle_policies", bundlePolicies);
        request.setAttribute("bitstream_policies", bitstreamPolicies);
    }
}
