/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.sql.SQLException;

import org.dspace.app.util.DCInputsReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;

/**
 * Service interface class for the InProgressSubmission.
 * All InProgressSubmission service classes should implement this class since it offers some basic methods which all
 * InProgressSubmissions
 * are required to have.
 *
 * @param <T> class type
 * @author kevinvandevelde at atmire.com
 */
public interface InProgressSubmissionService<T extends InProgressSubmission> {

    /**
     * Deletes submission wrapper, doesn't delete item contents
     *
     * @param context              context
     * @param inProgressSubmission submission
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void deleteWrapper(Context context, T inProgressSubmission) throws SQLException, AuthorizeException;

    /**
     * Update the submission, including the unarchived item.
     *
     * @param context              context
     * @param inProgressSubmission submission
     * @throws SQLException       if database error
     * @throws AuthorizeException if authorization error
     */
    public void update(Context context, T inProgressSubmission) throws SQLException, AuthorizeException;

    public void move(Context context, T inProgressSubmission, Collection fromCollection, Collection toCollection)
        throws DCInputsReaderException;

}
