/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import org.dspace.content.WorkspaceItem;
import org.dspace.discovery.indexobject.IndexableWorkspaceItem;

/**
 * Factory interface for indexing/retrieving workspace item objects in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface WorkspaceItemIndexFactory
        extends InprogressSubmissionIndexFactory<IndexableWorkspaceItem, WorkspaceItem> {
}