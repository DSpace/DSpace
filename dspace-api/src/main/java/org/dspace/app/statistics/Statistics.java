/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.statistics;

import java.util.ArrayList;
import java.util.List;

/**
 * This class provides a wrapper for a related set of statistics.  It contains
 * headers for the Stat key and value pairs for the convenience of displaying
 * them to users as, for example, HTML table headers.  It also holds the 
 * list of statistics, and can have them added to itself individually or in
 * arrays
 *
 * @author  Richard Jones
 */
public class Statistics
{
    // FIXME: this class could probably do with some tidying
    
    /** the header for the statistics type.  Useful for outputting to user */
    private String statName = null;
    
    /** the header for the results.  Useful for outputting to user */
    private String resultName = null;
    
    /** a list to hold all of the stat elements that this object contains */
    private List<Stat> stats = new ArrayList<Stat>();
    
    /** the floor value for this set of statistics */
    private int floor = 0;
    
    /** an explanation of this statistics set */
    private String explanation = null;
    
    /** the main section header for this set of statistics */
    private String sectionHeader = null;
    
    /**
     * constructor to create new set of statistics
     */
    Statistics()
    {
        // empty constructor
    }
    
    /**
     * constructor to create new statistic with relevant headers
     *
     * @param   statName     name of the statistic
     * @param   resultName   name of the result
     */
    Statistics(String statName, String resultName)
    {
        this.statName = statName;
        this.resultName = resultName;
    }
    
    /**
     * constructor to create new statistic with relevant headers
     *
     * @param   statName     name of the statistic
     * @param   resultName   name of the result
     */
    Statistics(String statName, String resultName, int floor)
    {
        this.statName = statName;
        this.resultName = resultName;
        this.floor = floor;
    }
    
    /**
     * add an individual statistic to this object
     *
     * @param   stat    a statistic for this object
     */
    public void add(Stat stat)
    {
        this.stats.add(stat);
        return;
    }
    
    /**
     * set the name of the statistic column
     *
     * @param   name    the name of the statistic column
     */
    public void setStatName(String name)
    {
        this.statName = name;
    }
    
    /**
     * set the name of the results column
     *
     * @param   name    the name of the results column
     */
    public void setResultName(String name)
    {
        this.resultName = name;
    }
    
    
    /**
     * set the explanatory or clarification information for this block of stats
     *
     * @param   explanation     the explanation for this stat block
     */
    public void setExplanation(String explanation)
    {
        this.explanation = explanation;
    }
    
    
    /**
     * get the explanation or clarification information for this block of stats
     *
     * @return      the explanation for this stat block
     */
    public String getExplanation()
    {
        return this.explanation;
    }
    
    
    /**
     * set the floor value used in this stat block
     *
     * @param   floor   the floor value for this stat block
     */
    public void setFloor(int floor)
    {
        this.floor = floor;
    }
    
    
    /**
     * get the floor value used in this stat block
     *
     * @return  the floor value for this stat block
     */
    public int getFloor()
    {
        return this.floor;
    }
    
    
    /**
     * set the header for this particular stats block
     *
     * @param   header for this stats block
     */
    public void setSectionHeader(String header)
    {
        this.sectionHeader = header;
    }
    
    
    /**
     * get the header for this particular stats block
     *
     * @return  the header for this stats block
     */
    public String getSectionHeader()
    {
        return this.sectionHeader;
    }
    
    /**
     * add an array of statistics to this object
     *
     * @param   stats   an array of statistics
     */
    public void add(Stat[] stats)
    {
        for (int i = 0; i < stats.length; i++)
        {
            this.stats.add(stats[i]);
        }
        return;
    }
    
    /**
     * get an array of statistics back from this object
     *
     * @return      the statistics array
     */
    public Stat[] getStats()
    {
        Stat[] myStats = new Stat[stats.size()];
        myStats = (Stat[]) stats.toArray(myStats);
        return myStats;
    }
    
    /**
     * get the name of the statistic
     *
     * @return      the name of the statistic
     */
    public String getStatName()
    {
        return statName;
    }
    
    /**
     * get the name of the result set
     *
     * @return      the name of the result set
     */
    public String getResultName()
    {
        return resultName;
    }
}
