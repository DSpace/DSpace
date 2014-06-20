/*
 */
package org.datadryad.journalstatistics.statistics;

import java.util.Date;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public interface StatisticsPackage {
    public void setBeginDate(Date startDate);
    public void setEndDate(Date endDate);
    public void run(String journalName);

}
