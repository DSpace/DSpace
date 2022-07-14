/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.MetadataSchemaEnum;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ObjectUtils;
import org.xml.sax.SAXException;

/**
 * Class representing a line in an input form.
 *
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 */
public class DCInput {

    private static final Logger log = LoggerFactory.getLogger(DCInput.class);

    /**
     * the DC element name
     */
    private String dcElement = null;

    /**
     * the DC qualifier, if any
     */
    private String dcQualifier = null;

    /**
     * the DC namespace schema
     */
    private String dcSchema = null;

    /**
     * the input language
     */
    private boolean language = false;

    /**
     * the language code use for the input
     */
    private static final String LanguageName = "common_iso_languages";

    /**
     * the language list and their value
     */
    private List<String> valueLanguageList = null;

    /**
     * a label describing input
     */
    private String label = null;

    /**
     * a style instruction to apply to the input. The exact way to use the style value is UI depending that receive the
     * value from the REST API as is
     */
    private String style = null;

    /**
     * the input type
     */
    private String inputType = null;

    /**
     * is input required?
     */
    private boolean required = false;

    /**
     * if required, text to display when missing
     */
    private String warning = null;

    /**
     * is input repeatable?
     */
    private boolean repeatable = false;

    /**
     * should name-variants be used?
     */
    private boolean nameVariants = false;

    /**
     * 'hint' text to display
     */
    private String hint = null;

    /**
     * if input list-controlled, name of list
     */
    private String valueListName = null;

    /**
     * if input list-controlled, the list itself
     */
    private List<String> valueList = null;

    /**
     * if non-null, visibility scope restriction
     */
    private String visibility = null;

    /**
     * if non-null, readonly out of the visibility scope
     */
    private String readOnly = null;

    /**
     * the name of the controlled vocabulary to use
     */
    private String vocabulary = null;

    /**
     * is the entry closed to vocabulary terms?
     */
    private boolean closedVocabulary = false;

    /**
     * the regex to comply with, null if nothing
     */
    private String regex = null;

    /**
     * Access Control List - is user allowed for particular ACL action on this input field in given
     */
    private ACL acl = null;

    /**
     * allowed document types
     */
    private List<String> typeBind = null;

    /**
     * for this input type the complex definition is loaded from the all complex definitions
     */
    private ComplexDefinition complexDefinition = null;

    private boolean isRelationshipField = false;
    private boolean isMetadataField = false;
    private String relationshipType = null;
    private String searchConfiguration = null;
    private String filter;
    private List<String> externalSources;

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
     * @param fieldMap named field values.
     * @param listMap  value-pairs map, computed from the forms definition XML file
     * @param complexDefinitions  definition of the complex input - more inputs in one row
     */
    public DCInput(Map<String, String> fieldMap, Map<String, List<String>> listMap,
                   ComplexDefinitions complexDefinitions) {
        dcElement = fieldMap.get("dc-element");
        dcQualifier = fieldMap.get("dc-qualifier");

        // Default the schema to dublin core
        dcSchema = fieldMap.get("dc-schema");
        if (dcSchema == null) {
            dcSchema = MetadataSchemaEnum.DC.getName();
        }

        //check if the input have a language tag
        language = Boolean.valueOf(fieldMap.get("language"));
        valueLanguageList = new ArrayList();
        if (language) {
            String languageNameTmp = fieldMap.get("value-pairs-name");
            if (StringUtils.isBlank(languageNameTmp)) {
                languageNameTmp = LanguageName;
            }
            valueLanguageList = listMap.get(languageNameTmp);
        }

        String repStr = fieldMap.get("repeatable");
        repeatable = "true".equalsIgnoreCase(repStr)
            || "yes".equalsIgnoreCase(repStr);
        String nameVariantsString = fieldMap.get("name-variants");
        nameVariants = (StringUtils.isNotBlank(nameVariantsString)) ?
                nameVariantsString.equalsIgnoreCase("true") : false;
        label = fieldMap.get("label");
        inputType = fieldMap.get("input-type");
        // these types are list-controlled
        if ("dropdown".equals(inputType) || "qualdrop_value".equals(inputType)
            || "list".equals(inputType)) {
            valueListName = fieldMap.get("value-pairs-name");
            valueList = listMap.get(valueListName);
        }
        if ("complex".equals(inputType)) {
            complexDefinition = complexDefinitions.getByName((fieldMap.get(DCInputsReader.COMPLEX_DEFINITION_REF)));
        }
        hint = fieldMap.get("hint");
        warning = fieldMap.get("required");
        required = (warning != null && warning.length() > 0);
        visibility = fieldMap.get("visibility");
        readOnly = fieldMap.get("readonly");
        vocabulary = fieldMap.get("vocabulary");
        regex = fieldMap.get("regex");
        acl = ACL.fromString(fieldMap.get("acl"));
        String closedVocabularyStr = fieldMap.get("closedVocabulary");
        closedVocabulary = "true".equalsIgnoreCase(closedVocabularyStr)
            || "yes".equalsIgnoreCase(closedVocabularyStr);

        // parsing of the <type-bind> element (using the colon as split separator)
        typeBind = new ArrayList<String>();
        String typeBindDef = fieldMap.get("type-bind");
        if (typeBindDef != null && typeBindDef.trim().length() > 0) {
            String[] types = typeBindDef.split(",");
            for (String type : types) {
                typeBind.add(type.trim());
            }
        }
        style = fieldMap.get("style");
        isRelationshipField = fieldMap.containsKey("relationship-type");
        isMetadataField = fieldMap.containsKey("dc-schema");
        relationshipType = fieldMap.get("relationship-type");
        searchConfiguration = fieldMap.get("search-configuration");
        filter = fieldMap.get("filter");
        externalSources = new ArrayList<>();
        String externalSourcesDef = fieldMap.get("externalsources");
        if (StringUtils.isNotBlank(externalSourcesDef)) {
            String[] sources = StringUtils.split(externalSourcesDef, ",");
            for (String source: sources) {
                externalSources.add(StringUtils.trim(source));
            }
        }

    }

