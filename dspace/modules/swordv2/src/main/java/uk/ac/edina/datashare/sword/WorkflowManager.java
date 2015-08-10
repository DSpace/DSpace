package uk.ac.edina.datashare.sword;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.sword2.DSpaceSwordException;
import org.dspace.sword2.DepositResult;
import org.dspace.sword2.VerboseDescription;
import org.dspace.sword2.WorkflowManagerDefault;
import org.dspace.sword2.WorkflowTools;
import org.swordapp.server.Deposit;

/**
 * DataShare SWORD workflow manager.
 */
public class WorkflowManager extends WorkflowManagerDefault{
    private static final Logger LOG = Logger.getLogger(WorkflowManager.class);
    
    public void resolveState(
            Context context,
            Deposit deposit,
            DepositResult result,
            VerboseDescription verboseDescription,
            boolean containerOperation)
            throws DSpaceSwordException
    {
        // see comment in super.resolveState
        if (!containerOperation){
            return;
        }
        
        // if we get to here this is a container operation, and we can decide how best to process
        Item item = result.getItem();        
        WorkflowTools wft = new WorkflowTools();
        LOG.debug("Resolve State: in progress = " + deposit.isInProgress() +
                ", in workspace = " + wft.isItemInWorkspace(context, item));
        // Note: METS ingest currently doesn't use the following as
        // wft.isItemInWorkspace returns false
        if (!deposit.isInProgress() && wft.isItemInWorkspace(context, item)){
            SwordUtil.complete(context, item);
        }
        
        super.resolveState(context, deposit, result, verboseDescription, containerOperation);
    }
}
