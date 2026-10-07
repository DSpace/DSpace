/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;

import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.eperson.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Security Level 1 implementation: Group-based metadata access control with administrator
 * and owner fallback.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This class restricts metadata visibility to members of a specific DSpace group (typically "Trusted"),
 * while also allowing administrators and item owners to view the metadata through cascading
 * security evaluation.</p>
 *
 * <p><strong>Security Hierarchy (Cascading Evaluation):</strong></p>
 * <p>Access is granted if <strong>ANY</strong> of the following conditions are met (evaluated in order):</p>
 * <ol>
 *   <li><strong>Administrator Access:</strong> Current user is a DSpace administrator (via {@code level2Security})</li>
 *   <li><strong>Owner Access:</strong> Current user owns the item (via {@code level2Security})</li>
 *   <li><strong>Group Membership:</strong> Current user belongs to the configured group (e.g., "Trusted")</li>
 * </ol>
 *
 * <p><strong>Configuration:</strong></p>
 * <p>This class is configured in {@code spring-dspace-security-metadata.xml}.
 *
 * <p><strong>Cascading Behavior via {@code level2Security}:</strong></p>
 * <p>This implementation delegates to {@link MetadataAdministratorAndOwnerAccess} first before
 * checking group membership. This ensures administrators and owners always have access regardless
 * of group membership, preventing scenarios where an administrator cannot view metadata simply
 * because they are not in the "Trusted" group.</p>
 *
 * @author Alba Aliu
 * @see MetadataSecurityEvaluation
 * @see MetadataAdministratorAndOwnerAccess
 * @see org.dspace.content.security.MetadataSecurityServiceImpl
 */
public class MetadataGroupBasedAccess implements MetadataSecurityEvaluation {

    @Autowired
    private GroupService groupService;

    /**
     * The Level 2 security evaluator for cascading access checks.
     * Allows administrators and item owners to bypass group membership requirements.
     */
    @Autowired
    private MetadataSecurityEvaluation level2Security;

    /**
     * The name of the DSpace group whose members can view metadata with this security level.
     * Typically set to "Trusted" via Spring configuration in {@code spring-dspace-security-metadata.xml}.
     */
    private String egroup;

    /**
     * Evaluates whether the current user can view a metadata field with security level 1.
     *
     * <p><strong>Evaluation Logic:</strong></p>
     * <ol>
     *   <li><strong>Check Level 2 Security:</strong> First delegates to {@code level2Security}
     *       (MetadataAdministratorAndOwnerAccess) to determine if the user is an administrator
     *       or item owner. If true, access is granted immediately.</li>
     *   <li><strong>Check Authentication:</strong> If the context or current user is {@code null},
     *       access is denied (anonymous users cannot be group members).</li>
     *   <li><strong>Check Group Membership:</strong> Verifies if the current user is a member of
     *       the configured group (stored in {@code egroup} field). Returns {@code true} if the user
     *       is a direct or indirect member of the group.</li>
     * </ol>
     *
     * <p><strong>Important Notes:</strong></p>
     * <ul>
     *   <li>Group membership is evaluated via {@link GroupService#isMember(Context, String)} which
     *       checks both direct membership and membership through parent groups</li>
     *   <li>The {@code metadataField} parameter is passed to {@code level2Security} but is not
     *       used in the group membership check itself</li>
     *   <li>The {@code item} parameter is passed to {@code level2Security} for ownership verification
     *       but is not used in the group membership check itself</li>
     * </ul>
     *
     * @param context       DSpace context containing the current user
     * @param item          the Item whose metadata access is being evaluated (used by {@code level2Security})
     * @param metadataField the metadata field being evaluated (used by {@code level2Security})
     * @return {@code true} if the user is an administrator, item owner, or member of the configured group;
     *         {@code false} otherwise
     * @throws SQLException if database operations fail during group membership or administrator checks
     */
    @Override
    public boolean allowMetadataFieldReturn(Context context, Item item, MetadataField metadataField)
        throws SQLException {

        if (level2Security.allowMetadataFieldReturn(context, item, metadataField)) {
            return true;
        }

        if (context == null || context.getCurrentUser() == null) {
            return false;
        }

        return groupService.isMember(context, getEgroup());
    }

    /**
     * Gets the name of the DSpace group that grants access to metadata with security level 1.
     *
     * @return the group name (e.g., "Trusted")
     */
    public String getEgroup() {
        return egroup;
    }

    /**
     * Sets the name of the DSpace group that grants access to metadata with security level 1.
     * This property is typically injected by Spring configuration in
     * {@code spring-dspace-security-metadata.xml}.
     *
     * @param egroup the group name (e.g., "Trusted")
     */
    public void setEgroup(String egroup) {
        this.egroup = egroup;
    }
}
