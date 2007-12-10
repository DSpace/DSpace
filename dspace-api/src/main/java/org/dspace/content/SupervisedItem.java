/*
 * SupervisedItem.java
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

import java.util.List;

import org.dspace.content.dao.SupervisedItemDAO;
import org.dspace.content.dao.SupervisedItemDAOFactory;
import org.dspace.content.dao.WorkspaceItemDAO;
import org.dspace.content.dao.WorkspaceItemDAOFactory;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.dao.GroupDAO;
import org.dspace.eperson.dao.GroupDAOFactory;

/**
 * Class to handle WorkspaceItems which are being supervised.  It extends the
 * WorkspaceItem class and adds the methods required to be a Supervised Item.
 *
 * @author Richard Jones
 * @version  $Revision$
 */
public class SupervisedItem extends WorkspaceItem
{
    private GroupDAO groupDAO;
    private WorkspaceItemDAO wsiDAO;
    
    public SupervisedItem(Context context, int id)
    {
        // construct a new workspace item
        super(context, id);

        wsiDAO = WorkspaceItemDAOFactory.getInstance(context);
        groupDAO = GroupDAOFactory.getInstance(context);
    }

    @Deprecated
    public static SupervisedItem[] getAll(Context context)
    {
        SupervisedItemDAO dao = SupervisedItemDAOFactory.getInstance(context);
        List<SupervisedItem> items = dao.getSupervisedItems();

        return items.toArray(new SupervisedItem[0]);
    }
    
    @Deprecated
    public static SupervisedItem[] findbyEPerson(Context context, EPerson ep)
    {
        SupervisedItemDAO dao = SupervisedItemDAOFactory.getInstance(context);
        List<SupervisedItem> items = dao.getSupervisedItems(ep);

        return items.toArray(new SupervisedItem[0]);
    }
    
    @Deprecated
    public Group[] getSupervisorGroups(Context context, int id)
    {
        WorkspaceItem wsi = wsiDAO.retrieve(id);
        List<Group> groups = groupDAO.getSupervisorGroups(wsi);

        return groups.toArray(new Group[0]);
    }
    
    @Deprecated
    public Group[] getSupervisorGroups()
    {
        return getSupervisorGroups(context, getID());
    }
}
