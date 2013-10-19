/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import gr.ekt.bte.core.DataLoadingSpec;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.RecordSet;
import gr.ekt.bte.core.StringValue;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.httpclient.HttpException;
import org.apache.commons.lang.StringUtils;
import org.dspace.core.Context;
import org.dspace.submit.util.SubmissionLookupPublication;


public abstract class ConfigurableLookupProvider implements SubmissionLookupProvider {
	
	Map<String, Set<String>> identifiers; //Searching by identifiers (DOI ...)
	Map<String, Set<String>> searchTerms; //Searching by author, title, date
	
	Map<String, String> fieldMap; //mapping between service fields and local intermediate fields
	
	String providerName;
	
	protected Record convert(Object bean) {
		try {
			return convert(providerName, bean);
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}
	
	@Override
	public List<Record> getByDOIs(Context context, Set<String> doiToSearch)
			throws HttpException, IOException {
		
		Map<String, Set<String>> keys = new HashMap<String, Set<String>>();
		keys.put(DOI, doiToSearch);
		
		return getByIdentifier(context, keys);
	}

	//BTE Data Loader interface methods
	@Override
	public RecordSet getRecords() throws MalformedSourceException {

		RecordSet recordSet = new RecordSet();

		List<Record> results = null;

		try {
			if (getIdentifiers()!=null){ //Search by identifiers
				results = getByIdentifier(null, getIdentifiers());
			}
			else {
				String title = getSearchTerms().get("title")!=null?getSearchTerms().get("title").iterator().next():null;
				String authors = getSearchTerms().get("authors")!=null?getSearchTerms().get("authors").iterator().next():null;
				String year = getSearchTerms().get("year")!=null?getSearchTerms().get("year").iterator().next():null;
				int yearInt = Integer.parseInt(year);
				results = search(null, title, authors, yearInt);
			}
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (results != null){
			for (Record record : results){
				recordSet.addRecord(record);
			}
		}

		return recordSet;
	}

	@Override
	public RecordSet getRecords(DataLoadingSpec arg0)
			throws MalformedSourceException {
		
		return getRecords();
	}
	
	public Map<String, Set<String>> getIdentifiers() {
		return identifiers;
	}

	public void setIdentifiers(Map<String, Set<String>> identifiers) {
		this.identifiers = identifiers;
	}

	public Map<String, Set<String>> getSearchTerms() {
		return searchTerms;
	}

	public void setSearchTerms(Map<String, Set<String>> searchTerms) {
		this.searchTerms = searchTerms;
	}
	
	public Map<String, String> getFieldMap() {
		return fieldMap;
	}

	public void setFieldMap(Map<String, String> fieldMap) {
		this.fieldMap = fieldMap;
	}

	public void setProviderName(String providerName) {
		this.providerName = providerName;
	}

	public Record convert(String shortName,
			Object bean) throws SecurityException, NoSuchMethodException,
			IllegalArgumentException, IllegalAccessException,
			InvocationTargetException {
		Record publication = new SubmissionLookupPublication(
				shortName);
		Field[] fields = bean.getClass().getDeclaredFields();
		for (Field field : fields) {
			     if (field.getType() == String.class) {
				       Method getter = bean.getClass().getMethod(
				           "get" + field.getName().substring(0, 1).toUpperCase()
				               + field.getName().substring(1));
				
				       String value = (String) getter.invoke(bean);
				
				       addMetadata(shortName, publication, field.getName(), value);
				
				     } else if (field.getType() == List.class) {
				       ParameterizedType pt = (ParameterizedType) field
				           .getGenericType();
				
				       Method getter = bean.getClass().getMethod(
				           "get" + field.getName().substring(0, 1).toUpperCase()
				               + field.getName().substring(1));
				
				       if (pt.getActualTypeArguments()[0] instanceof GenericArrayType) { // nomi
				                                         // di
				                                         // persone
				         List<String[]> values = (List<String[]>) getter.invoke(bean);
				         if (values != null) {
				           for (String[] nvalue : values) {
				             String value = nvalue[1] + ", " + nvalue[0];
				             addMetadata(shortName, publication,
				                 field.getName(), value);
				           }
				         }
				       } else { // metadati ripetibili
				         List<String> values = (List<String>) getter.invoke(bean);
				         if (values != null) {
				           for (String value : values) {
				             addMetadata(shortName, publication,
				                 field.getName(), value);
				           }
				         }
				       }
				     }
		}
		return publication;
	}
	
	private void addMetadata(String config, Record publication, String name, String value) {
		if (StringUtils.isBlank(value))
			return;
		
		String md = null;
		if (fieldMap!=null){
			md = this.fieldMap.get(name);
		}

		if (StringUtils.isBlank(md)) {
			return;
		} else {
			md = md.trim();
		}
		
		if (publication.isMutable()){
			publication.makeMutable().addValue(md, new StringValue(value));
		}
	}
}
