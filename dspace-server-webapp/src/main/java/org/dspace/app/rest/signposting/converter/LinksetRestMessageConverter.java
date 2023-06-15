/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.signposting.converter;

import static java.lang.String.format;
import static java.util.Objects.nonNull;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import org.dspace.app.rest.signposting.model.LinksetRest;

/**
 * Converter for converting LinksetRest message into application/linkset format.
 */
public class LinksetRestMessageConverter {

    private LinksetRestMessageConverter() {
    }

    /**
     * Converts LinksetRest object into string of application/linkset format.
     *
     * @param linksetRest linkset rest object
     * @return string of application/linkset format.
     */
    public static String convert(LinksetRest linksetRest) {
        StringBuilder responseBody = new StringBuilder();
        linksetRest.getLinksetNodes().forEach(linksetNodes -> {
            if (isNotBlank(linksetNodes.getLink())) {
                responseBody.append(format("<%s> ", linksetNodes.getLink()));
            }
            if (nonNull(linksetNodes.getRelation())) {
                responseBody.append(format("; rel=\"%s\" ", linksetNodes.getRelation().getName()));
            }
            if (isNotBlank(linksetNodes.getType())) {
                responseBody.append(format("; type=\"%s\" ", linksetNodes.getType()));
            }
            if (isNotBlank(linksetNodes.getAnchor())) {
                responseBody.append(format("; anchor=\"%s\" ", linksetNodes.getAnchor()));
            }
            responseBody.append(", ");
        });
        return responseBody.toString();
    }
}
