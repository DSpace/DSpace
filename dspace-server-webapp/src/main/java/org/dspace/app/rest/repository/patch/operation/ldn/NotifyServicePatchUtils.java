/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.patch.operation.ldn;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.ldn.NotifyServiceInboundPattern;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.app.rest.model.patch.JsonValueEvaluator;
import org.dspace.app.rest.model.patch.Operation;
import org.springframework.stereotype.Component;

/**
 * Util class for shared methods between the NotifyServiceEntity Operations
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Component
public final class NotifyServicePatchUtils {

    public static final String NOTIFY_SERVICE_INBOUND_PATTERNS = "notifyServiceInboundPatterns";

    private ObjectMapper objectMapper = new ObjectMapper();

    private NotifyServicePatchUtils() {
    }

    /**
     * Extract NotifyServiceInboundPattern from Operation by parsing the json
     * and mapping it to a NotifyServiceInboundPattern
     *
     * @param operation     Operation whose value is being parsed
     * @return NotifyServiceInboundPattern extracted from json in operation value
     */
    protected NotifyServiceInboundPattern extractNotifyServiceInboundPatternFromOperation(Operation operation) {
        NotifyServiceInboundPattern inboundPattern = null;
        try {
            if (operation.getValue() != null) {
                if (operation.getValue() instanceof JsonValueEvaluator) {
                    inboundPattern = objectMapper.readValue(((JsonValueEvaluator) operation.getValue())
                            .getValueNode().toString(), NotifyServiceInboundPattern.class);
                } else if (operation.getValue() instanceof String) {
                    inboundPattern = objectMapper.readValue((String) operation.getValue(),
                        NotifyServiceInboundPattern.class);
                }
            }
        } catch (IOException e) {
            throw new DSpaceBadRequestException("IOException: trying to map json from operation.value" +
                " to NotifyServiceInboundPattern class.", e);
        }
        if (inboundPattern == null) {
            throw new DSpaceBadRequestException("Could not extract NotifyServiceInboundPattern Object from Operation");
        }
        return inboundPattern;
    }

    /**
     * Extract list of NotifyServiceInboundPattern from Operation by parsing the json
     * and mapping it to a list of NotifyServiceInboundPattern
     *
     * @param operation     Operation whose value is being parsed
     * @return list of NotifyServiceInboundPattern extracted from json in operation value
     */
    protected List<NotifyServiceInboundPattern> extractNotifyServiceInboundPatternsFromOperation(Operation operation) {
        List<NotifyServiceInboundPattern> inboundPatterns = null;
        try {
            if (operation.getValue() != null) {
                if (operation.getValue() instanceof String) {
                    inboundPatterns = objectMapper.readValue((String) operation.getValue(),
                        objectMapper.getTypeFactory().constructCollectionType(ArrayList.class,
                            NotifyServiceInboundPattern.class));
                }
            }
        } catch (IOException e) {
            throw new DSpaceBadRequestException("IOException: trying to map json from operation.value" +
                " to List of NotifyServiceInboundPattern class.", e);
        }
        if (inboundPatterns == null) {
            throw new DSpaceBadRequestException("Could not extract list of NotifyServiceInboundPattern " +
                "Objects from Operation");
        }
        return inboundPatterns;
    }

    protected int extractIndexFromOperation(Operation operation) {
        String number = "";
        Pattern pattern = Pattern.compile("\\[(\\d+)\\]"); // Pattern to match [i]
        Matcher matcher = pattern.matcher(operation.getPath());
        if (matcher.find()) {
            number = matcher.group(1);
        }

        if (StringUtils.isEmpty(number)) {
            throw new DSpaceBadRequestException("path doesn't contain index");
        }

        return Integer.parseInt(number);
    }
}
