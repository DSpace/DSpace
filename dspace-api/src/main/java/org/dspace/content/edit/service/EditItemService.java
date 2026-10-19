/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit.service;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.edit.EditItem;
import org.dspace.content.edit.EditItemMode;
import org.dspace.content.service.InProgressSubmissionService;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Service interface class for the EditItem object.
 * The implementation of this class is responsible for all
 * business logic calls for the EditItem object and is autowired
 * by spring
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public interface EditItemService extends InProgressSubmissionService<EditItem> {

    /**
     * Returns the ItemService instance used for item operations.
     * 
     * @return the ItemService instance
     */
    ItemService getItemService();

    /**
     * Counts the total number of items in the repository that could potentially be edited.
     * This includes all archived items regardless of edit permissions.
     * 
     * @param context the DSpace context object
     * @return total count of all items in the repository
     * @throws SQLException if a database error occurs
     */
    int countTotal(Context context) throws SQLException;

    /**
     * Retrieves all items in the repository as EditItem instances with pagination support.
     * Each item is wrapped in an EditItem object without a specific edit mode applied.
     * 
     * <p><strong>Note:</strong> This method does not filter by edit permissions.
     * It returns all items but wraps them in EditItem objects for potential editing.</p>
     * 
     * @param context  the DSpace context object
     * @param pageSize the number of results per page
     * @param offset   the starting position (zero-based)
     * @return iterator of EditItem instances representing all items in the repository
     * @throws SQLException if a database error occurs
     */
    Iterator<EditItem> findAll(Context context, int pageSize, long offset) throws SQLException;

    /**
     * Finds all items submitted by a specific user and wraps them as EditItem instances.
     * 
     * <p><strong>Use case:</strong> Retrieve all items that a user originally submitted,
     * allowing them to potentially edit their own submissions.</p>
     * 
     * @param context  the DSpace context object
     * @param ep       the submitter (EPerson) whose items should be retrieved
     * @param pageSize the number of results per page
     * @param offset   the starting position (zero-based)
     * @return list of EditItem instances for items submitted by the specified user
     * @throws SQLException if a database error occurs
     */
    List<EditItem> findBySubmitter(Context context, EPerson ep, int pageSize, int offset) throws SQLException;

    /**
     * Counts the total number of items submitted by a specific user.
     * 
     * @param context the DSpace context object
     * @param ep      the submitter (EPerson) whose items should be counted
     * @return total count of items submitted by the specified user
     * @throws SQLException if a database error occurs
     */
    int countBySubmitter(Context context, EPerson ep) throws SQLException;

    /**
     * Finds an item by its UUID and wraps it as an EditItem without applying a specific edit mode.
     * 
     * <p><strong>Note:</strong> This method does not verify edit permissions or apply an edit mode.
     * It simply wraps the item in an EditItem object. Use {@link #find(Context, Item, String)}
     * to apply a specific edit mode with security checks.</p>
     * 
     * @param context the DSpace context object
     * @param id      the UUID of the item to find
     * @return EditItem instance wrapping the found item, or null if item doesn't exist
     * @throws SQLException if a database error occurs
     */
    public EditItem find(Context context, UUID id) throws SQLException;

    /**
     * Finds an item and applies a specific edit mode with full security validation.
     * 
     * <p><strong>Security checks performed:</strong></p>
     * <ol>
     *   <li>Verifies a user is authenticated (non-null current user)</li>
     *   <li>Retrieves the specified edit mode configuration for the item's entity type</li>
     *   <li>Validates the current user has access rights for the requested mode</li>
     * </ol>
     * 
     * <p><strong>Edit mode behavior:</strong></p>
     * <ul>
     *   <li>If mode is {@link EditItemMode#NONE}, returns an EditItem without a mode applied</li>
     *   <li>Otherwise, retrieves the mode configuration and validates user access</li>
     * </ul>
     * 
     * <p><strong>Example:</strong> Finding a person profile with "OWNER" mode ensures the current
     * user is the profile owner before allowing editing with the owner-specific submission definition.</p>
     * 
     * @param context the DSpace context object containing current user information
     * @param item    the item to be edited
     * @param mode    the name of the edit mode to apply (e.g., "FULL", "OWNER")
     * @return EditItem instance with the specified mode applied
     * @throws SQLException         if a database error occurs
     * @throws AuthorizeException   if the current user is not authenticated or lacks permission
     *                              to edit this item in the requested mode
     * @throws NotFoundException    if the specified edit mode does not exist for this item's entity type
     */
    public EditItem find(Context context, Item item, String mode) throws SQLException, AuthorizeException;

}
