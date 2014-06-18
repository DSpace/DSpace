/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import org.dspace.core.ConfigurationManager;

/**
 * Utility class for retrieving the size of the columns to be used in the
 * browse tables, and applying truncation to the strings that will be inserted
 * into the tables.
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
 * By default, the column sizes are '0' (unlimited), and no truncation is
 * applied, EXCEPT for Oracle, where we have to truncate the columns for it
 * to work! (in which case, both value and sort columns are by default limited
 * to 2000 characters).
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
     */
    public int getValueColumnMaxChars()
    {
        return this.valueColumnMaxChars;
    }

    /**
     * Get the size to use for the sort columns in characters
     */
    public int getSortColumnMaxChars()
    {
        return this.sortColumnMaxChars;
    }

    /**
     * Truncate strings that are to be used for the 'value' columns
     * 
     * @param value
     * @return the truncated value.
     */
    public String truncateValue(String value)
    {
        return this.trunctateString(value, this.valueColumnMaxChars, valueColumnOmissionMark);
    }
    
    /**
     * Truncate strings that are to be used for sorting
     *  
     * @param value
     * @return the truncated value.
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
     * @return the truncated value.
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
     * @return the truncated value.
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
     * @return the truncated value.
     */
    private String trunctateString(String value, int maxChars, String omissionMark)
    {
        if (value == null || maxChars < 1)
        {
            return value;
        }
        
        if (maxChars > value.length())
        {
            return value;
        }
        
        if (omissionMark != null && omissionMark.length() > 0)
        {
            return value.substring(0, maxChars - omissionMark.length()) + omissionMark;
        }
        
        return value.substring(0, maxChars);
    }
}
