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
