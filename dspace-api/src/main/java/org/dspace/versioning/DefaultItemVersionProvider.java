/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.versioning;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.core.Context;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;

import java.io.IOException;
import java.sql.SQLException;

/**
 *
 *
 * @author Fabio Bolognesi (fabio at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 */
public class DefaultItemVersionProvider extends AbstractVersionProvider implements ItemVersionProvider
{

    public Item createNewItemAndAddItInWorkspace(Context context, Item nativeItem) {
        try
        {
            WorkspaceItem workspaceItem = WorkspaceItem.create(context, nativeItem.getOwningCollection(), false);
            Item itemNew = workspaceItem.getItem();
            itemNew.update();
            return itemNew;
        }catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }catch (AuthorizeException e) {
           throw new RuntimeException(e.getMessage(), e);
        }catch (IOException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void deleteVersionedItem(Context c, Version versionToDelete, VersionHistory history)
    {
        try
        {
            // if versionToDelete is the current version we have to reinstate the previous version
            // and reset canonical
            if(history.isLastVersion(versionToDelete) && history.size() > 1)
            {
                // reset the previous version to archived
                Item item = history.getPrevious(versionToDelete).getItem();
                item.setArchived(true);
                item.update();
            }

            // assign tombstone to the Identifier and reset canonical to the previous version only if there is a previous version
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            Item itemToDelete=versionToDelete.getItem();
            identifierService.delete(c, itemToDelete);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (IdentifierException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public Item updateItemState(Context c, Item itemNew, Item previousItem)
    {
        try
        {
            copyMetadata(itemNew, previousItem);
            createBundlesAndAddBitstreams(c, itemNew, previousItem);
            IdentifierService identifierService = new DSpace().getSingletonService(IdentifierService.class);
            try
            {
                identifierService.reserve(c, itemNew);
            } catch (IdentifierException e) {
                throw new RuntimeException("Can't create Identifier!");
            }
            itemNew.update();
            return itemNew;
        }catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        } catch (AuthorizeException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }
}
