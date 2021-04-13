/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.dspace.harvest.model.OAIHarvesterValidationResult.buildFromException;
import static org.dspace.harvest.model.OAIHarvesterValidationResult.buildFromExceptions;
import static org.dspace.harvest.model.OAIHarvesterValidationResult.valid;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.XMLConstants;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.dspace.harvest.model.OAIHarvesterValidationResult;
import org.dspace.harvest.service.OAIHarvesterValidator;
import org.dspace.services.ConfigurationService;
import org.jdom.Element;
import org.jdom.transform.JDOMSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implementation of {@link OAIHarvesterValidator} that validate the given
 * element using an xsd.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterValidatorImpl implements OAIHarvesterValidator {

    private static final Map<String, Schema> SCHEMA_CACHE = new HashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(OAIHarvesterValidatorImpl.class);

    private ConfigurationService configurationService;

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
            Validator validator = schema.newValidator();

            CustomErrorHandler errorHandler = new CustomErrorHandler();
            validator.setErrorHandler(errorHandler);

            validator.validate(new JDOMSource(element));

            return buildFromExceptions(errorHandler.getExceptions());

        } catch (SAXException | IOException e) {
            return buildFromException(e);
        }

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
            LOGGER.warn("Harvest validation enabled but no oai.harvester.validation-dir property configured");
            return Optional.empty();
        }

        String metadataConfig = harvestRow.getHarvestMetadataConfig();
        String xsdNameProperty = "oai.harvester.validation." + metadataConfig + ".xsd";

        String xsdName = configurationService.getProperty(xsdNameProperty);
        if (StringUtils.isBlank(xsdName)) {
            LOGGER.warn("Harvest validation enabled but no " + xsdNameProperty + " property configured");
            return Optional.empty();
        }

        File xsdFile = new File(validationDirectory, xsdName);
        if (!xsdFile.exists()) {
            LOGGER.warn("Harvest validation enabled but no xsd found on path: " + xsdFile.getPath());
            return Optional.empty();
        }

        return Optional.of(xsdFile.getAbsolutePath());
    }

    private static class CustomErrorHandler implements ErrorHandler {

        private final List<Exception> exceptions;

        public CustomErrorHandler() {
            this.exceptions = new ArrayList<>();
        }

        @Override
        public void warning(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        @Override
        public void error(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        @Override
        public void fatalError(SAXParseException exception) throws SAXException {
            exceptions.add(exception);
        }

        public List<Exception> getExceptions() {
            return exceptions;
        }

    }

}
