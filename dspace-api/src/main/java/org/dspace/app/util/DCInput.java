/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.dspace.content.MetadataSchema;
import org.dspace.core.Context;
import org.xml.sax.SAXException;

import cz.cuni.mff.ufal.dspace.app.util.ACL;

/**
 * Class representing a line in an input form.
 *
 * based on class by Brian S. Hughes, based on work by Jenny Toves, OCLC
 * modified for LINDAT/CLARIN
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

    /** UFAL/jmisutka - */
    private String extraMappedToElement = null;

    /** UFAL/jmisutka - */
    private String extraRepeatableComponent = null;

    /** UFAL/jmisutka - */
    private String autocomplete = null;

    /** UFAL/jmisutka - */
    private String extra = null;

    /** UFAL/jmisutka - */
    private String collapsible = null;

    /** UFAL/okosarko - */
    private String regexp = null;

    /** UFAL/okosarko - */
    private String regexpWarning = null;

    /** UFAL/josifko - */
    private ACL acl = null;

    /** UFAL/josifko - */
    private Set<String> rends = null;

    /** a label describing input */
    private String label = null;

    /** a label describing input */
    private String component_label = null;

    /** the input type */
    private String inputType = null;

    /** is input required? */
    private boolean required = false;

    /** if required, text to display when missing */
    private String warning = null;

    /** is input repeatable? */
    private boolean repeatable = false;

    /** should repeatable input be parsed? */
    private boolean repeatable_parse = false;

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

	private ComplexDefinition complexDefinition = null;   
 	
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
    public DCInput(Map<String, String> fieldMap,
            Map<String, List<String>> listMap, ComplexDefinitions complexDefinitions)
    {
        dcElement = fieldMap.get("dc-element");
        dcQualifier = fieldMap.get("dc-qualifier");

        // UFAL / jmisutka
        extraMappedToElement = fieldMap.get("mapped-to");
        extraRepeatableComponent = fieldMap.get("repeatable-component");
        autocomplete = fieldMap.get("autocomplete");
        extra = fieldMap.get("extra");
        collapsible = fieldMap.get("collapsible");
        component_label = fieldMap.get("component-label");
        regexp = fieldMap.get("regexp");
        regexpWarning = fieldMap.get("regexp-warning");

        // UFAL / josifko
        acl = ACL.fromString(fieldMap.get("acl"));
        rends = new HashSet<String>();
        if(fieldMap.containsKey("class"))
        {
            rends.addAll(Arrays.asList(fieldMap.get("class").split(" ")));
        }

        // Default the schema to dublin core
        dcSchema = fieldMap.get("dc-schema");
        if (dcSchema == null)
        {
            dcSchema = MetadataSchema.DC_SCHEMA;
        }

        String repStr = fieldMap.get("repeatable");
        repeatable = "true".equalsIgnoreCase(repStr)
                || "yes".equalsIgnoreCase(repStr);
        //
        String repParseStr = fieldMap.get("repeatable-parse");
        repeatable_parse = "true".equalsIgnoreCase(repParseStr)
                || "yes".equalsIgnoreCase(repParseStr);

        label = fieldMap.get("label");
        inputType = fieldMap.get("input-type");
        // these types are list-controlled
        if ("dropdown".equals(inputType) || "qualdrop_value".equals(inputType)
                || "list".equals(inputType))
        {
            valueListName = fieldMap.get("value-pairs-name");
            valueList = listMap.get(valueListName);
        }

        if ("complex".equals(inputType)){
        	complexDefinition = complexDefinitions.getByName((fieldMap.get(DCInputsReader.COMPLEX_DEFINITION_REF)));
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
        	String[] types = typeBindDef.split(",");
        	for(String type : types) {
        		typeBind.add( type.trim() );
        	}
        }
        
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
        return isRepeatable(true);
    }

    /**
     * UFAL/jmisutka
     *
     * @param really_repeatable
     *            - if true than it will simulate the original behaviour if
     *            false it will include our extra component logic.
     */
    public boolean isRepeatable(boolean really_repeatable)
    {
        if (really_repeatable)
        {
            return repeatable;
        }
        else
        {
            return repeatable || null != extraRepeatableComponent;
        }
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

    public boolean getRepeatableParse()
    {
        return repeatable_parse;
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
     * UFAL/jmisutka Return dc element which should be mapped to this Input in
     * one string. Dots "." should be used for splitting.
     */
    public String getExtraMappedToElement()
    {
        return extraMappedToElement;
    }

    /**
     * UFAL/jmisutka Return component name which should be repeatable - this is
     * needed for hierarchical repeatable components.
     */
    public String getExtraRepeatableComponent()
    {
        return extraRepeatableComponent;
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
		
		return typeBind.contains(typeName);
	}
	
  /**
     * Check whether the supplied values is allowed (matching supplied email)
     * eg. email validity check
     *
     * @param value
     * @return
     */
    public boolean isAllowedValue(String value){
    	return isAllowedValue(value, regexp);
    }

    public static boolean isAllowedValue(String value, String regex)
    {
        if (regex == null || regex.isEmpty())
        {
            return true;
        }
        else if (value == null)
        {
            return false;
        }
        else
        {
            return value.matches(regex);
        }
    }

    public String getRegexp()
    {
        return regexp;
    }

    public String getRegexpWarning()
    {
        return regexpWarning;
    }

    public String getAutocomplete()
    {
        return autocomplete;
    }

    public String getExtra()
    {
        return extra;
    }

    public boolean hasExtraAttribute(String extra_string)
    {
        return null != extra && extra.contains(extra_string);
    }

    public String getComponentLabel()
    {
        return component_label;
    }

    public String getCollapsible()
    {
        return collapsible;
    }

    /**
     * Is user allowed for particular ACL action on this input field in given
     * Context?
     *
     * @param c
     *            Contex
     * @param action
     *            Action
     * @return true if allowed, false otherwise
     */
    public boolean isAllowedAction(Context c, int action)
    {
        return acl.isAllowedAction(c, action);
    }

    /**
     * Returns true if there is a ACL with at least one ACE bound to this input field
     *
     * @return
     */
    public boolean hasACL()
    {
        if(acl != null && !acl.isEmpty())
        {
            return true;
        }
        return false;
    }

    /**
     * Adds another rend to set of rends for further rendering in GUI
     *
     * @param rend
     */
    public void addRend(String rend)
    {
        rends.add(rend);
    }

    /**
     * Returns set of rends for further rendering in GUI
     *
     * @return
     */
    public Set<String> getRends() {
        return rends;
    }

    /**
     * Returns rends as space separated String
     *
     * @return
     */
    public String getRendsAsString() {
        return StringUtils.join(rends.toArray()," ");
    }

	public ComplexDefinition getComplexDefinition() {
		if(getInputType().equals("complex")){
			return complexDefinition;
		} else{
			throw new UnsupportedOperationException();
		}
	}

	public static class ComplexDefinitions{
		private Map<String, ComplexDefinition> definitions = null;
		private Map<String, List<String>> valuePairs = null;

		ComplexDefinitions(Map<String, List<String>> valuePairs){
			definitions = new HashMap<String, ComplexDefinition>();
			this.valuePairs = valuePairs;
		}

		public ComplexDefinition getByName(String name){
			return definitions.get(name);
		}

		public void addDefinition(ComplexDefinition definition) {
			definitions.put(definition.getName(), definition);
			definition.setValuePairs(valuePairs);
		}
	}

	public static class ComplexDefinition{
		//use something that wont get replaced when entering into db
		public static final String SEPARATOR = "@@";
		private SortedMap<String, Map<String, String>> inputs;
		private String name;
		private Map<String, List<String>> valuePairs = null;

		public ComplexDefinition(String definitionName) {
			name = definitionName;
			inputs = new TreeMap<String, Map<String, String>>();
		}

		public String getName() {
			return name;
		}

		public void addInput(Map<String, String> attributes) throws SAXException {
			// these two are a must, check if present
			String iName = attributes.get("name");
			String iType = attributes.get("type");

			if (iName == null || iType == null) {
				throw new SAXException(
						"Missing attributes (name or type) on complex definition input");
			}

			inputs.put(iName,attributes);

		}

		public Map<String, String> getInput(String name){
			return inputs.get(name);
		}

		public Set<String> getInputNames() {
			return inputs.keySet();
		}

		public int inputsCount() {
			return getInputNames().size();
		}

		void setValuePairs(Map<String, List<String>> valuePairs){
			this.valuePairs = valuePairs;
		}

		public java.util.List<String> getValuePairsForInput(String name) {
			String pairsRef = getInput(name).get("pairs");
			if(valuePairs != null && pairsRef != null){
				return valuePairs.get(pairsRef);
			}
			return null;
		}

	}

}
