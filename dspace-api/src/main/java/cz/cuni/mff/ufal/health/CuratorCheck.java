/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package cz.cuni.mff.ufal.health;


import cz.cuni.mff.ufal.curation.RequiredMetadata;
import org.apache.commons.lang.StringUtils;
import org.dspace.content.Site;
import org.dspace.core.Context;
import org.dspace.curate.Curator;
import org.dspace.health.Check;
import org.dspace.health.ReportInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

public class CuratorCheck extends Check {


    @Override
    public String run( ReportInfo ri ) {
        String ret = "";

        // These are considered successful
        // int[] goodStates = {Curator.CURATE_SUCCESS, Curator.CURATE_SKIP};
        // all curation

        for (String task_name : new String[] {
            "profileformats",
            "requiredmetadata",
            "fastchecklinks",
            "checkhandles",
            "openaire",
            "checkmetadata" }) {
            try {
                ret += String.format("=================\n\nCuration Task %s", task_name);

                Curator curator = new Curator();
                curator.addTask(task_name);
                // curator.setReporter("-");
                curator.setInvoked(Curator.Invoked.INTERACTIVE);
                try {
                    Context c = new Context();
                    // Curate this object & return result
                    long startTime = System.currentTimeMillis();
                    curator.curate(c, Site.getSiteHandle());
                    ArrayList<String> results = curator.getOverallResult(task_name);
                    long endTime = System.currentTimeMillis();
                    ret += String.format(" Took: %ds Returned: %d\n\n",
                        (endTime - startTime) / 1000,
                        curator.getOverallStatus(task_name));
                    // do for failed or for profile...
                    // checklinks has strange output - show only the bad ones
                    boolean output_all = ri.verbose();
                    if (!output_all) {
                        if ( task_name.equals("fastchecklinks") || task_name.equals("checkhandles") ) {
                            ret += output_checklinks(results);
                        } else if ( task_name.equals("requiredmetadata") || task_name.equals("checkmetadata") ) {
                            ret += output_group(results);
                        }else {
                            output_all = true;
                        }
                    }

                    if ( output_all ) {
                        ret = StringUtils.join(results, "\n");
                    }
                    c.complete();
                } catch (Exception e) {
                    error(e);
                }
            } catch (Exception e) {
                error(e);
            }
        }
        return ret;
    }

    private static String output_checklinks(ArrayList<String> results) {
        String ret = "";
        String last_item = null;
        for (String strs : results) {
            for (String str : strs.split("\n")) {
                if (str.trim().endsWith("- OK")) {
                    continue;
                } else if (str.trim().startsWith("Item:")) {
                    last_item = str;
                } else {
                    if (last_item != null) {
                        ret += last_item + "\n";
                        last_item = null;
                    }
                    ret += str + "\n";
                }
            }
        }
        return ret;
    }

    private static String output_group(ArrayList<String> results) {
        final long MAX_OUTPUT = 5;
        final ConcurrentMap<String, AtomicLong> counts = new ConcurrentHashMap<>();
        String ret = "";
        for (String strs : results) {
            for (String str : strs.split("\n")) {
                if ( str.contains("Object skipped.") ) {
                    continue;
                }

                String[] splits = str.split(RequiredMetadata.magicSplitWord);
                counts.putIfAbsent(splits[0], new AtomicLong(0));
                long cnt = counts.get(splits[0]).incrementAndGet();
                if ( cnt < MAX_OUTPUT ) {
                    ret += str + "\n";
                }
            }
        }

        boolean not_shown = false;
        for ( String key : counts.keySet() ) {
            long count = counts.get(key).get();
            if ( count >= MAX_OUTPUT ) {
                if ( !not_shown ) {
                    not_shown = true;
                    ret += "\n Not all lines shown:\n";
                }
                ret += String.format( "%s message count: [%d]\n", key, count );
            }
        }

        return ret;
    }
}
