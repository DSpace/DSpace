/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.importer.external.service.other;

import org.dspace.content.Item;
import org.dspace.importer.external.MetadataSourceException;
import org.dspace.importer.external.Query;
import org.dspace.importer.external.datamodel.ImportRecord;

import java.util.Collection;

/** Common interface for all import implementations.
 * @author Roeland Dillen (roeland at atmire dot com)
 */
public interface Imports {
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

    /**
     *
     * @param q
     * @return
     * @throws MetadataSourceException
     */
    public Collection<ImportRecord> getRecords(Query q)throws MetadataSourceException;

    /**
     *
     * @param id
     * @return
     * @throws MetadataSourceException
     */
    public ImportRecord getRecord(String id)throws MetadataSourceException;

    /**
     *
     * @param q
     * @return
     * @throws MetadataSourceException
     */
    public ImportRecord getRecord(Query q)throws MetadataSourceException;

	/**
     * The string that identifies this import implementation. Preferable a URI
     * @return the identifying uri
     */
    public String getImportSource();

    public Collection<ImportRecord> findMatchingRecords(Item item) throws MetadataSourceException;

    public Collection<ImportRecord> findMatchingRecords(Query q) throws MetadataSourceException;
}