    /**
     * Is this DCInput for display in the given scope? The scope should be
     * either "workflow" or "submit", as per the input forms definition. If the
     * internal visibility is set to "null" then this will always return true.
     *
     * @param scope String identifying the scope that this input's visibility
     *              should be tested for
     * @return whether the input should be displayed or not
     */
    public boolean isVisible(String scope) {
        return (visibility == null || visibility.equals(scope));
    }

    /**
     * Is this DCInput for display in readonly mode in the given scope?
     * If the scope differ from which in visibility field then we use the out attribute
     * of the visibility element. Possible values are: hidden (default) and readonly.
     * If the DCInput is visible in the scope then this methods must return false
     *
     * @param scope String identifying the scope that this input's readonly visibility
     *              should be tested for
     * @return whether the input should be displayed in a readonly way or fully hidden
     */
    public boolean isReadOnly(String scope) {
        if (isVisible(scope)) {
            return false;
        } else {
            return readOnly != null && readOnly.equalsIgnoreCase("readonly");
        }
    }


    /**
     * Get the repeatable flag for this row
     *
     * @return the repeatable flag
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * Alternate way of calling isRepeatable()
     *
     * @return the repeatable flag
     */
    public boolean getRepeatable() {
        return isRepeatable();
    }

    /**
     * Get the nameVariants flag for this row
     *
     * @return the nameVariants flag
     */
    public boolean areNameVariantsAllowed() {
        return nameVariants;
    }

    /**
     * Get the input type for this row
     *
     * @return the input type
     */
    public @Nullable String getInputType() {
        return inputType;
    }

    /**
     * Get the DC element for this form field.
     *
     * @return the DC element
     */
    public String getElement() {
        return dcElement;
    }

    /**
     * Get the DC namespace prefix for this form field.
     *
     * @return the DC namespace prefix
     */
    public String getSchema() {
        return dcSchema;
    }

    /**
     * Get the warning string for a missing required field, formatted for an
     * HTML table.
     *
     * @return the string prompt if required field was ignored
     */
    public String getWarning() {
        return warning;
    }

    /**
     * Is there a required string for this form field?
     *
     * @return true if a required string is set
     */
    public boolean isRequired() {
        return required;
    }

    /**
     * Get the DC qualifier for this form field.
     *
     * @return the DC qualifier
     */
    public String getQualifier() {
        return dcQualifier;
    }

    /**
     * Get the language for this form field.
     *
     * @return the language state
     */
    public boolean getLanguage() {
        return language;
    }

