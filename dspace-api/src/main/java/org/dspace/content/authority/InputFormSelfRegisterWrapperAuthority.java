/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;

/**
 * This authority is registered automatically by the ChoiceAuthorityService for
 * all the metadata that use a value-pair or a vocabulary in the submission-form.xml
 * 
 * It keeps a map of form-name vs ChoiceAuthority to delegate the execution of
 * the method to the specific ChoiceAuthority configured for the collection when
 * the same metadata have different vocabulary or value-pair on a collection
 * basis
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class InputFormSelfRegisterWrapperAuthority implements ChoiceAuthority {
	
	private static Logger log = Logger.getLogger(InputFormSelfRegisterWrapperAuthority.class);
	
	private Map<String, ChoiceAuthority> delegates = new HashMap<String, ChoiceAuthority>();

	private static DCInputsReader dci = null;
	
	private void init() {
        try
        {
            if (dci == null)
            {
                dci = new DCInputsReader();
            }
        }
        catch (DCInputsReaderException e)
        {
            log.error("Failed reading DCInputs initialization: ",e);
        }
	}
	@Override
	public Choices getMatches(String field, String query, Collection collection, int start, int limit, String locale) {
		String formName;
		try {
			init();
			if(collection==null) {
				Set<Choice> choices = new HashSet<Choice>(); 
				//workaround search in all authority configured
				for(ChoiceAuthority ca : delegates.values()) {
					Choices tmp = ca.getMatches(field, query, null, start, limit, locale);
					if(tmp.total>0) {
						Set<Choice> mySet = new HashSet<Choice>(Arrays.asList(tmp.values));
						choices.addAll(mySet);
					}
				}
				if(!choices.isEmpty()) {
					Choice[] results = new Choice[choices.size()-1];
					choices.toArray(results);
					return new Choices(results, 0, choices.size(), Choices.CF_AMBIGUOUS, false);
				}
			}
			else {
				formName = dci.getInputFormNameByCollectionAndField(collection, field);
				return delegates.get(formName).getMatches(field, query, collection, start, limit, locale);
			}
		} catch (DCInputsReaderException e) {
			log.error(e.getMessage(), e);
		}
		return new Choices(Choices.CF_NOTFOUND);
	}

	@Override
	public Choices getBestMatch(String field, String text, Collection collection, String locale) {
		String formName;
		try {
			init();
			if(collection==null) {
				Set<Choice> choices = new HashSet<Choice>(); 
				//workaround search in all authority configured
				for(ChoiceAuthority ca : delegates.values()) {
					Choices tmp = ca.getBestMatch(field, text, null, locale);
					if(tmp.total>0) {
						Set<Choice> mySet = new HashSet<Choice>(Arrays.asList(tmp.values));
						choices.addAll(mySet);
					}
				}
				if(!choices.isEmpty()) {
					Choice[] results = new Choice[choices.size()-1];
					choices.toArray(results);
					return new Choices(results, 0, choices.size(), Choices.CF_UNCERTAIN, false);
				}
			}
			else {
				formName = dci.getInputFormNameByCollectionAndField(collection, field);
				return delegates.get(formName).getBestMatch(field, text, collection, locale);
			}
		} catch (DCInputsReaderException e) {
			log.error(e.getMessage(), e);
		}
		return new Choices(Choices.CF_NOTFOUND);
	}

	@Override
	public String getLabel(String field, String key, String locale) {
		// TODO we need to manage REALLY the authority
		// WRONG BEHAVIOUR: now in each delegates can exists the same key with
		// different value
		for (ChoiceAuthority delegate : delegates.values()) {
			String label = delegate.getLabel(field, key, locale);
			if (StringUtils.isNotBlank(label)) {
				return label;
			}
		}
		return "UNKNOWN KEY " + key;
	}

	@Override
	public boolean isHierarchical() {
		// TODO we need to manage REALLY the authority
		// WRONG BEHAVIOUR: now in each delegates can exists the same key with
		// different value
		for (ChoiceAuthority delegate : delegates.values()) {
			return delegate.isHierarchical();
		}
		return false;
	}

	@Override
	public boolean isScrollable() {
		// TODO we need to manage REALLY the authority
		// WRONG BEHAVIOUR: now in each delegates can exists the same key with
		// different value
		for (ChoiceAuthority delegate : delegates.values()) {
			return delegate.isScrollable();
		}
		return false;
	}

	@Override
	public boolean hasIdentifier() {
		// TODO we need to manage REALLY the authority
		// WRONG BEHAVIOUR: now in each delegates can exists the same key with
		// different value
		for (ChoiceAuthority delegate : delegates.values()) {
			return delegate.hasIdentifier();
		}
		return false;
	}

	public Map<String, ChoiceAuthority> getDelegates() {
		return delegates;
	}

	public void setDelegates(Map<String, ChoiceAuthority> delegates) {
		this.delegates = delegates;
	}
}
