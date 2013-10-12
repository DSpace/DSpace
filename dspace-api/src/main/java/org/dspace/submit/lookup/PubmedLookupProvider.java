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
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.dspace.core.Context;
import org.dspace.core.LogManager;
import org.dspace.submit.importer.pubmed.PubmedItem;
import org.dspace.submit.util.SubmissionLookupPublication;

public class PubmedLookupProvider extends ConfigurableLookupProvider {
    private boolean searchProvider = true;
    private static Logger log = Logger.getLogger(PubmedLookupProvider.class);
    
    private PubmedService pubmedService;
	
	public void setPubmedService(PubmedService pubmedService) {
		this.pubmedService = pubmedService;
	}

	@Override
	public List<String> getSupportedIdentifiers() {
		return Arrays.asList(new String[] { PUBMED, DOI });
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
	public String getShortName() {
		return "pubmed";
	}

	@Override
	public List<SubmissionLookupPublication> getByIdentifier(Context context, 
			Map<String, String> keys) throws HttpException, IOException {
		String pmid = keys != null ? keys.get(PUBMED) : null;
		String doi = keys != null ? keys.get(DOI) : null;
		SubmissionLookupPublication publication = null;
		List<SubmissionLookupPublication> results = new ArrayList<SubmissionLookupPublication>();
		if (pmid != null && doi == null) {
			try
            {
                publication = convert(pubmedService.getByPubmedID(pmid));
            }
            catch (Exception e)
            {
                log.error(LogManager.getHeader(context, "getByIdentifier", "pmid="+pmid), e);
            }
			if (publication != null)
				results.add(publication);
		}
		else
		{
			List<PubmedItem> pubmedResults = pubmedService.search(doi, pmid);
			if (pubmedResults != null) {
				for (PubmedItem p : pubmedResults) {
					results.add(convert(p));
				}
			}
		}
		
		return results;
	}

	@Override
	public List<SubmissionLookupPublication> search(Context context, String title,
			String author, int year) throws HttpException, IOException {
		List<PubmedItem> pubmedResults = pubmedService.search(title, author,
				year);
		List<SubmissionLookupPublication> results = new ArrayList<SubmissionLookupPublication>();
		if (pubmedResults != null) {
			for (PubmedItem p : pubmedResults) {
				results.add(convert(p));
			}
		}
		return results;
	}

	@Override
	public List<SubmissionLookupPublication> getByDOIs(Context context, 
			Set<String> doiToSearch)
			throws HttpException, IOException {
		StringBuffer query = new StringBuffer();
		for (String d : doiToSearch) {
			if (query.length() > 0) {
				query.append(" OR ");
			}
			query.append(d).append("[AI]");
		}

		List<PubmedItem> pubmedResults = pubmedService.search(query.toString());
		List<SubmissionLookupPublication> results = new ArrayList<SubmissionLookupPublication>();
		if (pubmedResults != null) {
			for (PubmedItem p : pubmedResults) {
				results.add(convert(p));
			}
		}
		return results;
	}
}
