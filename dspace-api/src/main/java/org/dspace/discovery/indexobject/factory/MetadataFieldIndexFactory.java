/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject.factory;

import org.dspace.content.MetadataField;
import org.dspace.discovery.indexobject.IndexableMetadataField;

/**
 * Factory interface for indexing/retrieving {@link org.dspace.content.MetadataField} items in the search core
 *
 * @author Maria Verdonck (Atmire) on 14/07/2020
 */
public interface MetadataFieldIndexFactory extends IndexFactory<IndexableMetadataField, MetadataField> {
}
