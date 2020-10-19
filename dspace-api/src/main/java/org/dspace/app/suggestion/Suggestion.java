/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion;

import java.util.LinkedList;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.dto.MetadataValueDTO;

/**
 * 
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class Suggestion {

    private String id;

    private String display;

    private String source;

    private String externalSourceUri;

    private Item target;

    private List<SuggestionEvidence> evidences = new LinkedList<SuggestionEvidence>();

    private List<MetadataValueDTO> metadata = new LinkedList<MetadataValueDTO>();

    public Suggestion(String source, Item target, String idPart) {
        this.source = source;
        this.target = target;
        this.id = source + ":" + target.getID().toString() + ":" + idPart;
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

    public String getExternalSourceUri() {
        return externalSourceUri;
    }

    public void setExternalSourceUri(String externalSourceUri) {
        this.externalSourceUri = externalSourceUri;
    }

    public List<SuggestionEvidence> getEvidences() {
        return evidences;
    }

    public List<MetadataValueDTO> getMetadata() {
        return metadata;
    }

    public Item getTarget() {
        return target;
    }

    public String getID() {
        return id;
    }
}