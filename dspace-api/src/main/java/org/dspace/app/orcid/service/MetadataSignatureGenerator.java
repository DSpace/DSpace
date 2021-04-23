/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.Item;
import org.dspace.core.Context;

/**
 * Interface that mark classes that can be used to generate a signature for the
 * given metadataField/item pair.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface MetadataSignatureGenerator {

    /**
     * Generate a list of signature related to the metadata values of the given item
     * relative to the given metadataField. If the given metadataFields represents a
     * nested metadata group, all the other metadata of the same group can be used
     * to generate the signature. The list will contain a signature for each
     * metadata field or nested metadata, sorted by place.
     *
     * @param  context       the DSpace context
     * @param  item          the item
     * @param  metadataField the metadataField
     * @return               the generated signatures
     * @throws SQLException  if a SQL error occurs
     */
    public List<String> generate(Context context, Item item, List<String> metadataFields) throws SQLException;
}
