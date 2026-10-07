/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.core.Context;

/**
 * Strategy interface for evaluating whether metadata should be visible to the current user
 * based on security level.
 * <p>
 * <strong>How It Is Used:</strong>
 * <ol>
 *   <li>{@link org.dspace.content.MetadataValue} objects have a {@code security_level} column (Integer)</li>
 *   <li>When metadata needs to be filtered, {@link org.dspace.content.security.MetadataSecurityServiceImpl}
 *       retrieves the security level from the metadata value</li>
 *   <li>The service looks up the appropriate {@code MetadataSecurityEvaluation} implementation
 *       from a configuration map (defined in {@code spring-dspace-core-services.xml})</li>
 *   <li>It calls {@link #allowMetadataFieldReturn} to determine visibility</li>
 *   <li>The metadata is included or excluded from the response based on the return value</li>
 * </ol>
 *
 * @author Alba Aliu
 * @see org.dspace.content.security.service.MetadataSecurityService
 * @see org.dspace.content.security.MetadataSecurityServiceImpl#isMetadataValueReturnAllowed
 */
public interface MetadataSecurityEvaluation {

    /**
     * Determines whether metadata of a specific field should be visible to the current user.
     * <p>
     * Called by {@link org.dspace.content.security.MetadataSecurityServiceImpl} for each metadata
     * value that has a non-null security level, to decide if that value should be included in the
     * REST response or other metadata access operations.
     *
     * @param context the DSpace context providing the current user; may be null for anonymous access
     * @param item the item whose metadata visibility is being evaluated
     * @param metadataField the metadata field being evaluated
     * @return {@code true} if the metadata should be visible to the current user;
     *         {@code false} if it should be filtered out
     * @throws SQLException if database operations fail during evaluation
     */
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField)
        throws SQLException;

}
