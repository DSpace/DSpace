/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import edu.emory.mathcs.backport.java.util.Arrays;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.dspace.identifier.DOI;
import org.dspace.submit.util.SubmissionLookupDTO;

import gr.ekt.bte.core.TransformationEngine;

public class SubmissionLookupService {

	public static final String SL_NAMESPACE_PREFIX = "http://www.dspace.org/sl/"; 

	public static final String MANUAL_USER_INPUT = "manual";

	public static final String PROVIDER_NAME_FIELD = "provider_name_field";
	
	private static Logger log = Logger.getLogger(SubmissionLookupService.class);

	public static final String SEPARATOR_VALUE = "#######";

	public static final String SEPARATOR_VALUE_REGEX = SEPARATOR_VALUE;

	private List<SubmissionLookupProvider> providers;

	private Map<String, List<SubmissionLookupProvider>> idents2provs;

	private List<SubmissionLookupProvider> searchProviders;

	private TransformationEngine phase1TransformationEngine;
	private TransformationEngine phase2TransformationEngine;


	public void setPhase2TransformationEngine(TransformationEngine phase2TransformationEngine) {
		this.phase2TransformationEngine = phase2TransformationEngine;
	}
	
	public void setPhase1TransformationEngine(TransformationEngine phase1TransformationEngine) {
		this.phase1TransformationEngine = phase1TransformationEngine;
		
		MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader)phase1TransformationEngine.getDataLoader();
		
		this.idents2provs = new HashMap<String, List<SubmissionLookupProvider>>();
		this.searchProviders = new ArrayList<SubmissionLookupProvider>();

		if (providers == null) {
			this.providers = new ArrayList<SubmissionLookupProvider>();
			
			for (SubmissionLookupProvider p : dataLoader.getProviders()) {
				
				this.providers.add(p);
				
				if (p.isSearchProvider()) {
					searchProviders.add(p);
				}
				List<String> suppIdentifiers = p.getSupportedIdentifiers();
				if (suppIdentifiers != null) {
					for (String ident : suppIdentifiers) {
						List<SubmissionLookupProvider> tmp = idents2provs
								.get(ident);
						if (tmp == null) {
							tmp = new ArrayList<SubmissionLookupProvider>();
							idents2provs.put(ident, tmp);
						}
						tmp.add(p);
					}
				}
			}
		}
	}

	public TransformationEngine getPhase1TransformationEngine() {
		return phase1TransformationEngine;
	}

	public TransformationEngine getPhase2TransformationEngine() {
		return phase2TransformationEngine;
	}
	
	public List<String> getIdentifiers() {
		
		List<String> allSupportedIdentifiers = new ArrayList<String>();
		MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader)phase1TransformationEngine.getDataLoader();
		for (SubmissionLookupProvider provider : dataLoader.getProviders()){
			for (String identifier : provider.getSupportedIdentifiers()){
				if (!allSupportedIdentifiers.contains(identifier)){
					allSupportedIdentifiers.add(identifier);
				}
			}
		}

		return allSupportedIdentifiers;
	}

	public Map<String, List<SubmissionLookupProvider>> getProvidersIdentifiersMap() {
		return idents2provs;
	}

	public SubmissionLookupDTO getSubmissionLookupDTO(
			HttpServletRequest request, String uuidSubmission) {
		SubmissionLookupDTO dto = (SubmissionLookupDTO) request.getSession()
				.getAttribute("submission_lookup_" + uuidSubmission);
		if (dto == null) {
			dto = new SubmissionLookupDTO();
			storeDTOs(request, uuidSubmission, dto);
		}
		return dto;
	}

	public void invalidateDTOs(HttpServletRequest request, String uuidSubmission) {
		request.getSession().removeAttribute(
				"submission_lookup_" + uuidSubmission);
	}

	public void storeDTOs(HttpServletRequest request, String uuidSubmission,
			SubmissionLookupDTO dto) {
		request.getSession().setAttribute(
				"submission_lookup_" + uuidSubmission, dto);
	}

	public List<SubmissionLookupProvider> getSearchProviders() {
		return searchProviders;
	}

	public List<SubmissionLookupProvider> getProviders() {
		return providers;
	}

    public static String getFirstValue(Record rec, String field) {
        List<Value> values = rec.getValues(field);
        String provider_name = null;
        if (values != null && values.size() > 0) {
            provider_name = values.get(0).getAsString();
        }
        return provider_name;
    }

    public static String getProviderName(Record rec) {
        return getFirstValue(rec, SubmissionLookupService.PROVIDER_NAME_FIELD);
    }

    public static String getType(Record rec) {
        return getFirstValue(rec, SubmissionLookupProvider.TYPE);
    }
    
    public static List<String> getStringValuesFromValues(Record rec, String field) {
    	List<Value> values = rec.getValues(field);
    	if (values == null)
    		return null;
    	
    	List<String> stringValues = new ArrayList<String>();
    	for (Value value : values){
    		stringValues.add(value.getAsString());
    	}
    	
    	return stringValues;
	}
    
    public static String getPrintableString(Record record){
		StringBuilder result = new StringBuilder();
		
		result.append("\nPublication {\n");
		
		for (String field: record.getFields()){
			result.append("--"+field + ":\n");
			List<Value> values = record.getValues(field);
			for (Value value : values){
				result.append("\t"+value.getAsString()+"\n");
			}
		}
		
		result.append("}\n");
		
		return result.toString();
	}
}
