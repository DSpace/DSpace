/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.dspace.submit.lookup.SubmissionLookupProvider;

public class ItemSubmissionLookupDTO implements Serializable {
	private static final long serialVersionUID = 1;

	private static final String MERGED_PUBLICATION_PROVIDER = "merged";
	
	private List<SubmissionLookupPublication> publications;
	private String uuid;

	public ItemSubmissionLookupDTO(List<SubmissionLookupPublication> publications) {
		this.uuid = UUID.randomUUID().toString();
		this.publications = publications;
	}

	public List<SubmissionLookupPublication> getPublications() {
		return publications;
	}
	
	public Set<String> getProviders() {
		Set<String> orderedProviders = new LinkedHashSet<String>();
		for (SubmissionLookupPublication p : publications)
		{
			orderedProviders.add(p.getProviderName());
		}
		return orderedProviders;
	}

	public String getUUID() {
		return uuid;
	}

	public SubmissionLookupPublication getTotalPublication(
			List<SubmissionLookupProvider> providers) {
		if (publications == null)
		{
			return null;
		}
		else if (publications.size() == 1)
		{
			return publications.get(0);
		}
		else
		{
			SubmissionLookupPublication pub = new SubmissionLookupPublication(
					MERGED_PUBLICATION_PROVIDER);
			for (SubmissionLookupProvider prov : providers)
			{				
				for (SubmissionLookupPublication p : publications)
				{
					if (!p.getProviderName().equals(prov.getShortName()))
					{
						continue;
					}
					for (String field : p.getFields())
					{
						List<String> values = p.getValues(field);
						if (values != null && values.size() > 0)
						{
							if (!pub.getFields().contains(field))
							{
								for (String v : values)
								{
									pub.add(field, v);
								}
							}
						}
					}
				}
			}
			return pub;
		}
	}
}
