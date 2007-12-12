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

import java.util.List;
import java.util.UUID;

import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;
import org.dspace.dao.StackableDAO;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.EPerson.EPersonMetadataField;
import org.dspace.eperson.Group;

/**
 * @author James Rutherford
 */
public abstract class EPersonDAO extends StackableDAO<EPersonDAO>
        implements CRUD<EPerson>
{
    protected Logger log = Logger.getLogger(EPersonDAO.class);

    protected EPersonDAO childDAO;

    protected Context context;

    public EPersonDAO()
    {
    }

    public EPersonDAO(Context context)
    {
        this.context = context;
    }

    public EPersonDAO getChild()
    {
        return childDAO;
    }

    public void setChild(EPersonDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public EPerson create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public EPerson retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public EPerson retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public EPerson retrieve(EPersonMetadataField field, String value)
    {
        return childDAO.retrieve(field, value);
    }

    public void update(EPerson eperson) throws AuthorizeException
    {
        childDAO.update(eperson);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public List<EPerson> getEPeople()
    {
        return childDAO.getEPeople();
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
    public List<EPerson> getEPeople(int sortField)
    {
        return childDAO.getEPeople(sortField);
    }

    /**
     * FIXME: For consistency, this should take a sort parameter.
     */
    public List<EPerson> getEPeople(Group group)
    {
        return childDAO.getEPeople(group);
    }

    /**
     * FIXME: For consistency, this should take a sort parameter. The
     * difference between this and getEPeople(Group group) is that this one
     * recurses into subgroups, whereas the other doesn't. It would be possible
     * to implement this in an implementation-agnostic way, but that would
     * incur a performance penalty, at least for the (default) RDBMS
     * implementation.
     */
    public List<EPerson> getAllEPeople(Group group)
    {
        return childDAO.getAllEPeople(group);
    }

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
        return childDAO.search(query);
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
        return childDAO.search(query, offset, limit);
    }
}
