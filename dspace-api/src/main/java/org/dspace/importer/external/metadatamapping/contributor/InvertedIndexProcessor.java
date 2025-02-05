/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * @author adamo.fapohunda at 4science.com
 **/
public class InvertedIndexProcessor extends AbstractJsonPathMetadataProcessor {

    private static final Logger log = LogManager.getLogger(InvertedIndexProcessor.class);

    private String path;

    @Override
    protected String getStringValue(JsonNode node) {
        if (node == null || node.isEmpty()) {
            return "";
        }

        SortedMap<Integer, String> positionMap = new TreeMap<>();
        node.at(path).fields().forEachRemaining(entry -> entry.getValue()
                                                              .forEach(position ->
                                                                           positionMap
                                                                               .put(position.asInt(), entry.getKey())));

        return String.join(" ", positionMap.values());
    }

    @Override
    protected Logger getLogger() {
        return log;
    }

    @Override
    protected String getPath() {
        return "";
    }

    public void setPath(String path) {
        this.path = path;
    }
}