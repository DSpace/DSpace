/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.builder.EntityTypeBuilder.createEntityTypeBuilder;
import static org.dspace.builder.ItemBuilder.createItem;
import static org.dspace.builder.RelationshipBuilder.createRelationshipBuilder;
import static org.dspace.builder.RelationshipTypeBuilder.createRelationshipTypeBuilder;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.util.SubmissionConfigReaderException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.DynamicLayoutBoxBuilder;
import org.dspace.builder.DynamicLayoutFieldBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.ItemServiceImpl;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataFieldServiceImpl;
import org.dspace.content.RelationshipType;
import org.dspace.content.authority.Choices;
import org.dspace.content.authority.DCInputAuthority;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.eperson.EPerson;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.DynamicLayoutField;
import org.dspace.layout.LayoutSecurity;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link ReferCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalkIT extends AbstractIntegrationTestWithDatabase {
    static final String CFG_PREFIX = "identifier.doi.prefix";

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private ItemService itemService;

    private MetadataFieldService mfss;

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private VirtualFieldMapper virtualFieldMapper;

    private Community community;

    private Collection collection;

    private VirtualField virtualFieldId;

    private ConfigurationService configurationService;

    private MetadataAuthorityService metadataAuthorityService;

    private ChoiceAuthorityService choiceAuthorityService;

    private MetadataFieldService metadataFieldService;

    private PluginService pluginService;

    @Before
    public void setup() throws SQLException, AuthorizeException, SubmissionConfigReaderException {

        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        this.virtualFieldMapper =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getServiceByName("virtualFieldMapper", VirtualFieldMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        this.itemService = new DSpace().getSingletonService(ItemServiceImpl.class);
        this.mfss = new DSpace().getSingletonService(MetadataFieldServiceImpl.class);

        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
        this.metadataAuthorityService = ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
        this.metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        this.pluginService = CoreServiceFactory.getInstance().getPluginService();
        this.choiceAuthorityService = ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();


        this.virtualFieldId = this.virtualFieldMapper.getVirtualField("id");

        VirtualField mockedVirtualFieldId = mock(VirtualField.class);
        when(mockedVirtualFieldId.getMetadata(any(), any(), any())).thenReturn(new String[] { "mock-id" });
        this.virtualFieldMapper.setVirtualField("id", mockedVirtualFieldId);

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

        // CRIS enables authority control on these fields (modules/authority.cfg);
        // main leaves them disabled, so the CRIS test fixtures need them enabled here.
        // Only fields that actually exist in this registry are configured.
        String[] authorityControlledFields = {
            "dc.contributor.author",
            "oairecerif.author.affiliation",
            "dc.contributor.editor",
            "oairecerif.editor.affiliation",
            "dc.relation.product",
            "dc.relation.publication",
            "dc.relation.journal",
            "dc.relation.project",
            "dc.publisher",
            "dc.relation.funding",
            "dc.description.sponsorship",
            "crispj.coordinator",
            "crispj.organization",
            "crispj.partnerou",
            "crispj.investigator",
            "crispj.coinvestigators",
            "dc.relation.equipment",
            "person.affiliation.name",
            "oairecerif.person.affiliation",
            "crisrp.qualification",
            "crisrp.education",
            "organization.parentOrganization",
            "crisou.director",
            "oairecerif.funder",
            "oairecerif.fundingParent",
            "crisfund.investigators",
            "crisfund.coinvestigators",
            "crisfund.leadorganizations",
            "crisfund.leadcoorganizations",
            "cris.policy.eperson",
            "cris.policy.group",
            "dspace.object.owner",
            "dc.identifier.issn",
            "dc.type",
            "cris.virtual.department",
            "oairecerif.identifier.url",
        };
        for (String field : authorityControlledFields) {
            String[] parts = field.split("\\.");
            String qualifier = parts.length > 2 ? parts[2] : null;
            if (metadataFieldService.findByElement(context, parts[0], parts[1], qualifier) != null) {
                configurationService.setProperty("authority.controlled." + field, true);
            }
        }
        metadataAuthorityService.clearCache();

        // CRIS enables authority control on dc.type and wires it to a controlled
        // vocabulary (modules/authority.cfg). On main the equivalent ChoiceAuthority is the
        // self-named DSpaceControlledVocabulary instance "publication-coar-types".
        configurationService.setProperty("authority.controlled.dc.type", "true");
        configurationService.setProperty("vocabulary.plugin.authority.store", "true");
        configurationService.setProperty("choices.plugin.dc.type", "publication-coar-types");
        configurationService.setProperty("choices.presentation.dc.type", "suggest");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

    }

    @After
    public void after() {
        this.virtualFieldMapper.setVirtualField("id", virtualFieldId);
    }

    @Test
    public void testPersonXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withCrisrpVariantName("J.S.")
            .withCrisrpVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonalSiteUrl("www.test.com")
            .withPersonalSiteTitle("Test")
            .withPersonalSiteUrl("www.john-smith.com")
            .withPersonalSiteTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonalSiteUrl("www.site.com")
            .withPersonalSiteTitle("Site")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withDescriptionAbstract("Biography \n\t<This is my biography>")
            .withCrisrpCountry("England")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .withPersonQualification("First Qualification")
            .withPersonQualificationStartDate("2015-01-01")
            .withPersonQualificationEndDate("2016-01-01")
            .withPersonQualification("Second Qualification")
            .withPersonQualificationStartDate("2016-01-02")
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, personItem, out);

        try (FileInputStream fis = getFileInputStream("person.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPersonCvXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withDescriptionAbstract("This is my biography")
            .withCrisrpCountry("England")
            .withPersonKnowsLanguages("English")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .withPersonQualification("First Qualification")
            .withPersonQualificationStartDate("2015-01-01")
            .withPersonQualificationEndDate("2016-01-01")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        // The person-cv-xml format is not registered in the crosswalk mapper (it is
        // consumed only by the person-cv-pdf DocumentCrosswalk), so it is fetched by bean name.
        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPersonCvXml", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, personItem, out);

        String xml = out.toString();
        assertThat(xml, containsString("<preferred-name>John Smith</preferred-name>"));
        assertThat(xml, containsString("<full-name>John Smith</full-name>"));
        assertThat(xml, containsString("<given-name>John</given-name>"));
        assertThat(xml, containsString("<family-name>Smith</family-name>"));
        assertThat(xml, containsString("<job-title>Researcher</job-title>"));
        assertThat(xml, containsString("<main-affiliation>University</main-affiliation>"));
        assertThat(xml, containsString("<working-group>First work group</working-group>"));
        assertThat(xml, containsString("<working-group>Second work group</working-group>"));
        assertThat(xml, containsString("<biography>This is my biography</biography>"));
        assertThat(xml, containsString("<country>England</country>"));
        assertThat(xml, containsString("<name>School</name>"));
        assertThat(xml, containsString("<role>Student</role>"));
        assertThat(xml, containsString("<name>First Qualification</name>"));
        assertThat(xml, containsString("<title>First Publication</title>"));
        assertThat(xml, containsString("<date-issued>2020-01-01</date-issued>"));
    }

    @Test
    public void testPersonXmlCerifDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Smith, John")
            .withCrisrpVariantName("J.S.")
            .withCrisrpVariantName("Smith John")
            .withGender("M")
            .withPersonMainAffiliation("University")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("SA-01")
            .withPersonEmail("test@test.com")
            .withResearcherIdentifier("R-01")
            .withResearcherIdentifier("R-02")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliation("Another Company")
            .withPersonAffiliationStartDate("2017-01-01")
            .withPersonAffiliationEndDate("2017-12-31")
            .withPersonAffiliationRole("Developer")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, personItem, out);

        try (FileInputStream fis = getFileInputStream("person-cerif.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testManyPersonsXmlCerifDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item firstPerson = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Smith, John")
            .withCrisrpVariantName("J.S.")
            .withCrisrpVariantName("Smith John")
            .withGender("M")
            .withPersonMainAffiliation("University")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("SA-01")
            .withPersonEmail("test@test.com")
            .withResearcherIdentifier("R-01")
            .withResearcherIdentifier("R-02")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliation("Another Company")
            .withPersonAffiliationStartDate("2017-01-01")
            .withPersonAffiliationEndDate("2017-12-31")
            .withPersonAffiliationRole("Developer")
            .build();

        Item secondPerson = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("White, Walter")
            .withGender("M")
            .withPersonMainAffiliation("University")
            .withOrcidIdentifier("0000-0002-9079-5938")
            .withPersonEmail("w.w@test.com")
            .withResearcherIdentifier("R-03")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstPerson, secondPerson).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons-cerif.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPersonWithEmptyGroupsXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withCrisrpVariantName("J.S.")
            .withCrisrpVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withScopusAuthorIdentifier("111-222-333")
            .withResearcherIdentifier("0001")
            .withResearcherIdentifier("0002")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonAffiliationRole("Developer")
            .withCrisrpCountry("England")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("person-with-empty-groups.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPersonXmlDisseminateWithPersonalPicture() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
            .withName("ORIGINAL")
            .build();

        Bitstream bitstream = BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalk = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-xml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamCrosswalk.disseminate(context, item, out);

        assertThat(out.toString(), containsString("<personal-picture>" + bitstream.getID() + "</personal-picture>"));
    }

    @Test
    public void testPersonXmlDisseminateWithMultiplePersonalPictures() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
            .withName("ORIGINAL")
            .build();

        Bitstream bitstreamOne = BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        Bitstream bitstreamTwo = BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.png"))
            .withType("personal picture")
            .build();

        Bitstream bitstreamThree = BitstreamBuilder.createBitstream(context, bundle, getFileInputStream("picture.jpg"))
            .withType("personal picture")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk streamCrosswalk = (StreamDisseminationCrosswalk) CoreServiceFactory
            .getInstance().getPluginService().getNamedPlugin(StreamDisseminationCrosswalk.class, "person-xml");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamCrosswalk.disseminate(context, item, out);

        assertThat(out.toString(),
            containsString("<personal-picture>" + bitstreamOne.getID() + "</personal-picture>"));
        assertThat(out.toString(),
            containsString("<personal-picture>" + bitstreamTwo.getID() + "</personal-picture>"));
        assertThat(out.toString(),
            containsString("<personal-picture>" + bitstreamThree.getID() + "</personal-picture>"));
    }

    @Test
    public void testPersonJsonDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withCrisrpVernacularName("JOHN SMITH")
            .withCrisrpVariantName("J.S.")
            .withCrisrpVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonalSiteUrl("www.test.com")
            .withPersonalSiteTitle("Test")
            .withPersonalSiteUrl("www.john-smith.com")
            .withPersonalSiteTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonalSiteUrl("www.site.com")
            .withPersonalSiteTitle("Site")
            .withPersonEmail("test@test.com")
            .withSubject("Science")
            .withOrcidIdentifier("0000-0002-9079-5932")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withDescriptionAbstract("Biography: \n\t\"This is my biography\"")
            .withCrisrpCountry("England")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .withPersonQualification("First Qualification")
            .withPersonQualificationStartDate("2015-01-01")
            .withPersonQualificationEndDate("2016-01-01")
            .withPersonQualification("Second Qualification")
            .withPersonQualificationStartDate("2016-01-02")
            .withPersonQualificationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, personItem, out);

        try (FileInputStream fis = getFileInputStream("person.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson);
        }
    }

    @Test
    public void testManyPersonsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Item firstItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        Item secondItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Adam White")
            .withGivenName("Adam")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        // with multiple persons export the publications should not be exported
        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", firstItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstItem, secondItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testManyPersonsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Item firstItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withPersonAffiliation("Company")
            .withPersonAffiliationStartDate("2018-01-01")
            .withPersonAffiliationRole("Developer")
            .withPersonAffiliationEndDate(PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();
        Item secondItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Adam White")
            .withGivenName("Adam")
            .withFamilyName("White")
            .withBirthDate("1962-03-23")
            .withGender("M")
            .withJobTitle("Researcher")
            .withPersonMainAffiliation("University")
            .withPersonKnowsLanguages("English")
            .withPersonKnowsLanguages("Italian")
            .withPersonEducation("School")
            .withPersonEducationStartDate("2000-01-01")
            .withPersonEducationEndDate("2005-01-01")
            .withPersonEducationRole("Student")
            .build();

        // with multiple persons export the publications should not be exported
        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", firstItem.getID().toString())
            .withAuthor("Walter White")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("person-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstItem, secondItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("persons.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson);
        }
    }

    @Test
    public void testPublicationXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-04-01")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withAlternativeTitle("Alternative publication title")
            .withRelationPublication("Published in publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withLanguage("en")
            .withPublisher("Publication publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withSubject("test")
            .withSubject("export")
            .withType("Controlled Vocabulary for Resource Type Genres::text::review")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White", "6c36b2b0-b2cf-41a5-8241-11d0ea56ed97")
            .withAuthorAffiliation("Company")
            .withEditor("Editor", "25887329-a648-46f9-a2ac-99319b8e9766")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("publication.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPublicationXmlDisseminateWithAuthorityOnFunder() throws Exception {

        context.turnOffAuthorisationSystem();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withTitle("Test Project")
            .withInternalId("111-222-333")
            .withAcronym("TP")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-04-01")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Test Funder")
            .withAcronym("TFO")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Internal Funding")
            .withFunder("Test Funder", orgUnit.getID().toString())
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Another Test Funding")
            .withType("Contract")
            .withFunder("Another Test Funder")
            .withAcronym("ATF-01")
            .build();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withRelationDoi("doi:10.3972/test")
            .withDoiIdentifier("doi:111.111/publication")
            .withIsbnIdentifier("978-3-16-148410-0")
            .withIssnIdentifier("2049-3630")
            .withIsiIdentifier("111-222-333")
            .withScopusIdentifier("99999999")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book::book part")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", "6c36b2b0-b2cf-41a5-8241-11d0ea56ed97")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .withAuthor("Walter White")
            .withAuthorAffiliation("Company")
            .withEditor("Editor")
            .withEditorAffiliation("Editor Affiliation")
            .withRelationProject("Test Project", project.getID().toString())
            .withRelationFunding("Another Test Funding", funding.getID().toString())
            .withRelationConference("The best Conference")
            .withRelationProduct("DataSet")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("publication-with-authority-on-funder.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testManyPublicationXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstPublication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("First Publication")
            .withDoiIdentifier("doi:111.111/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::learning object")
            .withIssueDate("2019-12-31")
            .withAuthor("Edward Smith", "8556d5b5-14e5-4009-9539-1ef9686d684d")
            .withAuthorAffiliation("Company")
            .withAuthor("Walter White", "6c36b2b0-b2cf-41a5-8241-11d0ea56ed97")
            .withAuthorAffiliation(CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE)
            .build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test Funding")
            .withType("Contract")
            .withFunder("Test Funder")
            .withAcronym("TF-01")
            .build();

        Item secondPublication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Second Publication")
            .withDoiIdentifier("doi:222.222/publication")
            .withType("Controlled Vocabulary for Resource Type Genres::clinical trial")
            .withIssueDate("2010-02-01")
            .withAuthor("Jessie Pinkman")
            .withRelationFunding("Test Funding", funding.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstPublication, secondPublication).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void testPublicationEndnoteDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Test Publication")
            .withDoiIdentifier("doi:111.111/publication")
            .withHandle("123456789/xxx")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthor("Walter White")
            .withAuthor("Jessie Pinkman")
            .withIsPartOf("Journal")
            .withPublisher("Publisher")
            .withVolume("V.01")
            .withIssue("Issue")
            .withDescriptionAbstract("This is a publication to test export")
            .withSubject("test")
            .withSubject("publication")
            .withCitationStartPage("2")
            .withCitationEndPage("20")
            .withLanguage("en")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("endnote");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, publication, out);

        try (FileInputStream fis = getFileInputStream("endnote")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testProjectXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item coordinator = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit", coordinator.getID().toString())
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withCrispjProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, project, out);

        try (FileInputStream fis = getFileInputStream("project.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testProjectJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item coordinator = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Coordinator OrgUnit")
            .withAcronym("COU")
            .build();

        Item project = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("Coordinator OrgUnit", coordinator.getID().toString())
            .withProjectPartner("Partner OrgUnit")
            .withProjectPartner("Another Partner OrgUnit")
            .withProjectOrganization("First Member OrgUnit")
            .withProjectOrganization("Second Member OrgUnit")
            .withProjectOrganization("Third Member OrgUnit")
            .withCrispjProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withTitle("Test funding")
            .withType("Award")
            .withFunder("OrgUnit Funder")
            .withRelationProject("Test Project", project.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, project, out);

        try (FileInputStream fis = getFileInputStream("project.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyProjectsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("First Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withCrispjProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        Item secondProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("STP")
            .withTitle("Second Test Project")
            .withOpenaireId("55-66-77")
            .withOpenaireId("11-33-22")
            .withUrlIdentifier("www.project.test")
            .withOairecerifProjectStartDate("2010-01-01")
            .withOairecerifProjectEndDate("2012-12-31")
            .withProjectStatus("Status")
            .withProjectCoordinator("Second Coordinator OrgUnit")
            .withCrispjProjectInvestigator("Second investigator")
            .withProjectCoinvestigators("Coinvestigator")
            .withRelationEquipment("Another test equipment")
            .withOAMandateURL("oamandate")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstProject, secondProject).iterator(), out);

        try (FileInputStream fis = getFileInputStream("projects.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }

    }

    @Test
    public void testManyProjectsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("TP")
            .withTitle("Test Project")
            .withOpenaireId("11-22-33")
            .withUrlIdentifier("www.project.test")
            .withOairecerifProjectStartDate("2020-01-01")
            .withOairecerifProjectEndDate("2020-12-31")
            .withProjectStatus("OPEN")
            .withProjectCoordinator("First Coordinator OrgUnit")
            .withProjectPartner("Partner OrgUnit")
            .withProjectOrganization("Member OrgUnit")
            .withCrispjProjectInvestigator("Investigator")
            .withProjectCoinvestigators("First coinvestigator")
            .withProjectCoinvestigators("Second coinvestigator")
            .withRelationEquipment("Test equipment")
            .withSubject("project")
            .withSubject("test")
            .withDescriptionAbstract("This is a project to test the export")
            .withOAMandate("true")
            .withOAMandateURL("oamandate-url")
            .build();

        Item secondProject = ItemBuilder.createItem(context, collection)
            .withEntityType("Project")
            .withAcronym("STP")
            .withTitle("Second Test Project")
            .withOpenaireId("55-66-77")
            .withOpenaireId("11-33-22")
            .withUrlIdentifier("www.project.test")
            .withOairecerifProjectStartDate("2010-01-01")
            .withOairecerifProjectEndDate("2012-12-31")
            .withProjectStatus("Status")
            .withProjectCoordinator("Second Coordinator OrgUnit")
            .withCrispjProjectInvestigator("Second investigator")
            .withProjectCoinvestigators("Coinvestigator")
            .withRelationEquipment("Another test equipment")
            .withOAMandateURL("oamandate")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("project-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstProject, secondProject).iterator(), out);

        try (FileInputStream fis = getFileInputStream("projects.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson);
        }

    }

    @Test
    public void testOrgUnitXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item parent = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("POU")
            .withTitle("Parent OrgUnit")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit", parent.getID().toString())
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Jesse Pinkman")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, orgUnit, out);

        try (FileInputStream fis = getFileInputStream("orgUnit.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testOrgUnitJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item parent = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("POU")
            .withTitle("Parent OrgUnit")
            .build();

        Item orgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit", parent.getID().toString())
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        ItemBuilder.createItem(context, collection)
            .withEntityType("Person")
            .withTitle("Walter White")
            .withPersonMainAffiliationName("Test OrgUnit", orgUnit.getID().toString())
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, orgUnit, out);

        try (FileInputStream fis = getFileInputStream("orgUnit.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyOrgUnitsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        Item secondOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("ATOU")
            .withTitle("Another Test OrgUnit")
            .withType("Private non-profit")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-03")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstOrgUnit, secondOrgUnit).iterator(), out);

        try (FileInputStream fis = getFileInputStream("orgUnits.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyOrgUnitsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("TOU")
            .withTitle("Test OrgUnit")
            .withOrgUnitLegalName("Test OrgUnit LegalName")
            .withType("Strategic Research Insitute")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-01")
            .withOrgUnitIdentifier("ID-02")
            .withUrlIdentifier("www.orgUnit.com")
            .withUrlIdentifier("www.orgUnit.it")
            .build();

        Item secondOrgUnit = ItemBuilder.createItem(context, collection)
            .withEntityType("OrgUnit")
            .withAcronym("ATOU")
            .withTitle("Another Test OrgUnit")
            .withType("Private non-profit")
            .withParentOrganization("Parent OrgUnit")
            .withOrgUnitIdentifier("ID-03")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("orgUnit-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstOrgUnit, secondOrgUnit).iterator(), out);

        try (FileInputStream fis = getFileInputStream("orgUnits.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testFundingXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, funding, out);

        try (FileInputStream fis = getFileInputStream("funding.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testFundingJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, funding, out);

        try (FileInputStream fis = getFileInputStream("funding.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyFundingsXmlDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        Item secondFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("AT-FU")
            .withTitle("Another Test Funding")
            .withType("Grant")
            .withInternalId("ID-02")
            .withFundingIdentifier("0002")
            .withAmount("10.000,00")
            .withFunder("Test Funder")
            .withFundingStartDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstFunding, secondFunding).iterator(), out);

        try (FileInputStream fis = getFileInputStream("fundings.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testManyFundingsJsonDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        Item secondFunding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("AT-FU")
            .withTitle("Another Test Funding")
            .withType("Grant")
            .withInternalId("ID-02")
            .withFundingIdentifier("0002")
            .withAmount("10.000,00")
            .withFunder("Test Funder")
            .withFundingStartDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-json");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(firstFunding, secondFunding).iterator(), out);

        try (FileInputStream fis = getFileInputStream("fundings.json")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }
    }

    @Test
    public void testVirtualFieldDate() throws Exception {

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withIssueDate("2020-02-14")
            .withDateAccepted("2021")
            .withDateAccepted("2022")
            .withDateAccepted("2023")
            .build();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkVirtualFieldDate", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String currentDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
        String currentYear = new SimpleDateFormat("yyyy").format(new Date());

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(12));
        assertThat(resultLines[0].trim(), is("{"));
        assertThat(resultLines[1].trim(), is("\"only-year\": \"2020\","));
        assertThat(resultLines[2].trim(), is("\"date-without-time\": \"2020-02-14\","));
        assertThat(resultLines[3].trim(), is("\"another-date-without-time\": \"2020\\/02\\/14\","));
        assertThat(resultLines[4].trim(), is("\"date-with-time\": \"14-02-2020 00:00:00\","));
        assertThat(resultLines[5].trim(), is("\"another-date-with-time\": \"20200214 000000\","));
        assertThat(resultLines[6].trim(), is("\"current-timestamp\": \"" + currentDate + "\","));
        assertThat(resultLines[7].trim(), is("\"current-year\": \"" + currentYear + "\","));
        assertThat(resultLines[8].trim(), is("\"repeatable-date\": \"2021\","));
        assertThat(resultLines[9].trim(), is("\"repeatable-date\": \"2022\","));
        assertThat(resultLines[10].trim(), is("\"repeatable-date\": \"2023\""));
        assertThat(resultLines[11].trim(), is("}"));

    }

    @Test
    public void testVirtualFieldVocabulary() throws Exception {

        Item publication = ItemBuilder.createItem(context, collection)
            .withEntityType("Publication")
            .withType("Resource Type Genres::software::research software")
            .build();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkVirtualFieldVocabulary", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(7));
        assertThat(resultLines[0].trim(), is("{"));
        assertThat(resultLines[1].trim(), is("\"first-element\": \"Resource Type Genres\","));
        assertThat(resultLines[2].trim(), is("\"second-element\": \"software\","));
        assertThat(resultLines[3].trim(), is("\"last-element\": \"research software\","));
        assertThat(resultLines[4].trim(), is("\"second-last-element\": \"software\","));
        assertThat(resultLines[5].trim(), is("\"deep-element\": \"research software\""));
        assertThat(resultLines[6].trim(), is("}"));

    }

    @Test
    public void testVirtualFieldCitationsWithPerson() throws Exception {

        context.turnOffAuthorisationSystem();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("First Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withAuthor("Walter White")
            .withPublisher("Test publisher")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withHandle("123456789/111111")
            .build();

        ItemBuilder.createItem(context, collection)
            .withTitle("Second Publication")
            .withIssueDate("2020-04-01")
            .withAuthor("John Smith", personItem.getID().toString())
            .withHandle("123456789/99999")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPersonVirtualFieldCitations", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, personItem, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(6));
        assertThat(resultLines[0].trim(), is("<person>"));
        assertThat(resultLines[1].trim(), is("<citations>"));
        assertThat(resultLines[2].trim(), is("<citation>John Smith. (2020). "
            + "Second Publication. http://localhost:4000/handle/123456789/99999</citation>"));
        assertThat(resultLines[3].trim(), is("<citation>John Smith, &amp; Walter White. (2020). First "
            + "Publication. http://localhost:4000/handle/123456789/111111</citation>"));
        assertThat(resultLines[4].trim(), is("</citations>"));
        assertThat(resultLines[5].trim(), is("</person>"));

    }

    @Test
    public void testVirtualFieldCitationsWithPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("Publication")
            .withEntityType("Publication")
            .withIssueDate("2020-01-01")
            .withAuthor("John Smith")
            .withAuthor("Walter White")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withPublisher("Test publisher")
            .withHandle("123456789/111111")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPublicationVirtualFieldCitations", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(3));
        assertThat(resultLines[0].trim(), is("<publication>"));
        assertThat(resultLines[1].trim(), is("<citation>John Smith, &amp; Walter White. (2020). Publication. " +
            "http://localhost:4000/handle/123456789/111111</citation>"));
        assertThat(resultLines[2].trim(), is("</publication>"));

    }

    @Test
    public void testVirtualFieldCitationsWithFirstSelectedPublication() throws Exception {

        context.turnOffAuthorisationSystem();

        EntityType personEntityType = createEntityTypeBuilder(context, "Person").build();
        EntityType publicationEntityType = createEntityTypeBuilder(context, "Publication").build();

        RelationshipType selectedRelationshipType = createRelationshipTypeBuilder(context,
                                                                                  publicationEntityType,
                                                                                  personEntityType,
                                                                                  "isResearchoutputsSelectedFor",
                                                                                  "hasSelectedResearchoutputs",
                                                                                  0,
                                                                                  null,
                                                                                  0,
                                                                                  null)
            .build();

        Item personItem = createItem(context, collection)
            .withEntityType("Person")
            .withTitle("John Smith")
            .build();

        Item firstPublication = ItemBuilder.createItem(context, collection)
                               .withTitle("First Publication")
                               .withEntityType("Publication")
                               .withIssueDate("2020-01-01")
                               .withAuthor("John Smith", personItem.getID().toString())
                               .withAuthor("Walter White")
                               .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
                               .withPublisher("Test publisher")
                               .withHandle("123456789/111111")
                               .build();

        Item secondPublication = ItemBuilder.createItem(context, collection)
                                .withTitle("Second Publication")
                               .withEntityType("Publication")
                                .withIssueDate("2020-04-01")
                                .withAuthor("John Smith", personItem.getID().toString())
                                .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
                                .withHandle("123456789/99999")
                                .build();

        Item thirdPublication = ItemBuilder.createItem(context, collection)
                               .withTitle("Third Publication")
                               .withEntityType("Publication")
                               .withIssueDate("2022-03-02")
                               .withAuthor("John Smith", personItem.getID().toString())
                               .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
                               .withHandle("123456789/55555")
                               .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
                                                    .getServiceByName(
                                                        "referCrosswalkFirstSelectedVirtualFieldCitations",
                                                        ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, personItem, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(7));
        assertThat(resultLines[0].trim(), is("<person>"));
        assertThat(resultLines[1].trim(), is("<citations>"));
        assertThat(resultLines[4].trim(), is("<citation>John Smith, &amp; Walter White. (2020). " +
                                                 "First Publication. http://localhost:4000/handle/123456789/111111</citation>"));
        assertThat(resultLines[2].trim(), is("<citation>John Smith. (2020). Second Publication. " +
                                                 "http://localhost:4000/handle/123456789/99999</citation>"));
        assertThat(resultLines[3].trim(), is("<citation>John Smith. (2022). " +
                                                 "Third Publication. http://localhost:4000/handle/123456789/55555</citation>"));

        assertThat(resultLines[5].trim(), is("</citations>"));
        assertThat(resultLines[6].trim(), is("</person>"));

        createSelectedRelationship(personItem, secondPublication, selectedRelationshipType);

        out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, personItem, out);

        resultLines = out.toString().split("\n");

        assertThat(resultLines.length, is(7));
        assertThat(resultLines[0].trim(), is("<person>"));
        assertThat(resultLines[1].trim(), is("<citations>"));
        assertThat(resultLines[2].trim(), is("<citation>John Smith. (2022). Third Publication. " +
                                                 "http://localhost:4000/handle/123456789/55555</citation>"));
        assertThat(resultLines[3].trim(), is("<citation>John Smith. (2020). Second Publication. " +
                                                 "http://localhost:4000/handle/123456789/99999</citation>"));
        assertThat(resultLines[4].trim(), is("<citation>John Smith, &amp; Walter White. (2020). " +
                                                 "First Publication. http://localhost:4000/handle/123456789/111111</citation>"));
        assertThat(resultLines[5].trim(), is("</citations>"));
        assertThat(resultLines[6].trim(), is("</person>"));

        createSelectedRelationship(personItem, firstPublication, selectedRelationshipType);

        out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, personItem, out);

        resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(7));
        assertThat(resultLines[0].trim(), is("<person>"));
        assertThat(resultLines[1].trim(), is("<citations>"));
        assertThat(resultLines[2].trim(), is("<citation>John Smith. (2022). Third Publication. " +
                                                 "http://localhost:4000/handle/123456789/55555</citation>"));
        assertThat(resultLines[3].trim(), is("<citation>John Smith. (2020). Second Publication. " +
                                                 "http://localhost:4000/handle/123456789/99999</citation>"));
        assertThat(resultLines[4].trim(), is("<citation>John Smith, &amp; Walter White. (2020). " +
                                                 "First Publication. http://localhost:4000/handle/123456789/111111</citation>"));
        assertThat(resultLines[5].trim(), is("</citations>"));
        assertThat(resultLines[6].trim(), is("</person>"));

        createSelectedRelationship(personItem, thirdPublication, selectedRelationshipType);

        out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, personItem, out);

        resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(7));
        assertThat(resultLines[0].trim(), is("<person>"));
        assertThat(resultLines[1].trim(), is("<citations>"));
        assertThat(resultLines[2].trim(), is("<citation>John Smith. (2022). Third Publication. " +
                                                 "http://localhost:4000/handle/123456789/55555</citation>"));
        assertThat(resultLines[3].trim(), is("<citation>John Smith. (2020). Second Publication. " +
                                                 "http://localhost:4000/handle/123456789/99999</citation>"));
        assertThat(resultLines[4].trim(), is("<citation>John Smith, &amp; Walter White. (2020). " +
                                                 "First Publication. http://localhost:4000/handle/123456789/111111</citation>"));
        assertThat(resultLines[5].trim(), is("</citations>"));
        assertThat(resultLines[6].trim(), is("</person>"));

    }


    @Test
    public void testVirtualBitstreamFieldWithProject() throws Exception {

        context.turnOffAuthorisationSystem();

        Item projectItem = createItem(context, collection)
            .withEntityType("Project")
            .withTitle("project title")
            .build();


        Bundle bundle =
            BundleBuilder.createBundle(context, projectItem)
                         .withName("ORIGINAL")
                         .build();

        Bitstream jpegBitstream =
            BitstreamBuilder.createBitstream(context, bundle, InputStream.nullInputStream())
                            .withMimeType("image/jpeg")
                            .withType("project picture")
                            .build();

        Bitstream pngBitstream =
            BitstreamBuilder.createBitstream(context, bundle, InputStream.nullInputStream())
                            .withMimeType("image/png")
                            .build();

        Bitstream txtBitstream =
            BitstreamBuilder.createBitstream(context, bundle, InputStream.nullInputStream())
                            .withMimeType("text/plain")
                            .build();

        Bitstream pdfBitstreamOne =
            BitstreamBuilder.createBitstream(context, bundle, InputStream.nullInputStream())
                            .withMimeType("application/pdf")
                            .build();

        Bitstream pdfBitstreamTwo =
            BitstreamBuilder.createBitstream(context, bundle, InputStream.nullInputStream())
                            .withMimeType("application/pdf")
                            .build();

        Bitstream pdfBitstreamThree =
            BitstreamBuilder.createBitstream(context, bundle, InputStream.nullInputStream())
                            .withMimeType("application/pdf")
                            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk =
            new DSpace().getServiceManager()
                        .getServiceByName("referCrosswalkProjectVirtualFieldBitstreams", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, projectItem, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(55));
        assertThat(resultLines[0].trim(), equalTo("<project>"));

        assertThat(resultLines[2].trim(), equalTo("<all-bitstreams>"));
        assertThat(resultLines[3].trim(), equalTo("<bitstream>" + jpegBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[4].trim(), equalTo("<bitstream>" + pngBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[5].trim(), equalTo("<bitstream>" + txtBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[6].trim(), equalTo("<bitstream>" + pdfBitstreamOne.getID() + "</bitstream>"));
        assertThat(resultLines[7].trim(), equalTo("<bitstream>" + pdfBitstreamTwo.getID() + "</bitstream>"));
        assertThat(resultLines[8].trim(), equalTo("<bitstream>" + pdfBitstreamThree.getID() + "</bitstream>"));
        assertThat(resultLines[9].trim(), equalTo("</all-bitstreams>"));

        assertThat(resultLines[11].trim(), equalTo("<project-picture>"));
        assertThat(resultLines[12].trim(), equalTo("<bitstream>" + jpegBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[13].trim(), equalTo("</project-picture>"));

        assertThat(resultLines[15].trim(), equalTo("<image-bitstreams>"));
        assertThat(resultLines[16].trim(), equalTo("<bitstream>" + jpegBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[17].trim(), equalTo("<bitstream>" + pngBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[18].trim(), equalTo("</image-bitstreams>"));

        assertThat(resultLines[20].trim(), equalTo("<first-image-bitstreams>"));
        assertThat(resultLines[21].trim(), equalTo("<bitstream>" + jpegBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[22].trim(), equalTo("</first-image-bitstreams>"));

        assertThat(resultLines[24].trim(), equalTo("<pdf-bitstreams>"));
        assertThat(resultLines[25].trim(), equalTo("<bitstream>" + pdfBitstreamOne.getID() + "</bitstream>"));
        assertThat(resultLines[26].trim(), equalTo("<bitstream>" + pdfBitstreamTwo.getID() + "</bitstream>"));
        assertThat(resultLines[27].trim(), equalTo("<bitstream>" + pdfBitstreamThree.getID() + "</bitstream>"));
        assertThat(resultLines[28].trim(), equalTo("</pdf-bitstreams>"));

        assertThat(resultLines[30].trim(), equalTo("<first-two-pdf-bitstreams>"));
        assertThat(resultLines[31].trim(), equalTo("<bitstream>" + pdfBitstreamOne.getID() + "</bitstream>"));
        assertThat(resultLines[32].trim(), equalTo("<bitstream>" + pdfBitstreamTwo.getID() + "</bitstream>"));
        assertThat(resultLines[33].trim(), equalTo("</first-two-pdf-bitstreams>"));

        assertThat(resultLines[35].trim(), equalTo("<txt-bitstreams>"));
        assertThat(resultLines[36].trim(), equalTo("<bitstream>" + txtBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[37].trim(), equalTo("</txt-bitstreams>"));

        assertThat(resultLines[39].trim(), equalTo("<first-three-bitstreams>"));
        assertThat(resultLines[40].trim(), equalTo("<bitstream>" + jpegBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[41].trim(), equalTo("<bitstream>" + pngBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[42].trim(), equalTo("<bitstream>" + txtBitstream.getID() + "</bitstream>"));
        assertThat(resultLines[43].trim(), equalTo("</first-three-bitstreams>"));

        assertThat(resultLines[45].trim(), equalTo("<wrong-bundle-bitstreams>"));
        assertThat(resultLines[46].trim(), equalTo("</wrong-bundle-bitstreams>"));

        assertThat(resultLines[48].trim(), equalTo("<wrong-type-bitstreams>"));
        assertThat(resultLines[49].trim(), equalTo("</wrong-type-bitstreams>"));

        assertThat(resultLines[51].trim(), equalTo("<wrong-mimeType-bitstreams>"));
        assertThat(resultLines[52].trim(), equalTo("</wrong-mimeType-bitstreams>"));

        assertThat(resultLines[54].trim(), equalTo("</project>"));
    }

    @Test
    public void testReferCrosswalkPublicationDataciteXml() throws Exception {

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPublicationDataciteXml", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        Item orgunit = createItem(context, collection)
                .withEntityType("OrgUnit")
                .withTitle("OrgUnit Name")
                .withOrgUnitRORIdentifier("rorID")
                .build();
        Item publisher = createItem(context, collection)
                .withEntityType("OrgUnit")
                .withTitle("Publisher Name")
                .withOrgUnitRORIdentifier("rorID2")
                .build();

        Item author = createItem(context, collection)
                .withEntityType("Person")
                .withTitle("Author, Name")
                .withAffiliation("OrgUnit", orgunit.getID().toString())
                .withOrcidIdentifier("1234-5678-9012")
                .build();
        Item author2 = createItem(context, collection)
                .withEntityType("Person")
                .withTitle("Author2, Name")
                .withAffiliation("OrgUnit", orgunit.getID().toString())
                .withOrcidIdentifier("9876-5432-1012")
                .build();
        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Publication title")
            .withAuthor("Author, Name in Pub", author.getID().toString())
            .withAuthorAffiliation("OrgUnit in pub", orgunit.getID().toString())
            .withAuthor("External, Auth")
            .withAuthorAffiliationPlaceholder()
            .withAuthor("Author, Name in Pub", author2.getID().toString())
            .withAuthorAffiliation("OrgUnit in pub2")
            .withPublisher("Publisher", publisher.getID().toString())
            .withIssueDate("2023")
            .withDateAvailable("2023-10-20")
            .withType("text::journal article::review article", "publication-coar-types:c_dcae04bc")
            .withHandle("123456789/99999")
            .build();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        referCrosswalk.disseminate(context, item, byteArrayOutputStream);

        System.out.println(new String(byteArrayOutputStream.toByteArray()));

        try (FileInputStream fis = getFileInputStream("journal-article-datacite.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(byteArrayOutputStream.toString(), expectedXml);
        }

    }

    @Test

    public void testReferCrosswalkPublicationDataciteXmlWithoutTypeAndAuthor() throws Exception {


        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
                                                    .getServiceByName("referCrosswalkPublicationDataciteXml",
                                                                      ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        Item publisher = createItem(context, collection)
            .withEntityType("OrgUnit")
            .withTitle("Publisher Name")
            .withOrgUnitRORIdentifier("rorID2")
            .build();

        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Publication title")
            .withPublisher("Publisher", publisher.getID().toString())
            .withIssueDate("2023")
            .withDateAvailable("2023-10-20")
            .withHandle("123456789/99999")
            .build();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        referCrosswalk.disseminate(context, item, byteArrayOutputStream);

        System.out.println(new String(byteArrayOutputStream.toByteArray()));

        try (FileInputStream fis = getFileInputStream("publication-without-authors-and-type-datacite.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(byteArrayOutputStream.toString(), expectedXml);
        }
    }



    @Test
    public void testReferCrosswalkPublicationDataciteXmlWithVirtualPlace() throws Exception {

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPublicationDataciteXml", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        Item publisher = createItem(context, collection)
                .withEntityType("OrgUnit")
                .withTitle("Publisher Name")
                .withOrgUnitRORIdentifier("rorID2")
                .build();
        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withTitle("Publication title")
            .withPublisher("Publisher", publisher.getID().toString())
            .withIssueDate("2023")
            .withLanguage(Locale.ITALIAN.getLanguage())
            .withLanguage(Locale.ENGLISH.getLanguage())
            .withDateAvailable("2023-10-20")
            .withHandle("123456789/99999")
            .build();

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        referCrosswalk.disseminate(context, item, byteArrayOutputStream);

        System.out.println(new String(byteArrayOutputStream.toByteArray()));

        try (FileInputStream fis = getFileInputStream("publication-virtual-place-datacite.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(byteArrayOutputStream.toString(), expectedXml);
        }

    }

    @Test
    public void testExportToDataciteFormatItemWithThreeDOI() throws Exception {
        String prefix;
        prefix = this.configurationService.getProperty(CFG_PREFIX);
        if (null == prefix) {
            throw new RuntimeException("Unable to load DOI prefix from "
                    + "configuration. Cannot find property " +
                    CFG_PREFIX + ".");
        }

        context.turnOffAuthorisationSystem();

        Item publication = createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("publication title")
                .withDoiIdentifier("test doi")
                .withDoiIdentifier("test doi2")
                .withDoiIdentifier("test" + prefix + "test")
                .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
                .getServiceByName("referCrosswalkVirtualFieldDOI", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String[] resultLines = out.toString().split("\n");

        assertThat(resultLines.length, is(5));
        assertThat(resultLines[0].trim(), is("{"));
        assertThat(resultLines[1].trim(), is("\"primary-doi\": \"test" + prefix + "test\","));
        assertThat(resultLines[2].trim(), is("\"alternative-doi\": \"test doi\","));
        assertThat(resultLines[3].trim(), is("\"alternative-doi\": \"test doi2\""));
        assertThat(resultLines[4].trim(), is("}"));
    }

    @Test
    public void testExportToDataciteFormatItemWithSingleDOINotMatchingPrefix() throws Exception {
        String prefix;
        prefix = this.configurationService.getProperty(CFG_PREFIX);
        if (null == prefix) {
            throw new RuntimeException("Unable to load DOI prefix from "
                    + "configuration. Cannot find property " +
                    CFG_PREFIX + ".");
        }

        context.turnOffAuthorisationSystem();

        Item publication = createItem(context, collection)
                .withEntityType("Publication")
                .withTitle("publication title")
                .withDoiIdentifier("test doi")
                .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk = new DSpace().getServiceManager()
                .getServiceByName("referCrosswalkVirtualFieldDOI", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publication, out);

        String[] resultLines = out.toString().split("\n");

        assertThat(resultLines.length, is(3));
        assertThat(resultLines[0].trim(), is("{"));
        assertThat(resultLines[1].trim(), is("\"primary-doi\": \"test doi\""));
        assertThat(resultLines[2].trim(), is("}"));
    }



    @Test
    public void testPublicationVirtualFieldWithVocabularyValuePairList() throws Exception {

        Locale defaultLocale = context.getCurrentLocale();
        String[] defaultLocales = this.configurationService.getArrayProperty("webui.supported.locales");

        try {

            Locale ukranian = new Locale("uk");

            context.turnOffAuthorisationSystem();
            // reset supported locales
            this.configurationService.setProperty(
                "webui.supported.locales",
                new String[] {Locale.ENGLISH.getLanguage(), Locale.ITALIAN.getLanguage(), ukranian.getLanguage()}
            );
            this.metadataAuthorityService.clearCache();
            this.choiceAuthorityService.clearCache();
            // reload plugin
            DCInputAuthority.reset();
            DCInputAuthority.getPluginNames();
            // set italian locale
            context.setCurrentLocale(Locale.ITALIAN);

            String vocabularyName = "publication-coar-types";
            Collection publicationCollection =
                createCollection(context, community)
                .withEntityType("Publication")
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

            Item publicationItem = createItem(context, publicationCollection)
                .withEntityType("Publication")
                .withTitle("Publication title")
                .withType("not translated", vocabularyName + ":c_7bab")
                .withLanguage("en_US")
                .build();

            context.restoreAuthSystemState();

            ReferCrosswalk referCrosswalk =
                new DSpace().getServiceManager()
                    .getServiceByName(
                        "referCrosswalkPublicationVirtualVocabularyI18nFieldWithVocabulary", ReferCrosswalk.class
                    );
            assertThat(referCrosswalk, notNullValue());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            referCrosswalk.disseminate(context, publicationItem, out);

            String[] resultLines = out.toString().split("\n");
            assertThat(resultLines.length, is(6));
            assertThat(resultLines[0].trim(), equalTo("<publication>"));
            assertThat(resultLines[3].trim(), equalTo("<VocabularyType>articolo sul software</VocabularyType>"));
            assertThat(resultLines[4].trim(), equalTo("<ValuePairLanguage>Inglese (USA)</ValuePairLanguage>"));
            assertThat(resultLines[5].trim(), equalTo("</publication>"));

            context.setCurrentLocale(ukranian);
            out = new ByteArrayOutputStream();
            referCrosswalk.disseminate(context, publicationItem, out);

            resultLines = out.toString().split("\n");
            assertThat(resultLines.length, is(6));
            assertThat(resultLines[0].trim(), equalTo("<publication>"));
            assertThat(resultLines[3].trim(), equalTo("<VocabularyType>програмна стаття</VocabularyType>"));
            assertThat(resultLines[4].trim(), equalTo("<ValuePairLanguage>Американська (USA)</ValuePairLanguage>"));
            assertThat(resultLines[5].trim(), equalTo("</publication>"));

        } finally {
            context.setCurrentLocale(defaultLocale);
            this.configurationService.setProperty("webui.supported.locales",defaultLocales);
        }
    }

    @Test
    public void testPublicationVirtualFieldValuePairList() throws Exception {

        context.turnOffAuthorisationSystem();
        String vocabularyName = "publication-coar-types";
        Collection publicationCollection =
            createCollection(context, community)
                .withEntityType("Publication")
                .withSubmissionDefinition("publication")
                .withAdminGroup(eperson)
                .build();

        Item publicationItem = createItem(context, publicationCollection)
            .withTitle("Publication title")
            .withType("not translated", vocabularyName + ":c_7bab")
            .withLanguage("en_US")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrosswalk =
            new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPublicationVirtualVocabularyI18nField", ReferCrosswalk.class);
        assertThat(referCrosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrosswalk.disseminate(context, publicationItem, out);

        String[] resultLines = out.toString().split("\n");
        assertThat(resultLines.length, is(6));
        assertThat(resultLines[0].trim(), equalTo("<publication>"));
        assertThat(resultLines[3].trim(), equalTo("<VocabularyType>software paper</VocabularyType>"));
        assertThat(resultLines[4].trim(), equalTo("<ValuePairLanguage>English (United States)</ValuePairLanguage>"));
        assertThat(resultLines[5].trim(), equalTo("</publication>"));
    }

    @Test
    public void testPublicationMultilanguageVirtualFieldValuePairList() throws Exception {

        Locale defaultLocale = context.getCurrentLocale();
        String[] defaultLocales = this.configurationService.getArrayProperty("webui.supported.locales");
        try {

            Locale ukranian = new Locale("uk");

            context.turnOffAuthorisationSystem();
            // reset supported locales
            this.configurationService.setProperty(
                "webui.supported.locales",
                new String[] {Locale.ENGLISH.getLanguage(), Locale.ITALIAN.getLanguage(), ukranian.getLanguage()}
            );
            this.metadataAuthorityService.clearCache();
            this.choiceAuthorityService.clearCache();
            // reload plugin
            DCInputAuthority.reset();
            DCInputAuthority.getPluginNames();
            // set italian locale
            context.setCurrentLocale(Locale.ITALIAN);

            String subjectVocabularyName = "publication-coar-types";
            Collection publicationCollection =
                createCollection(context, community)
                .withEntityType("Publication")
                .withSubmissionDefinition("languagetestprocess")
                .withAdminGroup(eperson)
                .build();

            Item publicationItem = createItem(context, publicationCollection)
                .withTitle("Publication title")
                .withType("not translated", subjectVocabularyName + ":c_7bab")
                .withLanguage("en_US")
                .build();

            this.itemService.addMetadata(
                context, publicationItem,
                "organization", "address", "addressCountry",
                Item.ANY, "IT", null, Choices.CF_UNSET, 0
            );

            context.restoreAuthSystemState();

            ReferCrosswalk referCrosswalk =
                new DSpace().getServiceManager()
                    .getServiceByName(
                        "referCrosswalkPublicationVirtualVocabularyI18nFieldWithVocabulary", ReferCrosswalk.class
                    );
            assertThat(referCrosswalk, notNullValue());

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            referCrosswalk.disseminate(context, publicationItem, out);

            String[] resultLines = out.toString().split("\n");
            assertThat(resultLines.length, is(7));
            assertThat(resultLines[0].trim(), equalTo("<publication>"));
            assertThat(resultLines[3].trim(), equalTo("<VocabularyType>articolo sul software</VocabularyType>"));
            assertThat(resultLines[4].trim(), equalTo("<ValuePairLanguage>Inglese (USA)</ValuePairLanguage>"));
            assertThat(resultLines[5].trim(), equalTo("<Country>Italia</Country>"));
            assertThat(resultLines[6].trim(), equalTo("</publication>"));

            context.turnOffAuthorisationSystem();
            // set uk locale
            context.setCurrentLocale(ukranian);
            context.restoreAuthSystemState();

            out = new ByteArrayOutputStream();
            referCrosswalk.disseminate(context, publicationItem, out);

            resultLines = out.toString().split("\n");
            assertThat(resultLines.length, is(7));
            assertThat(resultLines[0].trim(), equalTo("<publication>"));
            assertThat(resultLines[3].trim(), equalTo("<VocabularyType>програмна стаття</VocabularyType>"));
            assertThat(resultLines[4].trim(), equalTo("<ValuePairLanguage>Американська (USA)</ValuePairLanguage>"));
            // take value from submission_forms (_uk doesn't have the value-pair)
            assertThat(resultLines[5].trim(), equalTo("<Country>Italia</Country>"));
            assertThat(resultLines[6].trim(), equalTo("</publication>"));

            context.turnOffAuthorisationSystem();
            // set uknown locale
            context.setCurrentLocale(new Locale("ru"));
            context.restoreAuthSystemState();

            out = new ByteArrayOutputStream();
            referCrosswalk.disseminate(context, publicationItem, out);

            // it uses the default locale (en)
            resultLines = out.toString().split("\n");
            assertThat(resultLines.length, is(7));
            // takes the value from default (_ru doesn't exist)
            assertThat(resultLines[0].trim(), equalTo("<publication>"));
            assertThat(resultLines[3].trim(), equalTo("<VocabularyType>software paper</VocabularyType>"));
            assertThat(
                resultLines[4].trim(), equalTo("<ValuePairLanguage>English (United States)</ValuePairLanguage>")
            );
            // takes the value from submission_forms (_ru doesn't exist)
            assertThat(resultLines[5].trim(), equalTo("<Country>Italia</Country>"));
            assertThat(resultLines[6].trim(), equalTo("</publication>"));

        } finally {
            context.setCurrentLocale(defaultLocale);
            configurationService.setProperty("webui.supported.locales", defaultLocales);
            DCInputAuthority.reset();
            DCInputAuthority.getPluginNames();
        }
    }

    private void createSelectedRelationship(Item author, Item publication, RelationshipType selectedRelationshipType) {
        createRelationshipBuilder(context, publication, author, selectedRelationshipType, -1, -1).build();
    }

    private void compareEachLine(String result, String expectedResult) {

        String[] resultLines = result.split("\n");
        String[] expectedResultLines = expectedResult.split("\n");

        assertThat(sameLineCountReason(resultLines, expectedResultLines),
            resultLines.length, equalTo(expectedResultLines.length));

        for (int i = 0; i < resultLines.length; i++) {
            if (expectedResultLines[i].contains("SKIP-IN-COMPARING")) {
                continue;
            }
            assertThat(removeTabs(resultLines[i]), equalTo(removeTabs(expectedResultLines[i])));
        }
    }

    private String sameLineCountReason(String[] resultLines, String[] expectedResultLines) {
        String message = "The result should have the same lines number of the expected result.";
        String result = String.join("\n", resultLines);
        String expectedResult = String.join("\n", expectedResultLines);
        return message + "\nExpected:\n" + expectedResult + "\nActual:\n" + result;
    }

    private String removeTabs(String string) {
        return string != null ? string.replace("\t", "").trim() : null;
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }

    @Test
    public void testFundingXmlDisseminateWithRestrictedMetadata() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType entityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Funding").build();
        MetadataField currency = mfss.findByElement(context,
            "oairecerif","amount","currency");
        MetadataField amount = mfss.findByElement(context,
            "oairecerif","amount",null);

        DynamicLayoutField crisLayoutFieldCurrency = DynamicLayoutFieldBuilder.createMetadataField(context,currency,1,1)
            .build();
        DynamicLayoutField crisLayoutFieldAmount = DynamicLayoutFieldBuilder.createMetadataField(context,amount,0,1)
            .build();

        DynamicLayoutBoxBuilder.createBuilder(context, entityType, false, false)
            .withType("METADATA")
            .withShortname("financialdata")
            .addMetadataSecurityField(currency)
            .addMetadataSecurityField(amount)
            .addField(crisLayoutFieldCurrency)
            .addField(crisLayoutFieldAmount)
            .withSecurity(LayoutSecurity.CUSTOM_DATA).build();

        Item funding = ItemBuilder.createItem(context, collection)
            .withEntityType("Funding")
            .withAcronym("T-FU")
            .withTitle("Test Funding")
            .withType("Gift")
            .withInternalId("ID-01")
            .withFundingIdentifier("0001")
            .withDescription("Funding to test export")
            .withAmount("30.000,00")
            .withAmountCurrency("EUR")
            .withFunder("OrgUnit Funder")
            .withFundingStartDate("2015-01-01")
            .withFundingEndDate("2020-01-01")
            .withOAMandate("true")
            .withOAMandateURL("www.mandate.url")
            .build();

        context.restoreAuthSystemState();
        context.commit();

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("funding-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, funding, out);

        try (FileInputStream fis = getFileInputStream("funding1.xml")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent);
        }

    }

    @Test
    public void xmlDisseminateMetadataSecurityFirstLevelTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withEntityType("Publication")
                                          .withName("Collection Title").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withDoiIdentifier("doi:111.111/publication")
                               .withIssueDate("2020-01-01")
                               .build();

        itemService.addSecuredMetadata(context, item, "dc", "title", null, null, "Title test", null, 0, 0);
        itemService.addSecuredMetadata(context, item, "dc", "subject", null, null, "Subject test", null, 0, 1);
        itemService.addSecuredMetadata(context, item, "dc", "contributor", "author", null, "John Smith", null, 0, 2);

        MetadataField subject = mfss.findByElement(context, "dc", "subject", null);
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField contributor = mfss.findByElement(context, "dc", "contributor", "author");

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        DynamicLayoutBox box1 = DynamicLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, subject, 1, 0)
                              .withLabel("LABEL SUBJECT")
                              .withRendering("RENDERIGN SUBJECT")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, contributor, 2, 0)
                              .withLabel("LABEL CONTRBUTOR")
                              .withRendering("RENDERIGN CONTRIBUTOR")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();
        context.commit();
        context.setCurrentUser(null);

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications2.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedXml);
        }
    }

    @Test
    public void xmlDisseminateMetadataSecuritySecondLevelTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson user = EPersonBuilder.createEPerson(context)
                                      .withEmail("test-email@test.com")
                                      .withPassword(password)
                                      .withNameInMetadata("Bob", "Charlton")
                                      .build();

        GroupBuilder.createGroup(context)
                    .withName("Trusted")
                    .addMember(user)
                    .build();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withEntityType("Publication")
                                          .withName("Collection Title").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withDoiIdentifier("doi:111.111/publication")
                               .withIssueDate("2020-01-01")
                               .build();

        itemService.addSecuredMetadata(context, item, "dc", "title", null, null, "Title test", null, 0, 0);
        itemService.addSecuredMetadata(context, item, "dc", "subject", null, null, "Subject test", null, 0, 1);
        itemService.addSecuredMetadata(context, item, "dc", "contributor", "author", null, "John Smith", null, 0, 2);

        MetadataField subject = mfss.findByElement(context, "dc", "subject", null);
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField contributor = mfss.findByElement(context, "dc", "contributor", "author");

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        DynamicLayoutBox box1 = DynamicLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, subject, 1, 0)
                              .withLabel("LABEL SUBJECT")
                              .withRendering("RENDERIGN SUBJECT")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, contributor, 2, 0)
                              .withLabel("LABEL CONTRBUTOR")
                              .withRendering("RENDERIGN CONTRIBUTOR")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();
        context.commit();

        // disseminate with user that belongs to 'Trusted' group
        context.setCurrentUser(user);

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications3.xml")) {
            compareEachLine(out.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }

        // disseminate with user that does not belongs to 'Trusted' group
        context.setCurrentUser(eperson);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out2);

        try (FileInputStream fis = getFileInputStream("publications2.xml")) {
            compareEachLine(out2.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }
    }

    @Test
    public void xmlDisseminateMetadataSecurityThirdLevelTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson owner = EPersonBuilder.createEPerson(context)
                                      .withEmail("test-email@test.com")
                                      .withPassword(password)
                                      .withNameInMetadata("Bob", "Charlton")
                                      .build();

        parentCommunity = CommunityBuilder.createCommunity(context).build();
        Collection col = CollectionBuilder.createCollection(context, parentCommunity)
                                          .withEntityType("Publication")
                                          .withName("Collection Title").build();

        Item item = ItemBuilder.createItem(context, col)
                               .withDoiIdentifier("doi:111.111/publication")
                               .withDspaceObjectOwner("Owner", owner.getID().toString())
                               .withIssueDate("2020-01-01")
                               .build();

        itemService.addSecuredMetadata(context, item, "dc", "title", null, null, "Title test", null, 0, 0);
        itemService.addSecuredMetadata(context, item, "dc", "subject", null, null, "Subject test", null, 0, 1);
        itemService.addSecuredMetadata(context, item, "dc", "contributor", "author", null, "John Smith", null, 0, 2);

        MetadataField subject = mfss.findByElement(context, "dc", "subject", null);
        MetadataField title = mfss.findByElement(context, "dc", "title", null);
        MetadataField contributor = mfss.findByElement(context, "dc", "contributor", "author");

        EntityType eType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        DynamicLayoutBox box1 = DynamicLayoutBoxBuilder.createBuilder(context, eType, true, true)
                                                 .withShortname("box-shortname-one")
                                                 .withSecurity(LayoutSecurity.PUBLIC)
                                                 .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, title, 0, 0)
                              .withLabel("LABEL TITLE")
                              .withRendering("RENDERIGN TITLE")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, subject, 1, 0)
                              .withLabel("LABEL SUBJECT")
                              .withRendering("RENDERIGN SUBJECT")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        DynamicLayoutFieldBuilder.createMetadataField(context, contributor, 2, 0)
                              .withLabel("LABEL CONTRBUTOR")
                              .withRendering("RENDERIGN CONTRIBUTOR")
                              .withRowStyle("STYLE")
                              .withBox(box1)
                              .build();

        context.restoreAuthSystemState();
        context.commit();

        // disseminate with admin
        context.setCurrentUser(admin);

        ReferCrosswalk referCrossWalk = (ReferCrosswalk) crosswalkMapper.getByType("publication-cerif-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications4.xml")) {
            compareEachLine(out.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }

        // disseminate with owner of item
        context.setCurrentUser(owner);

        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, Arrays.asList(item).iterator(), out2);

        try (FileInputStream fis = getFileInputStream("publications4.xml")) {
            compareEachLine(out2.toString(), IOUtils.toString(fis, Charset.defaultCharset()));
        }
    }

}
