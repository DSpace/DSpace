/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

/**
 * A simple abstraction to allow {@link RelationshipMetadataValue} and {@link AuthorityVirtualMetadataValue}
 * extend it while allowing reflection or instance checking for MetadataValue and VirtualMetadataValue
 *
 * @author Kim Shepherd
 */
public class VirtualMetadataValue extends MetadataValue {

    /**
     * This is a bit of a hack, much like {@link RelationshipMetadataValue} - the ID is not something that corresponds
     * to a real metadata value, but instead the source object. Unfortunately metadata value and relationships both
     * use integers and not Strings like authority values.
     * Returning -1 guarantees that any erroneous calls to an AuthorityVirtualMetadataValue object will not send
     * the caller off to a real, unrelated MetadataValue, so this default behaviour is implemented
     *
     * @return integerised authority key
     */
    @Override
    public Integer getID() {
        return -1;
    }

}
