/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.exceptions.MalformedSourceException;

/**
 * @author Kostas Stamatis
 *
 */
public class MultipleSubmissionLookupDataLoader implements DataLoader {

	private static final String NOT_FOUND_DOI = "NOT-FOUND-DOI";

	List<ConfigurableLookupProvider> providers;

	Map<String, Set<String>> identifiers; //Searching by identifiers (DOI ...)
	Map<String, Set<String>> searchTerms; //Searching by author, title, date

	/**
	 * Default constructor
	 */
	public MultipleSubmissionLookupDataLoader() {
	}

	/* (non-Javadoc)
	 * @see gr.ekt.bte.core.DataLoader#getRecords()
	 */
	@Override
	public RecordSet getRecords() throws MalformedSourceException {

		RecordSet recordSet = new RecordSet();

		//KSTA:ToDo: Support timeout (problematic) providers
		//List<String> timeoutProviders = new ArrayList<String>();
		for (ConfigurableLookupProvider provider : providers) {
			RecordSet subRecordSet = provider.getRecords();
			recordSet.addAll(subRecordSet);
			//Add in each record the provider name... a new provider doesn't need to know about it!
			for (Record record: subRecordSet.getRecords()){
				if (record.isMutable()){
					record.makeMutable().addValue(SubmissionLookupService.PROVIDER_NAME_FIELD, new StringValue(provider.getShortName()));
				}
			}
		}

		//for each publication in the record set, if it has a DOI, try to find extra pubs from the other providers
		if (searchTerms!=null || (identifiers!=null && !identifiers.containsKey(SubmissionLookupProvider.DOI))){ //Extend
			Map<String, Set<String>> provider2foundDOIs = new HashMap<String, Set<String>>();
			List<String> foundDOIs = new ArrayList<String>();

			for (Record publication : recordSet.getRecords()) {
				String providerName = SubmissionLookupUtils.getFirstValue(publication, SubmissionLookupService.PROVIDER_NAME_FIELD);

				String doi = null;

				if (publication.getValues(SubmissionLookupProvider.DOI) != null)
					doi = publication.getValues(SubmissionLookupProvider.DOI).iterator().next().getAsString();
				if (doi == null) {
					doi = NOT_FOUND_DOI;
				} else {
					doi = SubmissionLookupUtils.normalizeDOI(doi);
					if (!foundDOIs.contains(doi))
					{
						foundDOIs.add(doi);
					}
					Set<String> tmp = provider2foundDOIs.get(providerName);
					if (tmp == null) {
						tmp = new HashSet<String>();
						provider2foundDOIs.put(providerName, tmp);
					}
					tmp.add(doi);
				}
			}

			//KSTA:ToDo: providers must only be the ones that support DOI search!
			for (SubmissionLookupProvider provider : providers) {
				//if (evictProviders != null
						//		&& evictProviders.contains(provider.getShortName())) {
				//	continue;
				//}
				Set<String> doiToSearch = new HashSet<String>();
				Set<String> alreadyFoundDOIs = provider2foundDOIs.get(provider.getShortName());
				for (String doi : foundDOIs) {
					if (alreadyFoundDOIs == null
							|| !alreadyFoundDOIs.contains(doi)) {
						doiToSearch.add(doi);
					}
				}
				List<Record> pPublications = null;
				try {
					if (doiToSearch.size() > 0) {
						pPublications = provider
								.getByDOIs(null, doiToSearch);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (pPublications != null) {
					for (Record rec : pPublications){
						recordSet.addRecord(rec);
					}
				}
			}
		}

		return recordSet;
	}

	/* (non-Javadoc)
	 * @see gr.ekt.bte.core.DataLoader#getRecords(gr.ekt.bte.core.DataLoadingSpec)
	 */
	@Override
	public RecordSet getRecords(DataLoadingSpec loadingSpec)
			throws MalformedSourceException {

		if (loadingSpec.getOffset()>0) //Identify the end of loading
			return new RecordSet();
		
		return getRecords();
	}

	public void setProviders(List<ConfigurableLookupProvider> providers) {
		this.providers = providers;
	}
	
	public List<ConfigurableLookupProvider> getProviders() {
		return providers;
	}

	public void setIdentifiers(Map<String, Set<String>> identifiers) {
		this.identifiers = identifiers;

		if (providers!=null){
			for (ConfigurableLookupProvider provider : providers){
				provider.setIdentifiers(identifiers);	
			}
		}
	}

	public void setSearchTerms(Map<String, Set<String>> searchTerms) {
		this.searchTerms = searchTerms;

		if (providers!=null){
			for (ConfigurableLookupProvider provider : providers){
				provider.setSearchTerms(searchTerms);	
			}
		}
	}
}
