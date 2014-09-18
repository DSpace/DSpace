package org.dspace.app.webui.discovery.dto;

/**
 * Stores the result of the could construction
 * @author Márcio Ribeiro Gurgel do Amaral (marcio.rga@gmail.com)
 *
 */
public class CloudTagResult {

	
	private String cloudName;
	private String field;
	private Long relevance;
	
	/**
	 * Build a new could result registry
	 * @param cloudName
	 * @param link
	 * @param relevance
	 */
	public CloudTagResult(String cloudName, String field, Long relevance) {
		this.cloudName = cloudName;
		this.field = field;
		this.relevance = relevance;
	}
	
	public String getCloudName() {
		return cloudName;
	}
	public String getField() {
		return field;
	}
	public Long getRelevance() {
		return relevance;
	}
	
}
