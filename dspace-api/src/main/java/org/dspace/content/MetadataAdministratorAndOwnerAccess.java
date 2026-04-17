/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.eperson.service.EPersonService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Security Level 2 implementation: Administrator and owner-only metadata access control.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This class restricts metadata visibility to only DSpace administrators and the owner of the
 * specific item, blocking access for all other users including authenticated users and group members.</p>
 *
 * <p><strong>Access Policy:</strong></p>
 * <p>Access is granted if <strong>ANY</strong> of the following conditions are met:</p>
 * <ol>
 *   <li><strong>Administrator:</strong> Current user is a DSpace administrator (checked via
 *       {@link AuthorizeService#isAdmin(Context)})</li>
 *   <li><strong>Item Owner:</strong> Current user is the owner of the item (checked via
 *       {@link org.dspace.eperson.service.EPersonService#isOwnerOfItem})</li>
 * </ol>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>This class is configured in {@code spring-dspace-security-metadata.xml} and mapped to
 * security level {@code 2}.</p>
 *
 * <p><strong>Common Use Cases:</strong></p>
 * <ul>
 *   <li><strong>Sensitive personal information</strong> - Private details that only the owner
 *       and administrators should see</li>
 *   <li><strong>Internal administrative notes</strong> - Comments or flags for administrator review</li>
 *   <li><strong>Pre-moderation data</strong> - Metadata under review before being made visible</li>
 *   <li><strong>Privacy-protected fields</strong> - Information subject to privacy regulations
 *       (GDPR, FERPA) that should only be visible to the data subject and administrators</li>
 * </ul>
 *
 * <p><strong>Anonymous Users:</strong></p>
 * <p>If the current user is {@code null} (anonymous access) or the context is {@code null},
 * access is always denied.</p>
 *
 * @author Alba Aliu
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 * @see MetadataSecurityEvaluation
 * @see MetadataGroupBasedAccess
 * @see MetadataPublicAccess
 * @see org.dspace.content.security.MetadataSecurityServiceImpl
 */
public class MetadataAdministratorAndOwnerAccess implements MetadataSecurityEvaluation {

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private EPersonService ePersonService;

    /**
     * Evaluates whether the current user can view a metadata field with security level 2.
     *
     * <p><strong>Evaluation Logic:</strong></p>
     * <ol>
     *   <li><strong>Check Authentication:</strong> If the context or current user is {@code null},
     *       access is immediately denied (anonymous users cannot be administrators or owners).</li>
     *   <li><strong>Check Administrator:</strong> Verifies if the current user is a DSpace
     *       administrator using {@link AuthorizeService#isAdmin(Context)}. If true, access is
     *       granted regardless of ownership.</li>
     *   <li><strong>Check Ownership:</strong> Verifies if the current user owns the item using
     *       {@link EPersonService#isOwnerOfItem}. If true, access is granted.</li>
     * </ol>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>The {@code metadataField} parameter is provided for interface compliance but is not
     *       used in the evaluation logic. Access is based solely on user role (admin/owner),
     *       not on the specific field being accessed.</li>
     *   <li>Administrator check takes precedence over ownership check for efficiency (admins
     *       can see all items, so no need to check ownership).</li>
     *   <li>The OR logic means only one condition needs to be satisfied for access.</li>
     * </ul>
     *
     * @param context       DSpace context containing the current user
     * @param item          the Item whose metadata access is being evaluated (used for ownership check)
     * @param metadataField the metadata field being evaluated (not used in evaluation logic)
     * @return {@code true} if the user is an administrator or item owner; {@code false} otherwise
     * @throws SQLException if database operations fail during administrator or ownership checks
     */
    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField)
        throws SQLException {

        if (context == null || context.getCurrentUser() == null) {
            return false;
        }

        return authorizeService.isAdmin(context) || ePersonService.isOwnerOfItem(context.getCurrentUser(), item);
    }
}
