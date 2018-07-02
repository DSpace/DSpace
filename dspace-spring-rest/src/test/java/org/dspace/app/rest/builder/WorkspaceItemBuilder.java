/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Builder to construct WorkspaceItem objects
 *
 **/
public class WorkspaceItemBuilder extends AbstractDSpaceObjectBuilder<Item> {

    private WorkspaceItem workspaceItem;
    private Item item;

    protected WorkspaceItemBuilder(Context context) {
        super(context);
    }

    public static WorkspaceItemBuilder createWorkspaceItem(final Context context, final Collection col) {
        WorkspaceItemBuilder builder = new WorkspaceItemBuilder(context);
        return builder.create(context, col);
    }

    private WorkspaceItemBuilder create(final Context context, final Collection col) {
        this.context = context;

        try {
            workspaceItem = workspaceItemService.create(context, col, false);
            item = workspaceItem.getItem();
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    @Override
    public Item build() {
        try {
            return item;
        } catch (Exception e) {
            return handleException(e);
        }
    }

    protected void cleanup() throws Exception {
        delete(item);
    }

    @Override
    protected DSpaceObjectService<Item> getService() {
        return itemService;
    }

}
