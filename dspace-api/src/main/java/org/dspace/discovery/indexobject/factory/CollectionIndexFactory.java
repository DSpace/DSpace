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

import org.dspace.content.Collection;
import org.dspace.core.Context;
import org.dspace.discovery.indexobject.IndexableCollection;

/**
 * Factory interface for indexing/retrieving collections in the search core
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 */
public interface CollectionIndexFactory extends DSpaceObjectIndexFactory<IndexableCollection, Collection> {

    /**
     * Return a list of the identifiers of the owning communities from the provided collection prepended by "m"
     * @param context       DSpace context object
     * @param collection    DSpace collection
     * @return              A list of community identifiers with "m" prepended to every one
     * @throws SQLException If database error
     */
    public List<String> getCollectionLocations(Context context, Collection collection) throws SQLException;
}