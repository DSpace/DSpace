/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import org.dspace.app.rest.RestResourceController;

/**
 * The Suggestion REST Resource. A suggestion is an object, usually a
 * publication, proposed by a source related to a specific Person (target) to be
 * imported in the system.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
@LinksRest(links = { @LinkRest(name = SuggestionRest.TARGET, method = "getTarget") })
public class SuggestionRest extends BaseObjectRest<String> {
    private static final long serialVersionUID = 1L;
    public static final String NAME = "suggestion";
    public static final String PLURAL_NAME = "suggestions";
    public static final String TARGET = "target";
    public static final String CATEGORY = RestAddressableModel.INTEGRATION;

    private String display;
    private String source;
    private String externalSourceUri;
    private String score;
    private Map<String, EvidenceRest> evidences = new HashMap<String, EvidenceRest>();
    private MetadataRest metadata = new MetadataRest();

    @Override
    @JsonProperty(access = Access.READ_ONLY)
    public String getType() {
        return NAME;
    }

    @Override
    public String getTypePlural() {
        return PLURAL_NAME;
    }

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getExternalSourceUri() {
        return externalSourceUri;
    }

    public void setExternalSourceUri(String externalSourceUri) {
        this.externalSourceUri = externalSourceUri;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getScore() {
        return score;
    }

    public Map<String, EvidenceRest> getEvidences() {
        return evidences;
    }

    public void setEvidences(Map<String, EvidenceRest> evidences) {
        this.evidences = evidences;
    }

    public MetadataRest getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataRest metadata) {
        this.metadata = metadata;
    }

    /** 
     * inner class to encapsulate score & notes
     * and map {@link SuggestionEvidence}
     * */
    public static class EvidenceRest {
        public String score;
        public String notes;
        public EvidenceRest(String score, String notes) {
            this.score = score;
            this.notes = notes;
        }
    }
}
