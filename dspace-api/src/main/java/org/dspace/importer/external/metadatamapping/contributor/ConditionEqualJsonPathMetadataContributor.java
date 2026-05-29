/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.metadatamapping.contributor;

import java.util.Collection;
import java.util.Collections;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * A metadata contributor that extracts a value from JSON using a {@link JsonPathMetadataProcessor}
 * and compares it against a predefined right operand. If the extracted value matches the right operand,
 * the specified {@link SimpleJsonPathMetadataContributor} is used to extract additional metadata.
 * Otherwise, an empty collection is returned.
 *
 * This class extends {@link SimpleJsonPathMetadataContributor} and allows conditional metadata extraction
 * based on JSON path processing.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ConditionEqualJsonPathMetadataContributor extends SimpleJsonPathMetadataContributor {

    private static final Logger log = LogManager.getLogger(ConditionEqualJsonPathMetadataContributor.class);


    private JsonPathMetadataProcessor leftOperandProcessor;

    private String rightOperand;

    private SimpleJsonPathMetadataContributor metadatumContributor;

    /**
     * Extracts metadata from the provided JSON string. The extraction process follows these steps:
     * <ol>
     *     <li>Uses {@link JsonPathMetadataProcessor} to extract a value from the JSON.</li>
     *     <li>Compares the extracted value with the predefined right operand.</li>
     *     <li>If they match, the {@link SimpleJsonPathMetadataContributor} is used to extract and return metadata.</li>
     *     <li>If they do not match, an empty collection is returned.</li>
     * </ol>
     *
     * @param json The JSON string to process.
     * @return A collection of {@link MetadatumDTO} if the condition is met; otherwise, an empty collection.
     */
    @Override
    public Collection<MetadatumDTO> contributeMetadata(String json) {
        Collection<String> leftOperands = leftOperandProcessor.processMetadata(json);
        if (leftOperands == null || leftOperands.size() != 1) {
            log.debug("No left operands found in json path: {}", json);
            return Collections.emptyList();
        }

        if (rightOperand.equals(leftOperands.iterator().next())) {
            return metadatumContributor.contributeMetadata(json);
        }

        log.debug("No matching condition found in json path: {}", json);
        return Collections.emptyList();
    }

    /**
     * Sets the {@link JsonPathMetadataProcessor} responsible for extracting the left operand value from the JSON.
     *
     * @param leftOperandProcessor The JSON path processor used for extraction.
     */
    public void setLeftOperandProcessor(
        JsonPathMetadataProcessor leftOperandProcessor) {
        this.leftOperandProcessor = leftOperandProcessor;
    }

    /**
     * Sets the right operand value that the extracted JSON value must match for metadata extraction to proceed.
     *
     * @param rightOperand The expected value to compare against.
     */
    public void setRightOperand(String rightOperand) {
        this.rightOperand = rightOperand;
    }

    /**
     * Sets the {@link SimpleJsonPathMetadataContributor} responsible for extracting metadata if the condition is met.
     *
     * @param metadatumContributor The metadata contributor to use upon a successful match.
     */
    public void setMetadatumContributor(
        SimpleJsonPathMetadataContributor metadatumContributor) {
        this.metadatumContributor = metadatumContributor;
    }
}
