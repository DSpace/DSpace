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

import cz.cuni.mff.ufal.dspace.PIDService;
import cz.cuni.mff.ufal.dspace.handle.PIDLogMiner;
import cz.cuni.mff.ufal.dspace.handle.PIDLogStatistics;
import cz.cuni.mff.ufal.dspace.handle.PIDLogStatisticsEntry;
import org.dspace.core.ConfigurationManager;
import org.dspace.health.Check;
import org.dspace.health.Core;
import org.dspace.health.ReportInfo;
import org.dspace.storage.rdbms.TableRow;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

public class PIDCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        try {
            String whoami = PIDService.who_am_i("encoding=xml");
            ret += String.format("Who am I\n\t%s\n",
                whoami.replaceAll("\n", "\n\t"));
            String test_pid = ConfigurationManager.getProperty("lr", "lr.pid.service.testPid");
            ret += "Testing PID server\n\t";
            if (test_pid != null) {
                ret += PIDService.test_pid(test_pid);
            } else {
                ret += "Testing PID server not done! Test pid not in dspace.cfg!";
            }
        } catch(IllegalArgumentException e) {
            error(e, "Warning: PID service type is not defined or wrong");
        } catch (org.apache.commons.lang.NotImplementedException e) {
            error(e, "Testing PID server - method who_am_i not implemented");
        } catch (Exception e) {
            error(e, "Testing PID server failed - exception occurred: %s");
        }

        try {
            long total_count = Core.getHandlesTotalCount();
            ret += "\n";
            ret += String.format("Total count: %d\n", total_count);
            List<TableRow> invalid_handles = Core.getHandlesInvalidRows();
            ret += String.format("Invalid handles count: %d\n",
                invalid_handles.size());
            if (invalid_handles.size() > 0) {
                ret += "\n";
                ret += "Invalid handles:\n";
                ret += "----------------\n";
                ret += String.format("%-6s\t%-32s\t%-10s\t%-10s\t%s\n",
                    "Handle ID", "Handle", "Res. type ID", "Resource ID",
                    "URL");
                for (TableRow row : invalid_handles) {
                    int handle_id = row.getIntColumn("handle_id");
                    String handle = row.getStringColumn("handle");

                    Integer resource_type_id = row.getIntColumn("resource_type_id");
                    if (resource_type_id < 0)
                        resource_type_id = null;

                    Integer resource_id = row.getIntColumn("resource_id");
                    if (resource_id < 0)
                        resource_id = null;

                    String url = row.getStringColumn("url");
                    ret += String.format("%-10d\t%-32s\t%-10d\t%-10d\t%s\n",
                        handle_id, handle, resource_type_id, resource_id,
                        url);
                }
            }
        } catch (SQLException e) {
            error(e);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Calendar realEndDate = Calendar.getInstance();
        realEndDate.setTime(ri.till());
        realEndDate.add(Calendar.DATE, -1);

        final int topN = 10;
        StringBuffer buf = new StringBuffer();
        buf.append("============================================================\n");
        buf.append(String.format("PID resolution statistics\n"));
        buf.append("============================================================\n");
        buf.append("\n\n");
        PIDLogMiner logMiner = new PIDLogMiner();
        PIDLogStatistics statistics = logMiner.computeStatistics(
            ri.from(), realEndDate.getTime());
        Map<String, List<PIDLogStatisticsEntry>> topNEntries = statistics
            .getTopN(topN);
        String eventsToDisplay[] = { PIDLogMiner.FAILURE_EVENT,
            PIDLogMiner.REQUEST_EVENT, PIDLogMiner.SUCCESS_EVENT,
            PIDLogMiner.UNKNOWN_EVENT };
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss.SSS");
        for (String event : eventsToDisplay) {
            if (topNEntries.containsKey(event)) {
                buf.append(String.format(
                    "Top %d events of type %s between %s and %s\n", topN,
                    event, dateFormat.format(ri.from()),
                    dateFormat.format(realEndDate.getTime())));
                buf.append(String
                    .format("---------------------------------------------------------------\n",
                        topN, event));
                buf.append(String.format("%-10s%-40s%-25s%-25s\n", "Count",
                    "PID", "First occurence", "Last occurence"));
                buf.append(String
                    .format("--------------------------------------------------------------------------------------------------\n",
                        topN, event));
                for (PIDLogStatisticsEntry entry : topNEntries.get(event)) {
                    buf.append(String.format("%-10d%-40s%-25s%-25s\n",
                        entry.getCount(), entry.getPID(),
                        dateTimeFormat.format(entry.getFirstOccurence()),
                        dateTimeFormat.format(entry.getLastOccurence())));
                }
                buf.append("\n");
            }
        }
        return ret + buf.toString();
    }

}
