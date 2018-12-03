/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.curate;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.datadryad.api.DryadDataPackage;
import org.datadryad.api.DashService;
import org.datadryad.rest.models.Package;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.ConfigurationManager;

/**
 * TransferToDash processes a data package and sends it to a DASH-based Dryad system.
 *
 * The task succeeds if it was able to process all required metadata and stats,
 * otherwise it fails. If the transfer was successful, results are recorded in the
 * metadata field dryad.dashTransferDate
 *
 * Input: a single data package OR a collection that contains data packages
 * Output: list of DOIs of the processed items
 * Side Effects: Data package is transferred to DASH-based Dryad, Data Package is updated
 *               with metadata indicating date of successfull transfer.
 *
 * To use from the command line:
 * /opt/dryad/bin/dspace curate -v -t transfertodash -i <handle-without-hdl-prefix> -r -
 *
 * @author Ryan Scherle
 */

@Suspendable
public class TransferToDash extends AbstractCurationTask {
    
    private static Logger log = Logger.getLogger(TransferToDash.class);
    private static long total = 0;
    private static int delaySecs = 0;
    private DashService dashService;
    private Context context;

    
    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        dashService = new DashService();
        String stringDelaySecs = ConfigurationManager.getProperty("dash.submissions.delaySeconds");
        try {
            delaySecs = Integer.parseInt(stringDelaySecs);
            context = new Context();
        } catch (Exception e) {
            log.error("Unable to initialize transfer", e);
        }
    }
 
    
    /**
     * Perform the curation task upon passed DSO
     *
     * @param dso the DSpace object
     * @throws IOException
     */
    @Override
    public int perform(DSpaceObject dso) throws IOException {
        log.info("performing TransferToDash task " + total++ );
                        
        if (dso.getType() == Constants.COLLECTION) {
            // indicate that the "report" output from this class will consist only of packageDOIs
            report("packageDOI");
        } else if (dso.getType() == Constants.ITEM) {
            DryadDataPackage dataPackage = new DryadDataPackage((Item)dso);
            Package pkg = new Package(dataPackage);
            String packageDOI = dataPackage.getIdentifier();
            dashService.putDataset(pkg);
            dashService.migrateProvenances(pkg);
            dashService.postDataFileReferences(context, dataPackage);
            dashService.submitDashDataset(packageDOI);
            
            // provide output for the console
            setResult("Last processed item = " + packageDOI);        
            report(packageDOI);
            try {
                Thread.sleep(delaySecs * 1000);
            } catch (Exception e) {
                // ignore
            }
        } else {
            log.info("Skipping -- non-item DSpace object");
            setResult("Object skipped (not an item)");
            return Curator.CURATE_SKIP;
        }

        log.info("TransferToDash complete");
        return Curator.CURATE_SUCCESS;
    }
}
