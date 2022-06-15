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

import org.apache.commons.lang3.StringUtils;
import org.dspace.core.Utils;
/**
 * Class representing all DC inputs required for a submission, organized into pages
 *
 * @author Brian S. Hughes, based on work by Jenny Toves, OCLC
 * @version $Revision$
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

    /**
     * constructor
     *
     * @param formName       form name
     * @param mandatoryFlags
     * @param rows           the rows
     * @param listMap        map
     */
    public DCInputSet(String formName, List<List<Map<String, String>>> rows, Map<String, List<String>> listMap) {
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
     * Return the name of the form that defines this input set
     *
     * @return formName     the name of the form
     */
    public String getFormName() {
        return formName;
    }

    /**
     * Return the number of fields in this  input set
     *
     * @return number of pages
     */
    public int getNumberFields() {
        return inputs.length;
    }

    /**
     * Get all the fields
     *
     * @return an array containing the fields
     */

    public DCInput[][] getFields() {
        return inputs;
    }

    /**
     * Does this set of inputs include an alternate title field?
     *
     * @return true if the current set has an alternate title field
     */
    public boolean isDefinedMultTitles() {
        return isFieldPresent("dc.title.alternative");
    }

    /**
     * Does this set of inputs include the previously published fields?
     *
     * @return true if the current set has all the prev. published fields
     */
    public boolean isDefinedPubBefore() {
        return (isFieldPresent("dc.date.issued") &&
            isFieldPresent("dc.identifier.citation") &&
            isFieldPresent("dc.publisher"));
    }

    /**
     * Does the current input set define the named field?
     * Scan through every field in every page of the input set
     *
     * @param fieldName selects the field.
     * @return true if the current set has the named field
     */
    public boolean isFieldPresent(String fieldName) {
        for (int i = 0; i < inputs.length; i++) {
            for (int j = 0; j < inputs[i].length; j++) {
                DCInput field = inputs[i][j];
                // If this is a "qualdrop_value" field, then the full field name is the field + dropdown qualifier
                if (StringUtils.equals(field.getInputType(), "qualdrop_value")) {
                    List<String> pairs = field.getPairs();
                    for (int k = 0; k < pairs.size(); k += 2) {
                        String qualifier = pairs.get(k + 1);
                        String fullName = Utils.standardize(field.getSchema(), field.getElement(), qualifier, ".");
                        if (fullName.equals(fieldName)) {
                            return true;
                        }
                    }
                } else {
                    String fullName = field.getFieldName();
                    if (fullName.equals(fieldName)) {
                        return true;
                    }
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
     * Iterate DC input rows and populate a list of all allowed field names in this submission configuration.
     * This is important because an input can be configured repeatedly in a form (for example it could be required
     * for type Book, and allowed but not required for type Article).
     * If the field is allowed for this document type it'll never be stripped from metadata on validation.
     *
     * This can be more efficient than isFieldPresent to avoid looping the input set with each check.
     *
     * @param documentTypeValue     Document type eg. Article, Book
     * @return                      ArrayList of field names to use in validation
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
