/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.submit.step.validation;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.rest.model.ErrorRest;
import org.dspace.app.rest.repository.WorkspaceItemRestRepository;
import org.dspace.app.rest.submit.SubmissionService;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.coarnotify.NotifyConfigurationService;
import org.dspace.coarnotify.NotifyPattern;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * Execute check validation of Coar notify services filters
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyValidation extends AbstractValidation {

    private static final String ERROR_VALIDATION_INVALID_FILTER = "error.validation.coarnotify.invalidfilter";

    private NotifyConfigurationService coarNotifyConfigurationService;

    private NotifyPatternToTriggerService notifyPatternToTriggerService;

    @Override
    public List<ErrorRest> validate(SubmissionService submissionService, InProgressSubmission obj,
                                    SubmissionStepConfig config) throws DCInputsReaderException, SQLException {

        List<ErrorRest> errors = new ArrayList<>();
        Context context = ContextUtil.obtainCurrentRequestContext();
        Item item = obj.getItem();

        List<String> patterns =
            coarNotifyConfigurationService.getPatterns().getOrDefault(config.getId(), List.of())
                                          .stream()
                                          .map(NotifyPattern::getPattern)
                                          .collect(Collectors.toList());

        patterns.forEach(pattern -> {
            List<NotifyServiceEntity> services = findByItemAndPattern(context, item, pattern);
            IntStream.range(0, services.size()).forEach(i ->
                services.get(i)
                        .getInboundPatterns()
                        .stream()
                        .filter(inboundPattern -> inboundPattern.getPattern().equals(pattern))
                        .filter(inboundPattern -> !inboundPattern.isAutomatic() &&
                            !inboundPattern.getConstraint().isEmpty())
                        .forEach(inboundPattern -> {
                            LogicalStatement filter =
                                new DSpace().getServiceManager()
                                            .getServiceByName(inboundPattern.getConstraint(), LogicalStatement.class);

                            if (filter == null || !filter.getResult(context, item)) {
                                addError(errors, ERROR_VALIDATION_INVALID_FILTER,
                                    "/" + WorkspaceItemRestRepository.OPERATION_PATH_SECTIONS +
                                        "/" + config.getId() +
                                        "/" + inboundPattern.getPattern() +
                                        "/" + i
                                );
                            }
                        }));
        });

        return errors;
    }

    private List<NotifyServiceEntity> findByItemAndPattern(Context context, Item item, String pattern) {
        try {
            return notifyPatternToTriggerService.findByItemAndPattern(context, item, pattern)
                                                .stream()
                                                .map(NotifyPatternToTrigger::getNotifyService)
                                                .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public NotifyConfigurationService getCoarNotifyConfigurationService() {
        return coarNotifyConfigurationService;
    }

    public void setCoarNotifyConfigurationService(
        NotifyConfigurationService coarNotifyConfigurationService) {
        this.coarNotifyConfigurationService = coarNotifyConfigurationService;
    }

    public NotifyPatternToTriggerService getNotifyPatternToTriggerService() {
        return notifyPatternToTriggerService;
    }

    public void setNotifyPatternToTriggerService(
        NotifyPatternToTriggerService notifyPatternToTriggerService) {
        this.notifyPatternToTriggerService = notifyPatternToTriggerService;
    }

}
