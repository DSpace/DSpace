/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.cris.integration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;
import org.apache.log4j.Logger;
import org.dspace.authority.AuthorityValueGenerator;
import org.dspace.authority.openaireproject.OpenAIREProjectService;
import org.dspace.authority.openaireproject.OpenAireProject;
import org.dspace.content.authority.Choice;
import org.dspace.content.authority.Choices;
import org.dspace.core.ConfigurationManager;
import org.dspace.utils.DSpace;

public class OpenAIREProjectAuthority extends ProjectAuthority {
	
	private static Logger log = Logger.getLogger(OpenAIREProjectAuthority.class);
	//Results of first page
	private static final int DEFAULT_MAX_ROWS = 10;
	private static final String OPENAIRE_PROJECT_PREFIX="info:eu-repo/grantAgreement/";
	private static final String OPENAIRE_PROJECT_AUTHORITY_TYPE = "openAireProject";
	
	private OpenAIREProjectService openAIREProjectService = new DSpace().getServiceManager()
			.getServiceByName(OpenAIREProjectService.class.getName(), OpenAIREProjectService.class);
	
	public Choices getMatches(String field, String query, int collection, int start, int limit, String locale) {
		Choices choices = super.getMatches(field, query, collection, start, limit, locale);		
		return new Choices(addExtraResults(field, query, choices, start, limit <= 0?DEFAULT_MAX_ROWS:limit), choices.start, choices.total, choices.confidence, choices.more);
	}
	
	protected Choice[] addExtraResults(String field, String text, Choices choices, int start, int max) {
		List<Choice> res = new ArrayList<Choice>();
		List<OpenAireProject> pjs = openAIREProjectService.getProjects(OpenAIREProjectService.QUERY_FIELD_NAME, text, start, max);
		if(pjs!=null && !pjs.isEmpty()) {
			for(OpenAireProject pj : pjs) {
				Map<String, String> extras = new HashMap<String,String>();
				extras.put("insolr", "false");
				String value = pj.getTitle();
				String authority=AuthorityValueGenerator.GENERATE + OPENAIRE_PROJECT_AUTHORITY_TYPE + AuthorityValueGenerator.SPLIT + pj.getCode();
				if(ConfigurationManager.getBooleanProperty("openaireprojectauthority.prefix.enabled",false)) {
					value =OPENAIRE_PROJECT_PREFIX+pj.getFunder()+"/"+pj.getFundingProgram()+"/"+pj.getCode()+"/"
								+pj.getJurisdiction()+"/"+pj.getTitle();
					authority=null;
				}
				String label = pj.getTitle() +"("+pj.getCode()+")";
            	res.add(new Choice(authority,label, value,extras ) );
			}
			return (Choice[])ArrayUtils.addAll(choices.values, res.toArray(new Choice[res.size()]));
		}
		
		return choices.values;
	}

}
