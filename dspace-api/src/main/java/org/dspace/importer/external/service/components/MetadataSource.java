/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.components;

import org.dspace.content.Item;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.datamodel.Query;
import org.dspace.importer.external.datamodel.ImportRecord;

import java.util.Collection;

/** Common interface for all import implementations.
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public interface MetadataSource {
    /**
     * Gets the number of records matching a query
     * @param query the query in string format
     * @return the number of records matching the query
     * @throws MetadataSourceException
     */
    public int getNbRecords(String query) throws MetadataSourceException;
    /**
     * Gets the number of records matching a query
     * @param query the query object
     * @return the number of records matching the query
     * @throws MetadataSourceException
     */
    public int getNbRecords(Query query) throws MetadataSourceException;

    /**
     * Gets a set of records matching a query. Supports pagination
     * @param query the query. The query will generally be posted 'as is' to the source
     * @param start offset
     * @param count page size
     * @return a collection of fully transformed id's
     * @throws MetadataSourceException
     */
    public Collection<ImportRecord> getRecords(String query, int start, int count)throws MetadataSourceException;

    /** Find records based on a object query.
     *
     * @param query a query object to base the search on.
     * @return a set of records. Fully transformed.
     * @throws MetadataSourceException
     */
    public Collection<ImportRecord> getRecords(Query query)throws MetadataSourceException;

    /** Get a single record from the source.
     * The first match will be returned
     * @param id identifier for the record
     * @return a matching record
     * @throws MetadataSourceException
     */
    public ImportRecord getRecord(String id)throws MetadataSourceException;

    /** Get a single record from the source.
     * The first match will be returned
     * @param query a query matching a single record
     * @return a matching record
     * @throws MetadataSourceException
     */
    public ImportRecord getRecord(Query query)throws MetadataSourceException;

	/**
     * The string that identifies this import implementation. Preferable a URI
     * @return the identifying uri
     */
    public String getImportSource();

    /** Finds records based on an item
     * Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     * @param item an item to base the search on
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException if the underlying imports throw any exception.
     */
    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException;

    /** Finds records based on query object.
     *  Delegates to one or more MetadataSource implementations based on the uri.  Results will be aggregated.
     * @param query a query object to base the search on.
     * @return a collection of import records. Only the identifier of the found records may be put in the record.
     * @throws MetadataSourceException
     */
    public Collection<ImportRecord> findMatchingRecords(Query query) throws MetadataSourceException;
}
