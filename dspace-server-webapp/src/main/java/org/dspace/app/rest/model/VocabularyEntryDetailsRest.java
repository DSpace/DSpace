/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * The Vocabulary Entry Details REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = {
        @LinkRest(name = VocabularyEntryDetailsRest.PARENT, method = "getParent"),
        @LinkRest(name = VocabularyEntryDetailsRest.CHILDREN, method = "getChildren")
        })
public class VocabularyEntryDetailsRest extends BaseObjectRest<String> {
    public static final String PLURAL_NAME = "vocabularyEntryDetails";
    public static final String NAME = "vocabularyEntryDetail";
    public static final String PARENT = "parent";
    public static final String CHILDREN = "children";
    private String display;
    private String value;
    private Map<String, String> otherInformation;
    private boolean selectable;
    @JsonIgnore
    private boolean inHierarchicalVocabulary = false;

    @JsonIgnore
    private String vocabularyName;

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String value) {
        this.display = value;
    }

    public Map<String, String> getOtherInformation() {
        return otherInformation;
    }

    public void setOtherInformation(Map<String, String> otherInformation) {
        this.otherInformation = otherInformation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static String getName() {
        return NAME;
    }

    public String getVocabularyName() {
        return vocabularyName;
    }

    public void setVocabularyName(String vocabularyName) {
        this.vocabularyName = vocabularyName;
    }

    @Override
    public String getCategory() {
        return VocabularyRest.CATEGORY;
    }

    @Override
    public String getType() {
        return VocabularyEntryDetailsRest.NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public Boolean isSelectable() {
        return selectable;
    }

    public void setSelectable(Boolean selectable) {
        this.selectable = selectable;
    }

    public void setInHierarchicalVocabulary(boolean isInHierarchicalVocabulary) {
        this.inHierarchicalVocabulary = isInHierarchicalVocabulary;
    }

    public boolean isInHierarchicalVocabulary() {
        return inHierarchicalVocabulary;
    }
}
