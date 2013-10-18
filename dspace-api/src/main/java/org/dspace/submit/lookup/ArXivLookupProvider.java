/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.Record;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.dspace.core.Context;
import org.dspace.submit.importer.arxiv.ArXivItem;

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
	public List<Record> getByIdentifier(
			Context context, Map<String, Set<String>> keys) throws HttpException, IOException {
		List<Record> results = new ArrayList<Record>();
		if (keys != null) {
			Set<String> dois = keys.get(DOI);
			Set<String> arxivids = keys.get(ARXIV);
			List<ArXivItem> items = new ArrayList<ArXivItem>();
			if (dois!=null && dois.size()>0) {
				items.addAll(arXivService.getByDOIs(dois));
			}
			if (arxivids!=null && arxivids.size()>0) {
				for (String arxivid : arxivids){
					items.add(arXivService.getByArXivIDs(arxivid));
				}
			}

			for (ArXivItem item : items) {
				results.add(convert(item));
			}
		}
		return results;
	}

	@Override
	public List<Record> search(Context context, String title,
			String author, int year) throws HttpException, IOException {
		List<Record> results = new ArrayList<Record>();
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
}
