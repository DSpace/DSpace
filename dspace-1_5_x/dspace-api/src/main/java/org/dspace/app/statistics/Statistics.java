/*
 * Statistics.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */

package org.dspace.app.statistics;

import org.dspace.app.statistics.Stat;

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
    private List stats = new ArrayList();
    
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
