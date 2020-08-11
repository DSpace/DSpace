/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import java.sql.SQLException;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.DSpaceObject;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableDSpaceObject;

/**
 * Factory interface for indexing/retrieving DSpaceObjects in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface DSpaceObjectIndexFactory<T extends IndexableDSpaceObject, S extends DSpaceObject>
        extends IndexFactory<T, S> {


    /**
     * Return a list of the identifiers from the parents for the provided IndexableObject,
     * communities will be prepended by "m", collections b "c"
     * @param context       DSpace context object
     * @return              A list of community identifiers with "m" prepended to every one
     * @throws SQLException If database error
     */
    List<String> getLocations(Context context, T indexableDSpaceObject) throws SQLException;

    /**
     * Store the provided locations in the solr document
     * @param doc       The solr input document
     * @param locations The locations to be stored
     */
    void storeCommunityCollectionLocations(SolrInputDocument doc, List<String> locations);
}