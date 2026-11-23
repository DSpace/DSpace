/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;
import java.util.Map;

import org.dspace.content.vo.MetadataValueVO;
import org.dspace.core.Context;

/**
 * The purpose of this class is to manage MetadataValueVO transformations.
 * 
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.it)
 */
public class BulkImportTransformerService {

    private Map<String, BulkImportValueTransformer> field2ValueTransformer;

    public BulkImportTransformerService (Map<String, BulkImportValueTransformer> field2ValueTransformer) {
        this.field2ValueTransformer = field2ValueTransformer;
    }

    public MetadataValueVO converter(Context context, String field, MetadataValueVO metadataValue) {
        if (field2ValueTransformer.containsKey(field)) {
            BulkImportValueTransformer transformer = field2ValueTransformer.get(field);
            return transformer.transform(context, metadataValue);
        }
        return metadataValue;
    }

    public Map<String, BulkImportValueTransformer> getField2ValueTransformer() {
        return field2ValueTransformer;
    }

    public void setField2ValueTransformer(Map<String, BulkImportValueTransformer> field2ValueTransformer) {
        this.field2ValueTransformer = field2ValueTransformer;
    }

}