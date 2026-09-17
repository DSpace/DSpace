/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.security.service;

import java.sql.SQLException;
import java.util.List;

import org.dspace.content.DSpaceObject;
import org.dspace.content.MetadataValue;
import org.dspace.core.Context;

/**
 * Service to check metadata security. Answers both "is this field hidden from
 * non-admins?" (legacy {@code metadata.hide.*} configuration in {@code dspace.cfg})
 * and "which values on this item is the current user allowed to see?"
 * (per-value security levels configured in {@code metadata-security.cfg}).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface MetadataSecurityService {

    /**
     * Configuration prefix for the legacy admin-only metadata-hide mechanism
     * (e.g. {@code metadata.hide.dc.description.provenance = true}). Prefer
     * declaring {@code metadatavalue.visibility.*.settings} in
     * {@code metadata-security.cfg}; this prefix is kept for backwards
     * compatibility.
     */
    String LEGACY_HIDE_PREFIX = "metadata.hide.";

    /**
     * Returns whether the given metadata field is marked as hidden through the
     * legacy {@code metadata.hide.*} configuration. Administrators always see
     * hidden fields; for non-admins the field is hidden whenever any matching
     * {@code metadata.hide.<schema>.<element>[.<qualifier>]} property is set
     * to {@code true}.
     *
     * @param context   DSpace context (may be {@code null}, in which case no
     *                  admin override applies)
     * @param schema    metadata field schema (e.g. {@code "dc"})
     * @param element   metadata field element
     * @param qualifier metadata field qualifier, or {@code null}
     * @return {@code true} if the field should be hidden from the current user
     * @throws SQLException if a database error occurs while checking admin status
     */
    boolean isFieldHidden(Context context, String schema, String element, String qualifier) throws SQLException;

    /**
     * Return all the metadata values of the given item filtered by permission evaluations.
     *
     * @param context   DSpace Context
     * @param dso       the dso
     * @param schema    the metadata schema
     * @param element   the metadata element
     * @param qualifier the metadata qualifier, or null for unqualified metadata
     * @param language  the metadata language, or null for any language
     * @return the metadata values filtered by permissions
     */
    <T extends DSpaceObject> List<MetadataValue> getPermissionFilteredMetadataValues(Context context, T dso,
                                                                                     String schema,
                                                            String element, String qualifier, String language);

    /**
     * Returns all the metadata values of the given dso filtered by permission
     * evaluations.
     *
     * @param  context the DSpace Context
     * @param  dso     the dso
     * @return         the metadata values
     */
    <T extends DSpaceObject> List<MetadataValue> getPermissionFilteredMetadataValues(Context context, T dso);

    /**
     * Returns all the metadata values of the given dso filtered by permission
     * evaluations and language preferences.
     * 
     * <p>This method applies the same security filtering as {@link #getPermissionFilteredMetadataValues}
     * but additionally filters metadata by the current locale language from the context.
     * Only metadata values matching the current user's language preference are included
     * in the result.</p>
     *
     * @param  context                 the DSpace Context
     * @param  dso                     the dso
     * @return                         the metadata values filtered by both permissions and language
     */
    <T extends DSpaceObject> List<MetadataValue> getPermissionAndLangFilteredMetadataFields(Context context, T dso);

    /**
     * Apply the same permission and security-level filters used by
     * {@link #getPermissionFilteredMetadataValues} to an already-fetched list
     * of metadata values. Pure: takes values in, returns the allowed subset
     * without talking to the database. Idempotent.
     *
     * <p>This is the hook used by
     * {@link org.dspace.content.service.DSpaceObjectService#getMetadata
     * DSpaceObjectService.getMetadata} to filter in-place, and by callers that
     * already have a list of raw metadata values and want to apply the same
     * rules without re-fetching.</p>
     *
     * @param context the DSpace context (may be {@code null}; no admin override then)
     * @param dso     the owning DSpace object
     * @param values  the already-fetched metadata values
     * @return the subset of values the current user is allowed to see
     */
    <T extends DSpaceObject> List<MetadataValue> filter(Context context, T dso, List<MetadataValue> values);
}
