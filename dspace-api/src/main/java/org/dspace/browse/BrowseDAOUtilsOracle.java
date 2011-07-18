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
 * @author Richard Jones
 * @author Graham Triggs
 */
public class BrowseDAOUtilsOracle extends BrowseDAOUtilsDefault
{
    /**
     * Create a new instance of the Oracle specific set of utilities.  This
     * enforces a limit of 2000 characters on the value and sort columns
     * in the database.  Any configuration which falls outside this boundary
     * will be automatically brought within it.
     *
     */
	public BrowseDAOUtilsOracle()
	{
		valueColumnMaxChars = 2000;
		sortColumnMaxChars  = 2000;
        
        if (ConfigurationManager.getProperty("webui.browse.value_columns.max") != null)
        {
            valueColumnMaxChars = ConfigurationManager.getIntProperty("webui.browse.value_columns.max");
        }
        
        if (ConfigurationManager.getProperty("webui.browse.sort_columns.max") != null)
        {
            sortColumnMaxChars  = ConfigurationManager.getIntProperty("webui.browse.sort_columns.max");
        }

        // For Oracle, force the sort column to be no more than 2000 characters,
        // even if explicitly configured (have to deal with limitation of using VARCHAR2)
        if (sortColumnMaxChars < 1 || sortColumnMaxChars > 2000)
        {
        	sortColumnMaxChars = 2000;
        }
        
        valueColumnOmissionMark = ConfigurationManager.getProperty("webui.browse.value_columns.omission_mark");
        if (valueColumnOmissionMark == null)
        {
        	valueColumnOmissionMark = "...";
        }
	}

}
