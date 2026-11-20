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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections.IteratorUtils;
import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.EPersonBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ZipItemExportCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private ZipItemExportCrosswalk zipItemExportCrosswalk;

    private Community community;

    private Collection collection;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        zipItemExportCrosswalk = new DSpace().getServiceManager()
            .getServicesByType(ZipItemExportCrosswalk.class).get(0);

        // The default upload.temp.dir (${dspace.dir}/upload) does not exist in the test
        // environment, so the METS AIP disseminator fails to create its temporary file.
        // Point it at the JVM temp directory, which always exists (same approach as
        // ITDSpaceAIP and PackagerIT).
        DSpaceServicesFactory.getInstance().getConfigurationService()
            .setProperty("upload.temp.dir", System.getProperty("java.io.tmpdir"));

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testItemsExportWithAdmin() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item1 = createItem("Test Item 1", "2022-01-01", "Luca Giamminonni");
        Item item2 = createItem("Test Item 2", "2022-03-01", "Walter White");
        Item item3 = createItem("Test Item 3", "2020-01-01", "Andrea Bollini");

        Bitstream bitstream1 = createBitstream(item1, "test.txt", "This is a test");
        Bitstream bitstream2 = createBitstream(item3, "test.pdf", "Last test", Period.ofMonths(6));

        String expectedEmbargo = LocalDate.now().plus(6, ChronoUnit.MONTHS).format(DateTimeFormatter.ISO_DATE);

        context.restoreAuthSystemState();

        context.setCurrentUser(admin);

        File tempZip = File.createTempFile("test", "zip");
        tempZip.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempZip)) {
            zipItemExportCrosswalk.disseminate(context, List.of(item1, item2, item3).iterator(), fos);
        }

        try (ZipFile zipFile = new ZipFile(tempZip)) {

            ZipEntry zipEntry = zipFile.getEntry(item1.getID().toString() + "/mets.xml");
            assertThat(zipEntry, notNullValue());

            String metsContent = getZipEntryContent(zipFile, zipEntry);

            assertThat(metsContent, containsString(
                "<dim:field mdschema=\"dc\" element=\"date\" qualifier=\"issued\">2022-01-01</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"title\">Test Item 1</dim:field>"));
            assertThat(metsContent, containsString("<dim:field mdschema=\"dc\" element=\"contributor\" "
                    + "qualifier=\"author\">Luca Giamminonni</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"creator\">test@email.com</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"title\">test.txt</dim:field>"));

            zipEntry = zipFile.getEntry(item1.getID().toString() + "/bitstream_" + bitstream1.getID().toString());
            assertThat(zipEntry, notNullValue());
            assertThat(getZipEntryContent(zipFile, zipEntry), is("This is a test"));

            zipEntry = zipFile.getEntry(item2.getID().toString() + "/mets.xml");
            assertThat(zipEntry, notNullValue());

            metsContent = getZipEntryContent(zipFile, zipEntry);

            assertThat(metsContent, containsString(
                "<dim:field mdschema=\"dc\" element=\"date\" qualifier=\"issued\">2022-03-01</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"title\">Test Item 2</dim:field>"));
            assertThat(metsContent, containsString("<dim:field mdschema=\"dc\" element=\"contributor\" "
                + "qualifier=\"author\">Walter White</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"creator\">test@email.com</dim:field>"));

            zipEntry = zipFile.getEntry(item3.getID().toString() + "/mets.xml");
            assertThat(zipEntry, notNullValue());

            metsContent = getZipEntryContent(zipFile, zipEntry);

            assertThat(metsContent, containsString(
                "<dim:field mdschema=\"dc\" element=\"date\" qualifier=\"issued\">2020-01-01</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"title\">Test Item 3</dim:field>"));
            assertThat(metsContent, containsString("<dim:field mdschema=\"dc\" element=\"contributor\" "
                + "qualifier=\"author\">Andrea Bollini</dim:field>"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"creator\">test@email.com</dim:field>"));
            assertThat(metsContent, containsString("<rights:Context in-effect=\"false\" "
                + "start-date=\"" + expectedEmbargo + "\" CONTEXTCLASS=\"GENERAL PUBLIC\">"));
            assertThat(metsContent,
                containsString("<dim:field mdschema=\"dc\" element=\"title\">test.pdf</dim:field>"));

            zipEntry = zipFile.getEntry(item3.getID().toString() + "/bitstream_" + bitstream2.getID().toString());
            assertThat(zipEntry, notNullValue());
            assertThat(getZipEntryContent(zipFile, zipEntry), is("Last test"));

            assertThat(getAllEntries(zipFile), hasSize(5));

        }

    }

    @Test
    public void testItemsExportWithCurators() throws Exception {

        context.turnOffAuthorisationSystem();

        Group curators = GroupBuilder.createGroup(context)
            .withName("Curators")
            .build();

        EPerson user = EPersonBuilder.createEPerson(context)
            .withEmail("user@test.com")
            .withGroupMembership(curators)
            .build();

        Item item1 = createItem("Test Item 1", "2022-01-01", "Luca Giamminonni");
        Item item2 = createItem("Test Item 2", "2022-03-01", "Walter White");
        Item item3 = createItem("Test Item 3", "2020-01-01", "Andrea Bollini");

        context.restoreAuthSystemState();

        context.setCurrentUser(user);

        File tempZip = File.createTempFile("test", "zip");
        tempZip.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempZip)) {
            zipItemExportCrosswalk.disseminate(context, List.of(item1, item2, item3).iterator(), fos);
        }

        try (ZipFile zipFile = new ZipFile(tempZip)) {
            assertThat(getAllEntries(zipFile), hasSize(3));
        }

    }

    @Test
    public void testItemsExportWithNotAuthorizedUser() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item1 = createItem("Test Item 1", "2022-01-01", "Luca Giamminonni");
        Item item2 = createItem("Test Item 2", "2022-03-01", "Walter White");
        Item item3 = createItem("Test Item 3", "2020-01-01", "Andrea Bollini");

        context.restoreAuthSystemState();

        context.setCurrentUser(eperson);

        File tempZip = File.createTempFile("test", "zip");
        tempZip.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempZip)) {

            AuthorizeException authorizeException = Assert.assertThrows(AuthorizeException.class,
                () -> zipItemExportCrosswalk.disseminate(context, List.of(item1, item2, item3).iterator(), fos));

            assertThat(authorizeException.getMessage(),
                is("The current user is not allowed to perform a zip item export"));
        }

    }

    private Item createItem(String title, String issueDate, String author) {
        return ItemBuilder.createItem(context, collection)
            .withTitle(title)
            .withIssueDate(issueDate)
            .withAuthor(author)
            .build();
    }

    private Bitstream createBitstream(Item item, String name, String content) throws Exception {
        return BitstreamBuilder.createBitstream(context, item, getInputStream(content))
            .withName(name)
            .build();
    }

    private Bitstream createBitstream(Item item, String name, String content, Period embargoPeriod) throws Exception {
        return BitstreamBuilder.createBitstream(context, item, getInputStream(content))
            .withName(name)
            .withEmbargoPeriod(embargoPeriod)
            .build();
    }

    private String getZipEntryContent(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        return IOUtils.toString(zipFile.getInputStream(zipEntry), StandardCharsets.UTF_8);
    }

    private InputStream getInputStream(String str) {
        return IOUtils.toInputStream(str, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private List<ZipEntry> getAllEntries(ZipFile zipFile) {
        return IteratorUtils.toList(zipFile.entries().asIterator());
    }

}
