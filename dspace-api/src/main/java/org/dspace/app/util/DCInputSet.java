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
import java.util.Optional;

import org.apache.commons.lang3.Strings;
import org.apache.logging.log4j.Logger;
import org.dspace.core.Utils;

/**
 * Class representing all DC inputs required for a submission, organized into pages
 *
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 */

public class DCInputSet {
    /**
     * name of the input set
     */
    private String formName = null;
    /**
     * the inputs ordered by row position
     */
    private DCInput[][] inputs = null;

    private DCInputsReader inputReader;

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(DCInputSet.class);

    /**
     * Constructs a new DCInputSet instance by parsing and organizing input fields into a two-dimensional array.
     *
     * <p>This constructor processes the provided rows of field definitions and creates DCInput objects
     * for each field. The resulting structure organizes fields by row position, allowing for multi-column
     * layouts in submission forms.</p>
     *
     * @param inputReader the DCInputsReader instance used to resolve child forms for group and inline-group fields
     * @param formName the name of the form that defines this input set
     * @param rows a list of rows, where each row contains a list of field definitions represented as maps
     *             of string key-value pairs
     * @param listMap a map containing value-pairs for dropdown lists, keyed by list name
     * @throws DCInputsReaderException if there is an error parsing the field definitions or creating DCInput objects
     */
    public DCInputSet(DCInputsReader inputReader, String formName, List<List<Map<String, String>>> rows,
        Map<String, List<String>> listMap)
        throws DCInputsReaderException {
        this.inputReader = inputReader;
        this.formName = formName;
        this.inputs = new DCInput[rows.size()][];
        for (int i = 0; i < inputs.length; i++) {
            List<Map<String, String>> fields = rows.get(i);
            inputs[i] = new DCInput[fields.size()];
            for (int j = 0; j < inputs[i].length; j++) {
                Map<String, String> field = rows.get(i).get(j);
                inputs[i][j] = new DCInput(field, listMap);
            }
        }
    }

    /**
     * Returns the name of the form that defines this input set.
     *
     * <p>The form name is used to identify this particular input configuration and is specified
     * in the input-forms.xml configuration file.</p>
     *
     * @return the name of the form
     */
    public String getFormName() {
        return formName;
    }

    /**
     * Returns the number of field rows in this input set.
     *
     * <p>Note: This returns the number of rows (pages), not the total number of individual fields.
     * Each row may contain multiple fields arranged horizontally.</p>
     *
     * @return the number of field rows in this input set
     */
    public int getNumberFields() {
        return inputs.length;
    }

    /**
     * Returns all the fields in this input set organized in a two-dimensional array.
     *
     * <p>The outer array represents rows (pages) and the inner arrays represent fields within each row.
     * This structure allows for multi-column layouts in submission forms.</p>
     *
     * @return a two-dimensional array containing all DCInput fields organized by row and column position
     */
    public DCInput[][] getFields() {
        return inputs;
    }

    /**
     * Checks whether this input set includes an alternate title field.
     *
     * <p>This is a convenience method that checks for the presence of the "dc.title.alternative" field,
     * which is commonly used to capture alternative titles for items.</p>
     *
     * @return true if the current set has a "dc.title.alternative" field, false otherwise
     */
    public boolean isDefinedMultTitles() {
        return isFieldPresent("dc.title.alternative");
    }

    /**
     * Checks whether this input set includes all fields required for previously published items.
     *
     * <p>This method verifies the presence of three fields commonly used to capture information
     * about previously published works: "dc.date.issued", "dc.identifier.citation", and "dc.publisher".</p>
     *
     * @return true if the current set has all three previously published fields (date issued, citation, and publisher),
     *         false otherwise
     */
    public boolean isDefinedPubBefore() {
        return isFieldPresent("dc.date.issued") &&
            isFieldPresent("dc.identifier.citation") &&
            isFieldPresent("dc.publisher");
    }

