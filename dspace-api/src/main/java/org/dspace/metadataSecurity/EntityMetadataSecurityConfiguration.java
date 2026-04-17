/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metadataSecurity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration class that defines metadata security levels for a specific entity type.
 *
 * <p><strong>Purpose:</strong></p>
 * <p>This class encapsulates the security configuration for metadata fields on a specific
 * DSpace entity type (e.g., Person, Publication, Project, Organization). It allows administrators
 * to configure default security levels for all metadata fields on an entity type, and override
 * those defaults for specific metadata fields.</p>
 *
 * <p><strong>Configuration Structure:</strong></p>
 * <ul>
 *   <li><strong>Entity Type:</strong> The type of entity this configuration applies to
 *       (e.g., "Person", "Publication", "Project")</li>
 *   <li><strong>Default Security Levels:</strong> A list of security levels applied to all
 *       metadata fields on this entity type unless a custom override exists</li>
 *   <li><strong>Custom Security Levels:</strong> A map of specific metadata field names to
 *       their custom security level lists, overriding the default</li>
 * </ul>
 *
 * <p><strong>Security Level Values:</strong></p>
 * <p>Each security level is represented by an integer corresponding to a
 * {@link org.dspace.content.service.MetadataSecurityEvaluation} implementation:</p>
 * <ul>
 *   <li><strong>0</strong> - Public access (everyone can view) via
 *       {@link org.dspace.content.MetadataPublicAccess}</li>
 *   <li><strong>1</strong> - Group-based access (only "Trusted" group members) via
 *       {@link org.dspace.content.MetadataGroupBasedAccess}</li>
 *   <li><strong>2</strong> - Administrator and owner only via
 *       {@link org.dspace.content.MetadataAdministratorAndOwnerAccess}</li>
 * </ul>
 *
 * <p><strong>Configuration File Format:</strong></p>
 * <p>This class is populated from DSpace configuration properties in {@code metadata-security.cfg}.
 *
 * <p><strong>Multiple Security Levels:</strong></p>
 * <p>The security level lists can contain multiple values, allowing metadata to be visible
 * at multiple security tiers. For example, {@code [0 1 2]} means the metadata is visible to:</p>
 * <ul>
 *   <li>Everyone (level 0)</li>
 *   <li>Trusted group members (level 1)</li>
 *   <li>Administrators and owners (level 2)</li>
 * </ul>
 *
 * <p><strong>REST API Exposure:</strong></p>
 * <p>This configuration is exposed via the REST API at
 * {@code /api/core/securitysettings/{entityType}} to allow the Angular UI to display
 * appropriate visibility controls during item submission and editing.</p>
 *
 * @author Alba Aliu (alba.aliu@atis.al)
 * @see org.dspace.content.service.MetadataSecurityEvaluation
 */
public class EntityMetadataSecurityConfiguration {

    /**
     * The entity type this configuration applies to (e.g., "Person", "Publication", "Project").
     */
    private String entityType;

    /**
     * Default security levels applied to all metadata fields on this entity type.
     * Each integer corresponds to a security level (0 = public, 1 = group, 2 = admin/owner).
     * Multiple levels can be specified, making metadata visible at multiple security tiers.
     */
    private List<Integer> metadataSecurityDefault;

    /**
     * Custom security level overrides for specific metadata fields.
     * Map key: metadata field name (e.g., "dc.identifier.orcid")
     * Map value: list of security levels for that specific field, overriding the default
     */
    private Map<String, List<Integer>> metadataCustomSecurity;

    /**
     * Constructs a new EntityMetadataSecurityConfiguration for the specified entity type.
     *
     * <p>Initializes empty collections for default and custom security settings.</p>
     *
     * @param entityType the entity type this configuration applies to (e.g., "Person", "Publication")
     */
    public EntityMetadataSecurityConfiguration(String entityType) {
        this.entityType = entityType;
        this.metadataSecurityDefault = new ArrayList<>();
        this.metadataCustomSecurity = new HashMap<>();
    }

    /**
     * Gets the default security levels applied to all metadata fields on this entity type.
     *
     * <p>These levels apply to any metadata field that does not have a custom security
     * configuration defined in {@link #metadataCustomSecurity}.</p>
     *
     * @return list of security level integers (0 = public, 1 = group, 2 = admin/owner);
     *         may be empty if no default is configured
     */
    public List<Integer> getMetadataSecurityDefault() {
        return metadataSecurityDefault;
    }

    /**
     * Sets the default security levels for all metadata fields on this entity type.
     *
     * @param metadataSecurityDefault list of security level integers; empty list if no default configured
     */
    public void setMetadataSecurityDefault(List<Integer> metadataSecurityDefault) {
        this.metadataSecurityDefault = metadataSecurityDefault;
    }

    /**
     * Gets the map of custom security level overrides for specific metadata fields.
     *
     * <p>Keys in this map are metadata field names (e.g., "dc.identifier.orcid"), and values
     * are lists of security levels that override the default for that specific field.</p>
     *
     * @return map of metadata field names to security level lists; may be empty if no custom
     *         configurations exist
     */
    public Map<String, List<Integer>> getMetadataCustomSecurity() {
        return metadataCustomSecurity;
    }

    /**
     * Sets the custom security level overrides for specific metadata fields.
     *
     * @param metadataCustomSecurity map where keys are metadata field names and values are
     *                               security level lists
     */
    public void setMetadataCustomSecurity(Map<String, List<Integer>> metadataCustomSecurity) {
        this.metadataCustomSecurity = metadataCustomSecurity;
    }

    /**
     * Gets the entity type this configuration applies to.
     *
     * @return the entity type (e.g., "Person", "Publication", "Project")
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * Sets the entity type this configuration applies to.
     *
     * @param entityType the entity type (e.g., "Person", "Publication", "Project")
     */
    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }
}
