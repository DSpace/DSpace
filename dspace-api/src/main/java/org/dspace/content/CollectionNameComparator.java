/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.io.Serializable;
import java.util.Comparator;

import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.CollectionService;

/**
 * Compares the names of two {@link Collection}s.
 */
public class CollectionNameComparator implements Comparator<Collection>, Serializable {
    public static final CollectionService collectionService
        = ContentServiceFactory.getInstance().getCollectionService();

    @Override
    public int compare(Collection collection1, Collection collection2) {
        return collectionService.getName(collection1).compareTo(collectionService.getName(collection2));
    }
}