    /**
     * Checks whether the current input set defines the named field.
     *
     * <p>This method scans through every field in every row of the input set to determine if
     * the specified field exists. It delegates to the {@link #getField(String)} method for
     * the actual search logic.</p>
     *
     * @param fieldName the fully qualified field name to search for, in the format
     *                  {@code schema.element.qualifier} (e.g., "dc.contributor.author")
     * @return true if the current set contains the named field, false otherwise
     */
    public boolean isFieldPresent(String fieldName) {
        return getField(fieldName).isPresent();
    }

    /**
     * Recursively searches for a field by its fully qualified name within this input set.
     *
     * <p>The search process handles several field types differently:</p>
     *
     * <ul>
     *   <li><strong>qualdrop_value fields:</strong> Checks both the base field name and all possible
     *       qualifier combinations from the dropdown pairs</li>
     *   <li><strong>group fields:</strong> Recursively searches in child forms following the naming
     *       convention {@code {parentFormName}-{schema}-{element}-{qualifier}}. Child forms are loaded
     *       and searched for nested fields. This allows relation-fields with grouped metadata
     *       (e.g., author with affiliation) to be properly resolved.</li>
     *   <li><strong>inline-group fields:</strong> Similar to group fields, recursively searches in child
     *       forms for nested fields within inline groups. The difference from group is that inline-group
     *       is used for simple field grouping without relation-field associations.</li>
     *   <li><strong>relationship fields:</strong> Matches fields using the pattern
     *       {@code relation.{relationshipType}}</li>
     *   <li><strong>standard fields:</strong> Direct field name matching</li>
     * </ul>
     *
     * <p><strong>Behavior difference between group and inline-group:</strong></p>
     * <ul>
     *   <li><strong>group:</strong> Used within {@code <relation-field>} elements to define nested metadata
     *       that should be grouped with relationship entities. For example, when creating an author relationship,
     *       the author's affiliation can be captured as grouped metadata. The parent field stores a placeholder
     *       value or relationship reference.</li>
     *   <li><strong>inline-group:</strong> Used for simple grouping of related metadata fields without
     *       relationship associations. Fields are grouped purely for UI presentation and data organization.
     *       Both parent and child fields store actual metadata values directly on the item.</li>
     * </ul>
     *
     * @param fieldName the fully qualified field name to search for, in the format
     *                  {@code schema.element.qualifier} (e.g., "dc.contributor.author")
     * @return an Optional containing the DCInput if found, or Optional.empty() if not found
     *         or if an error occurs during recursive resolution
     */
    public Optional<DCInput> getField(String fieldName) {
        for (int i = 0; i < inputs.length; i++) {
            for (int j = 0; j < inputs[i].length; j++) {
                DCInput field = inputs[i][j];
                // If this is a "qualdrop_value" field, then the full field name is the field + dropdown qualifier
                if (Strings.CS.equals(field.getInputType(), "qualdrop_value")) {
                    List<String> pairs = field.getPairs();
                    for (int k = 0; k < pairs.size(); k += 2) {
                        String qualifier = pairs.get(k + 1);
                        String fullName = Utils.standardize(field.getSchema(), field.getElement(), qualifier, ".");
                        if (fullName.equals(fieldName)) {
                            return Optional.of(field);
                        }
                    }
                } else if (Strings.CS.equalsAny(field.getInputType(), "group", "inline-group")) {
                    // For group and inline-group types, recursively search in child form
                    // Child form naming convention: {parentFormName}-{schema}-{element}-{qualifier}
                    String formName = getFormName() + "-" + Utils.standardize(field.getSchema(),
                        field.getElement(), field.getQualifier(), "-");
                    try {
                        DCInputSet inputConfig = inputReader.getInputsByFormName(formName);
                        Optional<DCInput> f = inputConfig.getField(fieldName);
                        if (f.isPresent()) {
                            return f;
                        }
                    } catch (DCInputsReaderException e) {
                        log.error(e.getMessage(), e);
                    }
                } else if (field.isRelationshipField() &&
                    ("relation." + field.getRelationshipType()).equals(fieldName)) {
                    return Optional.of(field);
                } else {
                    String fullName = field.getFieldName();
                    if (fullName != null && fullName.equals(fieldName)) {
                        return Optional.of(field);
                    }
                }
            }
        }
        return Optional.empty();
    }

