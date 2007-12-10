/*
 * WorkspaceItem.java
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
package org.dspace.content;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.log4j.Logger;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.uri.ObjectIdentifier;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Class representing an item in the process of being submitted by a user.
 *
 * FIXME: this class could benefit from a proxy so the Collection and Item
 * aren't fully instantiated unless explicitly required. Could be wasted
 * effort, however, as the number of workspace items in memory at any given
 * time will typically be very low.
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public class WorkspaceItem implements InProgressSubmission
{
    /** log4j logger */
    private static Logger log = Logger.getLogger(WorkspaceItem.class);

    protected int id;
    protected Context context;

    private WorkspaceItemDAO dao;

    private ObjectIdentifier oid;
    private boolean hasMultipleFiles;
    private boolean hasMultipleTitles;
    private boolean publishedBefore;
    private int stageReached;
    private int pageReached;

    private Item item;
    private Collection collection;

    public WorkspaceItem(Context context, int id)
    {
        this.id = id;
        this.context = context;

        dao = WorkspaceItemDAOFactory.getInstance(context);
    }
    
    public int getID()
    {
        return id;
    }

    public ObjectIdentifier getIdentifier()
    {
        return oid;
    }

    public void setIdentifier(ObjectIdentifier oid)
    {
        this.oid = oid;
    }

    public int getStageReached()
    {
        return stageReached;
    }

    public void setStageReached(int stageReached)
    {
        this.stageReached = stageReached;
    }

    public int getPageReached()
    {
        return pageReached;
    }

    public void setPageReached(int pageReached)
    {
        this.pageReached = pageReached;
    }

    // InProgressSubmission methods
    public Item getItem()
    {
        return item;
    }

    public void setItem(Item item)
    {
        this.item = item;
    }

    public Collection getCollection()
    {
        return collection;
    }

    public void setCollection(Collection collection)
    {
        this.collection = collection;
    }

    public EPerson getSubmitter()
    {
        return item.getSubmitter();
    }

    public boolean hasMultipleFiles()
    {
        return hasMultipleFiles;
    }

    public void setMultipleFiles(boolean hasMultipleFiles)
    {
        this.hasMultipleFiles = hasMultipleFiles;
    }

    public boolean hasMultipleTitles()
    {
        return hasMultipleTitles;
    }

    public void setMultipleTitles(boolean hasMultipleTitles)
    {
        this.hasMultipleTitles = hasMultipleTitles;
    }

    public boolean isPublishedBefore()
    {
        return publishedBefore;
    }

    public void setPublishedBefore(boolean publishedBefore)
    {
        this.publishedBefore = publishedBefore;
    }

    ////////////////////////////////////////////////////////////////////
    // Utility methods
    ////////////////////////////////////////////////////////////////////

    public String toString()
    {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.MULTI_LINE_STYLE);
    }

    public boolean equals(Object o)
    {
        return EqualsBuilder.reflectionEquals(this, o);
    }

    public boolean equals(WorkspaceItem wsi)
    {
        return getID() == wsi.getID();
    }

    public int hashCode()
    {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /** Deprecated by the introduction of DAOs */
    @Deprecated
    public static WorkspaceItem create(Context context, Collection collection,
            boolean template)
        throws AuthorizeException, IOException
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        return dao.create(collection, template);
    }

    @Deprecated
    public static WorkspaceItem find(Context context, int id)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        return dao.retrieve(id);
    }

    @Deprecated
    public void update() throws AuthorizeException, IOException
    {
        dao.update(this);
    }

    @Deprecated
    public void deleteWrapper() throws AuthorizeException, IOException
    {
        dao.delete(getID());
    }

    @Deprecated
    public void deleteAll() throws AuthorizeException,
            IOException
    {
        dao.deleteAll(getID());
    }

    @Deprecated
    public static WorkspaceItem[] findAll(Context context)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems();

        return wsItems.toArray(new WorkspaceItem[0]);
    }

    @Deprecated
    public static WorkspaceItem[] findByEPerson(Context context, EPerson ep)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems(ep);

        return wsItems.toArray(new WorkspaceItem[0]);
    }

    @Deprecated
    public static WorkspaceItem[] findByCollection(Context context, Collection c)
    {
        WorkspaceItemDAO dao = WorkspaceItemDAOFactory.getInstance(context);
        List<WorkspaceItem> wsItems = dao.getWorkspaceItems(c);

        return wsItems.toArray(new WorkspaceItem[0]);
    }
}
