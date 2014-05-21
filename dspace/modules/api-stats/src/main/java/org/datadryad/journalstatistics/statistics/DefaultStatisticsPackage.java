/*
 */
package org.datadryad.journalstatistics.statistics;

import java.util.ArrayList;
import java.util.List;
import org.datadryad.journalstatistics.extractor.DataPackageCount;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DefaultStatisticsPackage implements StatisticsPackage {
    static List<Statistic> defaults = new ArrayList<Statistic>() {{
       add(new Statistic<Integer>("Cumulative total number of data packages", new DataPackageCount()));
    }};

    static DefaultStatisticsPackage sharedInstance = null;
    @Override
    public void run() {
        for(Statistic s : defaults) {
            s.setValue(s);
        }
    }

    public static DefaultStatisticsPackage getInstance() {
        if(sharedInstance == null) {
            sharedInstance = new DefaultStatisticsPackage();
        }
        return sharedInstance;
    }



}
