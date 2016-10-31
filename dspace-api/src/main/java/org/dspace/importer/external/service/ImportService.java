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
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.service.components.Destroyable;
import org.dspace.importer.external.service.components.MetadataSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/** Main entry point for the import framework.
 * Instead of calling the different importer implementations, the ImportService should be called instead.
 * This class contains the same methods as the other implementations, but has an extra parameter URL.
 * This URL should be the same identifier that is returned by the "getImportSource" method that is defined in the importer implementation you want to use.
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public class ImportService implements Destroyable {
    private HashMap<String, MetadataSource> importSources = new HashMap<>();

    Logger log = Logger.getLogger(ImportService.class);

    /**
     * Constructs an empty ImportService class object
     */
    public ImportService() {

    }

    protected static final String ANY = "*";

    /**
     * Sets the importsources that will be used to delegate the retrieving and matching of records to
     * @param importSources A list of {@link MetadataSource} to set to this service
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    @Autowired(required = false)
    public void setImportSources(List<MetadataSource> importSources) throws MetadataSourceException {
        log.info("Loading " + importSources.size() + " import sources.");
        for (MetadataSource metadataSource : importSources) {
            this.importSources.put(metadataSource.getImportSource(), metadataSource);
        }

    }

    /**
     * Retrieve the importSources set to this class.
     * @return  An unmodifiableMap of importSources
     */
    protected Map<String, MetadataSource> getImportSources() {
        return Collections.unmodifiableMap(importSources);
    }

    /**
     * Utility method to find what import implementations match the imports uri.
     * @param uri the identifier of the import implementation or * for all
     * @return matching MetadataSource implementations
     */
    protected Collection<MetadataSource> matchingImports(String uri) {
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
     * Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     * @param uri the identifier of the import implementation or * for all
     * @param item an item to base the search on
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> findMatchingRecords(String uri, Item item) throws MetadataSourceException {
        try {
            List<ImportRecord> recordList = new LinkedList<ImportRecord>();

            for (MetadataSource metadataSource : matchingImports(uri)) {
                recordList.addAll(metadataSource.findMatchingRecords(item));
            }

            return recordList;
        } catch (Exception e) {
            throw new MetadataSourceException(e);
        }
    }

    /** Finds records based on query object.
     *  Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     * @param uri the identifier of the import implementation or * for all
     * @param query a query object to base the search on. The implementation decides how the query is interpreted.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> findMatchingRecords(String uri, Query query) throws MetadataSourceException {
        try {
            List<ImportRecord> recordList = new LinkedList<ImportRecord>();
            for (MetadataSource metadataSource : matchingImports(uri)) {
                recordList.addAll(metadataSource.findMatchingRecords(query));
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
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public int getNbRecords(String uri, String query) throws MetadataSourceException {
        try {
            int total = 0;
            for (MetadataSource MetadataSource : matchingImports(uri)) {
                total += MetadataSource.getNbRecords(query);
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
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public int getNbRecords(String uri, Query query) throws MetadataSourceException {
        try {
            int total = 0;
            for (MetadataSource MetadataSource : matchingImports(uri)) {
                total += MetadataSource.getNbRecords(query);
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
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> getRecords(String uri, String query, int start, int count) throws MetadataSourceException {
        try {
            List<ImportRecord> recordList = new LinkedList<>();
            for (MetadataSource metadataSource : matchingImports(uri)) {
                recordList.addAll(metadataSource.getRecords(query, start, count));
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
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> getRecords(String uri, Query query) throws MetadataSourceException {
        try {
            List<ImportRecord> recordList = new LinkedList<>();
            for (MetadataSource metadataSource : matchingImports(uri)) {
                recordList.addAll(metadataSource.getRecords(query));
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
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public ImportRecord getRecord(String uri, String id) throws MetadataSourceException {
        try {
            for (MetadataSource metadataSource : matchingImports(uri)) {
                if (metadataSource.getRecord(id) != null) return metadataSource.getRecord(id);
    
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
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public ImportRecord getRecord(String uri, Query query) throws MetadataSourceException {
        try {
            for (MetadataSource metadataSource : matchingImports(uri)) {
                if (metadataSource.getRecord(query) != null) return metadataSource.getRecord(query);
    
            }
            return null;
        } catch (Exception e) {
            throw new MetadataSourceException(e);
        }
    }

    /** Retrieve the importUrls that are set on the importSources .
     *  @return a Collection of string, representing the configured importUrls
     */
    public Collection<String> getImportUrls() {
        return importSources.keySet();
    }

    /** Call destroy on all {@link Destroyable} {@link MetadataSource} objects set in this ImportService
     */
    @Override
    public void destroy() throws Exception {
        for (MetadataSource metadataSource : importSources.values()) {
            if (metadataSource instanceof Destroyable) ((Destroyable) metadataSource).destroy();
        }
    }
}
