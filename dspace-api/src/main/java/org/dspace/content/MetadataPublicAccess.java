/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;

/**
 * Security Level 0 implementation: Public metadata access with no restrictions.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This class grants unrestricted access to metadata values, allowing anyone (including anonymous
 * users) to view the metadata regardless of authentication status, group membership, or permissions.</p>
 *
 * <p><strong>Access Policy:</strong></p>
 * <p>This implementation always returns {@code true}, meaning:</p>
 * <ul>
 *   <li>All authenticated users can view the metadata</li>
 *   <li>All anonymous (unauthenticated) users can view the metadata</li>
 *   <li>No group membership is required</li>
 *   <li>No special permissions are required</li>
 * </ul>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>This class is configured in {@code spring-dspace-security-metadata.xml} and mapped to
 * security level {@code 0}.</p>
 *
 * <p><strong>When to Use Security Level 0:</strong></p>
 * <ul>
 *   <li><strong>Public announcements</strong> - Information that should be visible to everyone</li>
 *   <li><strong>Open access metadata</strong> - Fields that are explicitly intended to be public</li>
 *   <li><strong>Transparency requirements</strong> - Metadata that must be visible for compliance
 *       or policy reasons</li>
 * </ul>
 *
 * <p><strong>Important Distinction:</strong></p>
 * <p>Setting security level to {@code 0} is different from setting it to {@code null}:</p>
 * <ul>
 *   <li><strong>{@code null}</strong> (default) - No additional security; follows standard
 *       field-level visibility rules and READ permissions</li>
 *   <li><strong>{@code 0}</strong> (this class) - Explicitly grants public access; overrides
 *       normal field-level visibility to ensure the metadata is always visible</li>
 * </ul>
 *
 * @author Alba Aliu
 * @see MetadataSecurityEvaluation
 * @see MetadataGroupBasedAccess
 * @see MetadataAdministratorAndOwnerAccess
 * @see org.dspace.content.security.MetadataSecurityServiceImpl
 */

public class MetadataPublicAccess implements MetadataSecurityEvaluation {
    /**
     * Evaluates whether the current user can view a metadata field with security level 0.
     *
     * <p><strong>Implementation:</strong></p>
     * <p>This method always returns {@code true}, granting unrestricted access to all users
     * including anonymous users. The parameters are provided for interface compliance but
     * are not used in the evaluation.</p>
     *
     * @param context       DSpace context (not used; can be {@code null})
     * @param item          the Item whose metadata access is being evaluated (not used)
     * @param metadataField the metadata field being evaluated (not used)
     * @return always {@code true}, granting public access to everyone
     */
    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField) {
        // each user can see including anonymous
        return true;
    }
}
