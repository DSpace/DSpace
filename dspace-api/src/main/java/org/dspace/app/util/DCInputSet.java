/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

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
    /** the inputs ordered by row position */
    private DCInput[] inputs = null;
    
	/**
	 * constructor
	 * 
	 * @param formName
	 *            form name
	 * @param headings
	 * @param mandatoryFlags
	 * @param fields
	 *            fields
	 * @param listMap
	 *            map
	 */
	public DCInputSet(String formName,
			List<Map<String, String>> fields, Map<String, List<String>> listMap)
    {
        this.formName = formName;
        this.inputs = new DCInput[fields.size()];
        for ( int i = 0; i < inputs.length; i++ )
        {
            Map<String, String> field = fields.get(i);
            inputs[i] = new DCInput(field, listMap);
            
        }
    }
    
    /**
     * Return the name of the form that defines this input set
     * @return formName     the name of the form
     */
    public String getFormName()
    {
        return formName;
    }
    
    /**
     * Return the number of fields in this  input set
     * @return number of pages
     */
    public int getNumberFields()
    {
        return inputs.length;
    }
    
    /**
     * Get all the fields
     *
     * @return  an array containing the fields
     */
    
    public DCInput[] getFields()
    {
    	return inputs;
    }
    
    /**
     * Does this set of inputs include an alternate title field?
     *
     * @return true if the current set has an alternate title field
     */
    public boolean isDefinedMultTitles()
    {
        return isFieldPresent("dc.title.alternative");
    }
    
    /**
     * Does this set of inputs include the previously published fields?
     *
     * @return true if the current set has all the prev. published fields
     */
    public boolean isDefinedPubBefore()
    {
        return ( isFieldPresent("dc.date.issued") && 
                 isFieldPresent("dc.identifier.citation") &&
                 isFieldPresent("dc.publisher.null") );
    }
    
    /**
     * Does the current input set define the named field?
     * Scan through every field in every page of the input set
     *
     * @param fieldName selects the field.
     * @return true if the current set has the named field
     */
    public boolean isFieldPresent(String fieldName)
    {
        for (int i = 0; i < inputs.length; i++)
        {
            DCInput field = inputs[i];
                String fullName = field.getFieldName();
                if (fullName.equals(fieldName))
                {
                    return true;
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
         if (documentType == null) {
             documentType = "";
         }
         for (int i = 0; i < inputs.length; i++)
         {
             DCInput field = inputs[i];
                 String fullName = field.getFieldName();
                 if (fullName.equals(fieldName) )
                 {
                     if (field.isAllowedFor(documentType)) {
                         return true;
                     }
                 }
         }
         return false;
     }
    
    protected boolean doField(DCInput dcf, boolean addTitleAlternative,
                                   boolean addPublishedBefore)
    {
        String rowName = dcf.getFieldName();
        if ( rowName.equals("dc.title.alternative") && ! addTitleAlternative )
        {
            return false;
        }
        if (rowName.equals("dc.date.issued") && ! addPublishedBefore )
        {
            return false;
        }
        if (rowName.equals("dc.publisher.null") && ! addPublishedBefore )
        {
            return false;
        }
        if (rowName.equals("dc.identifier.citation") && ! addPublishedBefore )
        {
            return false;
        }

        return true;
    }

}
