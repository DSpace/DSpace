/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service;

import static java.util.regex.Pattern.compile;
import static org.dspace.app.launcher.ScriptLauncher.handleScript;
import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.app.matcher.ResourcePolicyMatcher.matches;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.dspace.core.Constants.READ;
import static org.dspace.core.Constants.WRITE;
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.CombinableMatcher.both;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.matcher.DSpaceObjectMatcher;
import org.dspace.app.matcher.LambdaMatcher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.content.dto.BitstreamDTO;
import org.dspace.content.dto.ItemDTO;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.dto.ResourcePolicyDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.ItemService;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link BulkImportWorkbookBuilder}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4Science)
 */
public class BulkImportWorkbookBuilderIT extends AbstractIntegrationTestWithDatabase {

    private static final Pattern UUID_PATTERN = compile(
        "[0-9a-fA-F]{8}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{4}\\-[0-9a-fA-F]{12}");

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();

    private final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    private final GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    private Collection publications;

    private Collection persons;

    private BulkImportWorkbookBuilder builder;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        builder = new DSpace().getServiceManager().getServicesByType(BulkImportWorkbookBuilder.class).get(0);

        context.turnOffAuthorisationSystem();
        parentCommunity = createCommunity(context).build();
        publications = createCollection(context, parentCommunity)
            .withEntityType("Publication")
            .build();
        persons = createCollection(context, parentCommunity)
            .withEntityType("Person")
            .build();
        context.restoreAuthSystemState();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testWorkbookBuildingFromItemDtos() throws Exception {

        configurationService.setProperty("uploads.local-folder", System.getProperty("java.io.tmpdir"));

        context.turnOffAuthorisationSystem();

        Item author = ItemBuilder.createItem(context, persons)
                                 .withTitle("White, Walter")
                                 .build();

        String authorId = author.getID().toString();

        Item testUser = ItemBuilder.createItem(context, persons)
                                   .withTitle("Test User")
                                   .build();

        Item jesse = ItemBuilder.createItem(context, persons)
                                .withTitle("Jesse Pinkman")
                                .build();

        context.restoreAuthSystemState();

        List<MetadataValueDTO> metadata = new ArrayList<>();
        metadata.add(new MetadataValueDTO("dc", "title", null, "Test Publication"));
        metadata.add(new MetadataValueDTO("dc", "date", "issued", "2020/02/15"));
        metadata.add(new MetadataValueDTO("dc", "type", null, "Article"));
        metadata.add(new MetadataValueDTO("dc", "subject", null, "Test"));
        metadata.add(new MetadataValueDTO("dc", "subject", null, "Java"));
        metadata.add(new MetadataValueDTO("dc", "subject", null, "DSpace"));
        metadata.add(new MetadataValueDTO("dc", "contributor", "author", null, "White, Walter", authorId, 600));
        metadata.add(new MetadataValueDTO("oairecerif", "author", "affiliation", PLACEHOLDER_PARENT_METADATA_VALUE));

        List<BitstreamDTO> bitstreams = new ArrayList<BitstreamDTO>();
        bitstreams.add(new BitstreamDTO("ORIGINAL", storeInTempLocation("First bitstream content"),
                                        List.of(new MetadataValueDTO("dc", "title", null, "Bitstream 1"))));
        bitstreams.add(new BitstreamDTO("ORIGINAL", storeInTempLocation("Second bitstream content"),
                                        List.of(new MetadataValueDTO("dc", "title", null, "Bitstream 2"))));

        ItemDTO firstItemDTO = new ItemDTO("DOI::12345", metadata, bitstreams);

        metadata = new ArrayList<>();
        metadata.add(new MetadataValueDTO("dc", "title", null, "Second Publication"));
        metadata.add(new MetadataValueDTO("dc", "date", "issued", "2022/02/15"));
        metadata.add(new MetadataValueDTO("dc", "type", null, "Book"));
        metadata.add(new MetadataValueDTO("dc", "language", "iso", "it"));
        metadata.add(new MetadataValueDTO("dc", "contributor", "author", null, "Jesse Pinkman",
                                          jesse.getID().toString(), 600));
        metadata.add(new MetadataValueDTO("oairecerif", "author", "affiliation", PLACEHOLDER_PARENT_METADATA_VALUE));
        metadata.add(new MetadataValueDTO("dc", "contributor", "author", null, "Test User",
                                          testUser.getID().toString(), 600));
        metadata.add(new MetadataValueDTO("oairecerif", "author", "affiliation", "Company"));

        bitstreams = new ArrayList<BitstreamDTO>();

        bitstreams.add(new BitstreamDTO("ORIGINAL", storeInTempLocation("Third bitstream content"),
                                        List.of(new MetadataValueDTO("dc", "title", null, "Bitstream 3"))));

        List<ResourcePolicyDTO> policies = new ArrayList<>();
        policies.add(new ResourcePolicyDTO("embargo", "Test policy", READ, TYPE_CUSTOM, parseDate("2025-02-12"), null));
        policies.add(new ResourcePolicyDTO("lease", "My policy", WRITE, TYPE_CUSTOM, null, parseDate("2025-02-12")));

        bitstreams.add(new BitstreamDTO("MY BUNDLE", storeInTempLocation("Fourth bitstream content"),
                                        List.of(new MetadataValueDTO("dc", "title", null, "Bitstream 4")), policies));

        ItemDTO secondItemDTO = new ItemDTO("DOI::98765", false, metadata, bitstreams);

        Workbook workbook = builder.build(context, publications, List.of(firstItemDTO, secondItemDTO).iterator());

        String tempLocation = storeInTempLocation(workbook);

        String[] args = new String[] {"bulk-import", "-c", publications.getID().toString(), "-f", tempLocation,
            "-e", admin.getEmail()};
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();

        handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl, eperson);
        assertThat(handler.getErrorMessages(), empty());
        assertThat(handler.getWarningMessages(), empty());

