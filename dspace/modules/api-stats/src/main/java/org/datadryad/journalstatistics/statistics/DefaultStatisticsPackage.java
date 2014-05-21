/*
 */
package org.datadryad.journalstatistics.statistics;

import java.util.ArrayList;
import java.util.List;
import org.datadryad.journalstatistics.extractor.DataPackageCount;
import org.dspace.core.Context;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DefaultStatisticsPackage implements StatisticsPackage {
    private List<Statistic> statistics = new ArrayList<Statistic>();
    public DefaultStatisticsPackage(Context context) {
        statistics.add(new Statistic<Integer>("Data packages by journal", new DataPackageCount(context)));
    }
    @Override
    public void run(String journalName) {
        for(Statistic s : statistics) {
            s.extractAndStore(journalName);
            System.out.println(s);
        }
    }
}
