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
 * This entity contains metadatas that should be added to the targeted Item
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class Suggestion {

    /** id of the suggestion */
    private String id;

    /** the dc.title of the item */
    private String display;

    /** the external source name the suggestion comes from */
    private String source;

    /** external uri of the item */
    private String externalSourceUri;

    /** item targeted by this suggestion */
    private Item target;

    private List<SuggestionEvidence> evidences = new LinkedList<SuggestionEvidence>();

    private List<MetadataValueDTO> metadata = new LinkedList<MetadataValueDTO>();

    /** suggestion creation
     * @param source name of the external source
     * @param target the targeted item in repository
     * @param idPart external item id, used mainly for suggestion @see #id creation
     * */
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

    public Double getScore() {
        if (evidences != null && evidences.size() > 0) {
            double score = 0;
            for (SuggestionEvidence evidence : evidences) {
                score += evidence.getScore();
            }
            return score;
        }
        return null;
    }
}