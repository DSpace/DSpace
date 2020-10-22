/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

import java.text.DecimalFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.app.nbevent.service.dto.MessageDto;
import org.dspace.content.NBEvent;
import org.hamcrest.Matcher;
import org.hamcrest.core.IsAnything;

public class NBEventMatcher {

    private NBEventMatcher() {
    }

    public static Matcher<? super Object> matchNBEventEntry(NBEvent event) {
        try {
            ObjectMapper jsonMapper = new JsonMapper();
            return allOf(hasJsonPath("$.id", is(event.getEventId())),
                    hasJsonPath("$.originalId", is(event.getOriginalId())),
                    hasJsonPath("$.title", is(event.getTitle())),
                    hasJsonPath("$.trust", is(new DecimalFormat("0.000").format(event.getTrust()))),
                    hasJsonPath("$.status", is("PENDING")),
                    hasJsonPath("$.message",
                            matchMessage(event.getTopic(), jsonMapper.readValue(event.getMessage(), MessageDto.class))),
                    hasJsonPath("$.type", is("nbevent")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Matcher<? super Object> matchMessage(String topic, MessageDto message) {
        if (StringUtils.endsWith(topic, "/ABSTRACT")) {
            return allOf(hasJsonPath("$.abstract", is(message.getAbstracts())));
        } else if (StringUtils.endsWith(topic, "/PID")) {
            return allOf(
                    hasJsonPath("$.value", is(message.getValue())),
                    hasJsonPath("$.type", is(message.getType())));
        } else if (StringUtils.endsWith(topic, "/PROJECT")) {
            return allOf(
                    hasJsonPath("$.openaireId", is(message.getOpenaireId())),
                    hasJsonPath("$.acronym", is(message.getAcronym())),
                    hasJsonPath("$.code", is(message.getCode())),
                    hasJsonPath("$.funder", is(message.getFunder())),
                    hasJsonPath("$.fundingProgram", is(message.getFundingProgram())),
                    hasJsonPath("$.jurisdiction", is(message.getJurisdiction())),
                    hasJsonPath("$.title", is(message.getTitle())));
        }
        return IsAnything.anything();
    }
}
