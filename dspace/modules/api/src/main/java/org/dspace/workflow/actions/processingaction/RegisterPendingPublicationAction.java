package org.dspace.workflow.actions.processingaction;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.workflow.Step;
import org.dspace.workflow.WorkflowItem;
import org.dspace.workflow.actions.ActionResult;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.log4j.Logger;
import org.dspace.content.DCDate;
import org.dspace.content.MetadataSchema;
import org.dspace.identifier.IdentifierException;
import org.dspace.identifier.IdentifierService;
import org.dspace.utils.DSpace;
import org.dspace.workflow.DryadWorkflowUtils;
import org.dspace.workflow.WorkflowManager;

/**
 * This action registers items that are in publication blackout.  It only applies to items in "Pending Publication Step"
 * This action must be executed after the item is in pendingPublicationStep.
 */
public class RegisterPendingPublicationAction extends ProcessingAction{

    private static final int BLACKOUT_REGISTERED = 0;
    private static final Logger log = Logger.getLogger(RegisterPendingPublicationAction.class);

    @Override
    public void activate(Context c, WorkflowItem wfItem) throws SQLException {
        try {
            if(DryadWorkflowUtils.isDataPackage(wfItem)) {
                Item dataPackage = wfItem.getItem();
                String usersName = WorkflowManager.getEPersonName(c.getCurrentUser());
                String now = DCDate.getCurrent().toString();
                String provDescription = getProvenanceStartId() + " Entered publication blackout by " + usersName + "," +
                        " on " + now + " (GMT) ";
                dataPackage.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
                dataPackage.addMetadata(MetadataSchema.DC_SCHEMA, "date", "accessioned", null, now.toString());
                dataPackage.update();
                // Also add this provenance to the files
                Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, dataPackage);
                for(Item dataFile : dataFiles) {
                    dataFile.addMetadata(MetadataSchema.DC_SCHEMA, "description", "provenance", "en", provDescription);
                    dataFile.addMetadata(MetadataSchema.DC_SCHEMA, "date", "accessioned", null, now.toString());
                    dataFile.update();
                }
            }
        } catch (AuthorizeException e) {
            log.error("Error while activating register pending publication action", e);
        }
    }

    @Override
    public ActionResult execute(Context c, WorkflowItem wfi, Step step, HttpServletRequest request) throws SQLException, AuthorizeException, IOException {
        return registerItemInBlackout(c, wfi);
    }

    private ActionResult registerItemInBlackout(Context c, WorkflowItem wfi) throws AuthorizeException, SQLException, IOException {
        DSpace dspace = new DSpace();
        IdentifierService service = new DSpace().getSingletonService(IdentifierService.class);
        try {
            service.register(c, wfi.getItem());
            Item[] dataFiles = DryadWorkflowUtils.getDataFiles(c, wfi.getItem());
            for (Item dataFile : dataFiles) {
                service.register(c, dataFile);
            }
        } catch (IdentifierException e) {
            throw new IOException(e);
        }
        return new ActionResult(ActionResult.TYPE.TYPE_OUTCOME, BLACKOUT_REGISTERED);
    }
}
