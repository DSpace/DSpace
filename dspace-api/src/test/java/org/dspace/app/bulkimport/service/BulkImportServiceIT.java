/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkimport.model.ImportParams;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.junit.Test;

public class BulkImportServiceIT extends AbstractIntegrationTestWithDatabase {

    private static final String BASE_XLS_DIR_PATH = "./target/testing/dspace/assetstore/bulk-import/";

    private BulkImportService bulkImportService = BulkImportServiceFactory.getInstance().getBulkImportService();

    @Test
    public void testImport() throws FileNotFoundException, IOException {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).build();
        Collection collection = CollectionBuilder.createCollection(context, community).build();
        context.restoreAuthSystemState();

        File xls = getXlsFile("test-ok.xls");

        try (FileInputStream fis = new FileInputStream(xls)) {
            bulkImportService.performImport(context, fis, new ImportParams(true, collection));
        }
    }

    private File getXlsFile(String name) {
        return new File(BASE_XLS_DIR_PATH, name);
    }
}
