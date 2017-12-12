/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.servlet.admin;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.time.DateUtils;

import org.dspace.app.util.AuthorizeUtil;
import org.dspace.app.webui.servlet.DSpaceServlet;
import org.dspace.app.webui.util.JSPManager;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.PolicySet;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.handle.HandleManager;

/**
 * Servlet for editing permissions
 * 
 * @author dstuve
 * @version $Revision$
 */
public class AuthorizeAdminServlet extends DSpaceServlet
{
    protected void doDSGet(Context c, HttpServletRequest request,
            HttpServletResponse response) throws ServletException, IOException,
            SQLException, AuthorizeException
    {
        // handle gets and posts with the post method
        doDSPost(c, request, response);

        // show the main page (select communities, collections, items, etc)
        //        showMainPage(c, request, response);
    }

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
            Collection[] collections = Collection.findAll(c);

            request.setAttribute("collections", collections);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/collection-select.jsp");
        }
        else if (button.equals("submit_community"))
        {
            // select a community to work on
            Community[] communities = Community.findAll(c);

            request.setAttribute("communities", communities);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/community-select.jsp");
        }
        else if (button.equals("submit_advanced"))
        {
            // select a collections to work on
            Collection[] collections = Collection.findAll(c);
            Group[] groups = Group.findAll(c, Group.NAME);

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

            int itemId = UIUtil.getIntParameter(request, "item_id");
            String handle = request.getParameter("handle");

            // if id is set, use it
            if (itemId > 0)
            {
                item = Item.find(c, itemId);
            }
            else if ((handle != null) && !handle.equals(""))
            {
                // otherwise, attempt to resolve handle
                DSpaceObject dso = HandleManager.resolveToObject(c, handle);

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
            Item item = Item
                    .find(c, UIUtil.getIntParameter(request, "item_id"));

            AuthorizeUtil.authorizeManageItemPolicy(c, item);
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.setResource(item);
            policy.update();

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Item item = Item
                    .find(c, UIUtil.getIntParameter(request, "item_id"));

            AuthorizeUtil.authorizeManageItemPolicy(c, item);
            int policyId = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;

            policy = ResourcePolicy.find(c, policyId);

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Item item = Item
                    .find(c, UIUtil.getIntParameter(request, "item_id"));
            Bundle bundle = Bundle.find(c, UIUtil.getIntParameter(request,
                    "bundle_id"));

            AuthorizeUtil.authorizeManageBundlePolicy(c, bundle);
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.setResource(bundle);
            policy.update();

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Item item = Item
                    .find(c, UIUtil.getIntParameter(request, "item_id"));
            Bitstream bitstream = Bitstream.find(c, UIUtil.getIntParameter(
                    request, "bitstream_id"));

            AuthorizeUtil.authorizeManageBitstreamPolicy(c, bitstream);
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.setResource(bitstream);
            policy.update();

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            // delete a permission from an item
            Item item = Item
                    .find(c, UIUtil.getIntParameter(request, "item_id"));
            
            AuthorizeUtil.authorizeManageItemPolicy(c, item);
            ResourcePolicy policy = ResourcePolicy.find(c, UIUtil
                    .getIntParameter(request, "policy_id"));

            // do the remove
            policy.delete();

            // show edit form!
            prepItemEditForm(c, request, item);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-item-edit.jsp");
        }
        // COLLECTIONS ////////////////////////////////////////////////////////
        else if (button.equals("submit_collection_add_policy"))
        {
            // want to add a policy, create an empty one and invoke editor
            Collection collection = Collection.find(c, UIUtil.getIntParameter(
                    request, "collection_id"));

            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.setResource(collection);
            policy.update();

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Community target = Community.find(c, UIUtil.getIntParameter(
                    request, "community_id"));
            List<ResourcePolicy> policies = AuthorizeManager.getPolicies(c, target);

            request.setAttribute("community", target);
            request.setAttribute("policies", policies);
            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-community-edit.jsp");
        }
        else if (button.equals("submit_collection_delete_policy"))
        {
            // delete a permission from a collection
            Collection collection = Collection.find(c, UIUtil.getIntParameter(
                    request, "collection_id"));
            
            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
            ResourcePolicy policy = ResourcePolicy.find(c, UIUtil
                    .getIntParameter(request, "policy_id"));

            // do the remove
            policy.delete();

            // return to collection permission page
            request.setAttribute("collection", collection);

            List<ResourcePolicy> policies = AuthorizeManager.getPolicies(c, collection);
            request.setAttribute("policies", policies);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-collection-edit.jsp");
        }
        else if (button.equals("submit_community_delete_policy"))
        {
            // delete a permission from a community
            Community community = Community.find(c, UIUtil.getIntParameter(
                    request, "community_id"));
            
            AuthorizeUtil.authorizeManageCommunityPolicy(c, community);
            ResourcePolicy policy = ResourcePolicy.find(c, UIUtil
                    .getIntParameter(request, "policy_id"));

            // do the remove
            policy.delete();

            // return to collection permission page
            request.setAttribute("community", community);

            List<ResourcePolicy> policies = AuthorizeManager.getPolicies(c, community);
            request.setAttribute("policies", policies);

            JSPManager.showJSP(request, response,
                    "/dspace-admin/authorize-community-edit.jsp");
        }
        else if (button.equals("submit_collection_edit_policy"))
        {
            // edit a collection's policy - set up and call policy editor
            Collection collection = Collection.find(c, UIUtil.getIntParameter(
                    request, "collection_id"));

            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
            int policyId = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;

            if (policyId == -1)
            {
                // create new policy
                policy = ResourcePolicy.create(c);
                policy.setResource(collection);
                policy.update();
            }
            else
            {
                policy = ResourcePolicy.find(c, policyId);
            }

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Community community = Community.find(c, UIUtil.getIntParameter(
                    request, "community_id"));
            
            AuthorizeUtil.authorizeManageCommunityPolicy(c, community);

            int policyId = UIUtil.getIntParameter(request, "policy_id");
            ResourcePolicy policy = null;

            if (policyId == -1)
            {
                // create new policy
                policy = ResourcePolicy.create(c);
                policy.setResource(community);
                policy.update();
            }
            else
            {
                policy = ResourcePolicy.find(c, policyId);
            }

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Collection collection = Collection.find(c, UIUtil.getIntParameter(
                    request, "collection_id"));

            AuthorizeUtil.authorizeManageCollectionPolicy(c, collection);
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.setResource(collection);
            policy.update();

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            Community community = Community.find(c, UIUtil.getIntParameter(
                    request, "community_id"));

            AuthorizeUtil.authorizeManageCommunityPolicy(c, community);
            ResourcePolicy policy = ResourcePolicy.create(c);
            policy.setResource(community);
            policy.update();

            Group[] groups = Group.findAll(c, Group.NAME);
            EPerson[] epeople = EPerson.findAll(c, EPerson.EMAIL);

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
            int groupId = UIUtil.getIntParameter(request, "group_id");
            int collectionId = UIUtil
                    .getIntParameter(request, "collection_id");
            int communityId = UIUtil.getIntParameter(request, "community_id");
            int itemId = UIUtil.getIntParameter(request, "item_id");
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

            ResourcePolicy policy = ResourcePolicy.find(c, policyId);
            AuthorizeUtil.authorizeManagePolicy(c, policy);
            Group group = Group.find(c, groupId);

            if (collectionId != -1)
            {
                collection = Collection.find(c, collectionId);

                // modify the policy
                policy.setAction(actionId);
                policy.setGroup(group);
                policy.update();

                // if it is a read, policy, modify the logo policy to match
                if (actionId == Constants.READ)
                {
                    // first get a list of READ policies from collection
                    List<ResourcePolicy> rps = AuthorizeManager.getPoliciesActionFilter(c,
                            collection, Constants.READ);

                    // remove all bitstream policies, then add READs
                    Bitstream bs = collection.getLogo();

                    if (bs != null)
                    {
                        AuthorizeManager.removeAllPolicies(c, bs);
                        AuthorizeManager.addPolicies(c, rps, bs);
                    }
                }

                // set up page attributes
                request.setAttribute("collection", collection);
                request.setAttribute("policies", AuthorizeManager.getPolicies(
                        c, collection));
                displayPage = "/dspace-admin/authorize-collection-edit.jsp";
            }
            else if (communityId != -1)
            {
                community = Community.find(c, communityId);

                // modify the policy
                policy.setAction(actionId);
                policy.setGroup(group);
                policy.update();

                // if it is a read, policy, modify the logo policy to match
                if (actionId == Constants.READ)
                {
                    // first get a list of READ policies from collection
                    List<ResourcePolicy> rps = AuthorizeManager.getPoliciesActionFilter(c,
                            community, Constants.READ);

                    // remove all bitstream policies, then add READs
                    Bitstream bs = community.getLogo();

                    if (bs != null)
                    {
                        AuthorizeManager.removeAllPolicies(c, bs);
                        AuthorizeManager.addPolicies(c, rps, bs);
                    }
                }

                // set up page attributes
                request.setAttribute("community", community);
                request.setAttribute("policies", AuthorizeManager.getPolicies(
                        c, community));
                displayPage = "/dspace-admin/authorize-community-edit.jsp";
            }
            else if (itemId != -1)
            {
                item = Item.find(c, itemId);

                // modify the policy
                policy.setAction(actionId);
                policy.setGroup(group);
                // start and end dates are used for Items and Bitstreams only.
                // Set start and end date even if they are null to be able to 
                // delete previously set dates.
                policy.setStartDate(startDate);
                policy.setEndDate(endDate);
                policy.update();

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
                ResourcePolicy rp = ResourcePolicy.find(c, policyId);
                AuthorizeUtil.authorizeManagePolicy(c, rp);
                rp.delete();
            }

            // return to the previous page
            int collectionId = UIUtil.getIntParameter(request, "collection_id");
            int communityId = UIUtil.getIntParameter(request, "community_id");
            int itemId = UIUtil.getIntParameter(request, "item_id");
            String displayPage = null;

            if (collectionId != -1)
            {
                // set up for return to collection edit page
                Collection t = Collection.find(c, collectionId);

                request.setAttribute("collection", t);
                request.setAttribute("policies", AuthorizeManager.getPolicies(
                        c, t));
                displayPage = "/dspace-admin/authorize-collection-edit.jsp";
            }
            else if (communityId != -1)
            {
                // set up for return to community edit page
                Community t = Community.find(c, communityId);

                request.setAttribute("community", t);
                request.setAttribute("policies", AuthorizeManager.getPolicies(
                        c, t));
                displayPage = "/dspace-admin/authorize-community-edit.jsp";
            }
            else if (itemId != -1)
            {
                // set up for return to item edit page
                Item t = Item.find(c, itemId);

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
            int collectionId = UIUtil.getIntParameter(request, "collection_id");
            int resourceType = UIUtil.getIntParameter(request, "resource_type");

            // if it's to bitstreams, do it to bundles too
            PolicySet.setPolicies(c, Constants.COLLECTION, collectionId,
                    resourceType, 0, 0, false, true);

            if (resourceType == Constants.BITSTREAM)
            {
                PolicySet.setPolicies(c, Constants.COLLECTION, collectionId,
                        Constants.BUNDLE, 0, 0, false, true);
            }

            // return to the main page
            showMainPage(c, request, response);
        }
        else if (button.equals("submit_advanced_add"))
        {
            AuthorizeUtil.requireAdminRole(c);
            // add a policy to a set of objects
            int collectionId = UIUtil.getIntParameter(request, "collection_id");
            int resourceType = UIUtil.getIntParameter(request, "resource_type");
            int actionId = UIUtil.getIntParameter(request, "action_id");
            int groupId = UIUtil.getIntParameter(request, "group_id");

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
            Collection collection = Collection.find(c, UIUtil.getIntParameter(
                    request, "collection_id"));
            List<ResourcePolicy> policies = AuthorizeManager.getPolicies(c, collection);

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
        List<ResourcePolicy> itemPolicies = AuthorizeManager.getPolicies(c, item);

        // Put bundle and bitstream policies in their own hashes
        Map<Integer, List<ResourcePolicy>> bundlePolicies = new HashMap<Integer, List<ResourcePolicy>>();
        Map<Integer, List<ResourcePolicy>> bitstreamPolicies = new HashMap<Integer, List<ResourcePolicy>>();

        Bundle[] bundles = item.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            Bundle myBundle = bundles[i];
            List<ResourcePolicy> myPolicies = AuthorizeManager.getPolicies(c, myBundle);

            // add bundle's policies to bundle_policies map
            bundlePolicies.put(Integer.valueOf(myBundle.getID()), myPolicies);

            // go through all bundle's bitstreams, add to bitstream map
            Bitstream[] bitstreams = myBundle.getBitstreams();

            for (int j = 0; j < bitstreams.length; j++)
            {
                Bitstream myBitstream = bitstreams[j];
                myPolicies = AuthorizeManager.getPolicies(c, myBitstream);
                bitstreamPolicies.put(Integer.valueOf(myBitstream.getID()),
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
