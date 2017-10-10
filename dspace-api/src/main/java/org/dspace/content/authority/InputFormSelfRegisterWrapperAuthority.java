/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.HashMap;
import java.util.Map;

import org.dspace.content.Collection;

/**
 * This authority is registered automatically by the ChoiceAuthorityService for
 * all the metadata that use a value-pair or a vocabulary in the input-form.xml
 * 
 * It keeps a map of form-name vs ChoiceAuthority to delegate the execution of
 * the method to the specific ChoiceAuthority configured for the collection when
 * the same metadata have different vocabulary or value-pair on a collection
 * basis
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class InputFormSelfRegisterWrapperAuthority implements ChoiceAuthority
{
	private Map<String, ChoiceAuthority> delegates = new HashMap<String, ChoiceAuthority>();
	
    @Override
    public Choices getMatches(String field, String query, Collection collection, int start, int limit, String locale)
    {
    	String formName = null; //Utils.getInputFormName(collection);
    	return delegates.get(formName).getMatches(field, query, collection, start, limit, locale);
    }

    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale)
    {
    	String formName = null; //Utils.getInputFormName(collection);
    	return delegates.get(formName).getBestMatch(field, text, collection, locale);
    }

    @Override
    public String getLabel(String field, String key, String locale)
    {
    	String formName = null; //Utils.getInputFormName(collection);
    	return delegates.get(formName).getLabel(field, key, locale);
    }

	public Map<String, ChoiceAuthority> getDelegates() {
		return delegates;
	}

	public void setDelegates(Map<String, ChoiceAuthority> delegates) {
		this.delegates = delegates;
	}
}
