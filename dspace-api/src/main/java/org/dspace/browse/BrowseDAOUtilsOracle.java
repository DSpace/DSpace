/*
 * BrowseDAOUtilsOracle.java
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
