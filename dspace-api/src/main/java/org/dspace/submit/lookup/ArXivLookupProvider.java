/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.dspace.submit.importer.arxiv.ArXivItem;
import org.dspace.submit.util.SubmissionLookupPublication;

public class ArXivLookupProvider extends ConfigurableLookupProvider {
	private ArXivService arXivService;
	private boolean searchProvider = true;
	
	public void setArXivService(ArXivService arXivService) {
		this.arXivService = arXivService;
	}

	@Override
	public List<String> getSupportedIdentifiers() {
		return Arrays.asList(new String[] { ARXIV, DOI });
	}

	public void setSearchProvider(boolean searchProvider)
    {
        this.searchProvider = searchProvider;
    }
	
	@Override
	public boolean isSearchProvider() {
		return searchProvider;
	}

	@Override
	public List<SubmissionLookupPublication> getByIdentifier(
			Context context, Map<String, String> keys) throws HttpException, IOException {
		List<SubmissionLookupPublication> results = new ArrayList<SubmissionLookupPublication>();
		if (keys != null) {
			String doi = keys.get(DOI);
			String arxivid = keys.get(ARXIV);
			List<ArXivItem> items = new ArrayList<ArXivItem>();
			if (StringUtils.isNotBlank(doi)) {
				Set<String> dois = new HashSet<String>();
				dois.add(doi);
				items.addAll(arXivService.getByDOIs(dois));
			}
			if (StringUtils.isNotBlank(arxivid)) {
				items.add(arXivService.getByArXivIDs(arxivid));
			}

			for (ArXivItem item : items) {
				results.add(convert(item));
			}
		}
		return results;
	}

	@Override
	public List<SubmissionLookupPublication> search(Context context, String title,
			String author, int year) throws HttpException, IOException {
		List<SubmissionLookupPublication> results = new ArrayList<SubmissionLookupPublication>();
		List<ArXivItem> items = arXivService.searchByTerm(title, author, year);
		for (ArXivItem item : items) {
			results.add(convert(item));
		}
		return results;
	}

	@Override
	public String getShortName() {
		return "arxiv";
	}

	@Override
	public List<SubmissionLookupPublication> getByDOIs(Context context, Set<String> doiToSearch)
			throws HttpException, IOException {
		List<SubmissionLookupPublication> results = new ArrayList<SubmissionLookupPublication>();
		if (doiToSearch != null && doiToSearch.size() > 0) {
			List<ArXivItem> items = arXivService.getByDOIs(doiToSearch);

			for (ArXivItem item : items) {
				results.add(convert(item));
			}
		}
		return results;
	}
}
