/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package org.dspace.health;

import org.dspace.checker.*;
import org.dspace.core.Context;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class ChecksumCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "No md5 checks made!";
        CheckerCommand checker = new CheckerCommand();
        Date process_start = Calendar.getInstance().getTime();
        checker.setProcessStartDate(process_start);
        checker.setDispatcher(
            // new LimitedCountDispatcher(new SimpleDispatcher(new
            // BitstreamInfoDAO(), null, false), 1)
            // loop through all files
            new SimpleDispatcher(new BitstreamInfoDAO(), process_start, false));

        md5_collector collector = new md5_collector();
        checker.setCollector(collector);
        checker.setReportVerbose(true);
        Context context = null;
        try {
            context = new Context();
            checker.process(context);
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
            for (BitstreamInfo bi : collector.arr) {
                if (!ChecksumCheckResults.CHECKSUM_MATCH.equals(bi
                    .getChecksumCheckResult())) {
                    ret += String
                        .format("md5 checksum FAILED (%s): %s id: %s bitstream-id: %s\n was: %s\n  is: %s\n",
                            bi.getChecksumCheckResult(), bi.getName(),
                            bi.getInternalId(), bi.getBitstreamId(),
                            bi.getStoredChecksum(),
                            bi.getCalculatedChecksum());
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
    public List<BitstreamInfo> arr = new ArrayList<>();

    public void collect(BitstreamInfo info) {
        arr.add(info);
    }
}
