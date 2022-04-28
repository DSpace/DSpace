/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.submit;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * The SelectableMetadata REST Resource. It is not addressable directly, only
 * used as inline object in the InputForm resource.
 *
 * SelectableMetadata was introduced to make a clear distinction between the
 * cases where a value-pairs was used as an authority list of acceptable values
 * (dropdown) and where it was used to allow to pick the metadata to use to
 * store the value (qualdrop_values). If a value-pair is used by a
 * qualdrop_value it is not autoregistered as an authority, instead
 * it is exposed as an array of SelectableMetadata object
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.it)
 */
public class SelectableMetadata {
    private String metadata;
    private String label;
    @JsonInclude(Include.NON_NULL)
    private String controlledVocabulary;
    @JsonInclude(Include.NON_NULL)
    private Boolean closed = false;

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String key) {
        this.metadata = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setControlledVocabulary(String vocabularyName) {
        this.controlledVocabulary = vocabularyName;
    }

    public String getControlledVocabulary() {
        return controlledVocabulary;
    }

    public Boolean isClosed() {
        return closed;
    }

    public void setClosed(Boolean closed) {
        this.closed = closed;
    }
}
