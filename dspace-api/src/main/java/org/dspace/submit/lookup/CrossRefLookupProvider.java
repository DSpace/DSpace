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

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.httpclient.HttpException;
import org.dspace.core.Context;
import org.dspace.submit.importer.crossref.CrossrefItem;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

public class CrossRefLookupProvider extends ConfigurableLookupProvider {
	private CrossRefService crossrefService;

	private boolean searchProvider = true;
	
	public void setSearchProvider(boolean searchProvider)
    {
        this.searchProvider = searchProvider;
    }
	
	public void setCrossrefService(CrossRefService crossrefService) {
		this.crossrefService = crossrefService;
	}

	@Override
	public List<String> getSupportedIdentifiers() {
		return Arrays.asList(new String[] { DOI });
	}

	@Override
	public List<Record> getByIdentifier(Context context, 
			Map<String, Set<String>> keys) throws HttpException, IOException {
		if (keys != null && keys.containsKey(DOI)) {
			Set<String> dois = keys.get(DOI);
			List<Record> results = new ArrayList<Record>();
			List<CrossrefItem> items = null;
			try {
				items = crossrefService.search(context, dois);
			} catch (JDOMException e) {
				throw new RuntimeException(e.getMessage(), e);
			} catch (ParserConfigurationException e) {
				throw new RuntimeException(e.getMessage(), e);
			} catch (SAXException e) {
				throw new RuntimeException(e.getMessage(), e);
			}
			if (items != null) {
				for (CrossrefItem p : items) {
					results.add(convert(p));
				}
				return results;
			}
		}
		return null;
	}

	@Override
	public List<Record> search(Context context, String title,
			String author, int year) throws HttpException, IOException {
		List<Record> results = new ArrayList<Record>();
		List<CrossrefItem> items = null;
		items = crossrefService.search(context, title, author, year, 10);
		if (items != null) {
			for (CrossrefItem p : items) {
				results.add(convert(p));
			}
			return results;
		}
		return null;
	}

	@Override
	public boolean isSearchProvider() {
		return searchProvider;
	}

	@Override
	public String getShortName() {
		return "crossref";
	}
}
