/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map.Entry;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is a metadata contributor implementation which get metadata values from
 * Json names (keys).
 * The class level variable "query" must match the key's parent node,
 * or in other words the element which contains the keys.
 * 
 * For example, starting from this json:
 * {
 *   "authors": {
 *     "primary": {
 *       "Surname, Name": {
 *         "role": ["author"]
 *       },
 *       ...
 *     }
 *   }
 * }
 *
 * To get the authors, json path expression must be $.authors.primary
 * 
 * @author Pasquale Cavallo (pasquale.cavallo at 4science dot it)
 *
 */
public class SimpleJsonPathKeyMetadataContributor extends SimpleJsonPathMetadataContributor {

    private static final Logger logger = LoggerFactory.getLogger(SimpleJsonPathKeyMetadataContributor.class);

    /**
     * This method return metadata from a JSON input using json names (key) as metadata value.
     * In this method, the class level variable "query" must match the parent element which contains the keys.
     * 
     * For example, starting from the following Json:
     * {
     *   "authors": {
     *     "primary": {
     *       "Surname, Name": {
     *         "role": ["author"]
     *       }
     *     }
     *   }
     * }
     *
     * To get the authors, json path expression must be $.authors.primary
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String fullJson) {
        Collection<MetadatumDTO> metadata = new ArrayList<>();
        Collection<String> metadataValue = new ArrayList<>();
        if (metadataProcessor != null) {
            metadataValue = metadataProcessor.processMetadata(fullJson);
        } else {
            try {
                ReadContext ctx = JsonPath.parse(fullJson);
                Object innerJson = ctx.read(getQuery());
                HashMap<String, Object> jsonHashMap = (HashMap<String, Object>) innerJson;
                if (jsonHashMap != null) {
                    for (Entry<String, Object> entry : jsonHashMap.entrySet()) {
                        metadataValue.add(entry.getKey());
                    }
                }
            } catch (Exception e) {
                logger.debug("Cannot extract Alicia author using jsonpath expression " + getQuery() +
                    " from json " + fullJson);
            }
        }
        for (String value : metadataValue) {
            MetadatumDTO metadatumDto = new MetadatumDTO();
            metadatumDto.setValue(value);
            metadatumDto.setElement(getField().getElement());
            metadatumDto.setQualifier(getField().getQualifier());
            metadatumDto.setSchema(getField().getSchema());
            metadata.add(metadatumDto);
        }
        return metadata;
    }
}
