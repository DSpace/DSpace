/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.app.rest.model.CrisLayoutBoxRelationConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutBoxRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Cell;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Field;
import org.dspace.app.rest.model.CrisLayoutMetadataConfigurationRest.Row;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutFieldBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.content.EntityType;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutBoxTypes;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration tests for {@link CrisLayoutBoxConverter}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutBoxConverterIT extends AbstractControllerIntegrationTest {

    @Autowired
    private CrisLayoutBoxConverter converter;

    @Autowired
    private MetadataSchemaService mdss;

    @Autowired
    private MetadataFieldService mfss;

    @Test
    public void testMetadataBoxConversion() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        MetadataSchema schema = mdss.find(context, "dc");
        MetadataSchema oairecerif = mdss.find(context, "oairecerif");
        MetadataField title = mfss.findByElement(context, schema, "title", null);
        MetadataField language = mfss.findByElement(context, schema, "language", "iso");
        MetadataField date = mfss.findByElement(context, schema, "date", "issued");
        MetadataField subject = mfss.findByElement(context, schema, "subject", null);
        MetadataField patentno = mfss.findByElement(context, schema, "identifier", "patentno");
        MetadataField author = mfss.findByElement(context, schema, "contributor", "author");
        MetadataField affiliation = mfss.findByElement(context, oairecerif, "author", "affiliation");

        CrisLayoutField titleField = CrisLayoutFieldBuilder.createMetadataField(context, title, 0, 0, 1)
            .withLabel("Title")
            .withLabelStyle("bold")
            .withCellStyle("cell-style")
            .withValueStyle("value style")
            .withLabelAsHeading(true)
            .withValuesInline(false)
            .build();

        CrisLayoutField issuedDate = CrisLayoutFieldBuilder.createMetadataField(context, date, 0, 0, 0)
            .withLabel("Date")
            .withRowStyle("row-style")
            .withLabelAsHeading(false)
            .withValuesInline(false)
            .build();

        CrisLayoutField languageField = CrisLayoutFieldBuilder.createMetadataField(context, language, 0, 1, 0)
            .withLabel("Language")
            .withLabelAsHeading(false)
            .withValuesInline(false)
            .build();

        CrisLayoutField image = CrisLayoutFieldBuilder.createBistreamField(context, null, "ORIGINAL", 1, 0, 0)
            .withRendering("attachment")
            .withLabelAsHeading(false)
            .withValuesInline(false)
            .build();

        CrisLayoutField authorField = CrisLayoutFieldBuilder.createMetadataField(context, author, 1, 0, 1)
            .withLabel("LABEL Author")
            .withRendering("crisref")
            .withRowStyle("STYLE")
            .withNestedField(List.of(author, affiliation))
            .withValuesInline(true)
            .withLabelAsHeading(false)
            .build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, entityType, true, true)
            .withContainer(false)
            .withHeader("Box Header")
            .withShortname("box")
            .withStyle("style")
            .withType("METADATA")
            .withSecurity(LayoutSecurity.ADMINISTRATOR)
            .addMetadataSecurityField(subject)
            .addMetadataSecurityField(patentno)
            .addField(languageField)
            .addField(image)
            .addField(titleField)
            .addField(issuedDate)
            .addField(authorField)
            .build();

        context.commit();

        box = context.reloadEntity(box);

        context.restoreAuthSystemState();

        CrisLayoutBoxRest rest = converter.convert(box, Projection.DEFAULT);
        assertThat(rest, notNullValue());
        assertThat(rest.getBoxType(), is("METADATA"));
        assertThat(rest.isContainer(), is(false));
        assertThat(rest.getCollapsed(), is(true));
        assertThat(rest.getEntityType(), is("Publication"));
        assertThat(rest.getHeader(), is("Box Header"));
        assertThat(rest.getShortname(), is("box"));
        assertThat(rest.getMaxColumns(), nullValue());
        assertThat(rest.getMinor(), is(true));
        assertThat(rest.getSecurity(), is(1));
        assertThat(rest.getStyle(), is("style"));
        assertThat(rest.getMetadataSecurityFields(), containsInAnyOrder("dc.subject", "dc.identifier.patentno"));
        assertThat(rest.getConfiguration(), instanceOf(CrisLayoutMetadataConfigurationRest.class));

        CrisLayoutMetadataConfigurationRest config = (CrisLayoutMetadataConfigurationRest) rest.getConfiguration();
        assertThat(config.getRows(), hasSize(2));

        Row firstRow = config.getRows().get(0);
        assertThat(firstRow.getStyle(), is("row-style"));
        assertThat(firstRow.getCells(), hasSize(2));

        Cell firstCell = firstRow.getCells().get(0);
        assertThat(firstCell.getStyle(), is("cell-style"));
        assertThat(firstCell.getFields(), hasSize(2));

        Field firstField = firstCell.getFields().get(0);
        assertThat(firstField.getFieldType(), is("METADATA"));
        assertThat(firstField.getBitstream(), nullValue());
        assertThat(firstField.getLabel(), is("Date"));
        assertThat(firstField.getStyleLabel(), nullValue());
        assertThat(firstField.getMetadata(), is("dc.date.issued"));
        assertThat(firstField.getMetadataGroup(), nullValue());
        assertThat(firstField.getRendering(), nullValue());
        assertThat(firstField.getStyleValue(), nullValue());
        assertThat(firstField.isLabelAsHeading(), is(false));
        assertThat(firstField.isValuesInline(), is(false));

        Field secondField = firstCell.getFields().get(1);
        assertThat(secondField.getFieldType(), is("METADATA"));
        assertThat(secondField.getBitstream(), nullValue());
        assertThat(secondField.getLabel(), is("Title"));
        assertThat(secondField.getStyleLabel(), is("bold"));
        assertThat(secondField.getMetadata(), is("dc.title"));
        assertThat(secondField.getMetadataGroup(), nullValue());
        assertThat(secondField.getRendering(), nullValue());
        assertThat(secondField.getStyleValue(), is("value style"));
        assertThat(secondField.isLabelAsHeading(), is(true));
        assertThat(secondField.isValuesInline(), is(false));

        Cell secondCell = firstRow.getCells().get(1);
        assertThat(secondCell.getFields(), hasSize(1));

        Field thirdField = secondCell.getFields().get(0);
        assertThat(thirdField.getFieldType(), is("METADATA"));
        assertThat(thirdField.getBitstream(), nullValue());
        assertThat(thirdField.getLabel(), is("Language"));
        assertThat(thirdField.getStyleLabel(), nullValue());
        assertThat(thirdField.getMetadata(), is("dc.language.iso"));
        assertThat(thirdField.getMetadataGroup(), nullValue());
        assertThat(thirdField.getRendering(), nullValue());
        assertThat(thirdField.getStyleValue(), nullValue());
        assertThat(thirdField.isLabelAsHeading(), is(false));
        assertThat(thirdField.isValuesInline(), is(false));

        Row secondRow = config.getRows().get(1);
        assertThat(secondRow.getCells(), hasSize(1));

        Cell thirdCell = secondRow.getCells().get(0);
        assertThat(thirdCell.getFields(), hasSize(2));

        Field fourthField = thirdCell.getFields().get(0);
        assertThat(fourthField.getFieldType(), is("BITSTREAM"));
        assertThat(fourthField.getBitstream(), notNullValue());
        assertThat(fourthField.getBitstream().getBundle(), is("ORIGINAL"));
        assertThat(fourthField.getRendering(), is("attachment"));

        Field fifthField = thirdCell.getFields().get(1);
        assertThat(fifthField.getFieldType(), is("METADATAGROUP"));
        assertThat(fifthField.getBitstream(), nullValue());
        assertThat(fifthField.getLabel(), is("LABEL Author"));
        assertThat(fifthField.getRendering(), is("crisref"));
        assertThat(fifthField.isValuesInline(), is(true));
        assertThat(fifthField.getMetadata(), is("dc.contributor.author"));
        assertThat(fifthField.getMetadataGroup(), notNullValue());
        assertThat(fifthField.getMetadataGroup().getLeading(), is("dc.contributor.author"));

        List<Field> nestedMetadataFields = fifthField.getMetadataGroup().getElements();
        assertThat(nestedMetadataFields, hasSize(2));
        assertThat(nestedMetadataFields.get(0).getMetadata(), is("dc.contributor.author"));
        assertThat(nestedMetadataFields.get(1).getMetadata(), is("oairecerif.author.affiliation"));
    }

    @Test
    public void testRelationBoxConversion() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, entityType, true, true)
            .withContainer(false)
            .withHeader("Box Header")
            .withShortname("box")
            .withType("RELATION")
            .withSecurity(LayoutSecurity.CUSTOM_DATA)
            .build();

        context.commit();

        context.restoreAuthSystemState();

        CrisLayoutBoxRest rest = converter.convert(box, Projection.DEFAULT);
        assertThat(rest, notNullValue());
        assertThat(rest.getBoxType(), is("RELATION"));
        assertThat(rest.isContainer(), is(false));
        assertThat(rest.getCollapsed(), is(true));
        assertThat(rest.getEntityType(), is("Publication"));
        assertThat(rest.getHeader(), is("Box Header"));
        assertThat(rest.getShortname(), is("box"));
        assertThat(rest.getMaxColumns(), nullValue());
        assertThat(rest.getMinor(), is(true));
        assertThat(rest.getSecurity(), is(4));
        assertThat(rest.getStyle(), nullValue());
        assertThat(rest.getMetadataSecurityFields(), empty());
        assertThat(rest.getConfiguration(), instanceOf(CrisLayoutBoxRelationConfigurationRest.class));

        CrisLayoutBoxRelationConfigurationRest config = (CrisLayoutBoxRelationConfigurationRest) rest
            .getConfiguration();

        assertThat(config.getDiscoveryConfiguration(), is("RELATION.Publication.box"));

    }

    @Test
    public void testVersioningBoxConversion() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, entityType, true, true)
            .withContainer(true)
            .withHeader("Box Header")
            .withShortname("versioning")
            .withType(CrisLayoutBoxTypes.VERSIONING.name())
            .withSecurity(LayoutSecurity.OWNER_ONLY)
            .withMaxColumns(1)
            .build();

        context.commit();

        context.restoreAuthSystemState();

        CrisLayoutBoxRest rest = converter.convert(box, Projection.DEFAULT);
        assertThat(rest, notNullValue());
        assertThat(rest.getBoxType(), is(CrisLayoutBoxTypes.VERSIONING.name()));
        assertThat(rest.isContainer(), is(true));
        assertThat(rest.getCollapsed(), is(true));
        assertThat(rest.getEntityType(), is("Publication"));
        assertThat(rest.getHeader(), is("Box Header"));
        assertThat(rest.getShortname(), is("versioning"));
        assertThat(rest.getMaxColumns(), is(1));
        assertThat(rest.getMinor(), is(true));
        assertThat(rest.getSecurity(), is(2));
        assertThat(rest.getStyle(), nullValue());
        assertThat(rest.getMetadataSecurityFields(), empty());
        assertEquals(rest.getConfiguration(), null);
    }
}
