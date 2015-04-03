/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PIDLogStatistics
{
    private HashMap<String, HashMap<String, PIDLogStatisticsEntry>> entries = null;

    PIDLogStatistics()
    {
        entries = new HashMap<String, HashMap<String, PIDLogStatisticsEntry>>();
    }

    public void updateStatistics(String event, String pid, Date eventDate)
    {
        if (!entries.containsKey(event))
        {
            entries.put(event, new HashMap<String, PIDLogStatisticsEntry>());
        }
        HashMap<String, PIDLogStatisticsEntry> subentries = entries.get(event);

        if (!subentries.containsKey(pid))
        {
            subentries.put(pid, new PIDLogStatisticsEntry(event, pid, 0,
                    eventDate, eventDate));
        }
        PIDLogStatisticsEntry entry = subentries.get(pid);
        entry.update(eventDate);
    }

    public List<PIDLogStatisticsEntry> getEntriesList()
    {
        List<PIDLogStatisticsEntry> entriesList = new ArrayList<PIDLogStatisticsEntry>();
        for (Map<String, PIDLogStatisticsEntry> subentries : entries
                .values())
        {
            for (PIDLogStatisticsEntry entry : subentries.values())
            {
                entriesList.add(entry);
            }
        }
        return entriesList;
    }    
    
    public Map<String, List<PIDLogStatisticsEntry>> getTopN(int topN) 
    {        
        Map<String, List<PIDLogStatisticsEntry>> topEntriesMap = new HashMap<String, List<PIDLogStatisticsEntry>>();
        for (String event: entries.keySet())
        {            
            List<PIDLogStatisticsEntry> subentries = new ArrayList<PIDLogStatisticsEntry>(entries.get(event).values());
            Collections.sort( subentries, new Comparator<PIDLogStatisticsEntry>(){
                public int compare( PIDLogStatisticsEntry a, PIDLogStatisticsEntry b ){
                    if(a.getCount() != b.getCount()) {
                        return b.getCount() - a.getCount();
                    }
                    else {
                        return a.getPID().compareTo(b.getPID());
                    }
                }
            });
            topEntriesMap.put(event, subentries.subList(0, topN > subentries.size() ? subentries.size() : topN));
            
        }
        return topEntriesMap;
    }

}
