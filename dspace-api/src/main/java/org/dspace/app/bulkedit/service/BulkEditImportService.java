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

import org.dspace.app.bulkedit.BulkEditChange;
import org.dspace.app.bulkedit.MetadataImportException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowException;

public interface BulkEditImportService {
    void importBulkEditChange(Context context, BulkEditChange bulkEditChange, boolean useCollectionTemplate,
                              boolean useWorkflow, boolean workflowNotify)
        throws SQLException, AuthorizeException, IOException, MetadataImportException, WorkflowException;
}
