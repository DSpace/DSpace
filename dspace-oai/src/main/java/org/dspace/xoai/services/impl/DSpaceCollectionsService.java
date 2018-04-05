/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.CollectionsService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CommunityService;

public class DSpaceCollectionsService implements CollectionsService {

    private static final CommunityService communityService
            = ContentServiceFactory.getInstance().getCommunityService();

    @Override
    public List<UUID> getAllSubCollections(ContextService contextService, UUID communityId)
            throws SQLException
    {
        Queue<Community> comqueue = new LinkedList<>();
        List<UUID> list = new ArrayList<>();
        try {
            comqueue.add(communityService.find(contextService.getContext(), communityId));
        } catch (ContextServiceException e) {
            throw new SQLException(e);
        }
        while (!comqueue.isEmpty())
        {
            Community c = comqueue.poll();
            for (Community sub : c.getSubcommunities())
                comqueue.add(sub);
            for (Collection col : c.getCollections())
                if (!list.contains(col.getID()))
                    list.add(col.getID());
        }
        return list;
    }

    @Override
    public List<Community> flatParentCommunities(Collection c)
            throws SQLException
    {
        Queue<Community> queue = new LinkedList<>();
        List<Community> result = new ArrayList<>();
        for (Community com : c.getCommunities())
            queue.add(com);

        while (!queue.isEmpty())
        {
            Community p = queue.poll();
            List<Community> par = p.getParentCommunities();
            if (par != null)
                queue.addAll(par);
            if (!result.contains(p))
                result.add(p);
        }

        return result;
    }

    @Override
    public List<Community> flatParentCommunities(Community c)
            throws SQLException
    {
        Queue<Community> queue = new LinkedList<>();
        List<Community> result = new ArrayList<>();

        queue.add(c);

        while (!queue.isEmpty())
        {
            Community p = queue.poll();
            List<Community> par = p.getParentCommunities();
            if (par != null)
                queue.addAll(par);
            if (!result.contains(p))
                result.add(p);
        }

        return result;
    }

    @Override
    public List<Community> flatParentCommunities(Context context, Item c)
            throws SQLException
    {
        Queue<Community> queue = new LinkedList<>();
        List<Community> result = new ArrayList<>();

        for (Collection collection : c.getCollections())
            queue.addAll(communityService.getAllParents(context, collection));

        while (!queue.isEmpty())
        {
            Community p = queue.poll();
            List<Community> par = p.getParentCommunities();
            if (par != null)
                queue.addAll(par);
            if (!result.contains(p))
                result.add(p);
        }

        return result;
    }
}