    /**
     * Get the hint for this form field
     *
     * @return the hints
     */
    public String getHints() {
        return hint;
    }

    /**
     * Get the label for this form field.
     *
     * @return the label
     */
    public String getLabel() {
        return label;
    }

    /**
     * Get the style for this form field
     * 
     * @return the style
     */
    public String getStyle() {
        return style;
    }

    /**
     * Get the name of the pairs type
     *
     * @return the pairs type name
     */
    public String getPairsType() {
        return valueListName;
    }

    /**
     * Get the name of the pairs type
     *
     * @return the pairs type name
     */
    public List getPairs() {
        return valueList;
    }

    /**
     * Get the list of language tags
     *
     * @return the list of language
     */

    public List<String> getValueLanguageList() {
        return valueLanguageList;
    }

    /**
     * Get the name of the controlled vocabulary that is associated with this
     * field
     *
     * @return the name of associated the vocabulary
     */
    public String getVocabulary() {
        return vocabulary;
    }

    /**
     * Set the name of the controlled vocabulary that is associated with this
     * field
     *
     * @param vocabulary the name of the vocabulary
     */
    public void setVocabulary(String vocabulary) {
        this.vocabulary = vocabulary;
    }

    /**
     * Gets the display string that corresponds to the passed storage string in
     * a particular display-storage pair set.
     *
     * @param pairTypeName Name of display-storage pair set to search
     * @param storedString the string that gets stored
     * @return the displayed string whose selection causes storageString to be
     * stored, null if no match
     */
    public String getDisplayString(String pairTypeName, String storedString) {
        if (valueList != null && storedString != null) {
            for (int i = 0; i < valueList.size(); i += 2) {
                if (storedString.equals(valueList.get(i + 1))) {
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
     * @param pairTypeName    Name of display-storage pair set to search
     * @param displayedString the string that gets displayed
     * @return the string that gets stored when displayString gets selected,
     * null if no match
     */
    public String getStoredString(String pairTypeName, String displayedString) {
        if (valueList != null && displayedString != null) {
            for (int i = 0; i < valueList.size(); i += 2) {
                if (displayedString.equals(valueList.get(i))) {
                    return valueList.get(i + 1);
                }
            }
        }
        return null;
    }

    /**
     * The closed attribute of the vocabulary tag for this field as set in
     * submission-forms.xml
     *
     * {@code
     * <field>
     * .....
     * <vocabulary closed="true">nsrc</vocabulary>
     * </field>
     * }
     *
     * @return the closedVocabulary flags: true if the entry should be restricted
     * only to vocabulary terms, false otherwise
     */
    public boolean isClosedVocabulary() {
        return closedVocabulary;
    }

    /**
     * Decides if this field is valid for the document type
     *
     * @param typeName Document type name
     * @return true when there is no type restriction or typeName is allowed
     */
    public boolean isAllowedFor(String typeName) {
        if (typeBind.size() == 0) {
            return true;
        }

        return typeBind.contains(typeName);
    }

    public String getScope() {
        return visibility;
    }

    public String getRegex() {
        return regex;
    }

    public String getFieldName() {
        return Utils.standardize(this.getSchema(), this.getElement(), this.getQualifier(), ".");
    }

    public String getRelationshipType() {
        return relationshipType;
    }

    public String getSearchConfiguration() {
        return searchConfiguration;
    }

    public String getFilter() {
        return filter;
    }

    public List<String> getExternalSources() {
        return externalSources;
    }

    /**
     * Is user allowed for particular ACL action on this input field in given Context?
     *
     * @param c current Context, load the user data based on the current Context
     * @param action read/write
     * @return true if allowed, false otherwise
     */
    public boolean isAllowedAction(Context c, int action) {
        return acl.isAllowedAction(c, action);
    }

    public boolean isQualdropValue() {
        if ("qualdrop_value".equals(getInputType())) {
            return true;
        }
        return false;
    }

    public ComplexDefinition getComplexDefinition() {
        return this.complexDefinition;
    }

    /**
     * Convert complex definition HashMap to the ordered JSON string
     * @return complex definition in the JSON string which will be parsed in the FE
     */
    public String getComplexDefinitionJSONString() {
        String resultJson = "";
        JSONArray complexDefinitionListJSON = null;

        if (!ObjectUtils.isEmpty(this.complexDefinition)) {
            List<JSONObject> complexDefinitionJsonList = new ArrayList<>();
            for (String CDInputName : this.complexDefinition.getInputs().keySet()) {
                JSONObject inputFieldJson = new JSONObject();
                Map<String, String> inputField = this.complexDefinition.getInputs().get(CDInputName);
                inputFieldJson.put(CDInputName, new JSONObject(inputField));
                complexDefinitionJsonList.add(inputFieldJson);
            }
            complexDefinitionListJSON = new JSONArray(complexDefinitionJsonList);
            resultJson = complexDefinitionListJSON.toString();
        }

        return resultJson;
    }

    public boolean validate(String value) {
        if (StringUtils.isNotBlank(value)) {
            try {
                if (StringUtils.isNotBlank(regex)) {
                    Pattern pattern = Pattern.compile(regex);
                    if (!pattern.matcher(value).matches()) {
                        return false;
                    }
                }
            } catch (PatternSyntaxException ex) {
                log.error("Regex validation failed!", ex.getMessage());
            }

        }

        return true;
    }

    /**
     * Get the type bind list for use in determining whether
     * to display this field in angular dynamic form building
     * @return list of bound types
     */
    public List<String> getTypeBindList() {
        return typeBind;
    }

    /**
     * Verify whether the current field contains an entity relationship
     * This also implies a relationship type is defined for this field
     * The field can contain both an entity relationship and a metadata field simultaneously
     */
    public boolean isRelationshipField() {
        return isRelationshipField;
    }

    /**
     * Verify whether the current field contains a metadata field
     * This also implies a field type is defined for this field
     * The field can contain both an entity relationship and a metadata field simultaneously
     */
    public boolean isMetadataField() {
        return isMetadataField;
    }

    /**
     * Class representing a Map of the ComplexDefinition object
     * Class is copied from UFAL/CLARIN-DSPACE (https://github.com/ufal/clarin-dspace) and modified by
     * @author Milan Majchrak (milan.majchrak at dataquest dot sk)
     */
    public static class ComplexDefinitions {
        /**
         * Map of the ComplexDefiniton object
         */
        private Map<String, ComplexDefinition> definitions = null;
        private Map<String, List<String>> valuePairs = null;
        private static final String separator = ";";

        public ComplexDefinitions(Map<String, List<String>> valuePairs) {
            definitions = new HashMap<>();
            this.valuePairs = valuePairs;
        }

        public ComplexDefinition getByName(String name) {
            return definitions.get(name);
        }

        public void addDefinition(ComplexDefinition definition) {
            definitions.put(definition.getName(), definition);
            definition.setValuePairs(valuePairs);
        }

        public static String getSeparator() {
            return separator;
        }
    }

    /**
     * Class representing a complex input field - multiple lines in input form
     * Class is copied from UFAL/CLARIN-DSPACE (https://github.com/ufal/clarin-dspace) and modified by
     * @author Milan Majchrak (milan.majchrak at dataquest dot sk)
     */
    public static class ComplexDefinition {
        /**
         * Input fields in the input form
         */
        private Map<String, Map<String, String>> inputs;
        private String name;
        private Map<String, List<String>> valuePairs = null;

        /**
         * Class constructor for creating a ComplexDefinition object
         *
         * @param definitionName the name of the complex input type
         */
        public ComplexDefinition(String definitionName) {
            name = definitionName;
            inputs = new LinkedHashMap<>();
        }

        public String getName() {
            return name;
        }

        /**
         * Add input field definition to the complex input field definition
         * @param attributes of the input field definition e.g., ["name","surname"]
         * @throws SAXException
         */
        public void addInput(Map<String, String> attributes) throws SAXException {
            // these two are a must, check if present
            String iName = attributes.get("name");
            String iType = attributes.get("input-type");

            if (iName == null || iType == null) {
                throw new SAXException(
                        "Missing attributes (name or input-type) on complex definition input");
            }

            inputs.put(iName,attributes);
        }

        public Map<String, Map<String, String>> getInputs() {
            return this.inputs;
        }

        void setValuePairs(Map<String, List<String>> valuePairs) {
            this.valuePairs = valuePairs;
        }
    }
}
