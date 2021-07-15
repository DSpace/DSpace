/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.packager;

import java.io.File;

import org.dspace.app.rest.test.AbstractEntityIntegrationTest;
import org.dspace.builder.CollectionBuilder; import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.service.ItemService;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


// See CsvImportIT for other examples involving rels
public class PackagerIT extends AbstractEntityIntegrationTest {

    @Autowired
    private ItemService itemService;

    @Test
    public void packagerExportUUIDTest() throws Exception {
        context.turnOffAuthorisationSystem();
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                .withName("Sub Community")
                .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();

        // Create a new Publication (which is an Article)
        Item article = ItemBuilder.createItem(context, col1)
                .withTitle("Article")
                .withIssueDate("2017-10-17")
                .withEntityType("Publication")
                .build();

        File tempFile = File.createTempFile("packagerExportTest", "zip");
        try {
            performExportScript(article.getHandle(), tempFile);
            // TODO: verify the file has the uuid in the right place
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void packagerImportUUIDTest() {

    }

    private void performExportScript(String handle, File outputFile) throws Exception {
        runDSpaceScript("packager", "-d", "-e", "admin@email.com", "-i", handle, "aip", outputFile.getPath());
    }
}
