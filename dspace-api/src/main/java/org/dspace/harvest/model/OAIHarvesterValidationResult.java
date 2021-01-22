/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;

import static java.util.stream.Collectors.toList;

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.util.ExceptionMessageUtils;

/**
 * Model the result of the validation performed by
 * {@link OAIHarvesterValidator}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public final class OAIHarvesterValidationResult {

    private final List<String> messages;

    private final boolean valid;

    public static OAIHarvesterValidationResult valid() {
        return new OAIHarvesterValidationResult(Collections.emptyList(), true);
    }

    public static OAIHarvesterValidationResult buildFromMessages(List<String> messages) {
        if (CollectionUtils.isEmpty(messages)) {
            return valid();
        }
        return new OAIHarvesterValidationResult(messages, false);
    }

    public static OAIHarvesterValidationResult buildFromException(Exception exception) {
        return buildFromExceptions(List.of(exception));
    }

    public static OAIHarvesterValidationResult buildFromExceptions(List<? extends Exception> exceptions) {
        if (CollectionUtils.isEmpty(exceptions)) {
            return valid();
        }
        return new OAIHarvesterValidationResult(getMessageFromExceptions(exceptions), false);
    }

    public OAIHarvesterValidationResult(List<String> messages, boolean valid) {
        this.messages = messages;
        this.valid = valid;
    }

    private static List<String> getMessageFromExceptions(List<? extends Exception> exceptions) {
        return exceptions.stream().map(ExceptionMessageUtils::getRootMessage).collect(toList());
    }

    public List<String> getMessages() {
        return messages;
    }

    public boolean isValid() {
        return valid;
    }

    public boolean isNotValid() {
        return !isValid();
    }

}
