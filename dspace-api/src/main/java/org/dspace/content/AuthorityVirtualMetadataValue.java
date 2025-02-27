/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dspace.core.Constants;

/**
 * Implementation of {@link VirtualMetadataValue} specifically for authority-based virtual metadata.
 * It is a more simple implementation than {@link RelationshipMetadataValue} as it doesn't have a way to calculate
 * relative place (place is set during value creation based on number of other values in the field) and does not
 * override {@link VirtualMetadataValue#getID()}
 *
 * @author Kim Shepherd
 */
public class AuthorityVirtualMetadataValue extends VirtualMetadataValue {

    /**
     * Use authority key (URI, UUID, etc), value, metadatafield ID, and place number to generate hash code
     * @return unique hash code
     */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getAuthority()
                .substring(Constants.VIRTUAL_AUTHORITY_PREFIX.length()))
                .append(getMetadataFieldId())
                .append(getValue())
                .append(getPlace()).hashCode();
    }

}
