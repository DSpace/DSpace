/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.contributor.JsonPathMetadataProcessor;

/**
 * This class is used for CrossRef's Live-Import to extract
 * issued attribute.
 * Beans are configured in the {@code crossref-integration.xml} file.
 *
 * @author Francesco Pio Scognamiglio (francescopio.scognamiglio at 4science.com)
 */
public class CrossRefDateMetadataProcessor implements JsonPathMetadataProcessor {

    private final static Logger log = LogManager.getLogger();

    private String pathToArray;

    @Override
    public Collection<String> processMetadata(String json) {
        JsonNode rootNode = convertStringJsonToJsonNode(json);
        Iterator<JsonNode> dates = rootNode.at(pathToArray).iterator();
        Collection<String> values = new ArrayList<>();
        while (dates.hasNext()) {
            JsonNode date = dates.next();
            LocalDate issuedDate = null;
            DateTimeFormatter issuedDateFormat = null;
            if (date.has(0) && date.has(1) && date.has(2)) {
                issuedDate = LocalDate.of(
                        date.get(0).numberValue().intValue(),
                        date.get(1).numberValue().intValue(),
                        date.get(2).numberValue().intValue());
                issuedDateFormat = DateTimeFormatter.ISO_LOCAL_DATE;
            } else if (date.has(0) && date.has(1)) {
                issuedDate = LocalDate.of(date.get(0).numberValue().intValue(),
                        date.get(1).numberValue().intValue(),
                        1);
                issuedDateFormat = DateTimeFormatter.ofPattern("yyyy-MM");
            } else if (date.has(0)) {
                issuedDate = LocalDate.of(date.get(0).numberValue().intValue(),
                        1,
                        1);
                issuedDateFormat = DateTimeFormatter.ofPattern("yyyy");
            }
            values.add(issuedDate.format(issuedDateFormat));
        }
        return values;
    }

    private JsonNode convertStringJsonToJsonNode(String json) {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode body = null;
        try {
            body = mapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.error("Unable to process json response.", e);
        }
        return body;
    }

    public void setPathToArray(String pathToArray) {
        this.pathToArray = pathToArray;
    }

}