/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dspace.curate;

import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.workflow.WorkflowConfigurationException;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.WorkflowManager;

/**
 * Task to fix files that were not completely installed due to an exception
 * in the workflow after installing a data package.
 * @author dan
 */
public class PartiallyInstalledDataFiles extends AbstractCurationTask {
    // From August-October 2013, there was a bug in WorkflowManager.java
    // that prevented files from being installed if the associated journal
    // was not integrated.  In these cases, data packages were installed
    // but their associated files were left in the workflow
    private static Logger log = Logger.getLogger(PartiallyInstalledDataFiles.class);

    // This query identifies workflow ids of files that are in the workflow
    // but their data packages are archived
    private static final String WORKFLOW_IDS_QUERY =
        "select" +
        " wfi.workflow_id as workflow_id, " +
        " mdv.text_value as file_doi " +
        "from" +
        " workflowitem wfi," +
        " metadatavalue mdv" +
        " where" +
        " mdv.metadata_field_id = 17" +
        " and mdv.item_id = wfi.item_id" +
        " and wfi.item_id in (" +
        "select item_f.item_id from" +
        "    item item_p," +
        "    item item_f," +
        "    metadatavalue mvp," +
        "    metadatavalue mvf" +
        "    where" +
        "    item_p.in_archive = 't' and" +
        "    item_p.withdrawn = 'f' and" +
        "    item_f.in_archive = 'f' and" +
        "    item_f.withdrawn = 'f' and" +
        "    item_p.item_id = mvp.item_id and" +
        "    item_f.item_id = mvf.item_id and" +
        "    mvp.metadata_field_id = 17 and" +
        "    mvf.metadata_field_id = 42 and" +
        "    mvp.text_value = mvf.text_value and" +
        "    item_p.item_id = ? and" +
        "    item_f.item_id not in (select item_id from workspaceitem)" +
        "    ) order by wfi.item_id asc";

    private Map<Integer, String> partiallyInstalledFiles;
    private Context dspaceContext;
    private PreparedStatement statement;
    @Override
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        try {
            dspaceContext = new Context();
        } catch (SQLException ex) {
            log.error("Exception instantiating context", ex);
        }
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        // This task is only valid to run on the Dryad Data Packages collection
        if(dso.getType() == Constants.COLLECTION) {
            partiallyInstalledFiles = new HashMap<Integer, String>();
            distribute(dso);
            formatResults();
            try {
                installPartiallyInstalledDataFiles();
            } catch (SQLException ex) {
                log.error("Exception installing data files", ex);
                return Curator.CURATE_ERROR;
            } catch (WorkflowConfigurationException ex) {
                log.error("Exception installing data files", ex);
                return Curator.CURATE_ERROR;
            } catch (AuthorizeException ex) {
                log.error("Exception installing data files", ex);
                return Curator.CURATE_ERROR;
            }
            return Curator.CURATE_SUCCESS;
        } else {
            return Curator.CURATE_SKIP;
        }
    }

    @Override
    protected void performItem(Item item) throws SQLException, IOException {
        statement = dspaceContext.getDBConnection().prepareStatement(WORKFLOW_IDS_QUERY);
        findPartiallyInstalledDataFiles(item);
        statement.close();
    }


    private void findPartiallyInstalledDataFiles(Item dataPackage) throws IOException, SQLException {
        Connection dbConnection = dspaceContext.getDBConnection();
        statement.setInt(1, dataPackage.getID());
        ResultSet rs = statement.executeQuery();
        while(rs.next()) {
            String fileDOI = rs.getString("file_doi");
            Integer workflowId = rs.getInt("workflow_id");
            partiallyInstalledFiles.put(workflowId, fileDOI);
        }
        rs.close();
    }

    private void installPartiallyInstalledDataFiles() throws IOException, SQLException, WorkflowConfigurationException, AuthorizeException {
        dspaceContext.turnOffAuthorisationSystem();
        for(Integer workflowId : partiallyInstalledFiles.keySet()) {
            WorkflowItem wfi = WorkflowItem.find(dspaceContext, workflowId);
            WorkflowManager.archive(dspaceContext, wfi, false);
        }
        dspaceContext.restoreAuthSystemState();
        dspaceContext.commit();
    }

    private void formatResults() {
        StringBuilder sb = new StringBuilder();
        sb.append("workflow_id, doi\n");
        List<Integer> workflowIDs = new ArrayList<Integer>() {{ addAll(partiallyInstalledFiles.keySet()); }};
        Collections.sort(workflowIDs);
        for(Integer workflowID : workflowIDs) {
            String DOI = partiallyInstalledFiles.get(workflowID);
            sb.append(workflowID);
            sb.append(",'");
            sb.append(DOI);
            sb.append("'\n");
        }
        report(sb.toString());
    }


}
