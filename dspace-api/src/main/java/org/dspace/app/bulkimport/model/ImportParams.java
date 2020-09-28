/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;


import org.dspace.content.Collection;
import org.springframework.util.Assert;

public final class ImportParams {

    private final boolean findBestMatches;

    private final Collection collection;

    public ImportParams(boolean findBestMatches, Collection collection) {
        Assert.notNull(collection, "The collection is mandatory");
        this.findBestMatches = findBestMatches;
        this.collection = collection;
    }

    public boolean isFindBestMatches() {
        return findBestMatches;
    }

    public Collection getCollection() {
        return collection;
    }

}
