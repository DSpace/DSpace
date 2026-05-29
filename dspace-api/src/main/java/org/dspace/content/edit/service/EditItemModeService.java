/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import org.dspace.content.Item;
import org.dspace.content.edit.EditItemMode;
import org.dspace.core.Context;

/**
 * Service interface for managing and retrieving {@link EditItemMode} configurations.
 * <p>
 * Edit item modes define different ways items can be edited, with each mode specifying:
 * <ul>
 *   <li>Security constraints (which users/groups can use this mode)</li>
 *   <li>Submission definition (which submission process to use)</li>
 *   <li>Additional filters for fine-grained access control</li>
 * </ul>
 * <p>
 * This service provides methods to:
 * <ul>
 *   <li>Find all edit modes available for a specific item</li>
 *   <li>Filter modes based on current user's permissions</li>
 *   <li>Check if an item is editable by the current user</li>
 *   <li>Retrieve a specific edit mode by name</li>
 * </ul>
 * <p>
 * Edit modes are configured per entity type or configuration key (e.g., "publication", "person", "orgunit")
 * and are validated at startup to ensure no duplicate mode names exist within the same configuration key.
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItemMode
 * @see EditItemModeValidator
 *
 */
public interface EditItemModeService {

    /**
     * Finds all edit modes available for the given item, filtered by the current user's privileges.
     * <p>
     * Only returns edit modes that the current user has permission to use based on the configured
     * security constraints.
     *
     * @param  context       the DSpace context
     * @param  item          the item to find edit modes for
     * @return               list of edit modes accessible to the current user (empty if none available)
     * @throws SQLException  if a database error occurs
     */
    List<EditItemMode> findModes(Context context, Item item) throws SQLException;

    /**
     * Finds all edit modes available for the item with the given ID, filtered by the current user's privileges.
     * <p>
     * Only returns edit modes that the current user has permission to use based on the configured
     * security constraints.
     *
     * @param  context       the DSpace context
     * @param  itemId        the UUID of the item to find edit modes for
     * @return               list of edit modes accessible to the current user (empty if none available)
     * @throws SQLException  if a database error occurs
     */
    List<EditItemMode> findModes(Context context, UUID itemId) throws SQLException;

    /**
     * Finds a specific edit mode by name for the given item.
     * <p>
     * Note: This method does NOT check security permissions. It retrieves the mode configuration
     * if it exists, regardless of whether the current user has access to use it.
     *
     * @param  context       the DSpace context
     * @param  item          the item to find the edit mode for
     * @param  name          the name of the edit mode to retrieve
     * @return               the edit mode with the specified name, or null if not found
     * @throws SQLException  if a database error occurs
     */
    EditItemMode findMode(Context context, Item item, String name) throws SQLException;

    /**
     * Check if the current user can edit the given item, based on the all the
     * configured edit modes (verifying if there is at least one edit mode enabled
     * for him).
     * 
     * @param  context the DSpace context
     * @param  item    the item
     * @return         true if the given item is editable, false otherwise
     */
    boolean canEdit(Context context, Item item);

    /**
     * Finds all edit modes for the given item, optionally filtered by the current user's privileges.
     * <p>
     * This method allows bypassing security checks when needed (e.g., for administrative purposes).
     * When security checking is disabled, all configured edit modes for the item are returned
     * regardless of the current user's permissions.
     *
     * @param  context        the DSpace context
     * @param  item           the item to find edit modes for
     * @param  checkSecurity  if true, filters modes based on current user's permissions;
     *                        if false, returns all configured modes without security filtering
     * @return                list of edit modes (filtered by security if checkSecurity is true)
     * @throws SQLException   if a database error occurs
     */
    List<EditItemMode> findModes(Context context, Item item, boolean checkSecurity)
        throws SQLException;
}
