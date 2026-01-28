/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import org.dspace.content.MetadataFieldName;

/**
 * Class with constants specific of broad DSpace-CRIS features
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it
 * @version $Revision$
 */
public class CrisConstants {
    /**
     * The value stored in nested metadata that were left empty to keep them in the
     * same number than the parent leading metadata
     */
    public static final String PLACEHOLDER_PARENT_METADATA_VALUE = "#PLACEHOLDER_PARENT_METADATA_VALUE#";
    public static final String DSPACE_BASE_VERSION = "DSpace 8.2";
    public static final MetadataFieldName MD_ENTITY_TYPE = new MetadataFieldName("dspace", "entity", "type");
    public static final MetadataFieldName MD_SUBMISSION_TYPE = new MetadataFieldName("cris", "submission",
                                                                                     "definition");
    public static final MetadataFieldName MD_WORKFLOW_NAME = new MetadataFieldName("cris", "workflow", "name");
    public static final MetadataFieldName MD_SHARED_WORKSPACE = new MetadataFieldName("cris", "workspace", "shared");

    /**
     * Make the constructor private as it is an utility class
     */
    private CrisConstants() {
    }
}