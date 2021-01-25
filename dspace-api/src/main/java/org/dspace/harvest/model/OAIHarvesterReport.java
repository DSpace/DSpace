/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that model a report of an OAI harvesting.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterReport {

    private int failureCount = 0;

    private int successCount = 0;

    private final int totalRecordSize;

    private final Map<String, ErrorDetails> errors;

    public OAIHarvesterReport(int totalRecordSize) {
        this.totalRecordSize = totalRecordSize;
        this.errors = new LinkedHashMap<String, ErrorDetails>();
    }

    public boolean noRecordImportFails() {
        return failureCount == 0;
    }

    public void incrementFailureCount() {
        this.failureCount++;
    }

    public void incrementSuccessCount() {
        this.successCount++;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public int getFailureCount() {
        return failureCount;
    }

    public int getTotalRecordSize() {
        return totalRecordSize;
    }

    public int getCurrentRecord() {
        return successCount + failureCount + 1;
    }

    public void addError(String recordId, List<String> messages, String action) {
        if (errors.containsKey(recordId)) {
            errors.get(recordId).addMessages(messages);
        } else {
            errors.put(recordId, new ErrorDetails(messages, action));
        }
    }

    public void addError(String recordId, String message, String action) {
        if (errors.containsKey(recordId)) {
            errors.get(recordId).addMessage(message);
        } else {
            errors.put(recordId, new ErrorDetails(message, action));
        }
    }

    public Map<String, ErrorDetails> getErrors() {
        return errors;
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public final class ErrorDetails {

        private final List<String> messages;

        private final String action;

        public ErrorDetails(String message, String action) {
            this(new ArrayList<>(), action);
            addMessage(message);
        }

        public ErrorDetails(List<String> messages, String action) {
            this.messages = messages;
            this.action = action;
        }

        public void addMessages(List<String> messages) {
            this.messages.addAll(messages);
        }

        public void addMessage(String message) {
            this.messages.add(message);
        }

        public List<String> getMessages() {
            return messages;
        }

        public String getAction() {
            return action;
        }

    }

}
