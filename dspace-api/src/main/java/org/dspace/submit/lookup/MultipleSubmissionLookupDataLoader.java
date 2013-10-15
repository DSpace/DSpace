/**
 * 
 */
package org.dspace.submit.lookup;

import java.util.List;
import java.util.Map;
import java.util.Set;


import gr.ekt.bte.core.DataLoader;
import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.exceptions.MalformedSourceException;

/**
 * @author Kostas Stamatis
 *
 */
public class MultipleSubmissionLookupDataLoader implements DataLoader {

	List<ConfigurableLookupProvider> providers;

	Map<String, Set<String>> identifiers; //Searching by identifiers (DOI ...)
	Map<String, Set<String>> searchTerms; //Searching by author, title, date

	/**
	 * 
	 */
	public MultipleSubmissionLookupDataLoader() {
	}

	/* (non-Javadoc)
	 * @see gr.ekt.bte.core.DataLoader#getRecords()
	 */
	@Override
	public RecordSet getRecords() throws MalformedSourceException {

		RecordSet recordSet = new RecordSet();

		//List<String> timeoutProviders = new ArrayList<String>();
		for (DataLoader dataLoader : providers) {
			RecordSet subRecordSet = dataLoader.getRecords();
			recordSet.addAll(subRecordSet);
		}

		//for each publication in the record set, if it has a DOI, try to find extra pubs from the other providers
		if (searchTerms!=null || !identifiers.containsKey(SubmissionLookupProvider.DOI)){ //Extend
			for (Record record : recordSet.getRecords()){

			}
		}

		return recordSet;
	}

	/* (non-Javadoc)
	 * @see gr.ekt.bte.core.DataLoader#getRecords(gr.ekt.bte.core.DataLoadingSpec)
	 */
	@Override
	public RecordSet getRecords(DataLoadingSpec arg0)
			throws MalformedSourceException {

		return getRecords();
	}

	public void setProviders(List<ConfigurableLookupProvider> providers) {
		this.providers = providers;
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
