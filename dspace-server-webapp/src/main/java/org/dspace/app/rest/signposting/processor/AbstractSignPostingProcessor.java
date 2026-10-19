/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.processor;

import org.dspace.app.rest.signposting.model.LinksetRelationType;

/**
 * An abstract class of generic signposting relation.
 * 
 * @author Luigi Andrea Pascarelli (luigiandrea.pascarelli at 4science.com)
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public abstract class AbstractSignPostingProcessor {

    private String metadataField;

    private LinksetRelationType relation;

    private String pattern;

    public String getMetadataField() {
        return metadataField;
    }

    public void setMetadataField(String metadataField) {
        this.metadataField = metadataField;
    }

    public LinksetRelationType getRelation() {
        return relation;
    }

    public void setRelation(LinksetRelationType relation) {
        this.relation = relation;
    }

    public String getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

}
