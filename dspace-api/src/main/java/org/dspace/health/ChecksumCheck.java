/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.health;

import org.dspace.checker.*;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @author LINDAT/CLARIN dev team
 */
public class ChecksumCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "No md5 checks made!";
        Context context =  new Context();
        CheckerCommand checker = new CheckerCommand(context);
        Date process_start = Calendar.getInstance().getTime();
        checker.setProcessStartDate(process_start);
        checker.setDispatcher(
            new SimpleDispatcher(context, process_start, false));

        md5_collector collector = new md5_collector();
        checker.setCollector(collector);
        checker.setReportVerbose(true);
        try {
            checker.process();
            context.complete();
            context = null;
        } catch (SQLException e) {
            error(e);
        } finally {
            if (context != null) {
                context.abort();
            }
        }

        if (collector.arr.size() > 0) {
            ret = String.format("Checksum performed on [%d] items:\n",
                collector.arr.size());
            int ok_items = 0;
            for (MostRecentChecksum bi : collector.arr) {
                if (!ChecksumResultCode.CHECKSUM_MATCH.equals(bi
                    .getChecksumResult().getResultCode())) {
                    ret += String
                        .format("md5 checksum FAILED (%s): %s id: %s bitstream-id: %s\n was: %s\n  is: %s\n",
                            bi.getChecksumResult(), bi.getBitstream().getName(),
                            bi.getBitstream().getInternalId(), bi.getBitstream().getID(),
                            bi.getExpectedChecksum(),
                            bi.getCurrentChecksum());
                } else {
                    ok_items++;
                }
            }

            ret += String.format("checksum OK for [%d] items\n", ok_items);
        }
        return ret;
    }
}

class md5_collector implements ChecksumResultsCollector {
    public List<MostRecentChecksum> arr = new ArrayList<>();

    @Override
    public void collect(Context context, MostRecentChecksum info) throws SQLException {
        arr.add(info);
    }
}
