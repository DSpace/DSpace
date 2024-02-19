/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.coarnotify.service;

import java.util.List;

import org.dspace.coarnotify.NotifySubmissionConfiguration;

/**
 * Service interface class for the Creative Submission COAR Notify.
 * The implementation of this class is responsible for all business logic calls for the Creative Submission COAR Notify
 * and is autowired by spring
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public interface SubmissionNotifyService {

    /**
     * Find the COARE Notify corresponding to the provided ID
     * found in the configuration
     *
     * @param id - the ID of the COAR Notify to be found
     * @return the corresponding COAR Notify if found or null when not found
     */
    public NotifySubmissionConfiguration findOne(String id);

    /**
     * Find all configured COAR Notifies
     *
     * @return all configured COAR Notifies
     */
    public List<NotifySubmissionConfiguration> findAll();

}
