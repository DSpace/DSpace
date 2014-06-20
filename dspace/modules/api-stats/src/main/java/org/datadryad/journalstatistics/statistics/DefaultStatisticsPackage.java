/*
 */
package org.datadryad.journalstatistics.statistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.datadryad.journalstatistics.extractor.DataFileCount;
import org.datadryad.journalstatistics.extractor.DataFileTotalSize;
import org.datadryad.journalstatistics.extractor.DataPackageCount;
import org.datadryad.journalstatistics.extractor.DataPackageUnpublishedCount;
import org.datadryad.journalstatistics.extractor.EmbargoedDataFileCount;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DefaultStatisticsPackage implements StatisticsPackage {
    private List<Statistic> statistics = new ArrayList<Statistic>();
    private Date beginDate, endDate;
    public DefaultStatisticsPackage(Context context) {
        statistics.add(new Statistic<Integer>("Data packages count", new DataPackageCount(context)));
        statistics.add(new Statistic<Integer>("Data files count", new DataFileCount(context)));
        statistics.add(new Statistic<Long>("Data files total size", new DataFileTotalSize(context)));
        statistics.add(new Statistic<Map<String, Integer>>("Unpublished data packages", new DataPackageUnpublishedCount(context)));
        statistics.add(new Statistic<Integer>("Embargoed data files", new EmbargoedDataFileCount(context)));
    }
    @Override
    public void run(String journalName) {
        for(Statistic s : statistics) {
            s.setBeginDate(beginDate);
            s.setEndDate(endDate);
            s.extractAndStore(journalName);
            System.out.println(s);
        }
    }

    @Override
    public void setBeginDate(Date beginDate) {
        this.beginDate = beginDate;
    }

    @Override
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
