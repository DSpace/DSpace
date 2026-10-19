/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.exception.RepositoryMethodNotImplementedException;
import org.dspace.app.rest.model.EditItemModeRest;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.edit.service.EditItemModeService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * REST repository for retrieving individual {@link EditItemMode} configurations.
 * <p>
 * This repository provides access to specific edit mode definitions that control how archived items
 * can be edited through the EditItem system. Each mode defines security constraints, which submission
 * definition to use, and optional metadata filters for role-based editing.
 * <p>
 * <strong>REST Endpoint:</strong>
 * <pre>
 * GET /api/core/edititemmodes/{itemUUID}:{modeName}
 * </pre>
 * <p>
 * <strong>Composite Identifier Format:</strong>
 * The repository uses a composite key format {@code "{itemUUID}:{modeName}"} where:
 * <ul>
 *   <li><strong>itemUUID</strong> - The UUID of the item being edited</li>
 *   <li><strong>modeName</strong> - The name of the edit mode (e.g., "FULL", "OWNER", "INVESTIGATOR")</li>
 * </ul>
 * <p>
 * <strong>Purpose:</strong>
 * This repository allows clients to:
 * <ul>
 *   <li>Retrieve the configuration details for a specific edit mode</li>
 *   <li>Determine which submission definition applies to the mode</li>
 *   <li>Understand what editing capabilities the mode provides</li>
 * </ul>
 * <p>
 * <strong>Example Request:</strong>
 * <pre>
 * GET /api/core/edititemmodes/a1b2c3d4-5678-90ab-cdef-1234567890ab:OWNER
 * </pre>
 * Returns the EditItemMode configuration for the "OWNER" mode applied to the specified item,
 * including the mode's name, label, and associated submission definition.
 * <p>
 * <strong>Configuration:</strong>
 * Edit modes are defined in {@code edititem-service.xml} and can vary by entity type. The repository
 * uses {@link EditItemModeService#findMode} to retrieve the mode configuration.
 * <p>
 * <strong>Security:</strong>
 * <ul>
 *   <li>{@code findOne} requires authentication ({@code @PreAuthorize("isAuthenticated()")})</li>
 *   <li>Anonymous users receive HTTP 401 Unauthorized when attempting to query mode configurations</li>
 *   <li>Authenticated users can view mode configurations, but authorization is enforced when attempting
 *       to use the mode to actually edit an item (handled by {@link EditItemRestRepository})</li>
 * </ul>
 * <p>
 * <strong>Limitations:</strong>
 * <ul>
 *   <li>{@link #findAll} is not implemented - edit modes are always queried in relation to a specific item</li>
 *   <li>No create/update/delete operations - modes are configuration-driven, not user-modifiable</li>
 * </ul>
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItemMode
 * @see EditItemModeService
 * @see EditItemModeLinkRepository
 * @see EditItemRestRepository
 */
@Component(EditItemModeRest.CATEGORY + "." + EditItemModeRest.PLURAL_NAME)
public class EditItemModeRestRepository
    extends DSpaceRestRepository<EditItemModeRest, String> {

    @Autowired
    private EditItemModeService eimService;

    @Autowired
    ItemService itemService;

    /**
     * Retrieves a specific EditItemMode configuration for an item and mode name combination.
     * <p>
     * This method parses the composite identifier to extract the item UUID and mode name, then
     * queries the {@link EditItemModeService} to find the corresponding mode configuration.
     * <p>
     * <strong>Composite ID Parsing:</strong>
     * <ul>
     *   <li>Expected format: {@code "{itemUUID}:{modeName}"}</li>
     *   <li>Example: {@code "a1b2c3d4-5678-90ab-cdef-1234567890ab:OWNER"}</li>
     *   <li>Invalid format throws {@link DSpaceBadRequestException}</li>
     * </ul>
     * <p>
     * <strong>Lookup Process:</strong>
     * <ol>
     *   <li>Split the {@code data} parameter on ":" to extract UUID and mode name</li>
     *   <li>Validate that exactly 2 parts are present</li>
     *   <li>Parse the UUID and lookup the item via {@link ItemService}</li>
     *   <li>Query {@link EditItemModeService#findMode} to get the mode configuration</li>
     *   <li>Convert the mode to REST representation</li>
     * </ol>
     * <p>
     * <strong>Security:</strong>
     * Requires authentication. Anonymous users receive HTTP 401 Unauthorized.
     * <p>
     * <strong>Return Value Semantics:</strong>
     * <ul>
     *   <li>Returns {@code null} if the mode is not configured for the item's entity type</li>
     *   <li>Returns {@link EditItemModeRest} with mode details if found</li>
     * </ul>
     *
     * @param context the DSpace context for database access
     * @param data the composite identifier in format "{itemUUID}:{modeName}"
     * @return the REST representation of the edit mode configuration, or {@code null} if not found
     * @throws DSpaceBadRequestException if the {@code data} parameter is not in the expected format
     * @throws ResourceNotFoundException if the item with the specified UUID does not exist
     * @throws RuntimeException if a database error occurs during lookup
     */
    @Override
    @PreAuthorize("isAuthenticated()")
    public EditItemModeRest findOne(Context context, String data) {
        EditItemMode mode = null;
        String uuid = null;
        String modeName = null;
        String[] values = data.split(":");
        if (values != null && values.length == 2) {
            uuid = values[0];
            modeName = values[1];
        } else {
            throw new DSpaceBadRequestException(
                    "Given parameters are incomplete. Expected <UUID-ITEM>:<MODE>, Received: " + data);
        }
        try {
            UUID itemUuid = UUID.fromString(uuid);
            Item item = itemService.find(context, itemUuid);
            if (item == null) {
                throw new ResourceNotFoundException("No such item with uuid : " + itemUuid);
            }
            mode = eimService.findMode(context, item, modeName);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        if (mode == null) {
            return null;
        }
        return converter.toRest(mode, utils.obtainProjection());
    }

    /**
     * Not implemented - edit modes are always queried in relation to a specific item.
     * <p>
     * Edit modes are configuration-driven and specific to item entity types. There is no meaningful
     * way to list "all edit modes" without the context of a specific item, as the available modes
     * depend on the item's entity type and configuration.
     * <p>
     * Use {@link #findOne} with a composite ID or {@link EditItemModeLinkRepository#getModes} to
     * retrieve modes for a specific item.
     *
     * @param context the DSpace context (unused)
     * @param pageable pagination parameters (unused)
     * @return never returns normally
     * @throws RepositoryMethodNotImplementedException always thrown
     */
    @Override
    public Page<EditItemModeRest> findAll(Context context, Pageable pageable) {
        throw new RepositoryMethodNotImplementedException("No implementation found; Method not Implemented!", "");
    }

    /**
     * Returns the domain class managed by this repository.
     *
     * @return {@link EditItemModeRest}.class
     */
    @Override
    public Class<EditItemModeRest> getDomainClass() {
        return EditItemModeRest.class;
    }

}
