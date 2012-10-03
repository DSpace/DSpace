/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * 
 * @author Lyncode Development Team <dspace@lyncode.com>
 */
public class XOAIDatabaseManager
{
    public static List<Integer> getAllSubCollections(Context ct, int i)
            throws SQLException
    {
        Queue<Community> comqueue = new LinkedList<Community>();
        List<Integer> list = new ArrayList<Integer>();
        comqueue.add(Community.find(ct, i));
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

    public static List<Community> flatParentCommunities(Collection c)
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

    public static List<Community> flatParentCommunities(Community c)
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

    public static List<Community> flatParentCommunities(Item c)
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
