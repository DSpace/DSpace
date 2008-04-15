/*
 * AuthorizeAdminServlet.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.app.webui.servlet.admin;

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
import org.dspace.content.dao.BitstreamDAO;
import org.dspace.content.dao.BitstreamDAOFactory;
import org.dspace.content.dao.BundleDAO;
import org.dspace.content.dao.BundleDAOFactory;
import org.dspace.content.dao.CollectionDAO;
import org.dspace.content.dao.CollectionDAOFactory;
import org.dspace.content.dao.CommunityDAO;
import org.dspace.content.dao.CommunityDAOFactory;
import org.dspace.content.dao.ItemDAO;
import org.dspace.content.dao.ItemDAOFactory;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.uri.ResolvableIdentifier;
import org.dspace.uri.IdentifierService;
import org.dspace.uri.IdentifierException;
import org.dspace.uri.dao.ExternalIdentifierDAO;
import org.dspace.uri.dao.ExternalIdentifierDAOFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        try
        {
            BitstreamDAO bitstreamDAO = null;
            BundleDAO bundleDAO = null;
            ItemDAO itemDAO = null;
            CollectionDAO collectionDAO = null;
            CommunityDAO communityDAO = null;

            int bitstreamID = UIUtil.getIntParameter(request, "bitstream_id");
            int bundleID = UIUtil.getIntParameter(request, "bundle_id");
            int itemID = UIUtil.getIntParameter(request, "item_id");
            int collectionID = UIUtil.getIntParameter(request, "collection_id");
            int communityID = UIUtil.getIntParameter(request, "community_id");

            if (bitstreamID > 0)
            {
                bitstreamDAO = BitstreamDAOFactory.getInstance(c);
            }
            if (bundleID > 0)
            {
                bundleDAO = BundleDAOFactory.getInstance(c);
            }
            if (itemID > 0)
            {
                itemDAO = ItemDAOFactory.getInstance(c);
            }
            if (collectionID > 0)
            {
                collectionDAO = CollectionDAOFactory.getInstance(c);
            }
            if (communityID > 0)
            {
                communityDAO = CommunityDAOFactory.getInstance(c);
            }

            ExternalIdentifierDAO identifierDAO =
                ExternalIdentifierDAOFactory.getInstance(c);

            String button = UIUtil.getSubmitButton(request, "submit");

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

                String uri = request.getParameter("uri");

                // if id is set, use it
                if (itemID > 0)
                {
                    item = itemDAO.retrieve(itemID);
                }
                else if ((uri != null) && !uri.equals(""))
                {
                    // otherwise, attempt to resolve uri
                    ResolvableIdentifier di = IdentifierService.resolve(c, uri);
                    DSpaceObject dso = (DSpaceObject) IdentifierService.getResource(c, di);

                    // make sure it's an item
                    if ((dso != null) && (dso.getType() == Constants.ITEM))
                    {
                        item = (Item) dso;
                    }
                }

                // no item set yet, failed ID & uri, ask user to try again
                if (item == null)
                {
                    request.setAttribute("invalid.id", new Boolean(true));
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
                Item item = itemDAO.retrieve(itemID);

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
                Item item = itemDAO.retrieve(itemID);

                int policy_id = UIUtil.getIntParameter(request, "policy_id");
                ResourcePolicy policy = null;

                policy = ResourcePolicy.find(c, policy_id);

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
                Item item = itemDAO.retrieve(itemID);
                Bundle bundle = bundleDAO.retrieve(bundleID);

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
                Item item = itemDAO.retrieve(itemID);
                Bitstream bitstream = bitstreamDAO.retrieve(bitstreamID);

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
                Item item = itemDAO.retrieve(itemID);
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
                Collection collection = collectionDAO.retrieve(collectionID);

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
                Community target = communityDAO.retrieve(communityID);
                List policies = AuthorizeManager.getPolicies(c, target);

                request.setAttribute("community", target);
                request.setAttribute("policies", policies);
                JSPManager.showJSP(request, response,
                        "/dspace-admin/authorize-community-edit.jsp");
            }
            else if (button.equals("submit_collection_delete_policy"))
            {
                // delete a permission from a collection
                Collection collection = collectionDAO.retrieve(collectionID);
                ResourcePolicy policy = ResourcePolicy.find(c, UIUtil
                        .getIntParameter(request, "policy_id"));

                // do the remove
                policy.delete();

                // return to collection permission page
                request.setAttribute("collection", collection);

                List policies = AuthorizeManager.getPolicies(c, collection);
                request.setAttribute("policies", policies);

                JSPManager.showJSP(request, response,
                        "/dspace-admin/authorize-collection-edit.jsp");
            }
            else if (button.equals("submit_community_delete_policy"))
            {
                // delete a permission from a community
                Community community = communityDAO.retrieve(communityID);
                ResourcePolicy policy = ResourcePolicy.find(c, UIUtil
                        .getIntParameter(request, "policy_id"));

                // do the remove
                policy.delete();

                // return to collection permission page
                request.setAttribute("community", community);

                List policies = AuthorizeManager.getPolicies(c, community);
                request.setAttribute("policies", policies);

                JSPManager.showJSP(request, response,
                        "/dspace-admin/authorize-community-edit.jsp");
            }
            else if (button.equals("submit_collection_edit_policy"))
            {
                // edit a collection's policy - set up and call policy editor
                Collection collection = collectionDAO.retrieve(collectionID);

                int policy_id = UIUtil.getIntParameter(request, "policy_id");
                ResourcePolicy policy = null;

                if (policy_id == -1)
                {
                    // create new policy
                    policy = ResourcePolicy.create(c);
                    policy.setResource(collection);
                    policy.update();
                }
                else
                {
                    policy = ResourcePolicy.find(c, policy_id);
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
                Community community = communityDAO.retrieve(communityID);

                int policy_id = UIUtil.getIntParameter(request, "policy_id");
                ResourcePolicy policy = null;

                if (policy_id == -1)
                {
                    // create new policy
                    policy = ResourcePolicy.create(c);
                    policy.setResource(community);
                    policy.update();
                }
                else
                {
                    policy = ResourcePolicy.find(c, policy_id);
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
                Collection collection = collectionDAO.retrieve(collectionID);

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
                Community community = communityDAO.retrieve(communityID);

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
                int policy_id = UIUtil.getIntParameter(request, "policy_id");
                int action_id = UIUtil.getIntParameter(request, "action_id");
                int group_id = UIUtil.getIntParameter(request, "group_id");

                Item item = null;
                Collection collection = null;
                Community community = null;
                String display_page = null;

                ResourcePolicy policy = ResourcePolicy.find(c, policy_id);
                Group group = Group.find(c, group_id);

                if (collectionID > 0)
                {
                    collection = collectionDAO.retrieve(collectionID);

                    // modify the policy
                    policy.setAction(action_id);
                    policy.setGroup(group);
                    policy.update();

                    // if it is a read, policy, modify the logo policy to match
                    if (action_id == Constants.READ)
                    {
                        // first get a list of READ policies from collection
                        List rps = AuthorizeManager.getPoliciesActionFilter(c,
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
                    display_page = "/dspace-admin/authorize-collection-edit.jsp";
                }
                else if (communityID > 0)
                {
                    community = communityDAO.retrieve(communityID);

                    // modify the policy
                    policy.setAction(action_id);
                    policy.setGroup(group);
                    policy.update();

                    // if it is a read, policy, modify the logo policy to match
                    if (action_id == Constants.READ)
                    {
                        // first get a list of READ policies from collection
                        List rps = AuthorizeManager.getPoliciesActionFilter(c,
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
                    display_page = "/dspace-admin/authorize-community-edit.jsp";
                }
                else if (itemID > 0)
                {
                    item = itemDAO.retrieve(itemID);

                    // modify the policy
                    policy.setAction(action_id);
                    policy.setGroup(group);
                    policy.update();

                    // show edit form!
                    prepItemEditForm(c, request, item);

                    display_page = "/dspace-admin/authorize-item-edit.jsp";
                }

                // now return to previous state
                JSPManager.showJSP(request, response, display_page);
            }
            else if (button.equals("submit_cancel_policy"))
            {
                // delete the policy that we created if it's a new one
                if ((request.getParameter("newpolicy") != null))
                {
                    int policy_id = UIUtil.getIntParameter(request, "policy_id");
                    ResourcePolicy rp = ResourcePolicy.find(c, policy_id);
                    rp.delete();
                }

                // return to the previous page
                String display_page = null;

                if (collectionID > 0)
                {
                    // set up for return to collection edit page
                    Collection t = collectionDAO.retrieve(collectionID);

                    request.setAttribute("collection", t);
                    request.setAttribute("policies", AuthorizeManager.getPolicies(
                            c, t));
                    display_page = "/dspace-admin/authorize-collection-edit.jsp";
                }
                else if (communityID > 0)
                {
                    // set up for return to community edit page
                    Community t = communityDAO.retrieve(communityID);

                    request.setAttribute("community", t);
                    request.setAttribute("policies", AuthorizeManager.getPolicies(
                            c, t));
                    display_page = "/dspace-admin/authorize-community-edit.jsp";
                }
                else if (itemID > 0)
                {
                    // set up for return to item edit page
                    Item t = itemDAO.retrieve(itemID);

                    // show edit form!
                    prepItemEditForm(c, request, t);

                    display_page = "/dspace-admin/authorize-item-edit.jsp";
                }

                JSPManager.showJSP(request, response, display_page);
            }
            else if (button.equals("submit_advanced_clear"))
            {
                // remove all policies for a set of objects
                int resource_type = UIUtil
                        .getIntParameter(request, "resource_type");

                // if it's to bitstreams, do it to bundles too
                PolicySet.setPolicies(c, Constants.COLLECTION, collectionID,
                        resource_type, 0, 0, false, true);

                if (resource_type == Constants.BITSTREAM)
                {
                    PolicySet.setPolicies(c, Constants.COLLECTION, collectionID,
                            Constants.BUNDLE, 0, 0, false, true);
                }

                // return to the main page
                showMainPage(c, request, response);
            }
            else if (button.equals("submit_advanced_add"))
            {
                // add a policy to a set of objects
                int resource_type = UIUtil
                        .getIntParameter(request, "resource_type");
                int action_id = UIUtil.getIntParameter(request, "action_id");
                int group_id = UIUtil.getIntParameter(request, "group_id");

                PolicySet.setPolicies(c, Constants.COLLECTION, collectionID,
                        resource_type, action_id, group_id, false, false);

                // if it's a bitstream, do it to the bundle too
                if (resource_type == Constants.BITSTREAM)
                {
                    PolicySet.setPolicies(c, Constants.COLLECTION, collectionID,
                            Constants.BUNDLE, action_id, group_id, false, false);
                }

                // return to the main page
                showMainPage(c, request, response);
            }
            else if (button.equals("submit_collection_select"))
            {
                // edit the collection's permissions
                Collection collection = collectionDAO.retrieve(collectionID);
                List policies = AuthorizeManager.getPolicies(c, collection);

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
        catch (IdentifierException e)
        {
            throw new ServletException(e);
        }
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
        List item_policies = AuthorizeManager.getPolicies(c, item);

        // Put bundle and bitstream policies in their own hashes
        Map bundle_policies = new HashMap();
        Map bitstream_policies = new HashMap();

        Bundle[] bundles = item.getBundles();

        for (int i = 0; i < bundles.length; i++)
        {
            Bundle myBundle = bundles[i];
            List myPolicies = AuthorizeManager.getPolicies(c, myBundle);

            // add bundle's policies to bundle_policies map
            bundle_policies.put(new Integer(myBundle.getID()), myPolicies);

            // go through all bundle's bitstreams, add to bitstream map
            Bitstream[] bitstreams = myBundle.getBitstreams();

            for (int j = 0; j < bitstreams.length; j++)
            {
                Bitstream myBitstream = bitstreams[j];
                myPolicies = AuthorizeManager.getPolicies(c, myBitstream);
                bitstream_policies.put(new Integer(myBitstream.getID()),
                        myPolicies);
            }
        }

        request.setAttribute("item", item);
        request.setAttribute("item_policies", item_policies);
        request.setAttribute("bundles", bundles);
        request.setAttribute("bundle_policies", bundle_policies);
        request.setAttribute("bitstream_policies", bitstream_policies);
    }
}
