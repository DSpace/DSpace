/*
 * CommunityDAOCore.java
 *
 * Version: $Revision: 1727 $
 *
 * Date: $Date: 2007-01-19 10:52:10 +0000 (Fri, 19 Jan 2007) $
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
package org.dspace.content.dao;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.dao.ResourcePolicyDAO;
import org.dspace.authorize.dao.ResourcePolicyDAOFactory;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.Group;
import org.dspace.uri.ExternalIdentifier;
import org.dspace.uri.ExternalIdentifierMint;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.uri.ObjectIdentifierMint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author James Rutherford
 * @author Richard Jones
 */
public class CommunityDAOCore extends CommunityDAO
{
    public CommunityDAOCore(Context context)
    {
        super(context);
    }

    @Override
    public Community create() throws AuthorizeException
    {
        // Only administrators and adders can create communities
        if (!(AuthorizeManager.isAdmin(context)))
        {
            throw new AuthorizeException(
                    "Only administrators can create communities");
        }

        Community community = childDAO.create();

        // now assign an object identifier
        /*
        ObjectIdentifier oid = new ObjectIdentifier(true);
        community.setIdentifier(oid);*/
        ObjectIdentifier oid = ObjectIdentifierMint.mint(context, community);

        // Create a default persistent identifier for this Community, and
        // add it to the in-memory Community object.
        //ExternalIdentifier identifier = identifierDAO.create(community);
        //community.addExternalIdentifier(identifier);

        // now assign any required external identifiers
        List<ExternalIdentifier> eids = ExternalIdentifierMint.mintAll(context, community);
        community.setExternalIdentifiers(eids);

        // create the default authorization policy for communities
        // of 'anonymous' READ
        Group anonymousGroup = groupDAO.retrieve(0);

        ResourcePolicyDAO policyDAO =
                ResourcePolicyDAOFactory.getInstance(context);
        ResourcePolicy policy = policyDAO.create();
        policy.setResource(community);
        policy.setAction(Constants.READ);
        policy.setGroup(anonymousGroup);
        policyDAO.update(policy);

        log.info(LogManager.getHeader(context, "create_community",
                "community_id=" + community.getID()) + ",uri=" +
                community.getIdentifier().getCanonicalForm());

        update(community);

        return community;
    }

    @Override
    public Community retrieve(int id)
    {
        Community community =
                (Community) context.fromCache(Community.class, id);

        if (community == null)
        {
            community = childDAO.retrieve(id);
        }

        return community;
    }

    @Override
    public void update(Community community) throws AuthorizeException
    {
        // Check authorization
        community.canEdit();

        log.info(LogManager.getHeader(context, "update_community",
                "community_id=" + community.getID()));

        // FIXME: Do we need to iterate through child Communities /
        // Collecitons to update / re-index? Probably not.

        // finally, deal with the item identifier/uuid
        ObjectIdentifier oid = community.getIdentifier();
        if (oid == null)
        {
            /*
            oid = new ObjectIdentifier(true);
            community.setIdentifier(oid);*/
            oid = ObjectIdentifierMint.mint(context, community);
        }
        oidDAO.update(community.getIdentifier());

        // deal with the external identifiers
        List<ExternalIdentifier> eids = community.getExternalIdentifiers();
        for (ExternalIdentifier eid : eids)
        {
            identifierDAO.update(eid);
        }

        childDAO.update(community);
    }

