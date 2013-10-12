/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.dspace.submit.lookup.SubmissionLookupProvider;

public class SubmissionLookupPublication implements Serializable {
	private String providerName;

	private Map<String, List<String>> storage = new HashMap<String, List<String>>();

	public SubmissionLookupPublication(String providerName) {
		this.providerName = providerName;
	}

	// necessario per poter effettuare la serializzazione in JSON dei dati
	public Map<String, List<String>> getStorage() {
		return storage;
	}
	
	public Set<String> getFields() {
		return storage.keySet();
	}

	public List<String> remove(String md) {
		return storage.remove(md);
	}

	public void add(String md, String nValue) {
		if (StringUtils.isNotBlank(nValue))
		{
			List<String> tmp = storage.get(md);
			if (tmp == null) {
				tmp = new ArrayList<String>();
				storage.put(md, tmp);
			}
			tmp.add(nValue);
		}
	}

	public String getFirstValue(String md) {
		List<String> tmp = storage.get(md);
		if (tmp == null || tmp.size() == 0) {
			return null;
		}
		return tmp.get(0);
	}

	public List<String> getValues(String md) {
		return storage.get(md);
	}

	public String getProviderName() {
		return providerName;
	}

	public String getType() {
		return getFirstValue(SubmissionLookupProvider.TYPE);
	}
}
