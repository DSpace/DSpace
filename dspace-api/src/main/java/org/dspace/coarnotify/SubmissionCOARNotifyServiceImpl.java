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

import org.dspace.coarnotify.service.SubmissionCOARNotifyService;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Service implementation of {@link SubmissionCOARNotifyService}
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class SubmissionCOARNotifyServiceImpl implements SubmissionCOARNotifyService {

    @Autowired(required = true)
    private COARNotifyConfigurationService coarNotifyConfigurationService;

    protected SubmissionCOARNotifyServiceImpl() {

    }

    @Override
    public COARNotifySubmissionConfiguration findOne(String id) {
        List<LDNPattern> patterns =
            coarNotifyConfigurationService.getPatterns().get(id);

        if (patterns == null) {
            return null;
        }

        return new COARNotifySubmissionConfiguration(id, patterns);
    }

    @Override
    public List<COARNotifySubmissionConfiguration> findAll() {
        List<COARNotifySubmissionConfiguration> coarNotifies = new ArrayList<>();

        coarNotifyConfigurationService.getPatterns().forEach((id, patterns) ->
            coarNotifies.add(new COARNotifySubmissionConfiguration(id, patterns)
            ));

        return coarNotifies;
    }

}
