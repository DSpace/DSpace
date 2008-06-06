/*
 * Report.java
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
import org.dspace.app.statistics.Statistics;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Sn interface to a generic report generating
 * class, and to provide the polymorphism necessary to allow the report
 * generator to generate any number of different formats of report
 *
 * Note: This used to be an abstract class, but has been made an interface as there wasn't
 * any logic contained within it. It's also been made public, so that you can create a Report
 * type without monkeying about in the statistics package.
 *
 * @author  Richard Jones
 */
public interface Report
{
    /**
     * output any top headers that this page needs
     *
     * @return      a string containing the header for the report
     */
    abstract public String header();
    
    /**
     * output any top headers that this page needs
     *
     * @param   title   the title of the report, useful for email subjects or
     *                  HTML headers
     *
     * @return      a string containing the header for the report
     */
    abstract public String header(String title);
    
    /**
     * output the title in the relevant format.  This requires that the title
     * has been set with setMainTitle()
     *
     * @return      a string containing the title of the report
     */
    abstract public String mainTitle();
    
    /**
     * output the date range in the relevant format.  This requires that the
     * date ranges have been set using setStartDate() and setEndDate()
     *
     * @return      a string containing date range information
     */
    abstract public String dateRange();
    
    /**
     * output the section header in the relevant format
     *
     * @param   title   the title of the current section header
     *
     * @return      a string containing the formatted section header
     */
    abstract public String sectionHeader(String title);
    
    /**
     * output the report block based on the passed statistics object array
     *
     * @param   content     a statistics object to form the basis of the displayed
     *                      stat block
     *
     * @return      a string containing the formatted statistics block
     */
    abstract public String statBlock(Statistics content);
    
    /**
     * output the floor information in the relevant format
     *
     * @param   floor       the floor value for the statistics block
     *
     * @return      a string containing the formatted floor information
     */
    abstract public String floorInfo(int floor);
    
    /**
     * output the explanation of the stat block in the relevant format
     *
     * @param   explanation     the explanatory or clarification text for the stats
     *
     * @return      a string contianing the formatted explanation
     */
    abstract public String blockExplanation(String explanation);
    
    /**
     * output the final footers for this file
     *
     * @return      a string containing the report footer
     */
    abstract public String footer();
    
    /**
     * set the main title for the report
     *
     * @param   name    the name of the service
     * @param   serverName  the name of the server
     */
    abstract public void setMainTitle (String name, String serverName);
    
    /**
     * add a statistics block to the report to the class register
     *
     * @param   stat    the statistics object to be added to the report
     */
    abstract public void addBlock(Statistics stat);
    
    /**
     * render the report
     *
     * @return      a string containing the full content of the report
     */
    abstract public String render();
    
    /**
     * set the starting date for the report
     *
     * @param   start   the start date for the report
     */
    abstract public void setStartDate(Date start);
    
    /**
     * set the end date for the report
     *
     * @param   end     the end date for the report
     */
    abstract public void setEndDate(Date end);
}
