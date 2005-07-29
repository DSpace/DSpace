/*
 * DCInputSet.java
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

package org.dspace.app.webui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.Map;

/**
 * Class representing all DC inputs required for a submission, organized into pages
 *
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 * @version $Revision$
 */

public class DCInputSet
{
	/** name of the input set  */
	private String formName = null; 
	/** the inputs ordered by page and row position */
	private DCInput[][] inputPages = null;
	
	/** constructor */
	public DCInputSet(String formName, Vector pages, Map listMap)
	{
		this.formName = formName;
		inputPages = new DCInput[pages.size()][];
		for ( int i = 0; i < inputPages.length; i++ )
		{
			Vector page = (Vector)pages.get(i);
			inputPages[i] = new DCInput[page.size()];
			for ( int j = 0; j < inputPages[i].length; j++ )
			{
				inputPages[i][j] = new DCInput((Map)page.get(j), listMap);
			}
		}
	}
	
	/**
	 * Return the name of the form that defines this input set
	 * @return formName 	the name of the form
	 */
	public String getFormName()
	{
		return formName;
	}
	
	/**
	 * Return the number of pages in this  input set
	 * @return number of pages
	 */
	public int getNumberPages()
	{
		return inputPages.length;
	}
	
    /**
     * Get all the rows for a page from the form definition
     *
     * @param  pageNum	desired page within set
     * @param  addTitleAlternative flag to add the additional title row
     * @param  addPublishedBefore  flag to add the additional published info
     *
     * @return  an array containing the page's displayable rows
     */
	
	public DCInput[] getPageRows(int pageNum, boolean addTitleAlternative,
		      					 boolean addPublishedBefore)
	{
		List filteredInputs = new ArrayList();
		if ( pageNum < inputPages.length )
		{
			for (int i = 0; i < inputPages[pageNum].length; i++ )
			{
				DCInput input = inputPages[pageNum][i];
				if (doField(input, addTitleAlternative, addPublishedBefore))
				{
					filteredInputs.add(input);
				}
			}
		}

		// Convert list into an array
		DCInput[] inputArray = new DCInput[filteredInputs.size()];
		return (DCInput[])filteredInputs.toArray(inputArray);
	}
	
    /**
     * Does this set of inputs include an alternate title field?
     *
     * @return true if the current set has an alternate title field
     */
    public boolean isDefinedMultTitles()
    {
    	return isFieldPresent("title.alternative");
    }
    
    /**
     * Does this set of inputs include the previously published fields?
     *
     * @return true if the current set has all the prev. published fields
     */
    public boolean isDefinedPubBefore()
    {
    	return ( isFieldPresent("date.issued") && 
    			 isFieldPresent("identifier.citation") &&
				 isFieldPresent("publisher.null") );
    }
    
    /**
     * Does the current input set define the named field?
     * Scan through every field in every page of the input set
     *
     * @return true if the current set has the named field
     */
    public boolean isFieldPresent(String fieldName)
    {
    	for (int i = 0; i < inputPages.length; i++)
	    {
    		DCInput[] pageInputs = inputPages[i];
    		for (int row = 0; row < pageInputs.length; row++)
    		{
    			String fullName = pageInputs[row].getElement() + "." + 
				              	  pageInputs[row].getQualifier();
    			if (fullName.equals(fieldName))
    			{
    				return true;
    			}
    		}
	    }
    	return false;
    }
	
    private static boolean doField(DCInput dcf, boolean addTitleAlternative, 
		    					   boolean addPublishedBefore)
    {
    	String rowName = dcf.getElement() + "." + dcf.getQualifier();
    	if ( rowName.equals("title.alternative") && ! addTitleAlternative )
    	{
    		return false;
    	}
    	if (rowName.equals("date.issued") && ! addPublishedBefore )
    	{
    		return false;
    	}
    	if (rowName.equals("publisher.null") && ! addPublishedBefore )
    	{
    		return false;
    	}
    	if (rowName.equals("identifier.citation") && ! addPublishedBefore )
    	{
    		return false;
    	}

    	return true;
    }
}
