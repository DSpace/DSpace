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
import org.dspace.core.Utils;

/**
 * This authority is registered automatically by the ChoiceAuthorityService for
 * all the metadata that use a value-pair or a vocabulary in the input-form.xml
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 */
public class InputFormSelfRegisterWrapperAuthority implements ChoiceAuthority
{
	private ChoiceAuthority delegate;
	
    @Override
    public Choices getMatches(String field, String query, Collection collection, int start, int limit, String locale)
    {
    	return delegate.getMatches(field, query, collection, start, limit, locale);
    }

    @Override
    public Choices getBestMatch(String field, String text, Collection collection, String locale)
    {
    	return delegate.getBestMatch(field, text, collection, locale);
    }

    @Override
    public String getLabel(String field, String key, String locale)
    {
    	return delegate.getLabel(field, key, locale);
    }

	public ChoiceAuthority getDelegate() {
		return delegate;
	}

	public void setDelegate(ChoiceAuthority delegate) {
		this.delegate = delegate;
	}

}
