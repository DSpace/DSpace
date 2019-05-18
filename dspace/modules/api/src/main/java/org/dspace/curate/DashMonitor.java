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
 * DashMonitor checks whether an item has been fully processed by DASH and had its files stored
 * in Merritt. If the files are fully processed (versionStatus = submitted), the
 * dryad.dashStoredDate is set. This happens EVEN IF the item is not set to "archived" status
 * within Dryad, because the Merrit processing status is different than the curation status.
 *
 * Input: a single data package OR a collection that contains data packages
 * Output: none
 * Side Effects: Data package is marked with a dryad.dashStoredDate 
 *
 * To use from the command line:
 * /opt/dryad/bin/dspace curate -v -t dashmonitor -i <handle-without-hdl-prefix> -r -
 *
 * @author Ryan Scherle
 */

@Suspendable
public class DashMonitor extends AbstractCurationTask {
    
    private static Logger log = Logger.getLogger(DashMonitor.class);
    private static long total = 0;
    private DashService dashService;
    private Context context;

    
    @Override 
    public void init(Curator curator, String taskId) throws IOException {
        super.init(curator, taskId);
        dashService = new DashService();
        try {
            context = new Context();
        } catch (Exception e) {
            log.error("Unable to initialize", e);
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
        log.info("performing DashMonitor task " + total++ );
                        
        if (dso.getType() == Constants.COLLECTION) {
            // don't do anything on the collection object itself... will automatically process the items
        } else if (dso.getType() == Constants.ITEM) {
            log.debug("create DryadDataPackage");
            DryadDataPackage dataPackage = new DryadDataPackage((Item)dso);
            log.debug("create Package");
            Package pkg = new Package(dataPackage);
            String versionlessPackageDOI = dataPackage.getVersionlessIdentifier();
            String packageDOI = dataPackage.getIdentifier();
            log.info("performing on " + packageDOI);

            boolean isStored = dashService.isDatasetStored(pkg);
                        
            // provide output for the console
            setResult("Last processed item = " + packageDOI + ", isStored? " + isStored);        
            report(packageDOI);
        } else {
            log.info("Skipping -- non-item DSpace object");
            setResult("Object skipped (not an item)");
            return Curator.CURATE_SKIP;
        }

        log.info("DashMonitor complete");
        return Curator.CURATE_SUCCESS;
    }
}
