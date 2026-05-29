/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.validation;

import static org.dspace.validation.service.ValidationService.OPERATION_PATH_SECTIONS;
import static org.dspace.validation.util.ValidationUtils.addError;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.app.ldn.NotifyPatternToTrigger;
import org.dspace.app.ldn.NotifyServiceEntity;
import org.dspace.app.ldn.service.NotifyPatternToTriggerService;
import org.dspace.app.util.SubmissionStepConfig;
import org.dspace.coarnotify.NotifyConfigurationService;
import org.dspace.coarnotify.NotifyPattern;
import org.dspace.content.InProgressSubmission;
import org.dspace.content.Item;
import org.dspace.content.logic.LogicalStatement;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;
import org.dspace.validation.model.ValidationError;

/**
 * Execute check validation of Coar notify services filters
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
public class NotifyValidation implements SubmissionStepValidator {

    private static final Logger logger = LogManager.getLogger(NotifyValidation.class);

    private static final String ERROR_VALIDATION_INVALID_FILTER = "error.validation.coarnotify.invalidfilter";

    private NotifyConfigurationService coarNotifyConfigurationService;

    private NotifyPatternToTriggerService notifyPatternToTriggerService;

    private String name;

    @Override
    public List<ValidationError> validate(
        Context context, InProgressSubmission<?> obj, SubmissionStepConfig config
    ) {

        List<ValidationError> errors = new ArrayList<>();
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
                                addError(
                                    errors,
                                    ERROR_VALIDATION_INVALID_FILTER,
                                    "/" + OPERATION_PATH_SECTIONS +
                                    "/" + config.getId() +
                                    "/" + inboundPattern.getPattern() +
                                    "/" + i
                                );
                            }
                        }));
        });

        return errors;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private List<NotifyServiceEntity> findByItemAndPattern(Context context, Item item, String pattern) {
        try {
            return notifyPatternToTriggerService.findByItemAndPattern(context, item, pattern)
                                                .stream()
                                                .map(NotifyPatternToTrigger::getNotifyService)
                                                .collect(Collectors.toList());
        } catch (SQLException e) {
            logger.info("Cannot retrieve the notify pattern linked to item {} and pattern {}: {}", item, pattern, e);
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
