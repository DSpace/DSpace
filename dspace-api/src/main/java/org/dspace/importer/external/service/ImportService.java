/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service;

import org.apache.log4j.Logger;
import org.dspace.content.Item;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.Query;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.service.other.Destroyable;
import org.dspace.importer.external.service.other.Imports;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/** Main entry point for the import framework.
 * Instead of calling the different importer implementations, the ImportService should be called instead.
 * This class contains the same methods as the other implementations, but has an extra parameter URL.
 * This URL should be the same identifier that is returned by the "getImportSource" method that is defined in the importer implementation you want to use.
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class ImportService implements Destroyable {
    private HashMap<String, Imports> importSources = new HashMap<String, Imports>();

    Logger log = Logger.getLogger(ImportService.class);

    public ImportService() {

    }

    protected static final String ANY = "*";

    @Autowired(required = false)
    public void setImportSources(List<Imports> importSources) throws MetadataSourceException {
        log.info("Loading " + importSources.size() + " import sources.");
        for (Imports imports : importSources) {
            this.importSources.put(imports.getImportSource(), imports);
        }

    }

    protected Map<String, Imports> getImportSources() {
        return Collections.unmodifiableMap(importSources);
    }

	/**
	 * Utility method to find what import implementations match the imports uri.
	 * @param uri the identifier of the import implementation or * for all
	 * @return matching Imports implementations
	 */
    protected Collection<Imports> matchingImports(String uri) {
        if (ANY.equals(uri)) {
            return importSources.values();
        } else {
			if(importSources.containsKey(uri))
				return Collections.singletonList(importSources.get(uri));
			else
				return Collections.emptyList();
		}
    }

	/** Finds records based on an item
	 * Delegates to one or more Imports implementations based on the uri.  Results will be aggregated.
	 * @param uri the identifier of the import implementation or * for all
	 * @param item an item to base the search on
	 * @return a collection of import records. Only the identifier of the found records may be put in the record.
	 * @throws MetadataSourceException if the underlying imports throw any exception.
	 */
    public Collection<ImportRecord> findMatchingRecords(String uri, Item item) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();

			for (Imports imports : matchingImports(uri)) {
				recordList.addAll(imports.findMatchingRecords(item));
			}

			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

	/** Finds records based on query object.
	 *  Delegates to one or more Imports implementations based on the uri.  Results will be aggregated.
	 * @param uri the identifier of the import implementation or * for all
	 * @param query a query object to base the search on. The implementation decides how the query is interpreted.
	 * @return a collection of import records. Only the identifier of the found records may be put in the record.
	 * @throws MetadataSourceException
	 */
    public Collection<ImportRecord> findMatchingRecords(String uri, Query query) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();
			for (Imports imports : matchingImports(uri)) {
				recordList.addAll(imports.findMatchingRecords(query));
			}

			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

	/** Find the number of records matching a string query;
	 *
	 * @param uri the identifier of the import implementation or * for all
	 * @param query a query to base the search on
	 * @return the sum of the matching records over all import sources
	 * @throws MetadataSourceException
	 */
    public int getNbRecords(String uri, String query) throws MetadataSourceException {
		try {
			int total = 0;
			for (Imports Imports : matchingImports(uri)) {
				total += Imports.getNbRecords(query);
			}
			return total;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}
	/** Find the number of records matching a query;
	 *
	 * @param uri the identifier of the import implementation or * for all
	 * @param query a query object to base the search on  The implementation decides how the query is interpreted.
	 * @return the sum of the matching records over all import sources
	 * @throws MetadataSourceException
	 */
	public int getNbRecords(String uri, Query query) throws MetadataSourceException {
		try {
			int total = 0;
			for (Imports Imports : matchingImports(uri)) {
				total += Imports.getNbRecords(query);
			}
			return total;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

	/**  Find the number of records matching a string query. Supports pagination
	 *
	 * @param uri the identifier of the import implementation or * for all
	 * @param query a query object to base the search on.  The implementation decides how the query is interpreted.
	 * @param start offset to start at
	 * @param count number of records to retrieve.
	 * @return a set of records. Fully transformed.
	 * @throws MetadataSourceException
	 */
    public Collection<ImportRecord> getRecords(String uri, String query, int start, int count) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();
			for (Imports imports : matchingImports(uri)) {
				recordList.addAll(imports.getRecords(query, start, count));
			}
			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

	/** Find the number of records matching a object query.
	 *
	 * @param uri the identifier of the import implementation or * for all
	 * @param query a query object to base the search on.  The implementation decides how the query is interpreted.
	 * @return a set of records. Fully transformed.
	 * @throws MetadataSourceException
	 */
    public Collection<ImportRecord> getRecords(String uri, Query query) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();
			for (Imports imports : matchingImports(uri)) {
				recordList.addAll(imports.getRecords(query));
			}
			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

	/** Get a single record from a source.
	 * The first match will be returned
	 * @param uri uri the identifier of the import implementation or * for all
	 * @param id identifier for the record
	 * @return a matching record
	 * @throws MetadataSourceException
	 */
    public ImportRecord getRecord(String uri, String id) throws MetadataSourceException {
		try {
			for (Imports imports : matchingImports(uri)) {
				if (imports.getRecord(id) != null) return imports.getRecord(id);
	
			}
			return null;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}
	/** Get a single record from the source.
	 * The first match will be returned
	 * @param uri uri the identifier of the import implementation or * for all
	 * @param query a query matching a single record
	 * @return a matching record
	 * @throws MetadataSourceException
	 */
    public ImportRecord getRecord(String uri, Query query) throws MetadataSourceException {
		try {
			for (Imports imports : matchingImports(uri)) {
				if (imports.getRecord(query) != null) return imports.getRecord(query);
	
			}
			return null;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

    public Collection<String> getImportUrls() {
        return importSources.keySet();
    }


    @Override
    public void destroy() throws Exception {
        for (Imports imports : importSources.values()) {
            if (imports instanceof Destroyable) ((Destroyable) imports).destroy();
        }
    }
}
