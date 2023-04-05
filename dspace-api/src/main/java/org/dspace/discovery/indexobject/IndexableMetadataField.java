/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.indexobject;

import org.dspace.content.MetadataField;
import org.dspace.discovery.IndexableObject;

/**
 * {@link MetadataField} implementation for the {@link IndexableObject}
 *
 * @author Maria Verdonck (Atmire) on 14/07/2020
 */
public class IndexableMetadataField extends AbstractIndexableObject<MetadataField, Integer> {

    private MetadataField metadataField;
    public static final String TYPE = MetadataField.class.getSimpleName();

    public IndexableMetadataField(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public Integer getID() {
        return this.metadataField.getID();
    }

    @Override
    public MetadataField getIndexedObject() {
        return this.metadataField;
    }

    @Override
    public void setIndexedObject(MetadataField metadataField) {
        this.metadataField = metadataField;
    }

    @Override
    public String getTypeText() {
        return TYPE.toUpperCase();
    }
}
