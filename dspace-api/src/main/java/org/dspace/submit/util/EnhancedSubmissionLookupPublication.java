/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.dspace.submit.lookup.EnhancerSubmissionLookup;

public class EnhancedSubmissionLookupPublication extends SubmissionLookupPublication {
	private SubmissionLookupPublication lookupPublication;
	private Map<String, EnhancerSubmissionLookup> enhancedMetadata;
	
	private Map<String, List<String>> cacheEnhanched = new HashMap<String, List<String>>();

	public EnhancedSubmissionLookupPublication(Map<String, EnhancerSubmissionLookup> enhancedMetadata, SubmissionLookupPublication pub) {
		super(pub.getProviderName());
		this.lookupPublication = pub;
		this.enhancedMetadata = enhancedMetadata;
	}	

	@Override
	public Set<String> getFields() {
		Set<String> eFields = new HashSet<String>();
		if (lookupPublication.getFields() != null)
		{
			for (String s : lookupPublication.getFields())
			{
				eFields.add(s);
			}
		}
		if (enhancedMetadata != null)
		{
			for (String s : enhancedMetadata.keySet())
			{
				if (getValues(s) != null)
				{
					eFields.add(s);
				}
			}
		}
		
		return eFields;
	}
	
	@Override
	public String getFirstValue(String md) {
		List<String> values = getValues(md);
		if (values != null && values.size() > 0)
		{
			return values.get(0);
		}
		return null;
	}
	
	@Override
	public List<String> getValues(String md) {
		if (enhancedMetadata != null && enhancedMetadata.keySet().contains(md))
		{
			if (cacheEnhanched != null && cacheEnhanched.keySet().contains(md))
			{
				return cacheEnhanched.get(md);
			}
			else
			{
				EnhancerSubmissionLookup enhancer = enhancedMetadata.get(md);
				List<String> values = enhancer.getValues(this);
				cacheEnhanched.put(md, values);
				return values;
			}
		}
		else
		{
			return lookupPublication.getValues(md);
		}
	}
}
