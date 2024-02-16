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
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;

import java.text.DecimalFormat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.apache.commons.lang3.StringUtils;
import org.dspace.content.QAEvent;
import org.dspace.qaevent.service.dto.OpenaireMessageDTO;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsAnything;

/**
 * Matcher related to {@link QAEventResource}.
 *
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class QAEventMatcher {

    private QAEventMatcher() {
    }

    public static Matcher<? super Object> matchQAEventFullEntry(QAEvent event) {
        return allOf(
                matchQAEventEntry(event),
                hasJsonPath("$._embedded.topic.name", is(event.getTopic())),
                hasJsonPath("$._embedded.target.id", is(event.getTarget())),
                event.getRelated() != null ?
                        hasJsonPath("$._embedded.related.id", is(event.getRelated())) :
                        hasJsonPath("$._embedded.related", is(emptyOrNullString()))
                );
    }

    public static Matcher<? super Object> matchQAEventEntry(QAEvent event) {
        try {
            ObjectMapper jsonMapper = new JsonMapper();
            return allOf(hasJsonPath("$.id", is(event.getEventId())),
                    hasJsonPath("$.originalId", is(event.getOriginalId())),
                    hasJsonPath("$.title", is(event.getTitle())),
                    hasJsonPath("$.trust", is(new DecimalFormat("0.000").format(event.getTrust()))),
                    hasJsonPath("$.status", Matchers.equalToIgnoringCase(event.getStatus())),
                    hasJsonPath("$.message",
                            matchMessage(event.getTopic(), jsonMapper.readValue(event.getMessage(),
                                OpenaireMessageDTO.class))),
                    hasJsonPath("$._links.target.href", Matchers.endsWith(event.getEventId() + "/target")),
                    hasJsonPath("$._links.related.href", Matchers.endsWith(event.getEventId() + "/related")),
                    hasJsonPath("$._links.topic.href", Matchers.endsWith(event.getEventId() + "/topic")),
                    hasJsonPath("$.type", is("qualityassuranceevent")));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static Matcher<? super Object> matchMessage(String topic, OpenaireMessageDTO message) {
        if (StringUtils.endsWith(topic, "/ABSTRACT")) {
            return allOf(hasJsonPath("$.abstract", is(message.getAbstracts())));
        } else if (StringUtils.endsWith(topic, "/PID")) {
            return allOf(
                    hasJsonPath("$.value", is(message.getValue())),
                    hasJsonPath("$.type", is(message.getType())),
                    hasJsonPath("$.pidHref", is(calculateOpenairePidHref(message.getType(), message.getValue()))));
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

    private static String calculateOpenairePidHref(String type, String value) {
        if (type == null) {
            return null;
        }

        String hrefPrefix = null;

        switch (type) {
            case "arxiv":
                hrefPrefix = "https://arxiv.org/abs/";
                break;
            case "handle":
                hrefPrefix = "https://hdl.handle.net/";
                break;
            case "urn":
                hrefPrefix = "";
                break;
            case "doi":
                hrefPrefix = "https://doi.org/";
                break;
            case "pmc":
                hrefPrefix = "https://www.ncbi.nlm.nih.gov/pmc/articles/";
                break;
            case "pmid":
                hrefPrefix = "https://pubmed.ncbi.nlm.nih.gov/";
                break;
            case "ncid":
                hrefPrefix = "https://ci.nii.ac.jp/ncid/";
                break;
            default:
                break;
        }

        return hrefPrefix != null ? hrefPrefix + value : null;

    }
}
