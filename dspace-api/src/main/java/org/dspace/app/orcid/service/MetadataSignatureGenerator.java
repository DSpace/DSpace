/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface that mark classes that can be used to generate a signature for
 * metadata fields.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface MetadataSignatureGenerator {

    /**
     * Generate a signature related to the metadata values of the given item
     * relative to the given metadataFields.
     *
     * @param  context       the DSpace context
     * @param  item          the item
     * @param  metadataField the metadataField
     * @return               the generated signature
     */
    public String generate(Context context, Item item, List<String> metadataFields);
}
