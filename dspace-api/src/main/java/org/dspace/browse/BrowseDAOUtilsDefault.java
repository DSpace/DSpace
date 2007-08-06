/*
 * BrowseDAOUtilsDefault.java
 *
 * Version: $Revision: $
 *
 * Date: $Date:  $
 *
 * Copyright (c) 2002-2007, Hewlett-Packard Company and Massachusetts
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
package org.dspace.browse;

import org.dspace.core.ConfigurationManager;

/**
 * Utility class for retrieving the size of the columns to be used in the browse tables,
 * and applying truncation to the strings that will be inserted into the tables.
 * 
 * Can be configured in dspace.cfg, with the following entries:
 * 
 * webui.browse.value_columns.max
 *   - the maximum number of characters in 'value' columns
 *     (0 is unlimited)
 *   
 * webui.browse.sort_columns.max
 *   - the maximum number of characters in 'sort' columns
 *     (0 is unlimited)
 *   
 * webui.browse.value_columns.omission_mark
 *   - a string to append to truncated values that will be entered into
 *     the value columns (ie. '...')
 *     
 * By default, the column sizes are '0' (unlimited), and no truncation is applied,
 * EXCEPT for Oracle, where we have to truncate the columns for it to work! (in which
 * case, both value and sort columns are by default limited to 2000 characters).
 *  
 * @author Graham Triggs
 * @author Richard Jones
 */
public class BrowseDAOUtilsDefault implements BrowseDAOUtils
{
	/** Maximum number of characters for value columns */
    public int valueColumnMaxChars;
    
    /** Maximum number of characters for sort columns */
    public int sortColumnMaxChars;
    
    /** string to insert where omissions have been made */
    public String valueColumnOmissionMark;
    
    /**
     * Create a new instance of the Default set of utils to use with the database.
     * This represents the most likely case with a database, in that it does not
     * require the fields to be truncated.  Other databases, such as Oracle, require
     * a set limit for their VARCHAR fields, and will therefore have a slightly 
     * different implementation
     * 
     * Other database implementations should extend this class for typing and
     * future proofing purposes
     *
     */
    public BrowseDAOUtilsDefault()
    {
    	// Default for all other databases is unlimited
    	valueColumnMaxChars = 0;
    	sortColumnMaxChars  = 0;
        
        if (ConfigurationManager.getProperty("webui.browse.value_columns.max") != null)
        {
            valueColumnMaxChars = ConfigurationManager.getIntProperty("webui.browse.value_columns.max");
        }
        
        if (ConfigurationManager.getProperty("webui.browse.sort_columns.max") != null)
        {
            sortColumnMaxChars  = ConfigurationManager.getIntProperty("webui.browse.sort_columns.max");
        }
        
        valueColumnOmissionMark = ConfigurationManager.getProperty("webui.browse.value_columns.omission_mark");
        if (valueColumnOmissionMark == null)
        {
        	valueColumnOmissionMark = "...";
        }
    }
    
    /**
     * Get the size to use for the 'value' columns in characters
     * 
     * @return
     */
    public int getValueColumnMaxChars()
    {
        return this.valueColumnMaxChars;
    }

    /**
     * Get the size to use for the sort columns in characters
     * 
     * @return
     */
    public int getSortColumnMaxChars()
    {
        return this.sortColumnMaxChars;
    }

    /**
     * Truncate strings that are to be used for the 'value' columns
     * 
     * @param value
     * @return
     */
    public String truncateValue(String value)
    {
        return this.trunctateString(value, this.valueColumnMaxChars, valueColumnOmissionMark);
    }
    
    /**
     * Truncate strings that are to be used for sorting
     *  
     * @param value
     * @return
     */
    public String truncateSortValue(String value)
    {
        return this.trunctateString(value, this.sortColumnMaxChars, null);
    }
    
    /**
     * Truncate strings that are to be used for the 'value' columns.
     * Characters is the maximum number of characters to allow.
     * Actual truncation applied will be the SMALLER of the passed
     * value, or that read from the configuration.
     * 
     * @param value
     * @param chars
     * @return
     * @deprecated
     */
    public String truncateValue(String value, int chars)
    {
        return this.trunctateString(value, Math.min(chars, this.valueColumnMaxChars), valueColumnOmissionMark);
    }
    
    /**
     * Truncate strings that are to be used for the sorting
     * Characters is the maximum number of characters to allow.
     * Actual truncation applied will be the SMALLER of the passed
     * value, or that read from the configuration.
     * 
     * @param value
     * @param chars
     * @return
     * @deprecated
     */
    public String truncateSortValue(String value, int chars)
    {
        return this.trunctateString(value, Math.min(chars, this.sortColumnMaxChars), null);
    }
    
    /**
     * Internal method to apply the truncation.
     * 
     * @param value
     * @param maxChars
     * @param omissionMark
     * @return
     */
    private String trunctateString(String value, int maxChars, String omissionMark)
    {
        if (value == null || maxChars < 1)
            return value;
        
        if (maxChars > value.length())
            return value;
        
        if (omissionMark != null && omissionMark.length() > 0)
            return value.substring(0, maxChars - omissionMark.length()) + omissionMark;
        
        return value.substring(0, maxChars);
    }
}
