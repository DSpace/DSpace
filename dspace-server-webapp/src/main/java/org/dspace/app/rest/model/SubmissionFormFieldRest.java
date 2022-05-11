/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.rest.model;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.dspace.app.rest.model.submit.SelectableMetadata;
import org.dspace.app.rest.model.submit.SelectableRelationship;
import org.dspace.submit.model.LanguageFormField;

/**
 * The SubmissionFormField REST Resource. It is not addressable directly, only used
 * as inline object in the InputForm resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@JsonInclude(value = Include.NON_NULL)
public class SubmissionFormFieldRest {
    /**
     * The SubmissionFormInputType for this field
     */
    private SubmissionFormInputTypeRest input;

    /**
     * The main scope of the field
     */
    private ScopeEnum scope;

    /**
     * The visibility restriction for the field
     */
    private SubmissionVisibilityRest visibility;

    /**
     * The label of the field
     */
    private String label;

    /**
     * <code>true</code> if the field is required
     */
    private boolean mandatory;

    /**
     * <code>true</code> if the field allows multiple value
     */
    private boolean repeatable;

    /**
     * The message to return if the information is missing
     */
    private String mandatoryMessage;

    /**
     * A text to help field input
     */
    private String hints;

    /**
     * Extra information to be used by the UI to customize the presentation of the field. The format is dependent from
     * the UI implementation, the default Angular UI expects whitespace separated CSS class to add to the field
     */
    private String style;

    private SelectableRelationship selectableRelationship;
    /**
     * The list of metadata, often a single element, to offer for the storage of the information. This map the DSpace <
     * 7 concepts of qualdrop
     */
    private List<SelectableMetadata> selectableMetadata;

    /**
     * The list of language that can be used to fill the field
     */
    private List<LanguageFormField> languageCodes;

    /**
     * The list of type bind value
     */
    private List<String> typeBind;

    /**
     * Getter for {@link #selectableMetadata}
     * 
     * @return {@link #selectableMetadata}
     */
    public List<SelectableMetadata> getSelectableMetadata() {
        return selectableMetadata;
    }

    /**
     * Setter for {@link #selectableMetadata}
     * 
     */
    public void setSelectableMetadata(List<SelectableMetadata> selectableMetadata) {
        this.selectableMetadata = selectableMetadata;
    }

    /**
     * Getter for {@link #label}
     * 
     * @return {@link #label}
     */
    public String getLabel() {
        return label;
    }

    /**
     * Setter for {@link #label}
     * 
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Getter for {@link #mandatory}
     * 
     * @return {@link #mandatory}
     */
    public boolean isMandatory() {
        return mandatory;
    }

    /**
     * Setter for {@link #mandatory}
     * 
     */
    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Getter for {@link #repeatable}
     * 
     * @return {@link #repeatable}
     */
    public boolean isRepeatable() {
        return repeatable;
    }

    /**
     * Setter for {@link #repeatable}
     * 
     */
    public void setRepeatable(boolean repeatable) {
        this.repeatable = repeatable;
    }

    /**
     * Getter for {@link #mandatoryMessage}
     * 
     * @return {@link #mandatoryMessage}
     */
    public String getMandatoryMessage() {
        return mandatoryMessage;
    }

    /**
     * Setter for {@link #mandatoryMessage}
     * 
     */
    public void setMandatoryMessage(String mandatoryMessage) {
        this.mandatoryMessage = mandatoryMessage;
    }

    /**
     * Getter for {@link #hints}
     * 
     * @return {@link #hints}
     */
    public String getHints() {
        return hints;
    }

    /**
     * Setter for {@link #hints}
     * 
     */
    public void setHints(String hints) {
        this.hints = hints;
    }

    /**
     * Getter for {@link #style}
     * 
     * @return {@link #style}
     */
    public String getStyle() {
        return style;
    }

    /**
     * Setter for {@link #style}
     * 
     */
    public void setStyle(String style) {
        this.style = style;
    }

    /**
     * Getter for {@link #languageCodes}
     * 
     * @return {@link #languageCodes}
     */
    public List<LanguageFormField> getLanguageCodes() {
        if (languageCodes == null) {
            languageCodes = new ArrayList<LanguageFormField>();
        }
        return languageCodes;
    }

    /**
     * Setter for {@link #languageCodes}
     * 
     */
    public void setLanguageCodes(List<LanguageFormField> languageCodes) {
        this.languageCodes = languageCodes;
    }

    /**
     * Getter for {@link #input}
     * 
     * @return {@link #input}
     */
    public SubmissionFormInputTypeRest getInput() {
        return input;
    }

    /**
     * Setter for {@link #input}
     * 
     */
    public void setInput(SubmissionFormInputTypeRest input) {
        this.input = input;
    }

    /**
     * Getter for {@link #scope}
     * 
     * @return {@link #selectableMetadata}
     */
    public ScopeEnum getScope() {
        return scope;
    }

    /**
     * Setter for {@link #scope}
     * 
     */
    public void setScope(ScopeEnum scope) {
        this.scope = scope;
    }

    public SubmissionVisibilityRest getVisibility() {
        return visibility;
    }

    public void setVisibility(SubmissionVisibilityRest visibility) {
        if (visibility != null && (visibility.getMain() != null || visibility.getOther() != null)) {
            this.visibility = visibility;
        }
    }

    public List<String> getTypeBind() {
        return typeBind;
    }

    public void setTypeBind(List<String> typeBind) {
        this.typeBind = typeBind;
    }

    public SelectableRelationship getSelectableRelationship() {
        return selectableRelationship;
    }

    public void setSelectableRelationship(SelectableRelationship selectableRelationship) {
        this.selectableRelationship = selectableRelationship;
    }
}
