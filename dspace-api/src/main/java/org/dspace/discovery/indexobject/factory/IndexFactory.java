/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.core.Context;
import org.dspace.discovery.IndexableObject;

/**
 * Basis factory interface for indexing/retrieving any IndexableObject in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface IndexFactory<T extends IndexableObject> {

    /**
     * Retrieve all instances of a certain indexable object type
     * @param context       DSpace context object
     * @return              An iterator containing all the objects to be indexed for the indexable object
     * @throws SQLException If database error
     */
    Iterator<T> findAll(Context context) throws SQLException;

    /**
     * Return the type of the indexable object
     * @return a string containing the type
     */
    String getType();

    /**
     * Create solr document with all the shared fields initialized.
     * @param indexableObject   the indexableObject that we want to index
     * @return                  initialized solr document
     */
    SolrInputDocument buildDocument(Context context, T indexableObject) throws SQLException, IOException;

    /**
     * Write the provided document to the solr core
     * @param context               DSpace context object
     * @param indexableObject       The indexable object that we want to store in the search core
     * @param solrInputDocument     Solr input document which will be written to our discovery search core
     * @throws SQLException         If database error
     * @throws IOException          If IO error
     * @throws SolrServerException  If the solr document could not be written to the search core
     */
    void writeDocument(Context context, T indexableObject, SolrInputDocument solrInputDocument)
            throws SQLException, IOException, SolrServerException;

    /**
     * Remove the provided indexable object from the solr core
     * @param indexableObject       The indexable object that we want to remove from the search core
     * @throws IOException          If IO error
     * @throws SolrServerException  If the solr document could not be removed to the search core
     */
    void delete(T indexableObject) throws IOException, SolrServerException;

    /**
     * Remove the provided indexable object from the solr core
     * @param indexableObjectIdentifier The identifier that we want to remove from the search core
     * @throws IOException              If IO error
     * @throws SolrServerException      If the solr document could not be removed to the search core
     */
    void delete(String indexableObjectIdentifier) throws IOException, SolrServerException;

    /**
     * Remove all indexable objects of the implementing type from the search core
     * @throws IOException          If IO error
     * @throws SolrServerException  If the solr document could not be removed to the search core
     */
    void deleteAll() throws IOException, SolrServerException;

    /**
     * Retrieve a single indexable object using the provided identifier
     * @param context       DSpace context object
     * @param id            The identifier for which we want to retrieve our indexable object
     * @return              An indexable object
     * @throws SQLException If database error
     */
    Optional<T> findIndexableObject(Context context, String id) throws SQLException;
}