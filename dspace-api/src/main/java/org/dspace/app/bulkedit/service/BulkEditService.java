/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit.service;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;
import org.dspace.workflow.WorkflowException;

/**
 * Service for processing and applying changes found in {@link BulkEditChange}
 *
 * Warning: This service and its implementation are stateful, in that a new instance will be created every time it is
 *          requested. This is by design because the service will keep information about multiple related changes until
 *          it is done applying them all and this ensures none of the information leaks between other calls/processes.
 *          This means the service should never be Autowired and should instead be requested through the
 *          {@link BulkEditServiceFactory} wherever the call is made to parse and/or apply the changes.
 */
public interface BulkEditService {
    /**
     * Import or update Items from a List of {@link BulkEditChange} in batches
     * The {@link BulkEditChange} objects may have some of their properties updated to reflect changes that were made
     * while applying the changes, for example attaching a newly created {@link org.dspace.content.Item}
     *
     * Warning: This method will process the list in batches, resulting in commits happening between each batch,
     *          so commits outside of this method are unnecessary to persist the changes made
     *
     * @param context               DSpace context
     * @param bulkEditChanges       List of BulkEditChanges containing information about the to-be-imported or updated
     *                              items
     */
    void applyBulkEditChanges(Context context, List<BulkEditChange> bulkEditChanges)
        throws SQLException, AuthorizeException, IOException, MetadataImportException, WorkflowException;

    /**
     * Import or update an Item from a {@link BulkEditChange}
     * The {@link BulkEditChange} object may have some of their properties updated to reflect changes that were made
     * while applying the change, for example attaching a newly created {@link org.dspace.content.Item}
     * @param context               DSpace context
     * @param bulkEditChange        BulkEditChange containing information about the to-be-imported or updated item
     */
    void applyBulkEditChange(Context context, BulkEditChange bulkEditChange)
        throws SQLException, AuthorizeException, IOException, MetadataImportException, WorkflowException;

    /**
     * Set the handler
     * @param handler   DSpaceRunnableHandler to output messages or content to
     */
    void setHandler(DSpaceRunnableHandler handler);

    /**
     * Set whether we want to use the collection's template
     * @param useCollectionTemplate Use the item's collection template when creating a new item
     */
    void setUseCollectionTemplate(boolean useCollectionTemplate);

    /**
     * Set whether to allow new items to go through workflow
     * @param useWorkflow   Allow new items to go through workflow
     */
    void setUseWorkflow(boolean useWorkflow);

    /**
     * Set whether to allow workflow notifications
     * @param workflowNotify    Allow workflow notifications for new workflow items
     */
    void setWorkflowNotify(boolean workflowNotify);

    /**
     * Set whether to archive items or leave them in their workspace state
     * @param archive   Archive newly created items
     */
    void setArchive(boolean archive);
}
