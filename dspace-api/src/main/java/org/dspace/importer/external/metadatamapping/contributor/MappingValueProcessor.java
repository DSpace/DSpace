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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.util.SimpleMapConverter;
/**
 * This Processor allows to map text values to controlled list of vocabularies using some mapConverter
 *
 * @author Florian Gantner (florian.gantner@uni-bamberg.de)
 */
public class MappingValueProcessor implements JsonPathMetadataProcessor {
    private final static Logger log = LogManager.getLogger();
    private SimpleMapConverter converter;
    @Override
    public Collection<String> processMetadata(String value) {
        Collection<String> mappedValues = new ArrayList<>();
        mappedValues.add(converter.getValue(value));
        return mappedValues;
    }
    public void setConverter(SimpleMapConverter converter) {
        this.converter = converter;
    }
}
