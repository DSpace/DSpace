/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.coarnotify;

import java.util.ArrayList;
import java.util.List;

import org.dspace.coarnotify.service.SubmissionNotifyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation of {@link SubmissionNotifyService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SubmissionNotifyServiceImpl implements SubmissionNotifyService {

    @Autowired(required = true)
    private NotifyConfigurationService coarNotifyConfigurationService;

    protected SubmissionNotifyServiceImpl() {

    }

    @Override
    public NotifySubmissionConfiguration findOne(String id) {
        List<NotifyPattern> patterns =
            coarNotifyConfigurationService.getPatterns().get(id);

        if (patterns == null) {
            return null;
        }

        return new NotifySubmissionConfiguration(id, patterns);
    }

    @Override
    public List<NotifySubmissionConfiguration> findAll() {
        List<NotifySubmissionConfiguration> coarNotifies = new ArrayList<>();

        coarNotifyConfigurationService.getPatterns().forEach((id, patterns) ->
            coarNotifies.add(new NotifySubmissionConfiguration(id, patterns)
            ));

        return coarNotifies;
    }

}
