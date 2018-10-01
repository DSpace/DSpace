/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.builder;

import org.dspace.content.Collection;
import org.dspace.content.WorkspaceItem;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.core.Context;

/**
 * Builder to construct WorkspaceItem objects
 *
 **/
public class WorkspaceItemBuilder extends AbstractBuilder<WorkspaceItem, WorkspaceItemService> {

    private WorkspaceItem workspaceItem;

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
        } catch (Exception e) {
            return handleException(e);
        }

        return this;
    }

    @Override
    public WorkspaceItem build() {
            return workspaceItem;
    }

    @Override
    public void delete(WorkspaceItem dso) throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            WorkspaceItem attachedDso = c.reloadEntity(dso);
            if (attachedDso != null) {
                getService().deleteAll(c, attachedDso);
            }
            c.complete();
        }

        indexingService.commit();
    }

    @Override
    protected void cleanup() throws Exception {
        delete(workspaceItem);
    }

    @Override
    protected WorkspaceItemService getService() {
        return workspaceItemService;
    }

}
