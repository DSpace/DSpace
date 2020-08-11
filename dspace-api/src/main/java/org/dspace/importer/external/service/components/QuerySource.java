/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.components;

import java.util.Collection;

import org.dspace.content.Item;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.exception.MetadataSourceException;


/**
 * Common interface for database-based imports.
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Pasquale Cavallo (pasquale.cavallo@4science.it)
 */

public interface QuerySource extends MetadataSource {

    /**
     * Get a single record from the source.
     * The first match will be returned
     *
     * @param id identifier for the record
     * @return a matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public ImportRecord getRecord(String id) throws MetadataSourceException;

    /**
     * Gets the number of records matching a query
     *
     * @param query the query in string format
     * @return the number of records matching the query
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public int getRecordsCount(String query) throws MetadataSourceException;

    /**
     * Gets the number of records matching a query
     *
     * @param query the query object
     * @return the number of records matching the query
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public int getRecordsCount(Query query) throws MetadataSourceException;

    /**
     * Gets a set of records matching a query. Supports pagination
     *
     * @param query the query. The query will generally be posted 'as is' to the source
     * @param start offset
     * @param count page size
     * @return a collection of fully transformed id's
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> getRecords(String query, int start, int count) throws MetadataSourceException;

    /**
     * Find records based on a object query.
     *
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> getRecords(Query query) throws MetadataSourceException;

    /**
     * Get a single record from the source.
     * The first match will be returned
     *
     * @param query a query matching a single record
     * @return a matching record
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public ImportRecord getRecord(Query query) throws MetadataSourceException;

    /**
     * Finds records based on query object.
     * Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     *
     * @param query a query object to base the search on.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException passed through.
     */
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException;

    /**
     * Finds records based on an item
     * Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     *
     * @param item an item to base the search on
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying methods throw any exception.
     */
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException;

}
