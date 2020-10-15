package org.dspace.harvest.cristin;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;
import org.dspace.harvest.HarvestedItem;
import org.jdom.Element;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class DefaultIngestionWorkflow implements IngestionWorkflow
{
    @Override
    public Item preUpdate(Context context, Item item, Collection targetCollection, HarvestedItem hi, List<Element> descMd, Element oreREM)
    {
        return item;
    }

    @Override
    public void postUpdate(Context context, Item item)
    {

    }

    @Override
    public Item postCreate(Context context, WorkspaceItem wi, String handle)
            throws SQLException, IOException, AuthorizeException
    {
        InstallItemService installItemService = ContentServiceFactory.getInstance().getInstallItemService();
        WorkspaceItemService workspaceItemService = ContentServiceFactory.getInstance().getWorkspaceItemService();

        try
        {
            return installItemService.installItem(context, wi, handle);
        }
        // clean up the workspace item if something goes wrong before
        catch (SQLException | IOException | AuthorizeException ex)
        {
            workspaceItemService.deleteWrapper(context, wi);
            throw ex;
        }
    }

    @Override
    public boolean updateBitstreams(Context context, Item item, HarvestedItem hi)
            throws SQLException, IOException, AuthorizeException
    {
        return true;
    }
}
