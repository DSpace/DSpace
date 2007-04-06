/*
 * DCInput.java
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

import java.util.List;
import java.util.Map;

import org.dspace.content.MetadataSchema;

/**
 * Class representing a line in an input form.
 * 
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 * @version
 */
public class DCInput
{
    /** the DC element name */
    private String dcElement = null;

    /** the DC qualifier, if any */
    private String dcQualifier = null;

    /** the DC namespace schema */
    private String dcSchema = null;

    /** a label describing input */
    private String label = null;

    /** the input type */
    private String inputType = null;

    /** is input required? */
    private boolean required = false;

    /** if required, text to display when missing */
    private String warning = null;

    /** is input repeatable? */
    private boolean repeatable = false;

    /** 'hint' text to display */
    private String hint = null;

    /** if input list-controlled, name of list */
    private String valueListName = null;

    /** if input list-controlled, the list itself */
    private List valueList = null;

    /** if non-null, visibility scope restriction */
    private String visibility = null;

    /** the name of the controlled vocabulary to use */
    private String vocabulary = null;

    /** is the entry closed to vocabulary terms? */
    private boolean closedVocabulary = false;
    
    /**
     * Class constructor for creating a DCInput object based on the contents of
     * a HashMap
     * 
     * @param row
     *            the corresponding row in the table
     */
    public DCInput(Map fieldMap, Map listMap)
    {
        dcElement = (String) fieldMap.get("dc-element");
        dcQualifier = (String) fieldMap.get("dc-qualifier");

        // Default the schema to dublin core
        dcSchema = (String) fieldMap.get("dc-schema");
        if (dcSchema == null)
        {
            dcSchema = MetadataSchema.DC_SCHEMA;
        }

        String repStr = (String) fieldMap.get("repeatable");
        repeatable = "true".equalsIgnoreCase(repStr)
                || "yes".equalsIgnoreCase(repStr);
        label = (String) fieldMap.get("label");
        inputType = (String) fieldMap.get("input-type");
        // these types are list-controlled
        if ("dropdown".equals(inputType) || "qualdrop_value".equals(inputType))
        {
            valueListName = (String) fieldMap.get("value-pairs-name");
            valueList = (List) listMap.get(valueListName);
        }
        hint = (String) fieldMap.get("hint");
        warning = (String) fieldMap.get("required");
        required = (warning != null && warning.length() > 0);
        visibility = (String) fieldMap.get("visibility");
        vocabulary = (String) fieldMap.get("vocabulary");
        String closedVocabularyStr = (String) fieldMap.get("closedVocabulary");
        closedVocabulary = "true".equalsIgnoreCase(closedVocabularyStr)
                            || "yes".equalsIgnoreCase(closedVocabularyStr);
    }

    /**
     * Is this DCInput for display in the given scope? The scope should be
     * either "workflow" or "submit", as per the input forms definition. If the
     * internal visibility is set to "null" then this will always return true.
     * 
     * @param scope
     *            String identifying the scope that this input's visibility
     *            should be tested for
     * 
     * @return whether the input should be displayed or not
     */
    public boolean isVisible(String scope)
    {
        return (visibility == null || visibility.equals(scope));
    }

    /**
     * Get the repeatable flag for this row
     * 
     * @return the repeatable flag
     */
    public boolean isRepeatable()
    {
        return repeatable;
    }

    /**
     * Alternate way of calling isRepeatable()
     * 
     * @return the repeatable flag
     */
    public boolean getRepeatable()
    {
        return isRepeatable();
    }

    /**
     * Get the input type for this row
     * 
     * @return the input type
     */
    public String getInputType()
    {
        return inputType;
    }

    /**
     * Get the DC element for this form row.
     * 
     * @return the DC element
     */
    public String getElement()
    {
        return dcElement;
    }

    /**
     * Get the DC namespace prefix for this form row.
     * 
     * @return the DC namespace prefix
     */
    public String getSchema()
    {
        return dcSchema;
    }

    /**
     * Get the warning string for a missing required field, formatted for an
     * HTML table.
     * 
     * @return the string prompt if required field was ignored
     */
    public String getWarning()
    {
        return "<tr><td colspan=\"4\" class=\"submitFormWarn\">" + warning
                + "</td></tr>";
    }

    /**
     * Is there a required string for this form row?
     * 
     * @return true if a required string is set
     */
    public boolean isRequired()
    {
        return required;
    }

    /**
     * Get the DC qualifier for this form row.
     * 
     * @return the DC qualifier
     */
    public String getQualifier()
    {
        return dcQualifier;
    }

    /**
     * Get the hint for this form row, formatted for an HTML table
     * 
     * @return the hints
     */
    public String getHints()
    {
        return "<tr><td colspan=\"4\" class=\"submitFormHelp\">" + hint
                + "</td></tr>";
    }

    /**
     * Get the label for this form row.
     * 
     * @return the label
     */
    public String getLabel()
    {
        return label;
    }

    /**
     * Get the name of the pairs type
     * 
     * @return the pairs type name
     */
    public String getPairsType()
    {
        return valueListName;
    }

    /**
     * Get the name of the pairs type
     * 
     * @return the pairs type name
     */
    public List getPairs()
    {
        return valueList;
    }

    /**
     * Get the name of the controlled vocabulary that is associated with this
     * field
     * 
     * @return the name of associated the vocabulary
     */
    public String getVocabulary()
    {
        return vocabulary;
    }

    /**
     * Set the name of the controlled vocabulary that is associated with this
     * field
     * 
     * @param vocabulary
     *            the name of the vocabulary
     */
    public void setVocabulary(String vocabulary)
    {
        this.vocabulary = vocabulary;
    }

    /**
     * Gets the display string that corresponds to the passed storage string in
     * a particular display-storage pair set.
     * 
     * @param allPairs
     *            HashMap of all display-storage pair sets
     * @param pairTypeName
     *            Name of display-storage pair set to search
     * @param storageString
     *            the string that gets stored
     * 
     * @return the displayed string whose selection causes storageString to be
     *         stored, null if no match
     */
    public String getDisplayString(String pairTypeName, String storedString)
    {
        if (valueList != null)
        {
            for (int i = 0; i < valueList.size(); i += 2)
            {
                if (((String) valueList.get(i + 1)).equals(storedString))
                {
                    return (String) valueList.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Gets the stored string that corresponds to the passed display string in a
     * particular display-storage pair set.
     * 
     * @param allPairs
     *            HashMap of all display-storage pair sets
     * @param pairTypeName
     *            Name of display-storage pair set to search
     * @param displayString
     *            the string that gets displayed
     * 
     * @return the string that gets stored when displayString gets selected,
     *         null if no match
     */
    public String getStoredString(String pairTypeName, String displayedString)
    {
        if (valueList != null)
        {
            for (int i = 0; i < valueList.size(); i += 2)
            {
                if (((String) valueList.get(i)).equals(displayedString))
                {
                    return (String) valueList.get(i + 1);
                }
            }
        }
        return null;
    }

	/**
	 * The closed attribute of the vocabulary tag for this field as set in 
	 * input-forms.xml
	 * 
	 * <code> 
	 * <field>
	 *     .....
	 *     <vocabulary closed="true">nsrc</vocabulary>
	 * </field>
	 * </code>
	 * @return the closedVocabulary flags: true if the entry should be restricted 
	 *         only to vocabulary terms, false otherwise
	 */
	public boolean isClosedVocabulary() {
		return closedVocabulary;
	}

}
