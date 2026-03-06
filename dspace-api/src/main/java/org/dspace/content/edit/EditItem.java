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
 * Class representing an item in the process of edit item submitted by a user
 * 
 * @author Danilo Di Nuzzo (danilo.dinuzzo at 4science.it)
 *
 */
public class EditItem implements InProgressSubmission<String> {

    private Item item;
    private EditItemMode mode;
    private Context context;

    public EditItem(Context context, Item item) {
        this.context = context;
        this.item = item;
    }

    public EditItem(Context context, Item item, EditItemMode mode) {
        this.context = context;
        this.item = item;
        this.mode = mode;
    }

    public static EditItem none(final Context context, final Item item) {
        return new EditItem(context, item);
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#getID()
     */
    @Override
    public String getID() {
        return item.getID().toString();
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#getItem()
     */
    @Override
    public Item getItem() {
        return item;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#getCollection()
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

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#getSubmitter()
     */
    @Override
    public EPerson getSubmitter() {
        return item.getSubmitter();
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#hasMultipleFiles()
     */
    @Override
    public boolean hasMultipleFiles() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#setMultipleFiles(boolean)
     */
    @Override
    public void setMultipleFiles(boolean b) {
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#hasMultipleTitles()
     */
    @Override
    public boolean hasMultipleTitles() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#setMultipleTitles(boolean)
     */
    @Override
    public void setMultipleTitles(boolean b) {
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#isPublishedBefore()
     */
    @Override
    public boolean isPublishedBefore() {
        return true;
    }

    /* (non-Javadoc)
     * @see org.dspace.content.InProgressSubmission#setPublishedBefore(boolean)
     */
    @Override
    public void setPublishedBefore(boolean b) {
    }

    public EditItemMode getMode() {
        return mode;
    }

    public void setMode(EditItemMode mode) {
        this.mode = mode;
    }

}
