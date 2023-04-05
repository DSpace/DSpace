/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.service.components.dto;

/**
 * Simple object to construct <key,value> items
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */
public class PlainMetadataKeyValueItem {

    private String key;
    private String value;

    /*
     * In a key-value items, like PlainMetadata, this method get the item's key
     */
    public String getKey() {
        return key;
    }

    /*
     * In a key-value items, like PlainMetadata, this method set the item's key.
     * Never set or leave this field to null
     * 
     */
    public void setKey(String key) {
        this.key = key;
    }

    /*
     * In key-value items, like PlainMetadata, this method get the item's value
     */
    public String getValue() {
        return value;
    }

    /*
     * In key-value items, like PlainMetadata, this method set the item's value
     */
    public void setValue(String value) {
        this.value = value;
    }

}
