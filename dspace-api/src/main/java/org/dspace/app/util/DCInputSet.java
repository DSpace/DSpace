/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.ArrayList;
import java.util.List;
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
	
        /** constructor
         * @param formName form name
         * @param pages pages
         * @param listMap map
         */
	public DCInputSet(String formName, List<List<Map<String, String>>> pages, Map<String, List<String>> listMap)
	{
		this.formName = formName;
		inputPages = new DCInput[pages.size()][];
		for ( int i = 0; i < inputPages.length; i++ )
		{
			List<Map<String, String>> page = pages.get(i);
			inputPages[i] = new DCInput[page.size()];
			for ( int j = 0; j < inputPages[i].length; j++ )
			{
				inputPages[i][j] = new DCInput(page.get(j), listMap);
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
		List<DCInput> filteredInputs = new ArrayList<DCInput>();
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
		return filteredInputs.toArray(inputArray);
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
     * @param fieldName
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
	
    /**
     * Does the current input set define the named field?
     * and is valid for the specified document type
     * Scan through every field in every page of the input set
     *
     * @param fieldName field name
     * @param documentType doc type
     * @return true if the current set has the named field
     */
     public boolean isFieldPresent(String fieldName, String documentType)
     {
     	if(documentType == null){
     		documentType = "";
     	}
     	for (int i = 0; i < inputPages.length; i++)
 	    {
     		DCInput[] pageInputs = inputPages[i];
     		for (int row = 0; row < pageInputs.length; row++)
     		{
     			String fullName = pageInputs[row].getElement() + "." + 
 				              	  pageInputs[row].getQualifier();
     			if (fullName.equals(fieldName) )
     			{
     				if(pageInputs[row].isAllowedFor(documentType)){
     					return true;
     				}
     			}
     		}
 	    }
     	return false;
     }
    
    protected boolean doField(DCInput dcf, boolean addTitleAlternative,
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
