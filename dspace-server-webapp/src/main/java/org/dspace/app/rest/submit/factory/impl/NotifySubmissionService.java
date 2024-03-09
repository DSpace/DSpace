/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.factory.impl;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.ldn.service.NotifyService;
import org.dspace.app.rest.exception.UnprocessableEntityException;
import org.dspace.app.rest.model.step.DataNotify;
import org.dspace.authorize.AuthorizeException;
import org.dspace.coarnotify.NotifyConfigurationService;
import org.dspace.coarnotify.NotifyPattern;
import org.dspace.content.InProgressSubmission;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Service to manipulate COAR Notify section of in-progress submissions.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com
 */
@Component
public class NotifySubmissionService {

    @Autowired
    private NotifyPatternToTriggerService notifyPatternToTriggerService;

    @Autowired
    private NotifyConfigurationService coarNotifyConfigurationService;

    @Autowired
    private NotifyService notifyService;

    private NotifySubmissionService() { }


    /**
     * Builds the COAR Notify data of an in-progress submission
     *
     * @param obj   - the in progress submission
     * @return an object representing the CC License data
     * @throws SQLException
     * @throws IOException
     * @throws AuthorizeException
     */
    public DataNotify getDataCOARNotify(InProgressSubmission obj) throws SQLException {
        Context context = new Context();

        List<NotifyPatternToTrigger> patternsToTrigger =
            notifyPatternToTriggerService.findByItem(context, obj.getItem());

        Map<String, List<Integer>> data =
            patternsToTrigger.stream()
                             .collect(Collectors.groupingBy(
                                 NotifyPatternToTrigger::getPattern,
                                 Collectors.mapping(patternToTrigger ->
                                         patternToTrigger.getNotifyService().getID(),
                                     Collectors.toList())
                             ));

        return new DataNotify(data);
    }


    public int extractIndex(String path) {
        Pattern pattern = Pattern.compile("/(\\d+)$");
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        } else {
            throw new UnprocessableEntityException("Index not found in the path");
        }
    }

    /**
     * extract pattern from path. see COARNotifyConfigurationService bean
     * @param path
     * @return the extracted pattern
     */
    public String extractPattern(String path) {
        Pattern pattern = Pattern.compile("/([^/]+)/([^/]+)/([^/]+)");
        Matcher matcher = pattern.matcher(path);
        if (matcher.find()) {
            String patternValue = matcher.group(3);
            String config = matcher.group(2);
            if (!isContainPattern(config, patternValue)) {
                throw new UnprocessableEntityException(
                    "Invalid Pattern (" + patternValue + ") of " + config);
            }
            return patternValue;
        } else {
            throw new UnprocessableEntityException("Pattern not found in the path");
        }
    }

    private boolean isContainPattern(String config, String pattern) {
        List<NotifyPattern> patterns = coarNotifyConfigurationService.getPatterns().get(config);
        if (CollectionUtils.isEmpty(patterns)) {
            return false;
        }

        return patterns.stream()
                       .map(NotifyPattern::getPattern)
                       .anyMatch(v ->
                           v.equals(pattern));
    }

    /**
     * check that the provided services ids are compatible
     * with the provided inbound pattern
     *
     * @param context the context
     * @param pattern the inbound pattern
     * @param servicesIds notify services ids
     * @throws SQLException if something goes wrong
     */
    public void checkCompatibilityWithPattern(Context context, String pattern, Set<Integer> servicesIds)
        throws SQLException {

        List<Integer> manualServicesIds =
            notifyService.findManualServicesByInboundPattern(context, pattern)
                         .stream()
                         .map(NotifyServiceEntity::getID)
                         .collect(Collectors.toList());

        for (Integer servicesId : servicesIds) {
            if (!manualServicesIds.contains(servicesId)) {
                throw new UnprocessableEntityException("notify service with id (" + servicesId +
                    ") is not compatible with pattern " + pattern);
            }
        }
    }

}
