/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

import java.util.Date;
import java.util.Map;

import org.dspace.app.rest.RestResourceController;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * The WorkspaceItem REST Resource
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class WorkspaceItemRest extends BaseObjectRest<Integer> {
	public static final String NAME = "workspaceitem";
	public static final String CATEGORY = RestModel.SUBMISSION;

	private Date lastModified = new Date();

	private Map<String, RestModel> sections;
	
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

	public Date getLastModified() {
		return lastModified;
	}
	
	public void setLastModified(Date lastModified){
		this.lastModified = lastModified;
	}

	public ItemRest getItem() {
		return item;
	}

	public void setItem(ItemRest item) {
		this.item = item;
	}

	public SubmissionDefinitionRest getSubmissionDefinition() {
		return submissionDefinition;
	}

	public void setSubmissionDefinition(SubmissionDefinitionRest submissionDefinition) {
		this.submissionDefinition = submissionDefinition;
	}

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

	public Map<String, RestModel> getSections() {
		return sections;
	}

	public void setSections(Map<String, RestModel> sections) {
		this.sections = sections;
	}
}