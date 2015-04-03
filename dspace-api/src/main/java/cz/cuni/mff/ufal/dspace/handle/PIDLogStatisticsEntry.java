/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal.dspace.handle;

import java.util.Date;

public class PIDLogStatisticsEntry
{
    private String event;

    private String pid;

    private int count;

    private Date firstOccurence;

    private Date lastOccurence;

    PIDLogStatisticsEntry(String event, String pid, int count,
            Date firstOccurence, Date lastOccurence)
    {
        this.event = event;
        this.pid = pid;
        this.count = count;
        this.firstOccurence = firstOccurence;
        this.lastOccurence = lastOccurence;
    }

    public void update(Date eventDate)
    {
        this.count++;
        
        if (eventDate.before(firstOccurence))
        {
            firstOccurence = eventDate;
        }
        
        if (eventDate.after(lastOccurence))
        {
            lastOccurence = eventDate;
        }
    }

    public String getEvent()
    {
        return event;
    }

    public String getPID()
    {
        return pid;
    }

    public int getCount()
    {
        return count;
    }

    public Date getFirstOccurence()
    {
        return firstOccurence;
    }

    public Date getLastOccurence()
    {
        return lastOccurence;
    }
    
    public String toString() {
        return String.format(""
            + "Event: %s\n"
            + "PID: %s\n"
            + "Count: %d\n"
            + "First occurence: %s\n"
            + "Last occurence: %s\n",
            event, pid, count, firstOccurence.toString(), lastOccurence.toString()        
        );
    }
    
}
