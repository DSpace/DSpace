/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.service.components.dto;

import java.util.List;


/**
 * Simple object used to construct a list of <key,value> items.
 * This type is used in file plain metadata import as RecordType.
 *
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 */

public class PlainMetadataSourceDto {

    private List<PlainMetadataKeyValueItem> metadata;

    /*
     * Method used to get the Metadata list
     */
    public List<PlainMetadataKeyValueItem> getMetadata() {
        return metadata;
    }

    /*
     * Method used to set the metadata list
     */
    public void setMetadata(List<PlainMetadataKeyValueItem> metadata) {
        this.metadata = metadata;
    }

}
