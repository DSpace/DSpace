/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.database;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.xoai.services.api.context.ContextService;
import org.dspace.xoai.services.api.context.ContextServiceException;
import org.dspace.xoai.services.api.database.CollectionsService;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class DSpaceCollectionsService implements CollectionsService {

    private ContextService contextService;

    public List<Integer> getAllSubCollections(int communityId)
            throws SQLException
    {
        Queue<Community> comqueue = new LinkedList<Community>();
        List<Integer> list = new ArrayList<Integer>();
        try {
            comqueue.add(Community.find(contextService.getContext(), communityId));
        } catch (ContextServiceException e) {
            throw new SQLException(e);
        }
        while (!comqueue.isEmpty())
        {
            Community c = comqueue.poll();
            for (Community sub : c.getSubcommunities())
                comqueue.add(sub);
            for (Collection col : c.getCollections())
                if (!list.contains(col))
                    list.add(col.getID());
        }
        return list;
    }

    public List<Community> flatParentCommunities(Collection c)
            throws SQLException
    {
        Queue<Community> queue = new LinkedList<Community>();
        List<Community> result = new ArrayList<Community>();
        for (Community com : c.getCommunities())
            queue.add(com);

        while (!queue.isEmpty())
        {
            Community p = queue.poll();
            Community par = p.getParentCommunity();
            if (par != null)
                queue.add(par);
            if (!result.contains(p))
                result.add(p);
        }

        return result;
    }

    public List<Community> flatParentCommunities(Community c)
            throws SQLException
    {
        Queue<Community> queue = new LinkedList<Community>();
        List<Community> result = new ArrayList<Community>();

        queue.add(c);

        while (!queue.isEmpty())
        {
            Community p = queue.poll();
            Community par = p.getParentCommunity();
            if (par != null)
                queue.add(par);
            if (!result.contains(p))
                result.add(p);
        }

        return result;
    }

    public List<Community> flatParentCommunities(Item c)
            throws SQLException
    {
        Queue<Community> queue = new LinkedList<Community>();
        List<Community> result = new ArrayList<Community>();

        for (Community com : c.getCommunities())
            queue.add(com);

        while (!queue.isEmpty())
        {
            Community p = queue.poll();
            Community par = p.getParentCommunity();
            if (par != null)
                queue.add(par);
            if (!result.contains(p))
                result.add(p);
        }

        return result;
    }
}
