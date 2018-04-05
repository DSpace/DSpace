/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import org.apache.commons.io.FileUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.services.ConfigurationService;
import org.dspace.storage.bitstore.DSBitStoreService;
import org.dspace.utils.DSpace;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author LINDAT/CLARIN dev team
 */
public class InfoCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        StringBuilder sb = new StringBuilder();
        ConfigurationService configurationService
                = new DSpace().getConfigurationService();
        sb.append("Generated: ").append(
            new Date().toString()
        ).append("\n");

        sb.append("From - Till: ").append(
            new SimpleDateFormat("yyyy-MM-dd").format(ri.from().getTime())
        ).append(" - ").append(
            new SimpleDateFormat("yyyy-MM-dd").format(ri.till().getTime())
        ).append("\n");

        sb.append("Url: ").append(
            ConfigurationManager.getProperty("dspace.url")
        ).append("\n");
        sb.append("\n");

        DSBitStoreService localStore = new DSpace().getServiceManager().getServicesByType(DSBitStoreService.class).get(0);
        for (String[] ss : new String[][] {
            new String[] {
                localStore.getBaseDir().toString(),
                "Assetstore size", },
            new String[] {
                    configurationService.getProperty("log.report.dir"),
                "Log dir size", }, })
        {
            if (ss[0] != null) {   
                try {
                    File dir = new File(ss[0]);
                    if (dir.exists()) {
                        long dir_size = FileUtils.sizeOfDirectory(dir);
                        sb.append(String.format("%-20s: %s\n", ss[1],
                                FileUtils.byteCountToDisplaySize(dir_size))
                        );
                    } else {
                        sb.append(String.format("Directory [%s] does not exist!\n", ss[0]));
                    }
                } catch(Exception e) {
                    error(e, "directory - " + ss[0]);
                }
            }
            else { // cannot read property for some reason
                    sb.append(String.format("Could not get information for %s!\n", ss[1]));
            }
        }

        return sb.toString();
    }

}
