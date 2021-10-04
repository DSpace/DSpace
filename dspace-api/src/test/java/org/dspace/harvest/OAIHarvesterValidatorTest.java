/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.dspace.harvest.model.OAIHarvesterValidationResult;
import org.dspace.services.ConfigurationService;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link OAIHarvesterValidatorImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class OAIHarvesterValidatorTest {

    private static final String OAI_PMH_CERIF_DIR_PATH = "./target/testing/dspace/assetstore/oai-pmh/cerif";
    private static final String VALIDATION_DIR = OAI_PMH_CERIF_DIR_PATH + "/validation/";
    private static final String CERIF_XSD_NAME = "openaire-cerif-profile.xsd";

    @Mock
    private ConfigurationService configurationService;

    @InjectMocks
    private OAIHarvesterValidatorImpl validator;

    private SAXBuilder builder = new SAXBuilder();

    @Test
    public void testPersonValidationWithoutErrors() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);
        when(configurationService.getProperty("oai.harvester.validation.cerif.xsd")).thenReturn(CERIF_XSD_NAME);

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "sample-person.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(true));
        assertThat(validationResult.getMessages(), empty());
    }

    @Test
    public void testPersonValidationWithSingleError() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);
        when(configurationService.getProperty("oai.harvester.validation.cerif.xsd")).thenReturn(CERIF_XSD_NAME);

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "invalid-person.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(false));
        assertThat(validationResult.getMessages(), hasSize(1));
        assertThat(validationResult.getMessages(), hasItem(containsString(
            "Invalid content was found starting with element "
                + "'{\"https://www.openaire.eu/cerif-profile/1.1/\":PersonName}'")));
    }

    @Test
    public void testPublicationValidationWithoutErrors() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);
        when(configurationService.getProperty("oai.harvester.validation.cerif.xsd")).thenReturn(CERIF_XSD_NAME);

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "valid-publication.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(true));
        assertThat(validationResult.getMessages(), empty());
    }

    @Test
    public void testPublicationValidationWithSingleError() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);
        when(configurationService.getProperty("oai.harvester.validation.cerif.xsd")).thenReturn(CERIF_XSD_NAME);

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "publication-with-wrong-order.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(false));
        assertThat(validationResult.getMessages(), hasSize(1));

        assertThat(validationResult.getMessages(), hasItem(containsString(
            "Invalid content was found starting with element "
                + "'{\"https://www.openaire.eu/cerif-profile/1.1/\":StartPage}'")));
    }

    @Test
    public void testPublicationValidationWithManyErrors() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);
        when(configurationService.getProperty("oai.harvester.validation.cerif.xsd")).thenReturn(CERIF_XSD_NAME);

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "invalid-publication.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(false));
        assertThat(validationResult.getMessages(), hasSize(4));

        assertThat(validationResult.getMessages(), hasItem(containsString(
            "Element 'oai_cerif:PartOf' cannot have character [children], "
                + "because the type's content type is element-only.")));

        assertThat(validationResult.getMessages(), hasItem(containsString(
            "The content of element 'oai_cerif:PartOf' is not complete.")));

        assertThat(validationResult.getMessages(), hasItem(containsString(
            "Invalid content was found starting with element "
                + "'{\"https://www.openaire.eu/cerif-profile/1.1/\":PublicationDate}'")));

        assertThat(validationResult.getMessages(), hasItem(containsString(
            "Invalid content was found starting with element "
                + "'{\"https://www.openaire.eu/cerif-profile/1.1/\":Type}'")));
    }

    @Test
    public void testValidationWithoutValidationDirectoryConfigured() {
        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "invalid-person.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(true));
        assertThat(validationResult.getMessages(), empty());
    }

    @Test
    public void testValidationWithoutCerifXsdConfigured() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "publication-with-wrong-order.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(true));
        assertThat(validationResult.getMessages(), empty());
    }

    @Test
    public void testValidationWithCerifXsdNotFound() {

        when(configurationService.getProperty("oai.harvester.validation-dir")).thenReturn(VALIDATION_DIR);
        when(configurationService.getProperty("oai.harvester.validation.cerif.xsd")).thenReturn("wrong.xsd");

        Element record = readDocument(OAI_PMH_CERIF_DIR_PATH, "publication-with-wrong-order.xml");
        HarvestedCollection harvestRow = buildHarvestedCollection("cerif");

        OAIHarvesterValidationResult validationResult = validator.validate(record, harvestRow);
        assertThat(validationResult.isValid(), is(true));
        assertThat(validationResult.getMessages(), empty());
    }

    private HarvestedCollection buildHarvestedCollection(String metadataConfig) {
        HarvestedCollection harvestedCollection = mock(HarvestedCollection.class);
        when(harvestedCollection.getHarvestMetadataConfig()).thenReturn(metadataConfig);
        return harvestedCollection;
    }

    private Element readDocument(String dir, String name) {
        try (InputStream inputStream = new FileInputStream(new File(dir, name))) {
            return builder.build(inputStream).getRootElement();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
