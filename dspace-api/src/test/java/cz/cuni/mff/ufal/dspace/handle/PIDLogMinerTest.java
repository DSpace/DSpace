/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import static org.junit.Assert.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class PIDLogMinerTest
{
    @Test
    public void testComputeStatistics()
    {
        Date startDate;
        try
        {
            startDate = new SimpleDateFormat("dd.MM.yyyy").parse("27.6.2014");
        }
        catch (ParseException pe)
        {
            throw new IllegalArgumentException(pe);
        }
        Calendar cal = GregorianCalendar.getInstance();
        cal.setTime(startDate);
        cal.add(Calendar.DATE, 2);
        Date endDate = cal.getTime();

        String logDir = this.getClass().getResource("/dspaceFolder/log/")
                .getPath();
        PIDLogMiner logMiner = new PIDLogMiner(logDir);
        int topN = 3;
        PIDLogStatistics statistics = logMiner.computeStatistics(startDate, endDate);
        Map<String, List<PIDLogStatisticsEntry>> entries = statistics.getTopN(topN);
        
        assertTrue("Top 3 entries must contain REQUEST event", entries.containsKey(PIDLogMiner.REQUEST_EVENT));        
        assertTrue("Top 3 entries must contain SUCCESS event", entries.containsKey(PIDLogMiner.SUCCESS_EVENT));
        assertTrue("Top 3 entries must contain FAILURE event", entries.containsKey(PIDLogMiner.FAILURE_EVENT));
        assertTrue("Top 3 entries must contain UNKNOWN event", entries.containsKey(PIDLogMiner.UNKNOWN_EVENT));
        assertEquals("Top 3 REQUEST events count must be 3", 3, entries.get(PIDLogMiner.REQUEST_EVENT).size());
        assertEquals("Top 3 FAILURE events count must be 3", 3, entries.get(PIDLogMiner.FAILURE_EVENT).size());
        assertEquals("Top 3 SUCCESS events count must be 1", 1, entries.get(PIDLogMiner.SUCCESS_EVENT).size());        
        assertEquals("Top 3 UNKNOWN events count must be 1", 1, entries.get(PIDLogMiner.UNKNOWN_EVENT).size());
        
        topN = 0;
        entries = statistics.getTopN(topN);        
        assertTrue("Top 3 entries must contain REQUEST event", entries.containsKey(PIDLogMiner.REQUEST_EVENT));
        assertTrue("Top 0 REQUEST events must be empty", entries.get(PIDLogMiner.REQUEST_EVENT).isEmpty());
        
    }
}