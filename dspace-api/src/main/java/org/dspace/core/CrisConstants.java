/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;


import org.dspace.content.MetadataFieldName;
import org.dspace.content.MetadataSchemaEnum;

/**
 * Class with constants specific to CRIS features in DSpace
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it
 * @version $Revision$
 */
public class CrisConstants {
    /**
     * The value stored in nested metadata that were left empty to keep them in the
     * same number as the parent leading metadata
     */
    public static final String PLACEHOLDER_PARENT_METADATA_VALUE = "#PLACEHOLDER_PARENT_METADATA_VALUE#";

    public static final MetadataFieldName MD_SHARED_WORKSPACE = new MetadataFieldName(MetadataSchemaEnum.DSPACE,
                                                                                      "workspace",
                                                                                      "shared");

    /**
     * Make the constructor private as it is an utility class
     */
    private CrisConstants() {
    }
}