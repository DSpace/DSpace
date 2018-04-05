/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;


import org.dspace.app.statistics.LogAnalyser;
import org.dspace.core.Context;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * @author LINDAT/CLARIN dev team
 */
public class LogAnalyserCheck extends Check {

    final static private String[][] interesting_fields = new String[][] {
        new String[] { "exceptions", "Exceptions" },
        new String[] { "warnings", "Warnings" },
        new String[] { "action.browse", "Archive browsed" },
        new String[] { "action.search", "Archive searched" },
        new String[] { "action.login", "Logged in" },
        new String[] { "action.oai_request", "OAI requests" },
    };

    @Override
    public String run( ReportInfo ri ) {
        StringBuilder sb = new StringBuilder();

        Map<String, String> info_map = new HashMap<>();
        for (String[] info : interesting_fields) {
            info_map.put(info[0], "unknown");
        }

        try {
            Context c = new Context();
            // parse logs
            String report = LogAnalyser.processLogs(
                c, null, null, null, null, ri.from(), ri.till(), false);

            // we have to deal with string report...
            for (String line : report.split("\\r?\\n")) {
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    info_map.put(parts[0], parts[1]);
                }
            }

            // create report
            for (String[] info : interesting_fields ) {
                sb.append( String.format("%-20s: %s\n", info[1], info_map.get(info[0])) );
            }
            sb.append( String.format("Items added since [%s] (db): %s\n",
                new SimpleDateFormat("yyyy-MM-dd").format(ri.from().getTime()),
                LogAnalyser.getNumItems(c)));

            c.complete();

        } catch (Exception e) {
            error(e);
        }

        return sb.toString();
    }
}
