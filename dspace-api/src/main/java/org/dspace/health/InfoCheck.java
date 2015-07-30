/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import java.text.SimpleDateFormat;
import org.apache.commons.io.FileUtils;
import org.dspace.core.ConfigurationManager;

import java.io.File;
import java.util.Date;

/**
 * @author LINDAT/CLARIN dev team
 */
public class InfoCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        StringBuilder sb = new StringBuilder();

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

        for (String[] ss : new String[][] {
            new String[] {
                ConfigurationManager.getProperty("assetstore.dir"),
                "Assetstore size:  ", },
            new String[] {
                ConfigurationManager.getProperty("search.dir"),
                "Search dir size:  ", },
            new String[] {
                ConfigurationManager.getProperty("log.dir"),
                "Log dir size:     ", }, })
        {
            try {
                File dir = new File(ss[0]);
                if (dir.exists()) {
                    long dir_size = FileUtils.sizeOfDirectory(dir);
                    sb.append(String.format("%s: %s\n", ss[1],
                            FileUtils.byteCountToDisplaySize(dir_size))
                    );
                } else {
                    sb.append(String.format("Directory [%s] does not exist!\n", ss[0]));
                }
            }catch(Exception e) {
                error(e, "directory - " + ss[0]);
            }
        }

        return sb.toString();
    }

}