/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.script;

import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.LambdaMatcher.matches;
import static org.dspace.layout.LayoutSecurity.OWNER_ONLY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CrisLayoutBoxBuilder;
import org.dspace.builder.CrisLayoutTabBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.content.EntityType;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutCell;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldBitstream;
import org.dspace.layout.CrisLayoutRow;
import org.dspace.layout.CrisLayoutTab;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.factory.CrisLayoutServiceFactory;
import org.dspace.layout.script.service.CrisLayoutToolValidator;
import org.dspace.layout.service.CrisLayoutTabService;
import org.junit.After;
import org.junit.Test;

/**
 * Integration tests for {@link CrisLayoutToolScript}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CrisLayoutToolScriptIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/layout/script";

    private CrisLayoutTabService tabService = CrisLayoutServiceFactory.getInstance().getTabService();

    @After
    public void after() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        List<CrisLayoutTab> tabs = tabService.findAll(context);
        for (CrisLayoutTab tab : tabs) {
            tabService.delete(context, tab);
        }
        context.restoreAuthSystemState();
    }

    @Test
    public void testConfigurationToolFile() throws Exception {

        context.turnOffAuthorisationSystem();
        List.of("Publication", "Person", "OrgUnit", "Patent", "Journal", "Event",
            "Equipment", "Funding", "Product", "Project").forEach(this::createEntityType);
        context.restoreAuthSystemState();

        assertThat(tabService.findAll(context), empty());

        String fileLocation = new File(getDspaceDir(), "etc/conftool/cris-layout-configuration.xls").getAbsolutePath();
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());

        assertThat(tabService.findAll(context), not(empty()));

    }

    @Test
    public void testWithEmptyFile() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("empty.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getInfoMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(7));
        assertThat(errorMessages, containsInAnyOrder(
            "The tab sheet is missing",
            "The box sheet is missing",
            "The tab2box sheet is missing",
            "The box2metadata sheet is missing",
            "The metadatagroups sheet is missing",
            "The boxpolicy sheet is missing",
            "The tabpolicy sheet is missing"));
    }

    @Test
    public void testWithEmptySheetsFile() throws InstantiationException, IllegalAccessException {

        String fileLocation = getXlsFilePath("empty-sheets.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getInfoMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(41));
        assertThat(errorMessages, containsInAnyOrder(
            "The sheet tab has no ENTITY column",
            "The sheet tab has no LEADING column",
            "The sheet tab has no PRIORITY column",
            "The sheet tab has no SECURITY column",
            "The sheet tab has no SHORTNAME column",
            "The sheet tab has no LABEL column",
            "The sheet box has no TYPE column",
            "The sheet box has no ENTITY column",
            "The sheet box has no COLLAPSED column",
            "The sheet box has no CONTAINER column",
            "The sheet box has no MINOR column",
            "The sheet box has no SECURITY column",
            "The sheet box has no SHORTNAME column",
            "The sheet tab2box has no ENTITY column",
            "The sheet tab2box has no TAB column",
            "The sheet tab2box has no BOXES column",
            "The sheet tab2box has no ROW_STYLE column",
            "The sheet box2metadata has no ROW column",
            "The sheet box2metadata has no CELL column",
            "The sheet box2metadata has no LABEL_AS_HEADING column",
            "The sheet box2metadata has no VALUES_INLINE column",
            "The sheet box2metadata has no BUNDLE column",
            "The sheet box2metadata has no VALUE column",
            "The sheet box2metadata has no ROW_STYLE column",
            "The sheet box2metadata has no FIELDTYPE column",
            "The sheet box2metadata has no METADATA column",
            "The sheet box2metadata has no ENTITY column",
            "The sheet box2metadata has no BOX column",
            "The sheet metadatagroups has no ENTITY column",
            "The sheet metadatagroups has no METADATA column",
            "The sheet metadatagroups has no PARENT column",
            "The sheet boxpolicy has no METADATA column",
            "The sheet boxpolicy has no ENTITY column",
            "The sheet boxpolicy has no SHORTNAME column",
            "The sheet tabpolicy has no METADATA column",
            "The sheet tabpolicy has no ENTITY column",
            "The sheet tabpolicy has no SHORTNAME column",
            "The sheet tabpolicy has no GROUP column",
            "The sheet boxpolicy has no GROUP column",
            "The sheet box2metadata has no RENDERING column",
            "The sheet metadatagroups has no RENDERING column"));
    }

    @Test
    public void testWithInvalidFile() throws InstantiationException, IllegalAccessException {

        context.turnOffAuthorisationSystem();
        createEntityType("Person");
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("invalid-tabs.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getInfoMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, containsInAnyOrder(
            "The tab contains an unknown entity type 'Publication' at row 3",
            "The LEADING value specified on the row 2 of sheet tab is not valid: u. Allowed values: [yes, y, no, n]",
            "The SECURITY value specified on the row 1 of sheet tab is not valid: INVALID. Allowed values: "
                + CrisLayoutToolValidator.ALLOWED_SECURITY_VALUES.toString(),
            "The sheet box contains an invalid type UNKNOWN at row 3",
            "The box contains an unknown entity type 'Publication' at row 5",
            "The box contains an unknown entity type 'Publication' at row 6",
            "The COLLAPSED value specified on the row 3 of sheet box is not valid: a. Allowed values: [yes, y, no, n]",
            "The COLLAPSED value specified on the row 5 of sheet box is empty. Allowed values: [yes, y, no, n]",
            "The CONTAINER value specified on the row 5 of sheet box is not valid: z. Allowed values: [yes, y, no, n]",
            "The MINOR value specified on the row 1 of sheet box is not valid: b. Allowed values: [yes, y, no, n]",
            "The SECURITY value specified on the row 5 of sheet box is not valid: INVALID. Allowed values: "
                + CrisLayoutToolValidator.ALLOWED_SECURITY_VALUES.toString(),
            "The Tab with name unkownTab and entity type Person in the row 1 of sheet tab2box"
                + " is not present in the tab sheet",
            "The Box with name unknownBox1 and entity type Person in the row 4 of sheet tab2box"
                + " is not present in the box sheet",
            "The Box with name unknownBox2 and entity type Publication in the row 6 of sheet tab2box"
                + " is not present in the box sheet",
            "The Box with name unknownBox3 and entity type Publication in the row 7 of sheet tab2box"
                + " is not present in the box sheet",
            "Row style conflict between rows 3 and rows [4] of sheet tab2box",
            "The ROW value specified on the row 3 of sheet box2metadata is not valid: X. "
                + "Allowed values: integer values",
            "The CELL value specified on the row 6 of sheet box2metadata is not valid: Y. "
                + "Allowed values: integer values",
            "The LABEL_AS_HEADING value specified on the row 6 of sheet box2metadata is not valid: a. "
                + "Allowed values: [yes, y, no, n]",
            "The VALUES_INLINE value specified on the row 7 of sheet box2metadata is not valid: invalid. "
                + "Allowed values: [yes, y, no, n]",
            "The box2metadata contains an unknown field type INVALID_TYPE at row 2",
            "The box2metadata contains an empty field type at row 3",
            "The box2metadata contains an empty field type at row 4",
            "The box2metadata contains a METADATA field METADATA with BUNDLE or VALUE set at row 5",
            "The box2metadata contains a METADATA field METADATA with BUNDLE or VALUE set at row 9",
            "The box2metadata contains a METADATA field METADATA with BUNDLE or VALUE set at row 11",
            "The box2metadata contains a BITSTREAM field  without BUNDLE at row 13",
            "The box2metadata contains an unknown metadata field invalid.metadata.field at row 7",
            "The box with name unknown and entity type Publication in the row 10 of sheet box2metadata"
                + " is not present in the box sheet",
            "The sheet boxpolicy has no GROUP column",
            "The sheet tabpolicy has no GROUP column",
            "The sheet box2metadata contains an invalid RENDERING type at row 3: "
                + "Rendering named thumbnail is not supported by field type 'METADATA'"));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWithValidLayout() throws InstantiationException, IllegalAccessException, SQLException {
        context.turnOffAuthorisationSystem();
        createEntityType("Publication");
        createEntityType("Person");
        GroupBuilder.createGroup(context)
            .withName("Researchers")
            .build();
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("valid-layout-with-3-tabs.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> infoMessages = handler.getInfoMessages();
        assertThat(infoMessages, hasSize(6));
        assertThat(infoMessages.get(0), containsString("The given workbook is valid. Proceed with the import"));
        assertThat(infoMessages.get(1), containsString("The workbook has been parsed correctly, "
            + "found 3 tabs to import"));
        assertThat(infoMessages.get(2), containsString("Proceed with the clearing of the previous layout"));
        assertThat(infoMessages.get(3), containsString("Found 0 tabs to delete"));
        assertThat(infoMessages.get(4), containsString("The previous layout has been deleted, "
            + "proceed with the import of the new configuration"));
        assertThat(infoMessages.get(5), containsString("Import completed successfully"));

        assertThat(tabService.findAll(context), hasSize(3));

        List<CrisLayoutTab> personTabs = tabService.findByEntityType(context, "Person", null);
        assertThat(personTabs, hasSize(2));

        CrisLayoutTab firstPersonTab = personTabs.get(0);
        assertThatTabHas(firstPersonTab, "details", "Person", "Profile", 1, 0, false, 0, LayoutSecurity.PUBLIC);

        CrisLayoutRow firstPersonTabRow = firstPersonTab.getRows().get(0);
        assertThat(firstPersonTabRow.getStyle(), is("person-details-style"));
        assertThat(firstPersonTabRow.getCells(), hasSize(1));

        CrisLayoutCell firstPersonTabCell = firstPersonTabRow.getCells().get(0);
        assertThat(firstPersonTabCell.getStyle(), nullValue());
        assertThat(firstPersonTabCell.getBoxes(), hasSize(2));

        CrisLayoutBox profileBox = firstPersonTabCell.getBoxes().get(0);
        assertThatBoxHas(profileBox, "researcherprofile", "METADATA", "Person", "Profile", 6,
            0, false, false, false, "profile-style", LayoutSecurity.PUBLIC);

        List<CrisLayoutField> profileFields = profileBox.getLayoutFields();

        CrisLayoutField profilePicture = profileFields.get(0);
        assertThatBitstreamFieldHas(profilePicture, null, "row", null, 1, 1, 0, "thumbnail", 0,
            "dc.type", "font-weight-bold col-4", null, false, false, "ORIGINAL", "personal picture");

        CrisLayoutField profileTitle = profileFields.get(1);
        assertThatMetadataFieldHas(profileTitle, "Preferred name", "row", "title-cell-style", 1, 2, 1, null, 0,
            "dc.title", "font-weight-bold", "bold", false, false);

        CrisLayoutField profileName = profileFields.get(2);
        assertThatMetadataFieldHas(profileName, "Official Name", "row", null, 1, 2, 2, null, 0,
            "crisrp.name", null, null, false, true);

        CrisLayoutField profileEmail = profileFields.get(3);
        assertThatMetadataFieldHas(profileEmail, "Email", "row", null, 1, 2, 3, "crisref.email", 0,
            "person.email", null, null, true, true);

        CrisLayoutField profileDescription = profileFields.get(4);
        assertThatMetadataFieldHas(profileDescription, "Biography", "row", null, 2, 1, 4, "longtext", 0,
            "dc.description.abstract", null, null, false, false);

        CrisLayoutField profileAffiliation = profileFields.get(5);
        assertThatMetadataFieldHas(profileAffiliation, "Affiliation", "row", null, 3, 1, 5, "table", 2,
            "oairecerif.person.affiliation", null, null, false, false);

        CrisMetadataGroup profileNestedAffiliationRole = profileAffiliation.getCrisMetadataGroupList().get(0);
        assertThat(profileNestedAffiliationRole.getLabel(), is("Role"));
        assertThat(profileNestedAffiliationRole.getMetadataField().toString('.'), is("oairecerif.affiliation.role"));
        assertThat(profileNestedAffiliationRole.getPriority(), is(0));
        assertThat(profileNestedAffiliationRole.getRendering(), is("text"));
        assertThat(profileNestedAffiliationRole.getStyleLabel(), is("col"));
        assertThat(profileNestedAffiliationRole.getStyleValue(), is("col"));

        CrisMetadataGroup profileNestedAffiliation = profileAffiliation.getCrisMetadataGroupList().get(1);
        assertThat(profileNestedAffiliation.getLabel(), is("Organisation"));
        assertThat(profileNestedAffiliation.getMetadataField().toString('.'), is("oairecerif.person.affiliation"));
        assertThat(profileNestedAffiliation.getPriority(), is(1));
        assertThat(profileNestedAffiliation.getRendering(), is("crisref"));
        assertThat(profileNestedAffiliation.getStyleLabel(), is("label-style"));
        assertThat(profileNestedAffiliation.getStyleValue(), is("value-style"));

        CrisLayoutBox profileSecuredBox = firstPersonTabCell.getBoxes().get(1);
        assertThatBoxHas(profileSecuredBox, "secured", "METADATA", "Person", "Secured infos", 1,
            0, false, false, true, null, LayoutSecurity.OWNER_ONLY);
        assertThat(profileSecuredBox.getLayoutFields().get(0).getMetadataField().toString('.'),
            is("oairecerif.person.gender"));

        CrisLayoutTab secondPersonTab = personTabs.get(1);
        assertThatTabHas(secondPersonTab, "publications", "Person", "Publications", 2, 1, false, 2, OWNER_ONLY);
        assertThat(secondPersonTab.getMetadataSecurityFields(),
            contains(matches(metadataField -> metadataField.toString('.').equals("dspace.policy.eperson"))));

        CrisLayoutRow secondPersonTabFirstRow = secondPersonTab.getRows().get(0);
        assertThat(secondPersonTabFirstRow.getStyle(), nullValue());
        assertThat(secondPersonTabFirstRow.getCells(), hasSize(1));
        assertThat(secondPersonTabFirstRow.getCells().get(0).getStyle(), nullValue());
        assertThat(secondPersonTabFirstRow.getCells().get(0).getBoxes(), hasSize(1));

        CrisLayoutBox profileNameCardBox = secondPersonTabFirstRow.getCells().get(0).getBoxes().get(0);
        assertThatBoxHas(profileNameCardBox, "namecard", "METADATA", "Person", "Person", 2,
            0, true, false, false, null, LayoutSecurity.PUBLIC);

        CrisLayoutRow secondPersonTabSecondRow = secondPersonTab.getRows().get(1);
        assertThat(secondPersonTabSecondRow.getStyle(), is("person-pub-style"));
        assertThat(secondPersonTabSecondRow.getCells(), hasSize(1));

        CrisLayoutCell secondPersonTabSecondRowCell = secondPersonTabSecondRow.getCells().get(0);
        assertThat(secondPersonTabSecondRowCell.getStyle(), nullValue());
        assertThat(secondPersonTabSecondRowCell.getBoxes(), hasSize(1));

        CrisLayoutBox profileResearchoutputsBox = secondPersonTabSecondRowCell.getBoxes().get(0);
        assertThatBoxHas(profileResearchoutputsBox, "researchoutputs", "RELATION", "Person", "Publications", 0,
            1, false, false, true, "researchoutputs-style", LayoutSecurity.PUBLIC);
        assertThat(profileResearchoutputsBox.getGroupSecurityFields(),
                   contains(matches(groupField -> groupField.getName().equals("Researchers"))));

        List<CrisLayoutTab> publicationTabs = tabService.findByEntityType(context, "Publication", null);
        assertThat(publicationTabs, hasSize(1));

        CrisLayoutTab publicationTab = publicationTabs.get(0);
        assertThatTabHas(publicationTab, "details", "Publication", "Details", 1, 1, true, 0,
            LayoutSecurity.CUSTOM_DATA_AND_ADMINISTRATOR);
        assertThat(publicationTab.getGroupSecurityFields(),
                   contains(matches(groupField -> groupField.getName().equals("Researchers"))));

        CrisLayoutRow publicationTabRow = publicationTab.getRows().get(0);
        assertThat(publicationTabRow.getStyle(), is("test-style"));
        assertThat(publicationTabRow.getCells(), hasSize(2));

        CrisLayoutCell publicationTabFirstCell = publicationTabRow.getCells().get(0);
        assertThat(publicationTabFirstCell.getStyle(), nullValue());
        assertThat(publicationTabFirstCell.getBoxes(), hasSize(1));

        CrisLayoutBox publicationDetailsBox = publicationTabFirstCell.getBoxes().get(0);
        assertThatBoxHas(publicationDetailsBox, "details", "METADATA", "Publication", "Details", 4, 0,
            true, false, true, null, LayoutSecurity.PUBLIC);

        CrisLayoutField publicationTitleField = publicationDetailsBox.getLayoutFields().get(0);
        assertThatMetadataFieldHas(publicationTitleField, "Title", "container row", null, 1, 1, 0, null,
            0, "dc.title", null, null, false, false);

        CrisLayoutField publicationIsPartOfField = publicationDetailsBox.getLayoutFields().get(1);
        assertThatMetadataFieldHas(publicationIsPartOfField, "Journal", "container row", null, 1, 2, 1, null,
            0, "dc.relation.ispartof", null, null, false, false);

        CrisLayoutField publicationAuthorsField = publicationDetailsBox.getLayoutFields().get(2);
        assertThatMetadataFieldHas(publicationAuthorsField, "Author(s)", "container row", null, 2, 1, 2, "inline",
            2, "dc.contributor.author", null, null, false, false);

        CrisLayoutField publicationAttachmentField = publicationDetailsBox.getLayoutFields().get(3);
        assertThatBitstreamFieldHas(publicationAttachmentField, "File(s)", "container row", null, 7, 1, 3,
            "attachment", 0, null, "font-weight-bold col-4", null, false, false, "ORIGINAL", null);

        CrisLayoutCell publicationTabSecondCell = publicationTabRow.getCells().get(1);
        assertThat(publicationTabSecondCell.getStyle(), is("cell-1-style"));
        assertThat(publicationTabSecondCell.getBoxes(), hasSize(1));

    }

    @Test
    public void testWithMissingMandatoryMetadataFields() throws InstantiationException, IllegalAccessException {

        context.turnOffAuthorisationSystem();
        createEntityType("Person");
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("missing-mandatory-metadata-fields.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getInfoMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasSize(5));
        assertThat(errorMessages, containsInAnyOrder(
            "The box2metadata contains an empty metadata field at row 3",
            "The box2metadata contains an empty metadata field at row 6",
            "The metadatagroups contains an empty metadata field at row 2",
            "The boxpolicy at row 1 contains invalid values for METADATA/GROUP column.",
            "The tabpolicy at row 0 contains invalid values for METADATA/GROUP column."));
    }

    @Test
    public void testWithMissingGroupColumn() throws InstantiationException, IllegalAccessException {
        String fileLocation = getXlsFilePath("missing-group-column.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, hasItems(
            "The sheet boxpolicy has no GROUP column",
            "The sheet tabpolicy has no GROUP column"));
    }

    /*
     * Each row must contain ONE value for METADATA column OR GROUP column.
     * Both values missing or both values present is considered invalid.
     */
    @Test
    public void testWithIllegalMetadataAndGroupValues() throws InstantiationException, IllegalAccessException {
        context.turnOffAuthorisationSystem();
        createEntityType("Publication");
        createEntityType("Person");
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("invalid-metadata-and-group-column.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, containsInAnyOrder(
            "The boxpolicy at row 2 contains invalid values for METADATA/GROUP column.",
            "The tabpolicy at row 0 contains invalid values for METADATA/GROUP column."));
    }

    /*
     * Test for group validation, the 'xls' file contains a group value
     * that does not exist.
     */
    @Test
    public void testWithInvalidGroupValue() throws InstantiationException, IllegalAccessException {
        context.turnOffAuthorisationSystem();
        createEntityType("Publication");
        createEntityType("Person");
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("invalid-group-value.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);

        List<String> errorMessages = handler.getErrorMessages();
        assertThat(errorMessages, containsInAnyOrder(
            "The boxpolicy contains an unknown group field: 'Researchers' at row 2",
            "The tabpolicy contains an unknown group field: 'Researchers' at row 0"));
    }

    private void assertThatBitstreamFieldHas(CrisLayoutField field, String label, String rowStyle, String cellStyle,
        int row, int cell, int priority, String rendering, int metadataGroupSize, String metadataField,
        String labelStyle, String valueStyle, boolean labelAsHeading, boolean valuesInline, String bundle,
        String value) {

        assertThat(field, instanceOf(CrisLayoutFieldBitstream.class));
        assertThat(((CrisLayoutFieldBitstream) field).getBundle(), is(bundle));
        assertThat(((CrisLayoutFieldBitstream) field).getMetadataValue(), is(value));
        assertThatFieldHas(field, label, rowStyle, cellStyle, row, cell, priority, rendering, metadataGroupSize,
            metadataField, labelStyle, valueStyle, labelAsHeading, valuesInline);

    }

    @Test
    public void testOldLayoutCleanup() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType publicationType = createEntityType("Publication");
        createEntityType("Person");

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
            .withHeader("New Box Header - priority")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withShortname("Shortname")
            .withStyle("STYLE")
            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, publicationType, 0)
            .withShortName("New Tab shortname")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .withLeading(true)
            .addBoxIntoNewRow(box)
            .build();

        GroupBuilder.createGroup(context)
            .withName("Researchers")
            .build();

        context.restoreAuthSystemState();

        assertThat(tabService.findAll(context), hasSize(1));

        String fileLocation = getXlsFilePath("valid-layout-with-3-tabs.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        assertThat(tabService.findAll(context), hasSize(3));
        assertThat(context.reloadEntity(tab), nullValue());
        assertThat(context.reloadEntity(box), nullValue());

    }

    @Test
    public void testNoCleanUpOccursIfNewLayoutIsInvalid() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType publicationType = createEntityType("Publication");
        createEntityType("Person");

        CrisLayoutBox box = CrisLayoutBoxBuilder.createBuilder(context, publicationType, false, false)
            .withHeader("New Box Header - priority")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withShortname("Shortname")
            .withStyle("STYLE")
            .build();

        CrisLayoutTab tab = CrisLayoutTabBuilder.createTab(context, publicationType, 0)
            .withShortName("New Tab shortname")
            .withSecurity(LayoutSecurity.PUBLIC)
            .withHeader("New Tab header")
            .withLeading(true)
            .addBoxIntoNewRow(box)
            .build();

        context.restoreAuthSystemState();
        context.commit();

        assertThat(tabService.findAll(context), hasSize(1));

        String fileLocation = getXlsFilePath("invalid-tabs.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), not(empty()));
        assertThat(handler.getWarningMessages(), empty());

        assertThat(tabService.findAll(context), hasSize(1));
        assertThat(context.reloadEntity(tab), notNullValue());
        assertThat(context.reloadEntity(box), notNullValue());

    }

    @Test
    public void testWithInvalidRendering() throws Exception {

        context.turnOffAuthorisationSystem();
        createEntityType("Publication");
        createEntityType("Person");
        context.restoreAuthSystemState();

        String fileLocation = getXlsFilePath("invalid-rendering.xls");
        String[] args = new String[] { "cris-layout-tool", "-f", fileLocation };
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getInfoMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());
        assertThat(handler.getErrorMessages(), containsInAnyOrder(
            "The sheet box2metadata contains an invalid RENDERING type at row 1: "
                + "Rendering named thumbnail is not supported by field type 'METADATA'",
            "The sheet box2metadata contains an unknown RENDERING type invalid at row 3",
            "The sheet box2metadata contains an invalid RENDERING type at row 7: "
                + "Rendering named longtext is not supported by field type 'BITSTREAM'",
            "The sheet box2metadata contains an invalid RENDERING type at row 8: "
                + "Rendering named table is not supported by field type 'METADATA'",
            "The sheet box2metadata contains an invalid RENDERING type at row 10: "
                + "Rendering named valuepair don't supports the configured sub type",
            "The sheet box2metadata contains an invalid RENDERING type at row 14: "
                + "Rendering named attachment don't supports sub types",
            "The sheet metadatagroups contains an invalid RENDERING type at row 5: "
                + "Rendering named identifier requires a sub type",
            "The sheet metadatagroups contains an invalid RENDERING type at row 6: "
                + "Rendering named identifier don't supports the configured sub type"));

    }

    private void assertThatMetadataFieldHas(CrisLayoutField field, String label, String rowStyle, String cellStyle,
        int row, int cell, int priority, String rendering, int metadataGroupSize, String metadataField,
        String labelStyle, String valueStyle, boolean labelAsHeading, boolean valuesInline) {

        assertThatFieldHas(field, label, rowStyle, cellStyle, row, cell, priority, rendering, metadataGroupSize,
            metadataField, labelStyle, valueStyle, labelAsHeading, valuesInline);

    }

    private void assertThatFieldHas(CrisLayoutField field, String label, String rowStyle, String cellStyle,
        int row, int cell, int priority, String rendering, int metadataGroupSize, String metadataField,
        String labelStyle, String valueStyle, boolean labelAsHeading, boolean valuesInline) {

        assertThat(field.getLabel(), is(label));
        assertThat(field.getRow(), is(row));
        assertThat(field.getCell(), is(cell));
        assertThat(field.getRowStyle(), is(rowStyle));
        assertThat(field.getCellStyle(), is(cellStyle));
        assertThat(field.getCrisMetadataGroupList(), hasSize(metadataGroupSize));
        if (metadataField != null) {
            assertThat(field.getMetadataField().toString('.'), is(metadataField));
        } else {
            assertThat(field.getMetadataField(), nullValue());
        }
        assertThat(field.getPriority(), is(priority));
        assertThat(field.getRendering(), is(rendering));
        assertThat(field.getStyleLabel(), is(labelStyle));
        assertThat(field.getStyleValue(), is(valueStyle));
        assertThat(field.isLabelAsHeading(), is(labelAsHeading));
        assertThat(field.isValuesInline(), is(valuesInline));

    }

    private void assertThatTabHas(CrisLayoutTab tab, String shortname, String entityType, String header, int rowsSize,
        int securityFieldsSize, boolean isLeading, int priority, LayoutSecurity security) {

        assertThat(tab.getEntity().getLabel(), is(entityType));
        assertThat(tab.getHeader(), is(header));
        assertThat(tab.getPriority(), is(priority));
        assertThat(tab.getRows(), hasSize(rowsSize));
        assertThat(tab.getSecurity(), is(security.getValue()));
        assertThat(tab.getShortName(), is(shortname));
        assertThat(tab.isLeading(), is(isLeading));
        assertThat(tab.getMetadataSecurityFields().size() +
                       tab.getGroupSecurityFields().size(), is(securityFieldsSize));
    }

    private void assertThatBoxHas(CrisLayoutBox box, String shortname, String type, String entityType,
        String header, int fieldsSize, int securityFieldsSize, boolean minor, boolean collapsed,
        boolean container, String style, LayoutSecurity security) {

        assertThat(box.getCollapsed(), is(collapsed));
        assertThat(box.isContainer(), is(container));
        assertThat(box.getEntitytype().getLabel(), is(entityType));
        assertThat(box.getHeader(), is(header));
        assertThat(box.getLayoutFields(), hasSize(fieldsSize));
        assertThat(box.getMaxColumns(), nullValue());
        assertThat(box.getMinor(), is(minor));
        assertThat(box.getSecurity(), is(security.getValue()));
        assertThat(box.getStyle(), is(style));
        assertThat(box.getShortname(), is(shortname));
        assertThat(box.getType(), is(type));
        assertThat(box.getMetadataSecurityFields().size() + box.getGroupSecurityFields().size(),
                   is(securityFieldsSize));
    }

    private EntityType createEntityType(String entityType) {
        return EntityTypeBuilder.createEntityTypeBuilder(context, entityType).build();
    }

    private String getXlsFilePath(String name) {
        return new File(BASE_XLS_DIR_PATH, name).getAbsolutePath();
    }
}
