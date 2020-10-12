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
import static org.dspace.core.CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.sql.SQLException;

import org.apache.commons.io.IOUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for the {@link ReferCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ReferCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_OUTPUT_DIR_PATH = "./target/testing/dspace/assetstore/refer-crosswalk/";

    private ReferCrosswalkMapper referCrosswalkMapper;

    private Community community;

    private Collection collection;

    @Before
    public void setup() throws SQLException, AuthorizeException {

        this.referCrosswalkMapper = new DSpace().getSingletonService(ReferCrosswalkMapper.class);
        assertThat(referCrosswalkMapper, notNullValue());

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    public void testBibtextDisseminate() throws Exception {

        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withTitle("Publication title")
            .withIssueDate("2018-05-17")
            .withAuthor("John Smith")
            .withAuthor("Edward Red")
            .withHandle("123456789/0001")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = referCrosswalkMapper.getReferCrosswalk("bibtex");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("publication.bib")) {
            String expectedBibtex = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedBibtex));
        }
    }

    @Test
    public void testPersonXmlDisseminate() throws Exception {
        context.turnOffAuthorisationSystem();

        Item item = createItem(context, collection)
            .withTitle("John Smith")
            .withFullName("John Smith")
            .withVariantName("J.S.")
            .withVariantName("Smith John")
            .withGivenName("John")
            .withFamilyName("Smith")
            .withBirthDate("1992-06-26")
            .withGender("M")
            .withJobTitle("Researcher")
            .withMainAffiliation("University")
            .withWorkingGroup("First work group")
            .withWorkingGroup("Second work group")
            .withPersonalSiteUrl("www.test.com")
            .withPersonalSiteTitle("Test")
            .withPersonalSiteUrl("www.john-smith.com")
            .withPersonalSiteTitle(PLACEHOLDER_PARENT_METADATA_VALUE)
            .withPersonalSiteUrl("www.site.com")
            .withPersonalSiteTitle("Site")
            .build();

        context.restoreAuthSystemState();

        ReferCrosswalk referCrossWalk = referCrosswalkMapper.getReferCrosswalk("person-xml");
        assertThat(referCrossWalk, notNullValue());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        referCrossWalk.disseminate(context, item, out);

        try (FileInputStream fis = getFileInputStream("person.xml")) {
            String expectedXml = IOUtils.toString(fis, Charset.defaultCharset());
            assertThat(out.toString(), equalTo(expectedXml));
        }
    }

    private FileInputStream getFileInputStream(String name) throws FileNotFoundException {
        return new FileInputStream(new File(BASE_OUTPUT_DIR_PATH, name));
    }
}
