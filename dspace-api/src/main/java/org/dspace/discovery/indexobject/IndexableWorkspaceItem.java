/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.content.WorkspaceItem;

/**
 * Workspace item implementation for the IndexableObject
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public class IndexableWorkspaceItem extends IndexableInProgressSubmission<WorkspaceItem>  {

    public static final String TYPE = WorkspaceItem.class.getSimpleName();

    public IndexableWorkspaceItem(WorkspaceItem inProgressSubmission) {
        super(inProgressSubmission);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Integer getID() {
        return getIndexedObject().getID();
    }

    @Override
    public String getTypeText() {
        return "WORKSPACEITEM";
    }
}