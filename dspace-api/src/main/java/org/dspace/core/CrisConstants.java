/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

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

    /**
     * Make the constructor private as it is an utility class
     */
    private CrisConstants() {
    }
}