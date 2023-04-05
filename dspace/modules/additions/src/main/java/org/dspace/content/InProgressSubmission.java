/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import java.sql.SQLException;
import java.util.List;

import org.dspace.core.ReloadableEntity;
import org.dspace.eperson.EPerson;

/**
 * Interface for manipulating in-progress submissions, without having to know at
 * which stage of submission they are (in workspace or workflow system)
 *
 * @author Robert Tansley
 */
public interface InProgressSubmission extends ReloadableEntity<Integer> {
    /**
     * Get the internal ID of this submission
     *
     * @return the internal identifier
     */
    @Override
    Integer getID();

    /**
     * Get the incomplete item object
     *
     * @return the item
     */
    Item getItem();

    /**
     * Get the collection being submitted to
     *
     * @return the collection
     */
    Collection getCollection();

    /**
     * Get the submitter
     *
     * @return the submitting e-person
     * @throws SQLException if database error
     */
    EPerson getSubmitter();

    /**
     * Find out if the submission has (or is intended to have) more than one
     * associated bitstream.
     *
     * @return <code>true</code> if there is to be more than one file.
     */
    boolean hasMultipleFiles();

    /**
     * Indicate whether the submission is intended to have more than one file.
     *
     * @param b if <code>true</code>, submission may have more than one
     *          file.
     */
    void setMultipleFiles(boolean b);

    /**
     * Find out if the submission has (or is intended to have) more than one
     * title.
     *
     * @return <code>true</code> if there is to be more than one file.
     */
    boolean hasMultipleTitles();

    /**
     * Indicate whether the submission is intended to have more than one title.
     *
     * @param b if <code>true</code>, submission may have more than one
     *          title.
     */
    void setMultipleTitles(boolean b);

//    // Customization for LIBDRUM-628
//    /**
//     * Find out if the submission is a dataset
//     *
//     * @return <code>true</code> if it is a dataset
//     */
//    boolean isDataset();
//
//    /**
//     * Indicate whether the submission is intended to contain a dataset.
//     *
//     * @param b
//     *          if <code>true</code>, submission is a dataset
//     */
//    void setDataset(boolean b);
//    // End customization for LIBDRUM-628

    /**
     * Find out if the submission has been published or publicly distributed
     * before
     *
     * @return <code>true</code> if it has been published before
     */
    boolean isPublishedBefore();

    /**
     * Indicate whether the submission has been published or publicly
     * distributed before
     *
     * @param b <code>true</code> if it has been published before
     */
    void setPublishedBefore(boolean b);

    // Begin UMD Customization
    /**
     * Map the given collections to the InProgressSubmission item.
     *
     * @param collections
     *                    Collections to be mapped to the InProgressSubmission item
     */
    void addMappedCollections(List<Collection> collections);

    /**
     * Remove all mapped collections from InProgressSubmission item.
     */
    void removeMappedCollections();

    /**
     * Return all mapped collections for the InProgressSubmission item.
     *
     * @return Collections mapped to the InProgressSubmission item
     */
    List<Collection> getMappedCollections();
    // End UMD Customization
}
