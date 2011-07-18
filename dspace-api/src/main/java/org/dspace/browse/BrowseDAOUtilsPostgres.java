/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

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
 */
public class BrowseDAOUtilsPostgres extends BrowseDAOUtilsDefault
{
	/**
	 * This is really just a type cast at the moment so we have a Postgres
	 * specific set of utils.  In reality, this is identical to the 
	 * Default class which it extends.
	 *
	 */
	public BrowseDAOUtilsPostgres()
	{
		super();
	}

}
