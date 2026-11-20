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
import static org.dspace.builder.ItemBuilder.createItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CSLItemDataCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLItemDataCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private StreamDisseminationCrosswalkMapper crosswalkMapper;

    private CSLItemDataCrosswalk publicationHtmlCrosswalk;

    private Community community;

    private Collection collection;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        this.crosswalkMapper = new DSpace().getSingletonService(StreamDisseminationCrosswalkMapper.class);
        assertThat(crosswalkMapper, notNullValue());

        this.publicationHtmlCrosswalk = new DSpace().getServiceManager()
            .getServiceByName("referCrosswalkPublicationIeeeHtml", CSLItemDataCrosswalk.class);

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testSingleItemDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();
        Item item = createItem(context, collection)
            .withTitle("Publication title")
            .withEntityType("Publication")
            .withIssueDate("2018-05-17")
            .withHandle("123456789/0004")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withAuthor("John Smith")
            .withAuthor("Edward Red")
            .build();
        context.restoreAuthSystemState();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        publicationHtmlCrosswalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("publication-ieee.html")) {
            String expectedHtml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedHtml, false);
        }
    }

    @Test
    public void testManyItemsDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item firstItem = createItem(context, collection)
            .withTitle("Publication title")
            .withEntityType("Publication")
            .withIssueDate("2018-05-17")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withAuthor("John Smith")
            .withAuthor("Edward Red")
            .withHandle("123456789/0001")
            .build();

        Item secondItem = createItem(context, collection)
            .withTitle("Test publication")
            .withEntityType("Publication")
            .withIssueDate("2020-01-31")
            .withHandle("123456789/0003")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withAuthor("Walter White")
            .build();

        context.restoreAuthSystemState();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        publicationHtmlCrosswalk.disseminate(context, Arrays.asList(firstItem, secondItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications-ieee.html")) {
            String expectedHtml = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedHtml, false);
        }
    }

    @Test
    public void testBibtexDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withLanguage("en")
            .withDoiIdentifier("10.1000/182")
            .withIsbnIdentifier("11-22-33")
            .withIssnIdentifier("0002")
            .withSubject("publication")
            .withPublisher("Publisher")
            .withVolume("V01")
            .withIssue("3")
            .withRelationConference("Conference")
            .withTitle("Publication title")
            .withIssueDate("2018-05-17")
            .withAuthor("Smith, John")
            .withAuthor("Red, Edward")
            .withEditor("Editor")
            .withHandle("123456789/0001")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk crosswalk = crosswalkMapper.getByType("bibtex");
        assertThat(crosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crosswalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("publication.bib")) {
            String expectedBibtex = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedBibtex, false);
        }
    }

    @Test
    public void testSingleItemJsonDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withLanguage("en")
            .withDoiIdentifier("10.1000/182")
            .withIsbnIdentifier("11-22-33")
            .withIssnIdentifier("0002")
            .withSubject("publication")
            .withPublisher("Publisher")
            .withVolume("V01")
            .withIssue("03")
            .withRelationConference("Conference")
            .withTitle("Publication title")
            .withIssueDate("2018-05-17")
            .withAuthor("Smith, John")
            .withAuthor("Red, Edward")
            .withEditor("Editor")
            .withHandle("123456789/0001")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk crosswalk = crosswalkMapper.getByType("publication-json");
        assertThat(crosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crosswalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("publication.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson, true);
        }
    }

    @Test
    public void testMutlipleItemsJsonDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withLanguage("en")
            .withDoiIdentifier("10.1000/182")
            .withIsbnIdentifier("11-22-33")
            .withIssnIdentifier("0002")
            .withSubject("publication")
            .withPublisher("Publisher")
            .withVolume("V01")
            .withIssue("03")
            .withRelationConference("Conference")
            .withTitle("Publication title")
            .withIssueDate("2018-05-17")
            .withAuthor("Smith, John")
            .withAuthor("Red, Edward")
            .withEditor("Editor")
            .withHandle("123456789/0001")
            .build();

        Item anotherItem = createItem(context, collection)
            .withEntityType("Publication")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book")
            .withLanguage("en")
            .withDoiIdentifier("10.1000/183")
            .withTitle("Another Publication title")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter")
            .withHandle("123456789/0002")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk crosswalk = crosswalkMapper.getByType("publication-json");
        assertThat(crosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crosswalk.disseminate(context, Arrays.asList(item, anotherItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("publications.json")) {
            String expectedJson = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedJson, true);
        }
    }

    @Test
    public void testMutlipleItemsApaDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withEntityType("Publication")
            .withType("Controlled Vocabulary for Resource Type Genres::text::periodical::journal")
            .withLanguage("en")
            .withDoiIdentifier("10.1000/182")
            .withIsbnIdentifier("11-22-33")
            .withIssnIdentifier("0002")
            .withSubject("publication")
            .withPublisher("Publisher")
            .withVolume("V01")
            .withIssue("3")
            .withRelationConference("Conference")
            .withTitle("Publication title")
            .withIssueDate("2018-05-17")
            .withAuthor("Smith, John")
            .withAuthor("Red, Edward")
            .withEditor("Editor")
            .withHandle("123456789/0001")
            .build();

        Item anotherItem = createItem(context, collection)
            .withEntityType("Publication")
            .withType("Controlled Vocabulary for Resource Type Genres::text::book")
            .withLanguage("en")
            .withDoiIdentifier("10.1000/183")
            .withTitle("Another Publication title")
            .withIssueDate("2020-01-01")
            .withAuthor("White, Walter")
            .withHandle("123456789/0002")
            .build();

        context.restoreAuthSystemState();

        StreamDisseminationCrosswalk crosswalk = crosswalkMapper.getByType("publication-apa");
        assertThat(crosswalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        crosswalk.disseminate(context, Arrays.asList(item, anotherItem).iterator(), out);

        try (FileInputStream fis = getFileInputStream("apa.txt")) {
            String expectedContent = IOUtils.toString(fis, Charset.defaultCharset());
            compareEachLine(out.toString(), expectedContent, true);
        }
    }

    private void compareEachLine(String result, String expectedResult, boolean skipId) {

        String[] resultLines = result.split("\n");
        String[] expectedResultLines = expectedResult.split("\n");

        assertThat("The result should have the same lines number of the expected result",
            resultLines.length, equalTo(expectedResultLines.length));

        for (int i = 0; i < resultLines.length; i++) {
            String expectedResultLine = expectedResultLines[i];
            if (skipId && expectedResultLine.contains("id")) {
                continue;
            }
            assertThat(resultLines[i], equalTo(expectedResultLine));
        }
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
