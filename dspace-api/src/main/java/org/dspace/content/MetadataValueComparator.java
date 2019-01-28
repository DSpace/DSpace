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

/**
 * Compares two {@link MetadataValue}s.
 * The comparator will sort by schema, element, qualifier, place (in that order)
 */
public class MetadataValueComparator
    implements Comparator<MetadataValue>, Serializable {
    @Override
    public int compare(MetadataValue mv1, MetadataValue mv2) {
        int compare = mv1.getMetadataField().getMetadataSchema().getID()
                         .compareTo(mv2.getMetadataField().getMetadataSchema().getID());
        if (compare != 0) {
            return compare;
        }
        compare = mv1.getMetadataField().getElement().compareTo(mv2.getMetadataField().getElement());
        //TODO: continue comparison
        return compare;
    }
}
