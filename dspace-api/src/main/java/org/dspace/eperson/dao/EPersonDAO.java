/*
 * EPersonDAO.java
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
package org.dspace.eperson.dao;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson.EPersonMetadataField;
import org.dspace.eperson.Group;
import org.dspace.storage.dao.CRUD;

/**
 * @author James Rutherford
 */
public abstract class EPersonDAO implements CRUD<EPerson>
{
    protected Logger log = Logger.getLogger(EPersonDAO.class);

    protected Context context;

    public EPerson create() throws AuthorizeException
    {
        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to create an EPerson");
        }

        return null;
    }

    // FIXME: This should be called something else, but I can't think of
    // anything suitable. The reason this can't go in create() is because we
    // need access to the object that was created, but we can't reach into the
    // subclass to get it (storing it as a protected member variable would be
    // even more filthy).
    protected final EPerson create(EPerson eperson) throws AuthorizeException
    {
        log.info(LogManager.getHeader(context, "create_eperson", "eperson_id="
                    + eperson.getID()));

        return eperson;
    }

    public EPerson retrieve(int id)
    {
        return (EPerson) context.fromCache(EPerson.class, id);
    }

    public EPerson retrieve(UUID uuid)
    {
        return null;
    }

    public EPerson retrieve(EPersonMetadataField field, String value)
    {
        if ((field != EPersonMetadataField.EMAIL) &&
            (field != EPersonMetadataField.NETID))
        {
            throw new IllegalArgumentException(field + " isn't allowed here");
        }

        return null;
    }

    public void update(EPerson eperson) throws AuthorizeException
    {
        // Check authorisation - if you're not the eperson
        // see if the authorization system says you can
        if (!context.ignoreAuthorization() && (
                    (context.getCurrentUser() == null) ||
                    !eperson.equals(context.getCurrentUser())))
        {
            AuthorizeManager.authorizeAction(context, eperson, Constants.WRITE);
        }

        log.info(LogManager.getHeader(context, "update_eperson",
                "eperson_id=" + eperson.getID()));
    }

    public void delete(int id) throws AuthorizeException
    {
        EPerson eperson = retrieve(id);
        update(eperson); // Sync in-memory object before removal

        // authorized?
        if (!AuthorizeManager.isAdmin(context))
        {
            throw new AuthorizeException(
                    "You must be an admin to delete an EPerson");
        }

        // Remove from cache
        context.removeCached(eperson, id);

        log.info(LogManager.getHeader(context, "delete_eperson",
                "eperson_id=" + id));
    }

    public List<EPerson> getEPeople()
    {
        return getEPeople(EPerson.LASTNAME);
    }

    /**
     * Find all the epeople that match a particular query
     * <ul>
     * <li><code>ID</code></li>
     * <li><code>LASTNAME</code></li>
     * <li><code>EMAIL</code></li>
     * <li><code>NETID</code></li>
     * </ul>
     * 
     * @return array of EPerson objects
     */
    public abstract List<EPerson> getEPeople(int sortField);

    /**
     * FIXME: For consistency, this should take a sort parameter.
     */
    public abstract List<EPerson> getEPeople(Group group);

    /**
     * FIXME: For consistency, this should take a sort parameter. The
     * difference between this and getEPeople(Group group) is that this one
     * recurses into subgroups, whereas the other doesn't. It would be possible
     * to implement this in an implementation-agnostic way, but that would
     * incur a performance penalty, at least for the (default) RDBMS
     * implementation.
     */
    public abstract List<EPerson> getAllEPeople(Group group);

    // For reference, here's how we'd do it in a storage-layer agnostic way.
//    public List<EPerson> getAllEPeople(Group group)
//    {
//        List<EPerson> epeople = getEPeople(group);
//
//        for (Group subGroup :
//                GroupDAOFactory.getInstance(context).getMemberGroups(group))
//        {
//            epeople.addAll(getAllEPeople(subGroup));
//        }
//
//        return epeople;
//    }

    /**
     * Find the epeople that match the search query across firstname, lastname
     * or email
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     *
     * @return array of EPerson objects
     */
    public List<EPerson> search(String query)
    {
        return search(query, -1, -1);
    }

    /**
     * Find the epeople that match the search query across firstname, lastname
     * or email. This method also allows offsets and limits for pagination
     * purposes.
     *
     * @param context
     *            DSpace context
     * @param query
     *            The search string
     * @param offset
     *            Inclusive offset
     * @param limit
     *            Maximum number of matches returned
     *
     * @return array of EPerson objects
     */
    public List<EPerson> search(String query, int offset, int limit)
    {
        if (limit == 0)
        {
            return new ArrayList<EPerson>();
        }
        
        if (query == null || "".equals(query))
        {
            List<EPerson> epeople = getEPeople();

            if ((offset > -1) || (limit > -1))
            {
                int toIndex = epeople.size();

                if (offset < 0)
                {
                    offset = 0;
                }
                if (limit != -1)
                {
                    // If the limit is set to -1 that means there is no limit,
                    // and we use the toIndex from above, otherwise we just add
                    // the limit to the offset to get the toIndex.
                    if ((offset + limit) <= epeople.size())
                    {
                        toIndex = offset + limit;
                    }
                }

                return epeople.subList(offset, toIndex);
            }
            else
            {
                return epeople;
            }
        }

        return null;
    }
}
