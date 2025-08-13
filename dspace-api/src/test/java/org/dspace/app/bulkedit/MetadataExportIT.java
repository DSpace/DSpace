/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkedit;

import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.commons.cli.ParseException;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.scripts.DSpaceRunnable;
import org.dspace.scripts.configuration.ScriptConfiguration;
import org.dspace.scripts.factory.ScriptServiceFactory;
import org.dspace.scripts.service.ScriptService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class MetadataExportIT
        extends AbstractIntegrationTestWithDatabase {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private final ConfigurationService configurationService
            = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Test
    public void metadataExportToCsvTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                .build();
        Item item = ItemBuilder.createItem(context, collection)
                .withAuthor("Donald, Smith")
                .build();
        context.restoreAuthSystemState();
        String fileLocation = configurationService.getProperty("dspace.dir")
                + testProps.get("test.exportcsv").toString();

        String[] args = new String[] {"metadata-export",
            "-i", String.valueOf(item.getHandle()),
            "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
                = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl),
                testDSpaceRunnableHandler, kernelImpl);
        File file = new File(fileLocation);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Donald, Smith"));
        assertTrue(fileContent.contains(String.valueOf(item.getID())));
    }

    @Test(expected = ParseException.class)
    public void metadataExportWithoutFileParameter()
        throws IllegalAccessException, InstantiationException, ParseException {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .build();
        Item item = ItemBuilder.createItem(context, collection)
                               .withAuthor("Donald, Smith")
                               .build();
        context.restoreAuthSystemState();

        String[] args = new String[] {"metadata-export",
            "-i", String.valueOf(item.getHandle())};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                script.run();
            }
        }
    }

    @Test
    public void metadataExportToCsvTestUUID() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .build();
        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Donald, Smith")
            .build();
        context.restoreAuthSystemState();
        String fileLocation = configurationService.getProperty("dspace.dir")
            + testProps.get("test.exportcsv").toString();

        String[] args = new String[] {"metadata-export",
            "-i", String.valueOf(item.getID()),
            "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
            = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl),
            testDSpaceRunnableHandler, kernelImpl);
        File file = new File(fileLocation);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Donald, Smith"));
        assertTrue(fileContent.contains(String.valueOf(item.getID())));
    }

    @Test
    public void metadataExportToCsvTestUUIDParent() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .build();
        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Donald, Smith")
            .build();
        context.restoreAuthSystemState();
        String fileLocation = configurationService.getProperty("dspace.dir")
            + testProps.get("test.exportcsv").toString();

        String[] args = new String[] {"metadata-export",
            "-i", String.valueOf(collection.getID()),
            "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
            = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl),
            testDSpaceRunnableHandler, kernelImpl);
        File file = new File(fileLocation);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Donald, Smith"));
        assertTrue(fileContent.contains(String.valueOf(item.getID())));
    }

    @Test
    public void metadataExportToCsvTestUUIDGrandParent() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .build();
        Item item = ItemBuilder.createItem(context, collection)
            .withAuthor("Donald, Smith")
            .build();
        context.restoreAuthSystemState();
        String fileLocation = configurationService.getProperty("dspace.dir")
            + testProps.get("test.exportcsv").toString();

        String[] args = new String[] {"metadata-export",
            "-i", String.valueOf(community.getID()),
            "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
            = new TestDSpaceRunnableHandler();

        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl),
            testDSpaceRunnableHandler, kernelImpl);
        File file = new File(fileLocation);
        String fileContent = IOUtils.toString(new FileInputStream(file), StandardCharsets.UTF_8);
        assertTrue(fileContent.contains("Donald, Smith"));
        assertTrue(fileContent.contains(String.valueOf(item.getID())));
    }

    @Test
    public void metadataExportToCsvTest_NonValidIdentifier() throws Exception {
        String fileLocation = configurationService.getProperty("dspace.dir")
                              + testProps.get("test.exportcsv").toString();

        String nonValidUUID = String.valueOf(UUID.randomUUID());
        String[] args = new String[] {"metadata-export", "-i", nonValidUUID, "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
            = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                script.run();
            }
        }

        Exception exceptionDuringTestRun = testDSpaceRunnableHandler.getException();
        assertTrue("Random UUID caused IllegalArgumentException",
            exceptionDuringTestRun instanceof IllegalArgumentException);
        assertTrue("IllegalArgumentException contains mention of the non-valid UUID",
            StringUtils.contains(exceptionDuringTestRun.getMessage(), nonValidUUID));
    }

    @Test
    public void metadataExportToCsvTest_NonValidDSOType() throws Exception {
        String fileLocation = configurationService.getProperty("dspace.dir")
                              + testProps.get("test.exportcsv").toString();

        String uuidNonValidDSOType = String.valueOf(eperson.getID());
        String[] args = new String[] {"metadata-export", "-i", uuidNonValidDSOType, "-f", fileLocation};
        TestDSpaceRunnableHandler testDSpaceRunnableHandler
            = new TestDSpaceRunnableHandler();

        ScriptService scriptService = ScriptServiceFactory.getInstance().getScriptService();
        ScriptConfiguration scriptConfiguration = scriptService.getScriptConfiguration(args[0]);

        DSpaceRunnable script = null;
        if (scriptConfiguration != null) {
            script = scriptService.createDSpaceRunnableForScriptConfiguration(scriptConfiguration);
        }
        if (script != null) {
            if (DSpaceRunnable.StepResult.Continue.equals(script.initialize(args, testDSpaceRunnableHandler, null))) {
                script.run();
            }
        }

        Exception exceptionDuringTestRun = testDSpaceRunnableHandler.getException();
        assertTrue("UUID of non-supported dsoType IllegalArgumentException",
            exceptionDuringTestRun instanceof IllegalArgumentException);
        assertTrue("IllegalArgumentException contains mention of the UUID of non-supported dsoType",
            StringUtils.contains(exceptionDuringTestRun.getMessage(), uuidNonValidDSOType));
        assertTrue("IllegalArgumentException contains mention of the non-supported dsoType",
            StringUtils.contains(exceptionDuringTestRun.getMessage(), Constants.typeText[eperson.getType()]));
    }
}