    @Override
    public void delete(int id) throws AuthorizeException
    {
        try
        {
            Community community = retrieve(id);

            context.removeCached(community, id);

            // Check authorisation
            // FIXME: If this was a subcommunity, it is first removed from it's
            // parent.
            // This means the parentCommunity == null
            // But since this is also the case for top-level communities, we would
            // give everyone rights to remove the top-level communities.
            // The same problem occurs in removing the logo
            for (Community parent : getParentCommunities(community))
            {
                if (!AuthorizeManager.authorizeActionBoolean(context, parent,
                            Constants.REMOVE))
                {
                    AuthorizeManager.authorizeAction(context, community,
                            Constants.DELETE);
                }
            }

            // If not a top-level community, have parent remove me; this
            // will call delete() after removing the linkage
            // FIXME: Maybe it shouldn't though.
            // FIXME: This is totally broken.
            for (Community parent : getParentCommunities(community))
            {
                unlink(parent, community);
            }

            log.info(LogManager.getHeader(context, "delete_community",
                    "community_id=" + id));

            // Remove collections
            for (Collection child :
                    collectionDAO.getChildCollections(community))
            {
                unlink(community, child);
            }

            // Remove subcommunities
            for (Community child : getChildCommunities(community))
            {
                unlink(community, child);
            }

            // FIXME: This won't delete the logo. Needs more
            // bitstreamDAO.delete(logoId)
            community.setLogo(null);

            // Remove all authorization policies
            AuthorizeManager.removeAllPolicies(context, community);

            // remove the object identifier
            oidDAO.delete(community);
        }
        catch (IOException ioe)
        {
            throw new RuntimeException(ioe);
        }

        childDAO.delete(id);
    }

    @Override
    public List<Community> getAllParentCommunities(DSpaceObject dso)
    {
        List<Community> parents = getParentCommunities(dso);
        List<Community> superParents = new ArrayList<Community>(parents);

        for (Community parent : parents)
        {
            superParents.addAll(getAllParentCommunities(parent));
        }

        return superParents;
    }

    @Override
    public void link(DSpaceObject parent, DSpaceObject child)
        throws AuthorizeException
    {
        assert(parent instanceof Community);
        assert((child instanceof Community) || (child instanceof Collection));

        if ((parent instanceof Community) &&
            (child instanceof Collection))
        {
            AuthorizeManager.authorizeAction(context,
                    (Community) parent, Constants.ADD);

            log.info(LogManager.getHeader(context, "add_collection",
                        "community_id=" + parent.getID() +
                        ",collection_id=" + child.getID()));
        }
        else if ((parent instanceof Community) &&
            (child instanceof Community))
        {
            AuthorizeManager.authorizeAction(context, parent,
                    Constants.ADD);

            log.info(LogManager.getHeader(context, "add_subcommunity",
                    "parent_comm_id=" + parent.getID() +
                    ",child_comm_id=" + child.getID()));
        }

        childDAO.link(parent, child);
    }

    @Override
    public void unlink(DSpaceObject parent, DSpaceObject child)
        throws AuthorizeException
    {
        assert(parent instanceof Community);
        assert((child instanceof Community) || (child instanceof Collection));

        if ((parent instanceof Community) &&
            (child instanceof Collection))
        {
            AuthorizeManager.authorizeAction(context, child,
                    Constants.REMOVE);

            log.info(LogManager.getHeader(context, "remove_collection",
                    "collection_id = " + parent.getID() +
                    ",item_id = " + child.getID()));
        }
        else if ((parent instanceof Community) &&
            (child instanceof Community))
        {
            AuthorizeManager.authorizeAction(context, child,
                    Constants.REMOVE);

            log.info(LogManager.getHeader(context,
                    "remove_subcommunity",
                    "parent_comm_id = " + parent.getID() +
                    ",child_comm_id = " + child.getID()));
        }
        else
        {
            throw new RuntimeException("Not allowed!");
        }

        childDAO.unlink(parent, child);

        if (getParentCommunities(child).size() == 0)
        {
            // make the right to remove the child explicit because the
            // implicit relation has been removed. This only has to concern the
            // currentUser because he started the removal process and he will
            // end it too. also add right to remove from the child to
            // remove it's items.
            AuthorizeManager.addPolicy(context, child, Constants.DELETE,
                    context.getCurrentUser());
            AuthorizeManager.addPolicy(context, child, Constants.REMOVE,
                    context.getCurrentUser());

            // Orphan; delete it
            if (child instanceof Collection)
            {
                collectionDAO.delete(child.getID());
            }
            else if (child instanceof Community)
            {
                delete(child.getID());
            }
        }
    }

    @Override
    public int itemCount(Community community)
    {
    	int total = 0;

        for (Collection collection :
                collectionDAO.getChildCollections(community))
        {
        	total += collectionDAO.itemCount(collection);
        }

        for (Community child : getChildCommunities(community))
        {
        	total += itemCount(child);
        }

        return total;
    }
}

