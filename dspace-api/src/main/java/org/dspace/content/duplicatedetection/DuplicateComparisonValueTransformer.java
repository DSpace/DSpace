/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.duplicatedetection;

/**
 * Defines a transformer for modifying or processing duplicate comparison values.
 * Implementations of this interface are used to transform a given string value into
 * a representation that is suitable for deduplication comparison purposes.
 * Common transformations might include standardization or formatting
 * of string values to ensure consistency and compatibility in duplicate detection processes.
 */
public interface DuplicateComparisonValueTransformer {
    /**
     * Transforms a given string value into a modified representation suitable for further processing
     * @param value the input string value to be transformed
     * @return the transformed string after applying the required modifications
     */
    String transform(String value);
}
