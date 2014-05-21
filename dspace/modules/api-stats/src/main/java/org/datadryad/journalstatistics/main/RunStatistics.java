/*
 */
package org.datadryad.journalstatistics.main;

import org.datadryad.journalstatistics.statistics.DefaultStatisticsPackage;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class RunStatistics {

    public static void main(String args[]) {
        DefaultStatisticsPackage.getInstance().run();
    }
}
