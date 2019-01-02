/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.dspace.app.rest.RestResourceController;

/**
 * The WorkflowItem REST Resource
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class WorkflowItemRest extends BaseObjectRest<Integer> {
    public static final String NAME = "workflowitem";
    public static final String CATEGORY = RestAddressableModel.WORKFLOW;

    private Date lastModified = new Date();

    private Map<String, Serializable> sections;

    @JsonIgnore
    private CollectionRest collection;

    @JsonIgnore
    private ItemRest item;

    @JsonIgnore
    private SubmissionDefinitionRest submissionDefinition;

    @JsonIgnore
    private EPersonRest submitter;

    @Override
    public String getCategory() {
        return CATEGORY;
    }

    @Override
    public String getType() {
        return NAME;
    }

    /**
     * 
     * @return the timestamp of the last modification done to the workflowitem
     */
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * 
     * @return the item wrapped by the workflowitem
     */
    public ItemRest getItem() {
        return item;
    }

    public void setItem(ItemRest item) {
        this.item = item;
    }

    /**
     * 
     * @return the SubmissionDefinition used by the workflowitem
     */
    public SubmissionDefinitionRest getSubmissionDefinition() {
        return submissionDefinition;
    }

    public void setSubmissionDefinition(SubmissionDefinitionRest submissionDefinition) {
        this.submissionDefinition = submissionDefinition;
    }

    /**
     * 
     * @return the submitter
     */
    public EPersonRest getSubmitter() {
        return submitter;
    }

    public void setSubmitter(EPersonRest submitter) {
        this.submitter = submitter;
    }

    @Override
    public Class getController() {
        return RestResourceController.class;
    }

    /**
     * 
     * @return the data of the workflowitem organized according to the submission definition
     */
    public Map<String, Serializable> getSections() {
        if (sections == null) {
            sections = new HashMap<String, Serializable>();
        }
        return sections;
    }

    public void setSections(Map<String, Serializable> sections) {
        this.sections = sections;
    }

    /**
     * 
     * @return the collection where the workflow is in progress
     */
    public CollectionRest getCollection() {
        return collection;
    }

    public void setCollection(CollectionRest collection) {
        this.collection = collection;
    }
}