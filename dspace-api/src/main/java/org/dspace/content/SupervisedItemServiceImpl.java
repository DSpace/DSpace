/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.service.SupervisedItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.springframework.beans.factory.annotation.Autowired;

public class SupervisedItemServiceImpl implements SupervisedItemService
{

    @Autowired(required = true)
    protected WorkspaceItemService workspaceItemService;

    protected SupervisedItemServiceImpl()
    {

    }

    @Override
    public List<WorkspaceItem> getAll(Context context)
        throws SQLException
    {
        return workspaceItemService.findAllSupervisedItems(context);
    }

    @Override
    public List<WorkspaceItem> findbyEPerson(Context context, EPerson ep)
        throws SQLException
    {
        return workspaceItemService.findSupervisedItemsByEPerson(context, ep);
    }
    
}
