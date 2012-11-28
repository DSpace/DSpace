/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

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
public interface BrowseDAOUtils
{
    /**
     * Get the size to use for the 'value' columns in characters
     */
    public int getValueColumnMaxChars();

    /**
     * Get the size to use for the sort columns in characters
     */
    public int getSortColumnMaxChars();

    /**
     * Truncate strings that are to be used for the 'value' columns
     * 
     * @param value
     * @return the truncated value.
     */
    public String truncateValue(String value);
    
    /**
     * Truncate strings that are to be used for sorting
     *  
     * @param value
     * @return the truncated value.
     */
    public String truncateSortValue(String value);
    
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
    public String truncateValue(String value, int chars);
    
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
    public String truncateSortValue(String value, int chars);
}