    /**
     * Does the current input set define the named field?
     * and is valid for the specified document type
     * Scan through every field in every page of the input set
     *
     * @param fieldName    field name
     * @param documentType doc type
     * @return true if the current set has the named field
     */
    public boolean isFieldPresent(String fieldName, String documentType) {
        if (documentType == null) {
            documentType = "";
        }
        for (int i = 0; i < inputs.length; i++) {
            for (int j = 0; j < inputs[i].length; j++) {
                DCInput field = inputs[i][j];
                String fullName = field.getFieldName();
                if (fullName.equals(fieldName)) {
                    if (field.isAllowedFor(documentType)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean doField(DCInput dcf, boolean addTitleAlternative,
                              boolean addPublishedBefore) {
        String rowName = dcf.getFieldName();
        if (rowName.equals("dc.title.alternative") && !addTitleAlternative) {
            return false;
        }
        if (rowName.equals("dc.date.issued") && !addPublishedBefore) {
            return false;
        }
        if (rowName.equals("dc.publisher.null") && !addPublishedBefore) {
            return false;
        }
        if (rowName.equals("dc.identifier.citation") && !addPublishedBefore) {
            return false;
        }

        return true;
    }

    /**
     * Iterates through all DC input rows and populates a list of all allowed field names for a specific document type.
     *
     * <p>This method is important because an input can be configured repeatedly in a form with different
     * requirements for different document types. For example, a field could be required for type "Book"
     * but optional for type "Article". If a field is allowed for the specified document type, it will
     * never be stripped from metadata during validation.</p>
     *
     * <p>This method is more efficient than repeatedly calling {@link #isFieldPresent(String, String)}
     * because it builds a complete list in a single pass through the input set.</p>
     *
     * <p>Special handling for qualdrop_value fields: Both the base field name and all qualified variations
     * (field name + qualifier) are added to the list to ensure proper validation.</p>
     *
     * @param documentTypeValue the document type to check against, for example "Article" or "Book"
     * @return a list of field names that are allowed for the specified document type
     */
    public List<String> populateAllowedFieldNames(String documentTypeValue) {
        List<String> allowedFieldNames = new ArrayList<>();
        // Before iterating each input for validation, run through all inputs + fields and populate a lookup
        // map with inputs for this type. Because an input can be configured repeatedly in a form (for example
        // it could be required for type Book, and allowed but not required for type Article), allowed=true will
        // always take precedence
        for (DCInput[] row : inputs) {
            for (DCInput input : row) {
                if (input.isQualdropValue()) {
                    List<Object> inputPairs = input.getPairs();
                    //starting from the second element of the list and skipping one every time because the display
                    // values are also in the list and before the stored values.
                    for (int i = 1; i < inputPairs.size(); i += 2) {
                        String fullFieldname = input.getFieldName() + "." + inputPairs.get(i);
                        if (input.isAllowedFor(documentTypeValue)) {
                            if (!allowedFieldNames.contains(fullFieldname)) {
                                allowedFieldNames.add(fullFieldname);
                            }
                            // For the purposes of qualdrop, we have to add the field name without the qualifier
                            // too, or a required qualdrop will get confused and incorrectly reject a value
                            if (!allowedFieldNames.contains(input.getFieldName())) {
                                allowedFieldNames.add(input.getFieldName());
                            }
                        }
                    }
                } else {
                    if (input.isAllowedFor(documentTypeValue) && !allowedFieldNames.contains(input.getFieldName())) {
                        allowedFieldNames.add(input.getFieldName());
                    }
                }
            }
        }
        return allowedFieldNames;
    }

}
