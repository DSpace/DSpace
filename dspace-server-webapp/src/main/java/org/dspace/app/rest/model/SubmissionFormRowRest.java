/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import java.util.List;

/**
 * The InputFormRow REST Resource. It is not addressable directly, only used
 * as inline object in the InputForm resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class SubmissionFormRowRest {
    /**
     * The list of fields in the row
     */
    private List<SubmissionFormFieldRest> fields;

    /**
     * Getter for {@link #fields}
     * 
     * @return {@link #fields}
     */
    public List<SubmissionFormFieldRest> getFields() {
        return fields;
    }

    /**
     * Setter for {@link #fields}
     * 
     */
    public void setFields(List<SubmissionFormFieldRest> fields) {
        this.fields = fields;
    }
}