        assertThat(handler.getInfoMessages(), contains(
            is("Start reading all the metadata group rows"),
            is("Found 3 metadata groups to process"),
            is("Start reading all the bitstream rows"),
            is("Found 4 bitstreams to process"),
            is("Found 2 items to process"),
            containsString("Sheet bitstream-metadata - Row 2 - Bitstream created successfully"),
            containsString("Sheet bitstream-metadata - Row 3 - Bitstream created successfully"),
            containsString("Row 2 - WorkflowItem created successfully"),
            containsString("Sheet bitstream-metadata - Row 4 - Bitstream created successfully"),
            containsString("Sheet bitstream-metadata - Row 5 - Bitstream created successfully"),
            containsString("Row 3 - WorkflowItem created successfully")));

        Item firstItem = getItemFromMessage(handler.getInfoMessages().get(7));
        assertThat(firstItem, notNullValue());
        assertThat(firstItem.getMetadata(), hasSize(19));
        assertThat(firstItem.getMetadata(), hasItems(
            with("dc.title", "Test Publication"),
            with("dc.date.issued", "2020/02/15"),
            with("dspace.entity.type", "Publication"),
            with("dc.type", "Article"),
            with("dc.subject", "Test"),
            with("dc.subject", "Java", 1),
            with("dc.subject", "DSpace", 2),
            with("dc.contributor.author", "White, Walter", authorId, 600),
            with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)));

        assertThat(getItemBitstreamsByBundle(firstItem, "ORIGINAL"), contains(
            bitstreamWith("Bitstream 1", "First bitstream content"),
            bitstreamWith("Bitstream 2", "Second bitstream content")));

        Item secondItem = getItemFromMessage(handler.getInfoMessages().get(10));
        assertThat(secondItem, notNullValue());
        assertThat(secondItem.getMetadata(), hasSize(23));
        assertThat(secondItem.getMetadata(), hasItems(
            with("dc.title", "Second Publication"),
            with("dc.date.issued", "2022/02/15"),
            with("dspace.entity.type", "Publication"),
            with("dc.type", "Book"),
            with("dc.language.iso", "it"),
            with("dc.contributor.author", "Jesse Pinkman", jesse.getID().toString(), 600),
            with("dc.contributor.author", "Test User", testUser.getID().toString(), 1, 600),
            with("oairecerif.author.affiliation", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("oairecerif.author.affiliation", "Company", 1),
            with("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.department", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE),
            with("cris.virtual.orcid", PLACEHOLDER_PARENT_METADATA_VALUE)
        ));

        assertThat(getItemBitstreamsByBundle(secondItem, "ORIGINAL"), contains(
            bitstreamWith("Bitstream 3", "Third bitstream content")));

        List<Bitstream> secondItemBitstreams = getItemBitstreamsByBundle(secondItem, "MY BUNDLE");
        assertThat(secondItemBitstreams, hasSize(1));

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Bitstream bitstream = secondItemBitstreams.get(0);
        assertThat(bitstream, bitstreamWith("Bitstream 4", "Fourth bitstream content"));
        assertThat(bitstream.getResourcePolicies(), contains(
            matches(READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2025-02-12", null, "Test policy")));

    }

    private Item getItemFromMessage(String message) throws SQLException {
        Matcher matcher = UUID_PATTERN.matcher(message);
        if (!matcher.find()) {
            return null;
        }

        String uuid = matcher.group(0);
        return itemService.find(context, UUID.fromString(uuid));
    }

    private List<Bitstream> getItemBitstreamsByBundle(Item item, String bundleName) {
        try {
            return itemService.getBundles(item, bundleName).stream()
                              .flatMap(bundle -> bundle.getBitstreams().stream())
                              .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private org.hamcrest.Matcher<Bitstream> bitstreamWith(String title, String content) {
        return both(hasContent(content)).and(hasTitle(title));
    }

    private org.hamcrest.Matcher<Bitstream> hasContent(String content) {
        return LambdaMatcher.matches(bitstream -> content.equals(getBitstreamContent(bitstream)),
                                     "Expected bitstream with content " + content);
    }

    @SuppressWarnings("unchecked")
    private org.hamcrest.Matcher<? super Bitstream> hasTitle(String title) {
        return DSpaceObjectMatcher.withMetadata(hasItems(with("dc.title", title)));
    }

    private String getBitstreamContent(Bitstream bitstream) {
        context.turnOffAuthorisationSystem();
        try {
            InputStream inputStream = bitstreamService.retrieve(context, bitstream);
            return IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        } catch (IOException | SQLException | AuthorizeException e) {
            throw new RuntimeException(e);
        } finally {
            context.restoreAuthSystemState();
        }
    }

    private LocalDate parseDate(String date) throws ParseException {
        return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
    }

    private String storeInTempLocation(String fileContent) throws IOException {
        File tempFile = File.createTempFile("test-bulk-import-workbook-builder", "txt");
        IOUtils.write(fileContent, new FileOutputStream(tempFile), StandardCharsets.UTF_8);
        return "file://" + tempFile.getAbsolutePath();
    }

    private String storeInTempLocation(Workbook workbook) throws IOException {
        File tempFile = File.createTempFile("test-bulk-import-workbook-builder", "xls");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            workbook.write(fos);
        }
        return tempFile.getAbsolutePath();
    }

}
