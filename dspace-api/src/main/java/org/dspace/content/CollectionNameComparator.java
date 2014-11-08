/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.util.Comparator;

public class CollectionNameComparator implements Comparator<Collection> {
    @Override
    public int compare(Collection collection1, Collection collection2) {
        return collection1.getName().compareTo(collection2.getName());
    }
}
