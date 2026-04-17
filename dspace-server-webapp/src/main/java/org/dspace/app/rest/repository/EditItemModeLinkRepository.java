/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import jakarta.annotation.Nullable;
import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.model.EditItemModeRest;
import org.dspace.app.rest.model.EditItemRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * Link repository that provides access to the available edit modes for a specific EditItem.
 * <p>
 * This repository implements the {@code /modes} sub-resource endpoint for EditItem objects,
 * allowing REST clients to discover which edit modes are available for a given item and mode combination.
 * <p>
 * <strong>REST Endpoint:</strong>
 * <pre>
 * GET /api/core/edititems/{itemUUID}:{modeName}/modes
 * </pre>
 * <p>
 * <strong>Purpose:</strong>
 * When editing an archived item, different users may have access to different editing modes based on
 * their permissions and roles (e.g., administrator, owner, investigator). This repository returns the
 * list of all modes configured for the item's entity type, regardless of whether the current user
 * has permission to use each mode.
 * <p>
 * <strong>Example Request:</strong>
 * <pre>
 * GET /api/core/edititems/a1b2c3d4-5678-90ab-cdef-1234567890ab:FULL/modes
 * </pre>
 * Returns all configured modes for the item, such as:
 * <ul>
 *   <li>FULL - Complete editing access (administrators)</li>
 *   <li>OWNER - Limited editing for item owners</li>
 *   <li>INVESTIGATOR - Restricted editing for specific user groups</li>
 * </ul>
 * <p>
 * <strong>Configuration:</strong>
 * Edit modes are defined in {@code edititem-service.xml} per entity type. The repository queries
 * {@link EditItemModeService#findModes} to retrieve all configured modes for the item.
 * <p>
 * <strong>Security:</strong>
 * This endpoint requires authentication ({@code @PreAuthorize("isAuthenticated()")}) - anonymous users
 * receive HTTP 401 Unauthorized. The method returns all modes configured for the item without checking
 * whether the current user has permission to use each mode. Authorization for specific modes is enforced
 * when attempting to access or modify the item using a mode (handled by {@link EditItemRestRepository}).
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItemModeService#findModes
 * @see EditItemRestRepository
 * @see org.dspace.content.edit.EditItemMode
 */
@Component(EditItemRest.CATEGORY + "." + EditItemRest.NAME_PLURAL + "." + EditItemRest.MODE)
public class EditItemModeLinkRepository extends AbstractDSpaceRestRepository
    implements LinkRestRepository {

    @Autowired
    private EditItemModeService eimService;

    /**
     * Retrieves all available edit modes for a specific EditItem.
     * <p>
     * This method extracts the item UUID from the composite ID, queries the {@link EditItemModeService}
     * to find all modes configured for that item's entity type, and converts them to a pageable REST response.
     * <p>
     * <strong>ID Format:</strong> The {@code id} parameter uses the EditItem composite identifier format:
     * {@code {itemUUID}:{currentModeName}}. The item UUID is extracted to query available modes.
     * <p>
     * <strong>Example:</strong>
     * <pre>
     * // Client requests modes for EditItem "abc123...:FULL"
     * GET /api/core/edititems/abc123-4567-89ab-cdef-1234567890ab:FULL/modes
     *
     * // Repository extracts UUID "abc123-4567-89ab-cdef-1234567890ab"
     * // Queries EditItemModeService.findModes(context, uuid)
     * // Returns all configured modes: [FULL, OWNER, INVESTIGATOR]
     * </pre>
     * <p>
     * <strong>Security:</strong>
     * Requires authentication. Anonymous users receive HTTP 401 Unauthorized. This method returns all
     * configured modes for the item without checking whether the current user has permission to use each
     * mode. Authorization is enforced separately when the user attempts to access or modify the EditItem
     * using a specific mode (handled by {@link EditItemRestRepository}).
     *
     * @param request the HTTP servlet request (unused but required by interface)
     * @param id the EditItem composite identifier in format "{itemUUID}:{modeName}"
     * @param pageable pagination parameters for the response
     * @param projection the projection to apply when converting modes to REST (unused in this implementation)
     * @return a page of {@link EditItemModeRest} objects representing all available modes for the item;
     *         {@code null} if no modes are configured for the item's entity type
     * @throws RuntimeException if a database error occurs during mode retrieval
     */
    @PreAuthorize("isAuthenticated()")
    public Page<EditItemModeRest> getModes(
            @Nullable HttpServletRequest request, String id,
            @Nullable Pageable pageable, Projection projection) {
        Context context = obtainContext();
        String[] values = id.split(":");
        List<EditItemMode> modes = null;
        try {
            modes = eimService.findModes(context, UUID.fromString(values[0]));
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (modes == null) {
            return null;
        }
        return converter.toRestPage(modes, pageable, modes.size(), utils.obtainProjection());
    }

}
