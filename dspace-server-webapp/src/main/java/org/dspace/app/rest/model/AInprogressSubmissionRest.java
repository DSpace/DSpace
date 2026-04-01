/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract class to share common aspects between the REST representation of inprogressSubmission
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public abstract class AInprogressSubmissionRest<ID extends Serializable> extends BaseObjectRest<ID> {


    private Instant lastModified = Instant.now();
    private Map<String, Serializable> sections;
    @JsonIgnore
    private SubmissionDefinitionRest submissionDefinition;

    public Instant getLastModified() {
        return lastModified;
    }

    public void setLastModified(Instant lastModified) {
        this.lastModified = lastModified;
    }

    public SubmissionDefinitionRest getSubmissionDefinition() {
        return submissionDefinition;
    }

    public void setSubmissionDefinition(SubmissionDefinitionRest submissionDefinition) {
        this.submissionDefinition = submissionDefinition;
    }

    public Map<String, Serializable> getSections() {
        if (sections == null) {
            sections = new HashMap<String, Serializable>();
        }
        return sections;
    }

    public void setSections(Map<String, Serializable> sections) {
        this.sections = sections;
    }



}
