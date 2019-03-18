/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.dspace.eperson.Group;

/**
 * Class representing a line in an input form.
 * 
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
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
    private List<String> valueList = null;

    /** if non-null, visibility scope restriction */
    private String visibility = null;
    
    /** if non-null, readonly out of the visibility scope */
    private String readOnly = null;

    /** the name of the controlled vocabulary to use */
    private String vocabulary = null;

    /** is the entry closed to vocabulary terms? */
    private boolean closedVocabulary = false;
    /** allowed document types */
    private List<String> typeBind = null;
    
    /* SEDICI-BEGIN */
    /** indicates the opposite of types */
	private boolean negateTypeBind = false;

    /** if the field is internationalizable */
    private boolean i18nable = false;
        
    /**
     * Group-based mandatory attribute
     * Saves a Map containing the list of groups on wich this field's obligatoriness is restricted on.
     * Map's keys are group names and map's values determine whether there is a NOT modifier for that group 
     */
    private Map<String, Boolean> requirementOnGroup = null;
    
    /**
     * Group-based visibility restriction
     * Saves a Map containing the list of groups on wich this field's visibility is restricted on.
     * Map's keys are group names and map's values determine whether there is a NOT modifier for that group 
     */
    private Map<String, Boolean> visibilityOnGroup = null;

    /* SEDICI-END */

    /** 
     * The scope of the input sets, this restricts hidden metadata fields from 
     * view during workflow processing. 
     */
    public static final String WORKFLOW_SCOPE = "workflow";

    /** 
     * The scope of the input sets, this restricts hidden metadata fields from 
     * view by the end user during submission. 
     */
    public static final String SUBMISSION_SCOPE = "submit";
    
    /**
     * Class constructor for creating a DCInput object based on the contents of
     * a HashMap
     * 
     * @param fieldMap
     *            ???
     * @param listMap
     */
    public DCInput(Map<String, String> fieldMap, Map<String, List<String>> listMap)
    {
        dcElement = fieldMap.get("dc-element");
        dcQualifier = fieldMap.get("dc-qualifier");

        // Default the schema to dublin core
        dcSchema = fieldMap.get("dc-schema");
        if (dcSchema == null)
        {
            dcSchema = MetadataSchema.DC_SCHEMA;
        }

        String repStr = fieldMap.get("repeatable");
        repeatable = "true".equalsIgnoreCase(repStr)
                || "yes".equalsIgnoreCase(repStr);
        label = fieldMap.get("label");
        inputType = fieldMap.get("input-type");
        // these types are list-controlled
        if ("dropdown".equals(inputType) || "qualdrop_value".equals(inputType)
                || "list".equals(inputType))
        {
            valueListName = fieldMap.get("value-pairs-name");
            valueList = listMap.get(valueListName);
        }
        hint = fieldMap.get("hint");
        warning = fieldMap.get("required");
        required = (warning != null && warning.length() > 0);
        visibility = fieldMap.get("visibility");
        readOnly = fieldMap.get("readonly");
        vocabulary = fieldMap.get("vocabulary");
        String closedVocabularyStr = fieldMap.get("closedVocabulary");
        closedVocabulary = "true".equalsIgnoreCase(closedVocabularyStr)
                            || "yes".equalsIgnoreCase(closedVocabularyStr);
        
        // parsing of the <type-bind> element (using the colon as split separator)
        typeBind = new ArrayList<String>();
        String typeBindDef = fieldMap.get("type-bind");
        if(typeBindDef != null && typeBindDef.trim().length() > 0) {
	 	/* SEDICI-BEGIN */
        	if(typeBindDef.startsWith("!")){
        		//Se esta negando todo el type-bind
        		negateTypeBind = true;
        		typeBindDef = typeBindDef.substring(1);
        	}
        	/* SEDICI-END */
        	String[] types = typeBindDef.split(",");
        	for(String type : types) {
        		typeBind.add( type.trim() );
        	}
        }
        
	 /* SEDICI-BEGIN */
        // is i18nable ?
        String i18nableStr = fieldMap.get("i18n");
        i18nable = "true".equalsIgnoreCase(i18nableStr)
                || "yes".equalsIgnoreCase(i18nableStr);
        
        // Is it a group-based field?
        requirementOnGroup = new HashMap<String, Boolean>();
        String requiredOnGroupDef = fieldMap.get("required-on-group");
        if(requiredOnGroupDef != null && requiredOnGroupDef.trim().length() > 0) {
            // Splits the field's content and parses them individually
            for(String restriction : requiredOnGroupDef.split(",")) {
                restriction = restriction.trim();
                Boolean isPositiveRestriction = true;
                if(restriction.startsWith("!")) {
                    isPositiveRestriction = false;
                    restriction = restriction.substring(1);
                }
                // Register the restriction
                requirementOnGroup.put(restriction, isPositiveRestriction);
            }
        }
        
        // Has it a group-based visibility restriction?
        visibilityOnGroup = new HashMap<String, Boolean>();
        String visibilityOnGroupContent = fieldMap.get("visibility-on-group");
        if(visibilityOnGroupContent != null && visibilityOnGroupContent.trim().length() > 0) {
        	// Splits the field's content and parses them individually 
        	for(String restriction : visibilityOnGroupContent.split(",")) {
        		restriction = restriction.trim();
        		Boolean isPositiveRestriction = true;
	        	if(restriction.startsWith("!")) {
	        		isPositiveRestriction = false;
	        		restriction = restriction.substring(1);
	        	}
	        	// Register the restriction
	        	visibilityOnGroup.put(restriction, isPositiveRestriction);
        	}
        }
        
        /* SEDICI-END */
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
     * Is this DCInput for display in readonly mode in the given scope? 
     * If the scope differ from which in visibility field then we use the out attribute
     * of the visibility element. Possible values are: hidden (default) and readonly.
     * If the DCInput is visible in the scope then this methods must return false
     * 
     * @param scope
     *            String identifying the scope that this input's readonly visibility
     *            should be tested for
     * 
     * @return whether the input should be displayed in a readonly way or fully hidden
     */
    public boolean isReadOnly(String scope)
    {
        if (isVisible(scope))
        {
            return false;
        }
        else
        {
            return readOnly != null && readOnly.equalsIgnoreCase("readonly");
        }
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
        return warning;
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
        return hint;
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
     * @param pairTypeName
     *            Name of display-storage pair set to search
     * @param storedString
     *            the string that gets stored
     * 
     * @return the displayed string whose selection causes storageString to be
     *         stored, null if no match
     */
    public String getDisplayString(String pairTypeName, String storedString)
    {
        if (valueList != null && storedString != null)
        {
            for (int i = 0; i < valueList.size(); i += 2)
            {
                if (storedString.equals(valueList.get(i + 1)))
                {
                    return valueList.get(i);
                }
            }
        }
        return null;
    }

    /**
     * Gets the stored string that corresponds to the passed display string in a
     * particular display-storage pair set.
     * 
     * @param pairTypeName
     *            Name of display-storage pair set to search
     * @param displayedString
     *            the string that gets displayed
     * 
     * @return the string that gets stored when displayString gets selected,
     *         null if no match
     */
    public String getStoredString(String pairTypeName, String displayedString)
    {
        if (valueList != null && displayedString != null)
        {
            for (int i = 0; i < valueList.size(); i += 2)
            {
                if (displayedString.equals(valueList.get(i)))
                {
                    return valueList.get(i + 1);
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

	/**
	 * Decides if this field is valid for the document type
	 * @param typeName Document type name
	 * @return true when there is no type restriction or typeName is allowed
	 */
	public boolean isAllowedFor(String typeName) {
		if(typeBind.size() == 0)
			return true;
		else if (negateTypeBind)
			return !typeBind.contains(typeName);
		else
			return typeBind.contains(typeName);
	}
	
        /* SEDICI-BEGIN */
	/**
	 * Returns true if this field has a group-based mandatory restriction
	 * @return true
	 */
	public boolean isGroupBased() {
	    return !(requirementOnGroup.size() == 0);
	}
	/**
	 * Get the names of the groups in the required-on-group clause.
	 * @return an String array of group names
	 */
	public String[] getRequiredRestrictions() {
	    return requirementOnGroup.keySet().toArray( new String[requirementOnGroup.size()] );
	}
	
	/**
	 * Returns @true if the specified group is negated or not in required-on-group configuration.
	 */
	public boolean isRequiredPositiveRestriction(String groupName) {
	    return requirementOnGroup.get(groupName);
	}
	
	public boolean isI18nable() {
		return i18nable;
	}
	
	public boolean hasVisibilityOnGroup() {
		return !(visibilityOnGroup.size() == 0);
	}
	
	public String[] getVisibilityRestrictions() {
		return visibilityOnGroup.keySet().toArray( new String[visibilityOnGroup.size()] );
	}
	
	public boolean isVisibilityPositiveRestriction(String groupName) {
		return visibilityOnGroup.get(groupName);
	}
	
    /**
     * Mini-cache of loaded groups for group-based validation
     * 
     * @return Group instance
     */
    private Map<String, Group> loadedGroups = new HashMap<String, Group>();
    private Group findGroup(Context context, String groupName) throws SQLException {
    	Group group = loadedGroups.get(groupName);
    	if(group == null) {
    		group = Group.findByName(context, groupName);
    		loadedGroups.put(groupName, group);
    	}
    	return group;
    }
    
    /**
     * Evaluates all conditions in visibility-on-group. If any is true, then the all expression is true.
     */
    public boolean isVisibleOnGroup(Context context) throws SQLException, AuthorizeException {
    	
    	if(!hasVisibilityOnGroup())
    		return true;
    	
    	for(String groupName : getVisibilityRestrictions()) {
    		Group group = findGroup(context, groupName);
        	if( group == null) {
        		throw new AuthorizeException("Group "+groupName+ " does not exist, check your input_forms.xml");
        	}
            if(isVisibilityPositiveRestriction(groupName)) {
                if(Group.isMember(context, group.getID())) {
                    return true;
                }
            } else { //if not positive
                if(!Group.isMember(context, group.getID())) {
                    return true;
                }
            }
    	}
		return false;
    }

    /**
     * Evaluates all conditions in required-on-group. If any is true, then the all expression is true.
     */
    public boolean isRequiredOnGroup(Context context) throws SQLException, AuthorizeException {
	    if(!isGroupBased()) {
	        return true;
	    }
	    for(String groupName : getRequiredRestrictions()) {
	        Group group = findGroup(context, groupName);
	        if( group == null) {
	            throw new AuthorizeException("Group "+groupName+ " does not exist, check your input_forms.xml");
	        }
	        if(isRequiredPositiveRestriction(groupName)) {
	            if(Group.isMember(context, group.getID())) {
	                return true;
	            }
	        } else { //if not positive
	            if(!Group.isMember(context, group.getID())) {
                    return true;
                }
	        }
	    }
	    return false;
    }
	
    /* SEDICI-END */

}
