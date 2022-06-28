/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.service;

import java.util.List;

import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Interface that mark classes that can be used to generate a signature for
 * metadata values. The signature must be a unique identification of a metadata,
 * based on the attributes that compose it (such as field, value and authority).
 * It is possible to generate a signature for a single metadata value and also
 * for a list of values. Given an item, a signature can for example be used to
 * check if the associated metadata is present in the item.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface MetadataSignatureGenerator {

    /**
     * Generate a signature related to the given metadata values.
     *
     * @param  context        the DSpace context
     * @param  metadataValues the metadata values to sign
     * @return                the generated signature
     */
    public String generate(Context context, List<MetadataValue> metadataValues);

    /**
     * Returns the metadata values traceable by the given item related with the
     * given signature.
     *
     * @param  context   the DSpace context
     * @param  item      the item
     * @param  signature the metadata signature
     * @return           the founded metadata
     */
    public List<MetadataValue> findBySignature(Context context, Item item, String signature);
}
