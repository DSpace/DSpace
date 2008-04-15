/*
 * CommunityDAO.java
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

import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.dao.CRUD;
import org.dspace.dao.Link;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;
import org.dspace.uri.dao.*;

import java.util.List;
import java.util.UUID;

/**
 * @author James Rutherford
 */
public abstract class CommunityDAO extends ContentDAO<CommunityDAO>
        implements CRUD<Community>, Link<DSpaceObject, DSpaceObject>
{
    protected static Logger log = Logger.getLogger(CommunityDAO.class);

    protected Context context;
    protected BitstreamDAO bitstreamDAO;
    protected CollectionDAO collectionDAO;
    protected GroupDAO groupDAO;
    protected ExternalIdentifierDAO identifierDAO;
    protected ObjectIdentifierDAO oidDAO;

    protected CommunityDAO childDAO;

    /**
     * The allowed metadata fields for Communities are defined in the following
     * enum. This should make reading / writing all metadatafields a lot less
     * error-prone, not to mention concise and tidy!
     *
     * FIXME: Do we want this exposed anywhere else? Probably not...
     */
    protected enum CommunityMetadataField
    {
        NAME ("name"),
        SHORT_DESCRIPTION ("short_description"),
        INTRODUCTORY_TEXT ("introductory_text"),
        COPYRIGHT_TEXT ("copyright_text"),
        SIDE_BAR_TEXT ("side_bar_text");

        private String name;

        private CommunityMetadataField(String name)
        {
            this.name = name;
        }

        public String toString()
        {
            return name;
        }
    }

    public CommunityDAO()
    {
    }

    public CommunityDAO(Context context)
    {
        try
        {
            this.context = context;

            bitstreamDAO = BitstreamDAOFactory.getInstance(context);
            collectionDAO = CollectionDAOFactory.getInstance(context);
            groupDAO = GroupDAOFactory.getInstance(context);
            identifierDAO = ExternalIdentifierDAOFactory.getInstance(context);
            oidDAO = ObjectIdentifierDAOFactory.getInstance(context);
        }
        catch (ExternalIdentifierStorageException e)
        {
            throw new RuntimeException(e);
        }
        catch (ObjectIdentifierStorageException e)
        {
            throw new RuntimeException(e);
        }
    }

    public CommunityDAO getChild()
    {
        return childDAO;
    }

    public void setChild(CommunityDAO childDAO)
    {
        this.childDAO = childDAO;
    }

    public Community create() throws AuthorizeException
    {
        return childDAO.create();
    }

    public Community retrieve(int id)
    {
        return childDAO.retrieve(id);
    }

    public Community retrieve(UUID uuid)
    {
        return childDAO.retrieve(uuid);
    }

    public void update(Community community) throws AuthorizeException
    {
        childDAO.update(community);
    }

    public void delete(int id) throws AuthorizeException
    {
        childDAO.delete(id);
    }

    public List<Community> getCommunities()
    {
        return childDAO.getCommunities();
    }

    public List<Community> getTopLevelCommunities()
    {
        return childDAO.getTopLevelCommunities();
    }

    public List<Community> getChildCommunities(Community community)
    {
        return childDAO.getChildCommunities(community);
    }

    /**
     * Get the communities the given community or collection appears in. Note
     * that this only returns the immediate parents.
     */
    public List<Community> getParentCommunities(DSpaceObject dso)
    {
        return childDAO.getParentCommunities(dso);
    }

    public List<Community> getAllParentCommunities(DSpaceObject dso)
    {
        return childDAO.getAllParentCommunities(dso);
    }

    public void link(DSpaceObject parent, DSpaceObject child)
            throws AuthorizeException
    {
        childDAO.link(parent, child);
    }

    public void unlink(DSpaceObject parent, DSpaceObject child)
            throws AuthorizeException
    {
        childDAO.unlink(parent, child);
    }

    public boolean linked(DSpaceObject parent, DSpaceObject child)
    {
        return childDAO.linked(parent, child);
    }

    /**
     * Straightforward utility method for counting the number of Items in the
     * given Community. There is probably a way to be smart about this. Also,
     * this strikes me as the kind of method that shouldn't really be in here.
     */
    public int itemCount(Community community)
    {
        return childDAO.itemCount(community);
    }
}

