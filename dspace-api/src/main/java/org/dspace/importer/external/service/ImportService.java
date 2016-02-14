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

/**
 * Created by Roeland Dillen (roeland at atmire dot com)
 * Date: 17/09/12
 * Time: 14:19
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

    protected Collection<Imports> matchingImports(String url) {
        if (ANY.equals(url)) {
            return importSources.values();
        } else {
			if(importSources.containsKey(url))
				return Collections.singletonList(importSources.get(url));
			else
				return Collections.emptyList();
		}
    }


    public Collection<ImportRecord> findMatchingRecords(String url, Item item) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();

			for (Imports imports : matchingImports(url)) {
				recordList.addAll(imports.findMatchingRecords(item));
			}

			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

    public Collection<ImportRecord> findMatchingRecords(String url, Query query) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();
			for (Imports imports : matchingImports(url)) {
				recordList.addAll(imports.findMatchingRecords(query));
			}

			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

    public int getNbRecords(String url, String query) throws MetadataSourceException {
		try {
			int total = 0;
			for (Imports Imports : matchingImports(url)) {
				total += Imports.getNbRecords(query);
			}
			return total;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

    public int getNbRecords(String url, Query query) throws MetadataSourceException {
		try {
			int total = 0;
			for (Imports Imports : matchingImports(url)) {
				total += Imports.getNbRecords(query);
			}
			return total;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}


    public Collection<ImportRecord> getRecords(String url, String query, int start, int count) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();
			for (Imports imports : matchingImports(url)) {
				recordList.addAll(imports.getRecords(query, start, count));
			}
			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}
    public Collection<ImportRecord> getRecords(String url, Query query) throws MetadataSourceException {
		try {
			List<ImportRecord> recordList = new LinkedList<ImportRecord>();
			for (Imports imports : matchingImports(url)) {
				recordList.addAll(imports.getRecords(query));
			}
			return recordList;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}


    public ImportRecord getRecord(String url, String id) throws MetadataSourceException {
		try {
			for (Imports imports : matchingImports(url)) {
				if (imports.getRecord(id) != null) return imports.getRecord(id);
	
			}
			return null;
		} catch (Exception e) {
			throw new MetadataSourceException(e);
		}
	}

    public ImportRecord getRecord(String url, Query query) throws MetadataSourceException {
		try {
			for (Imports imports : matchingImports(url)) {
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
