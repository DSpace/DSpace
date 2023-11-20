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

import java.util.List;

import org.dspace.app.rest.signposting.model.LinksetNode;

/**
 * Converter for converting list of linkset nodes into application/linkset format.
 */
public class LinksetRestMessageConverter {

    private LinksetRestMessageConverter() {
    }

    /**
     * Converts list of linkset nodes into string of application/linkset format.
     *
     * @param linksetNodes link of linkset nodes
     * @return string of application/linkset format.
     */
    public static String convert(List<List<LinksetNode>> linksetNodes) {
        StringBuilder responseBody = new StringBuilder();
        linksetNodes.stream().flatMap(List::stream).forEach(linksetNode -> {
            if (isNotBlank(linksetNode.getLink())) {
                responseBody.append(format("<%s> ", linksetNode.getLink()));
            }
            if (nonNull(linksetNode.getRelation())) {
                responseBody.append(format("; rel=\"%s\" ", linksetNode.getRelation().getName()));
            }
            if (isNotBlank(linksetNode.getType())) {
                responseBody.append(format("; type=\"%s\" ", linksetNode.getType()));
            }
            if (isNotBlank(linksetNode.getAnchor())) {
                responseBody.append(format("; anchor=\"%s\" ", linksetNode.getAnchor()));
            }
            responseBody.append(", ");
        });
        return responseBody.toString();
    }
}
