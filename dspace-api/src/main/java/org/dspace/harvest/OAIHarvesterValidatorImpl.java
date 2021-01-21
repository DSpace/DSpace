/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.dspace.harvest.model.OAIHarvesterValidationResult.invalid;
import static org.dspace.harvest.model.OAIHarvesterValidationResult.valid;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.commons.lang3.StringUtils;
import org.dspace.harvest.model.OAIHarvesterValidationResult;
import org.dspace.harvest.service.OAIHarvesterValidator;
import org.dspace.services.ConfigurationService;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.SAXException;

/**
 * Implementation of {@link OAIHarvesterValidator} that validate the given
 * element using an xsd.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterValidatorImpl implements OAIHarvesterValidator {

    private ConfigurationService configurationService;

    private static final Map<String, Schema> SCHEMA_CACHE = new HashMap<>();

    @Autowired
    public OAIHarvesterValidatorImpl(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @Override
    public OAIHarvesterValidationResult validate(Element element, HarvestedCollection harvestRow) {
        return getXsdPath(harvestRow)
            .map(xsdPath -> validate(element, xsdPath))
            .orElseGet(() -> valid());
    }

    private OAIHarvesterValidationResult validate(Element element, String xsdPath) {
        try {
            Schema schema = getSchema(xsdPath);
            schema.newValidator().validate(new JDOMSource(element));
        } catch (IOException | SAXException e) {
            return invalid(e.getMessage());
        }
        return valid();
    }

    private Schema getSchema(String xsdPath) throws SAXException {
        Schema schema = SCHEMA_CACHE.get(xsdPath);
        if (schema == null) {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            schema = factory.newSchema(new File(xsdPath));
            SCHEMA_CACHE.put(xsdPath, schema);
        }
        return schema;
    }

    private Optional<String> getXsdPath(HarvestedCollection harvestRow) {

        String validationDirectory = configurationService.getProperty("oai.harvester.validation-dir");
        if (StringUtils.isBlank(validationDirectory)) {
            return Optional.empty();
        }

        String metadataConfig = harvestRow.getHarvestMetadataConfig();
        String xsdName = configurationService.getProperty("oai.harvester.validation." + metadataConfig + ".xsd");
        if (StringUtils.isBlank(xsdName)) {
            return Optional.empty();
        }

        return Optional.of(new File(validationDirectory, xsdName).getAbsolutePath());
    }

}
