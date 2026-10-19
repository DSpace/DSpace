/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.edit;

import java.sql.SQLException;

import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;

/**
 * Represents an <strong>already-published</strong> item that is being edited through a configurable edit mode.
 *
 * <p>
 * <strong>Relationship with {@link EditItemMode}:</strong>
 * An EditItem is always associated with an EditItemMode that defines:
 * <ul>
 *   <li>Which submission definition controls the editing interface</li>
 *   <li>Which users have permission to edit in this mode</li>
 *   <li>Any additional access control filters</li>
 * </ul>
 * <p>
 * This class implements {@link InProgressSubmission} to reuse the submission UI infrastructure,
 * but operates on published items rather than workspace items.
 * <p>
 *
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 * @see EditItemMode
 * @see org.dspace.content.WorkspaceItem
 * @see InProgressSubmission
 *
 */
public class EditItem implements InProgressSubmission<String> {

    private Item item;
    private EditItemMode mode;
    private Context context;

    /**
     * Constructs a new EditItem instance for managing an item in edit mode.
     *
     * @param context the DSpace context object
     * @param item    the item to be edited
     */
    public EditItem(Context context, Item item) {
        this.context = context;
        this.item = item;
    }

    /**
     * Constructs a new EditItem instance for managing an item in edit mode with a specific edit mode.
     *
     * @param context the DSpace context object
     * @param item    the item to be edited
     * @param mode    the edit mode to be applied to this item
     */
    public EditItem(Context context, Item item, EditItemMode mode) {
        this.context = context;
        this.item = item;
        this.mode = mode;
    }

    /**
     * Factory method to create an EditItem instance without a specific edit mode.
     * This is a convenience method that creates an EditItem with no predefined mode.
     *
     * @param context the DSpace context object
     * @param item    the item to be edited
     * @return a new EditItem instance without a specific mode
     */
    public static EditItem none(final Context context, final Item item) {
        return new EditItem(context, item);
    }

    /**
     * Retrieves the unique identifier of the item being edited.
     * Returns the item's UUID as a String representation.
     *
     * @return the string representation of the item's UUID
     */
    @Override
    public String getID() {
        return item.getID().toString();
    }

    /**
     * Retrieves the item object that is being edited.
     *
     * @return the Item object being edited
     */
    @Override
    public Item getItem() {
        return item;
    }

    /**
     * Retrieves the collection to which the item being edited belongs.
     * The collection is obtained by querying the parent object of the item.
     *
     * @return the Collection containing this item, or null if the item has no parent collection
     * @throws RuntimeException if a database error occurs while retrieving the collection
     */
    @Override
    public Collection getCollection() {
        Collection c = null;
        try {
            if (item != null) {
                c = (Collection) item.getItemService().getParentObject(context, item);
            }
        } catch (SQLException e) {
            throw new RuntimeException();
        }
        return c;
    }

    /**
     * Retrieves the person who originally submitted the item.
     *
     * @return the EPerson who submitted this item
     */
    @Override
    public EPerson getSubmitter() {
        return item.getSubmitter();
    }

    /**
     * Indicates whether the submission can have multiple files.
     * For EditItem, this always returns true as items being edited can contain multiple files.
     *
     * @return true, indicating that multiple files are supported
     */
    @Override
    public boolean hasMultipleFiles() {
        return true;
    }

    /**
     * Sets whether the submission can have multiple files.
     * This is a no-op implementation as EditItem always supports multiple files by default.
     *
     * @param b the flag indicating whether multiple files are allowed (ignored in this implementation)
     */
    @Override
    public void setMultipleFiles(boolean b) {
    }

    /**
     * Indicates whether the submission can have multiple titles.
     * For EditItem, this always returns true as items being edited can have multiple titles.
     *
     * @return true, indicating that multiple titles are supported
     */
    @Override
    public boolean hasMultipleTitles() {
        return true;
    }

    /**
     * Sets whether the submission can have multiple titles.
     * This is a no-op implementation as EditItem always supports multiple titles by default.
     *
     * @param b the flag indicating whether multiple titles are allowed (ignored in this implementation)
     */
    @Override
    public void setMultipleTitles(boolean b) {
    }

    /**
     * Indicates whether the item has been published before.
     * For EditItem, this always returns true as items being edited are typically already published.
     *
     * @return true, indicating that the item has been published before
     */
    @Override
    public boolean isPublishedBefore() {
        return true;
    }

    /**
     * Sets whether the item has been published before.
     * This is a no-op implementation as EditItem items are typically already published.
     *
     * @param b the flag indicating whether the item was published before (ignored in this implementation)
     */
    @Override
    public void setPublishedBefore(boolean b) {
    }

    /**
     * Retrieves the current edit mode for this item.
     * The edit mode determines how the item is being edited (e.g., metadata only, full edit, etc.).
     *
     * @return the EditItemMode currently applied to this item, or null if no mode is set
     */
    public EditItemMode getMode() {
        return mode;
    }

    /**
     * Sets the edit mode for this item.
     * The edit mode controls what aspects of the item can be edited.
     *
     * @param mode the EditItemMode to apply to this item
     */
    public void setMode(EditItemMode mode) {
        this.mode = mode;
    }

}
