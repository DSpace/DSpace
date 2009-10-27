/*
 * InProgressSubmission.java
 *
 * Version: $Revision$
 *
 * Date: $Date$
 *
 * Copyright (c) 2002-2005, Hewlett-Packard Company and Massachusetts
 * Institute of Technology.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * - Neither the name of the Hewlett-Packard Company nor the name of the
 * Massachusetts Institute of Technology nor the names of their
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH
 * DAMAGE.
 */
package org.dspace.content;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.eperson.EPerson;

/**
 * Interface for manipulating in-progress submissions, without having to know at
 * which stage of submission they are (in workspace or workflow system)
 * 
 * @author Robert Tansley
 * @version $Revision$
 */
public interface InProgressSubmission
{
    /**
     * Get the internal ID of this submission
     * 
     * @return the internal identifier
     */
    int getID();

    /**
     * Deletes submission wrapper, doesn't delete item contents
     */
    void deleteWrapper() throws SQLException, IOException, AuthorizeException;

    /**
     * Update the submission, including the unarchived item.
     */
    void update() throws SQLException, IOException, AuthorizeException;

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
     */
    EPerson getSubmitter() throws SQLException;

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
     * @param b
     *            if <code>true</code>, submission may have more than one
     *            file.
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
     * @param b
     *            if <code>true</code>, submission may have more than one
     *            title.
     */
    void setMultipleTitles(boolean b);

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
     * @param b
     *            <code>true</code> if it has been published before
     */
    void setPublishedBefore(boolean b);
}
