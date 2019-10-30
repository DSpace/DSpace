/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl.pubmed.metadatamapping.utils;

import org.dspace.mock.MockMetadataField;
import org.dspace.mock.MockMetadataValue;

/**
 * This class will contain utility methods for MetadatumContributors
 */
public final class MetadatumContributorUtils {

    private MetadatumContributorUtils() {}
    /**
     * @param field MetadataFieldConfig representing what to map the value to
     * @param value The value to map to a MetadatumDTO
     * @return A metadatumDTO created from the field and value
     */
    public static MockMetadataValue toMockMetadataValue(MockMetadataField field, String value) {
        MockMetadataValue mockMetadataValue = new MockMetadataValue();

        if (field == null) {
            return null;
        }
        mockMetadataValue.setValue(value);
        mockMetadataValue.setElement(field.getElement());
        mockMetadataValue.setQualifier(field.getQualifier());
        mockMetadataValue.setSchema(field.getSchema());
        return mockMetadataValue;
    }
}
