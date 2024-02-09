/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkaccesscontrol;

import static org.dspace.app.matcher.ResourcePolicyMatcher.matches;
import static org.dspace.authorize.ResourcePolicy.TYPE_CUSTOM;
import static org.dspace.authorize.ResourcePolicy.TYPE_INHERITED;
import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;
import static org.dspace.core.Constants.DEFAULT_BUNDLE_NAME;
import static org.dspace.core.Constants.READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.mediafilter.FormatFilter;
import org.dspace.app.mediafilter.factory.MediaFilterServiceFactory;
import org.dspace.app.mediafilter.service.MediaFilterService;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.GroupBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic integration testing for the Bulk Access conditions Feature{@link BulkAccessControl}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessControlIT extends AbstractIntegrationTestWithDatabase {

    //key (in dspace.cfg) which lists all enabled filters by name
    private static final String MEDIA_FILTER_PLUGINS_KEY = "filter.plugins";

    //prefix (in dspace.cfg) for all filter properties
    private static final String FILTER_PREFIX = "filter";

    //suffix (in dspace.cfg) for input formats supported by each filter
    private static final String INPUT_FORMATS_SUFFIX = "inputFormats";

    private Path tempDir;
    private String tempFilePath;

    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private SearchService searchService = SearchUtils.getSearchService();
    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        tempDir = Files.createTempDirectory("bulkAccessTest");
        tempFilePath = tempDir + "/bulk-access.json";
    }

    @After
    @Override
    public void destroy() throws Exception {
        PathUtils.deleteDirectory(tempDir);
        super.destroy();
    }

    @Test
    public void performBulkAccessWithAnonymousEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("An eperson to do the the Bulk Access Control must be specified")
        ));
    }

    @Test
    public void performBulkAccessWithNotExistingEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        String randomUUID = UUID.randomUUID().toString();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
        "-e", randomUUID};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("EPerson cannot be found: " + randomUUID)
        ));
    }

    @Test
    public void performBulkAccessWithNotAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
        "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("Current user is not eligible to execute script bulk-access-control")
        ));
    }

    @Test
    public void performBulkAccessWithCommunityAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .withAdminGroup(eperson)
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", community.getID().toString(), "-f", tempFilePath,
        "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(0));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
    }

    @Test
    public void performBulkAccessWithCollectionAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .withAdminGroup(eperson)
                                                 .build();

        ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", collection.getID().toString(), "-f", tempFilePath,
        "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(0));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
    }

    @Test
    public void performBulkAccessWithItemAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withAdminUser(eperson).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
        "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(0));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
    }

    @Test
    public void performBulkAccessWithNotCollectionAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        // add eperson to admin group
        Collection collectionOne = CollectionBuilder.createCollection(context, community)
                                                    .withName("collection")
                                                    .withAdminGroup(eperson)
                                                    .build();

        Collection collectionTwo = CollectionBuilder.createCollection(context, community)
                                                    .withName("collection")
                                                    .build();

        ItemBuilder.createItem(context, collectionOne).build();
        ItemBuilder.createItem(context, collectionTwo).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control",
            "-u", collectionOne.getID().toString(),
            "-u", collectionTwo.getID().toString(),
            "-f", tempFilePath,
            "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("Current user is not eligible to execute script bulk-access-control")
        ));
    }

    @Test
    public void performBulkAccessWithNotCommunityAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        // add eperson to admin group
        Community communityOne = CommunityBuilder.createCommunity(context)
                                                 .withName("community")
                                                 .withAdminGroup(eperson)
                                                 .build();

        Community communityTwo = CommunityBuilder.createCommunity(context)
                                                 .withName("community")
                                                 .build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control",
            "-u", communityOne.getID().toString(),
            "-u", communityTwo.getID().toString(),
            "-f", tempFilePath,
            "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("Current user is not eligible to execute script bulk-access-control")
        ));
    }

    @Test
    public void performBulkAccessWithNotItemAdminEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                    .withName("collection")
                                                    .build();
        // add eperson to admin group
        Item itemOne = ItemBuilder.createItem(context, collection)
                                  .withAdminUser(eperson)
                                  .build();

        Item itemTwo = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control",
            "-u", itemOne.getID().toString(),
            "-u", itemTwo.getID().toString(),
            "-f", tempFilePath,
            "-e", eperson.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("Current user is not eligible to execute script bulk-access-control")
        ));
    }

    @Test
    public void performBulkAccessWithoutRequiredParamTest() throws Exception {

        buildJsonFile("");

        String[] args = new String[] {"bulk-access-control", "-f", tempFilePath, "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("A target uuid must be provided with at least on uuid")
        ));
    }

    @Test
    public void performBulkAccessWithEmptyJsonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).withTitle("title").build();

        context.restoreAuthSystemState();

        buildJsonFile("");

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("Error parsing json file")
        ));
    }

    @Test
    public void performBulkAccessWithWrongModeOfItemValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"wrong\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("wrong value for item mode<wrong>")
        ));
    }

    @Test
    public void performBulkAccessWithMissingModeOfItemValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("item mode node must be provided")
        ));
    }

    @Test
    public void performBulkAccessWithWrongModeOfBitstreamValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"bitstream\": {\n" +
            "      \"mode\": \"wrong\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("wrong value for bitstream mode<wrong>")
        ));
    }

    @Test
    public void performBulkAccessWithMissingModeOfBitstreamValueTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"bitstream\": {\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("bitstream mode node must be provided")
        ));
    }

    @Test
    public void performBulkAccessWithNotFoundAccessConditionNameTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"wrongAccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("wrong access condition <wrongAccess>")
        ));
    }

    @Test
    public void performBulkAccessWithInvalidEmbargoAccessConditionDateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"endDate\": \"2024-06-24T00:00:00Z\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("invalid access condition, The access condition embargo requires a start date.")
        ));
    }

    @Test
    public void performBulkAccessWithInvalidLeaseAccessConditionDateTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String json = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"lease\",\n" +
            "            \"startDate\": \"2024-06-24T00:00:00Z\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath,
            "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("invalid access condition, The access condition lease requires an end date.")
        ));
    }

    @Test
    public void performBulkAccessForCommunityItemsWithBitstreamConstraintsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community communityOne = CommunityBuilder.createCommunity(context)
                                                 .withName("community one")
                                                 .build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"bitstream\": {\n" +
            "      \"constraints\": {\n" +
            "          \"uuid\": [\"" + UUID.randomUUID() + "\"]\n" +
            "      },\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args =
            new String[] {"bulk-access-control",
                "-u", communityOne.getID().toString(),
                "-f", tempFilePath,
                "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("constraint is not supported when uuid isn't an Item")
        ));
    }

    @Test
    public void performBulkAccessForMultipleItemsWithBitstreamConstraintsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community communityOne = CommunityBuilder.createCommunity(context)
                                                 .withName("community one")
                                                 .build();

        Community communityTwo = CommunityBuilder.createCommunity(context)
                                                 .withName("community two")
                                                 .build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"bitstream\": {\n" +
            "      \"constraints\": {\n" +
            "          \"uuid\": [\"" + UUID.randomUUID() + "\"]\n" +
            "      },\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args =
            new String[] {"bulk-access-control",
                "-u", communityOne.getID().toString(),
                "-u", communityTwo.getID().toString(),
                "-f", tempFilePath,
                "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("constraint isn't supported when multiple uuids are provided")
        ));
    }

    @Test
    public void performBulkAccessForSingleItemWithBitstreamConstraintsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                                              .withName("community one")
                                              .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("ORIGINAL")
                                     .build();

        String bitstreamOneContent = "Dummy content one";
        Bitstream bitstreamOne;
        try (InputStream is = IOUtils.toInputStream(bitstreamOneContent, CharEncoding.UTF_8)) {
            bitstreamOne  = BitstreamBuilder.createBitstream(context, bundle, is)
                                            .withName("bistream one")
                                            .build();
        }

        String bitstreamTwoContent = "Dummy content of bitstream two";
        Bitstream bitstreamTwo;
        try (InputStream is = IOUtils.toInputStream(bitstreamTwoContent, CharEncoding.UTF_8)) {
            bitstreamTwo  = BitstreamBuilder.createBitstream(context, bundle, is)
                                            .withName("bistream two")
                                            .build();
        }

        context.restoreAuthSystemState();

        String jsonOne = "{ \"bitstream\": {\n" +
            "      \"constraints\": {\n" +
            "          \"uuid\": [\"" + bitstreamOne.getID().toString() + "\"]\n" +
            "      },\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args =
            new String[] {"bulk-access-control",
                "-u", item.getID().toString(),
                "-f", tempFilePath,
                "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(1));

        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasItem(
            containsString("Replacing Bitstream {" + bitstreamOne.getID() +
                "} policy to access conditions:{embargo, start_date=2024-06-24}")));

        bitstreamOne = context.reloadEntity(bitstreamOne);
        bitstreamTwo = context.reloadEntity(bitstreamTwo);

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        assertThat(bitstreamOne.getResourcePolicies(), hasSize(1));
        assertThat(bitstreamOne.getResourcePolicies(), hasItem(
            matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
        ));

        assertThat(bitstreamTwo.getResourcePolicies(), hasSize(1));
        assertThat(bitstreamTwo.getResourcePolicies(), hasItem(
            matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
        ));
    }

    @Test
    public void performBulkAccessWithAddModeAndEmptyAccessConditionsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                                    .withName("parent community")
                                                    .build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"add\"\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args =
            new String[] {"bulk-access-control", "-u", parentCommunity.getID().toString(), "-f", tempFilePath,
                "-e", admin.getEmail()};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("accessConditions of item must be provided with mode<add>")
        ));
    }

    @Test
    public void performBulkAccessWithValidJsonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                                    .withName("parent community")
                                                    .build();

        Community subCommunityOne = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                    .withName("sub community one")
                                                    .build();

        Community subCommunityTwo = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                    .withName("sub community two")
                                                    .build();

        Community subCommunityThree = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                      .withName("sub community three")
                                                      .build();

        Collection collectionOne = CollectionBuilder.createCollection(context, subCommunityOne)
                                                    .withName("collection one")
                                                    .build();

        Collection collectionTwo = CollectionBuilder.createCollection(context, subCommunityTwo)
                                                    .withName("collection two")
                                                    .build();

        Collection collectionThree = CollectionBuilder.createCollection(context, subCommunityThree)
                                                      .withName("collection three")
                                                      .build();

        Item itemOne = ItemBuilder.createItem(context, collectionOne).build();

        Item itemTwo = ItemBuilder.createItem(context, collectionTwo).build();

        Item itemThree = ItemBuilder.createItem(context, collectionThree).withTitle("item three title").build();

        Item itemFour = ItemBuilder.createItem(context, collectionThree).withTitle("item four title").build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", subCommunityOne.getID().toString(),
            "-u", collectionTwo.getID().toString(),
            "-u", itemThree.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(3));

        assertThat(testDSpaceRunnableHandler.getInfoMessages(), containsInAnyOrder(
            containsString("Replacing Item {" + itemOne.getID() +
                "} policy to access conditions:{embargo, start_date=2024-06-24}"),
            containsString("Replacing Item {" + itemTwo.getID() +
                "} policy to access conditions:{embargo, start_date=2024-06-24}"),
            containsString("Replacing Item {" + itemThree.getID() +
                "} policy to access conditions:{embargo, start_date=2024-06-24}")
        ));

        itemOne = context.reloadEntity(itemOne);
        itemTwo = context.reloadEntity(itemTwo);
        itemThree = context.reloadEntity(itemThree);
        itemFour = context.reloadEntity(itemFour);

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        assertThat(itemOne.getResourcePolicies(), hasSize(1));
        assertThat(itemOne.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
        ));

        assertThat(itemTwo.getResourcePolicies(), hasSize(1));
        assertThat(itemTwo.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
        ));

        assertThat(itemThree.getResourcePolicies(), hasSize(1));
        assertThat(itemThree.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
        ));

        assertThat(itemFour.getResourcePolicies().size(), is(1));
        assertThat(itemFour.getResourcePolicies(), hasItem(
            matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
        ));




    }

    @Test
    public void performBulkAccessWithReplaceModeAndEmptyAccessConditionsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("parent community")
                                              .build();

        Community subCommunityOne = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                              .withName("sub community one")
                                              .build();

        Community subCommunityTwo = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                    .withName("sub community two")
                                                    .build();

        Collection collectionOne = CollectionBuilder.createCollection(context, subCommunityOne)
                                                 .withName("collection one")
                                                 .build();

        Collection collectionTwo = CollectionBuilder.createCollection(context, subCommunityTwo)
                                                 .withName("collection two")
                                                 .build();

        for (int i = 0; i < 20 ; i++) {
            ItemBuilder.createItem(context, collectionOne).build();
        }

        for (int i = 0; i < 5 ; i++) {
            Item item = ItemBuilder.createItem(context, collectionTwo).build();

            Bundle bundle = BundleBuilder.createBundle(context, item)
                                         .withName("ORIGINAL")
                                         .build();

            String bitstreamContent = "Dummy content";
            try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
                BitstreamBuilder.createBitstream(context, bundle, is)
                                .withName("bistream")
                                .build();
            }
        }

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"replace\"\n" +
            "   },\n" +
            " \"bitstream\": {\n" +
            "      \"mode\": \"replace\"\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", subCommunityOne.getID().toString(),
            "-u", collectionTwo.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(60));

        List<Item> itemsOfSubCommOne = findItems("location.comm:" + subCommunityOne.getID());
        List<Item> itemsOfSubCommTwo = findItems("location.comm:" + subCommunityTwo.getID());

        assertThat(itemsOfSubCommOne, hasSize(10));
        assertThat(itemsOfSubCommTwo, hasSize(5));

        assertThat(itemsOfSubCommOne.stream()
                                    .flatMap(item -> findAllBitstreams(item).stream())
                                    .count(), is(0L));

        assertThat(itemsOfSubCommTwo.stream()
                                    .flatMap(item -> findAllBitstreams(item).stream())
                                    .count(), is(5L));

        for (Item item : itemsOfSubCommOne) {
            assertThat(item.getResourcePolicies(), hasSize(1));
            assertThat(item.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
            ));
        }

        for (Item item : itemsOfSubCommTwo) {
            assertThat(item.getResourcePolicies(), hasSize(1));
            assertThat(item.getResourcePolicies(), hasItem(
                matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
            ));

            assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasItems(
                containsString("Cleaning Item {" + item.getID() + "} policies"),
                containsString("Inheriting policies from owning Collection in Item {" + item.getID() + "")
            ));

            List<Bitstream> bitstreams = findAllBitstreams(item);

            for (Bitstream bitstream : bitstreams) {
                assertThat(bitstream.getResourcePolicies(), hasSize(1));
                assertThat(bitstream.getResourcePolicies(), hasItem(
                    matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
                ));

                assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasItems(
                    containsString("Cleaning Bitstream {" + bitstream.getID() + "} policies"),
                    containsString("Inheriting policies from owning Collection in Bitstream {" + bitstream.getID() + "")
                ));
            }
        }
    }

    @Test
    public void performBulkAccessWithAddModeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("parent community")
                                              .build();

        Community subCommunityOne = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                              .withName("sub community one")
                                              .build();

        Collection collectionOne = CollectionBuilder.createCollection(context, subCommunityOne)
                                                 .withName("collection one")
                                                 .build();

        for (int i = 0; i < 5 ; i++) {

            Item item = ItemBuilder.createItem(context, collectionOne).build();

            Bundle bundle = BundleBuilder.createBundle(context, item)
                                         .withName("ORIGINAL")
                                         .build();

            String bitstreamContent = "Dummy content";
            try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
                BitstreamBuilder.createBitstream(context, bundle, is)
                                .withName("bistream")
                                .build();
            }
        }

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   },\n" +
            " \"bitstream\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"lease\",\n" +
            "            \"endDate\": \"2023-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", subCommunityOne.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(10));

        List<Item> itemsOfSubCommOne = findItems("location.comm:" + subCommunityOne.getID());

        assertThat(itemsOfSubCommOne, hasSize(5));

        assertThat(itemsOfSubCommOne.stream()
                                    .flatMap(item -> findAllBitstreams(item).stream())
                                    .count(), is(5L));

        for (Item item : itemsOfSubCommOne) {
            assertThat(item.getResourcePolicies(), hasSize(3));
            assertThat(item.getResourcePolicies(), containsInAnyOrder(
                matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED),
                matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
                matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
            ));

            List<Bitstream> bitstreams = findAllBitstreams(item);

            for (Bitstream bitstream : bitstreams) {
                assertThat(bitstream.getResourcePolicies(), hasSize(3));
                assertThat(bitstream.getResourcePolicies(), containsInAnyOrder(
                    matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED),
                    matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
                    matches(Constants.READ, anonymousGroup, "lease", TYPE_CUSTOM, null, "2023-06-24", null)
                ));
            }
        }
    }

    @Test
    public void performBulkAccessWithReplaceModeTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("parent community")
                                              .build();

        Community subCommunityOne = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                              .withName("sub community one")
                                              .build();

        Collection collectionOne = CollectionBuilder.createCollection(context, subCommunityOne)
                                                 .withName("collection one")
                                                 .build();

        for (int i = 0; i < 3 ; i++) {

            Item item = ItemBuilder.createItem(context, collectionOne).build();

            Bundle bundle = BundleBuilder.createBundle(context, item)
                                         .withName("ORIGINAL")
                                         .build();

            String bitstreamContent = "Dummy content";
            try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
                BitstreamBuilder.createBitstream(context, bundle, is)
                                .withName("bistream")
                                .build();
            }
        }

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   },\n" +
            " \"bitstream\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"lease\",\n" +
            "            \"endDate\": \"2023-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", subCommunityOne.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(6));

        List<Item> itemsOfSubCommOne = findItems("location.comm:" + subCommunityOne.getID());

        assertThat(itemsOfSubCommOne, hasSize(3));

        assertThat(itemsOfSubCommOne.stream()
                                    .flatMap(item -> findAllBitstreams(item).stream())
                                    .count(), is(3L));

        for (Item item : itemsOfSubCommOne) {
            assertThat(item.getResourcePolicies(), hasSize(2));
            assertThat(item.getResourcePolicies(), containsInAnyOrder(
                matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
                matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
            ));

            assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasItem(
                containsString("Replacing Item {" + item.getID() +
                    "} policy to access conditions:{openaccess, embargo, start_date=2024-06-24}")
            ));

            List<Bitstream> bitstreams = findAllBitstreams(item);

            for (Bitstream bitstream : bitstreams) {
                assertThat(bitstream.getResourcePolicies(), hasSize(2));
                assertThat(bitstream.getResourcePolicies(), containsInAnyOrder(
                    matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
                    matches(Constants.READ, anonymousGroup, "lease", TYPE_CUSTOM, null, "2023-06-24", null)
                ));

                assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasItem(
                    containsString("Replacing Bitstream {" + bitstream.getID() +
                        "} policy to access conditions:{openaccess, lease, end_date=2023-06-24}")
                ));
            }
        }
    }

    @Test
    public void performBulkAccessAndCheckDerivativeBitstreamsPoliciesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                              .withName("parent community")
                                              .build();

        Community subCommunityOne = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                              .withName("sub community one")
                                              .build();

        Collection collectionOne = CollectionBuilder.createCollection(context, subCommunityOne)
                                                 .withName("collection one")
                                                 .build();

        Item item = ItemBuilder.createItem(context, collectionOne).build();

        Bundle bundle = BundleBuilder.createBundle(context, item)
                                     .withName("ORIGINAL")
                                     .build();

        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            BitstreamBuilder.createBitstream(context, bundle, is)
                            .withName("bitstream")
                            .withFormat("TEXT")
                            .withMimeType("text/plain")
                            .build();
        }

        List<FormatFilter> formatFilters = new ArrayList<>();
        Map<String, List<String>> filterFormats = new HashMap<>();
        MediaFilterService mediaFilterService = MediaFilterServiceFactory.getInstance().getMediaFilterService();

        String[] filterNames =
            DSpaceServicesFactory.getInstance()
                                 .getConfigurationService()
                                 .getArrayProperty(MEDIA_FILTER_PLUGINS_KEY);


        for (int i = 0; i < filterNames.length; i++) {

            //get filter of this name & add to list of filters
            FormatFilter filter =
                (FormatFilter) CoreServiceFactory.getInstance()
                                                 .getPluginService()
                                                 .getNamedPlugin(FormatFilter.class, filterNames[i]);
            formatFilters.add(filter);

            String filterClassName = filter.getClass().getName();

            String pluginName = null;

            if (SelfNamedPlugin.class.isAssignableFrom(filter.getClass())) {
                //Get the plugin instance name for this class
                pluginName = ((SelfNamedPlugin) filter).getPluginInstanceName();
            }

            String[] formats =
                DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty(
                    FILTER_PREFIX + "." + filterClassName +
                        (pluginName != null ? "." + pluginName : "") +
                        "." + INPUT_FORMATS_SUFFIX);

            //add to internal map of filters to supported formats
            if (ArrayUtils.isNotEmpty(formats)) {
                filterFormats.put(filterClassName +
                        (pluginName != null ? MediaFilterService.FILTER_PLUGIN_SEPARATOR +
                            pluginName : ""),
                    Arrays.asList(formats));
            }
        }

        mediaFilterService.setFilterClasses(formatFilters);
        mediaFilterService.setFilterFormats(filterFormats);

        // here will create derivative bitstreams
        mediaFilterService.applyFiltersItem(context, item);

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   },\n" +
            " \"bitstream\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          },\n" +
            "          {\n" +
            "            \"name\": \"lease\",\n" +
            "            \"endDate\": \"2023-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }\n" +
            "}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", subCommunityOne.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(2));

        item = context.reloadEntity(item);

        Bundle originalBundle = item.getBundles(DEFAULT_BUNDLE_NAME).get(0);
        Bundle textBundle = item.getBundles("TEXT").get(0);

        assertThat(item.getResourcePolicies(), hasSize(2));
        assertThat(item.getResourcePolicies(), containsInAnyOrder(
            matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
            matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null)
        ));

        assertThat(originalBundle.getBitstreams().get(0).getResourcePolicies(), hasSize(2));
        assertThat(originalBundle.getBitstreams().get(0).getResourcePolicies(), containsInAnyOrder(
            matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
            matches(Constants.READ, anonymousGroup, "lease", TYPE_CUSTOM, null, "2023-06-24", null)
        ));

        assertThat(textBundle.getBitstreams().get(0).getResourcePolicies(), hasSize(2));
        assertThat(textBundle.getBitstreams().get(0).getResourcePolicies(), containsInAnyOrder(
            matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
            matches(Constants.READ, anonymousGroup, "lease", TYPE_CUSTOM, null, "2023-06-24", null)
        ));
    }

    @Test
    public void performBulkAccessWithReplaceModeAndAppendModeIsEnabledTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group = GroupBuilder.createGroup(context).withName("special network").build();

        Community community = CommunityBuilder.createCommunity(context)
                                                    .withName("parent community")
                                                    .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
                                                 .withName("collection one")
                                                 .withDefaultItemRead(group)
                                                 .build();

        Item item = ItemBuilder.createItem(context, collection).build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", item.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        try {
            configurationService.setProperty("core.authorization.installitem.inheritance-read.append-mode", true);

            TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
            ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl),
                testDSpaceRunnableHandler, kernelImpl);

            assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
            assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
            assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(2));

            assertThat(testDSpaceRunnableHandler.getInfoMessages(), containsInAnyOrder(
                containsString("Replacing Item {" + item.getID() + "} policy to access conditions:" +
                    "{embargo, start_date=2024-06-24}"),
                containsString("Inheriting policies from owning Collection in Item {" + item.getID() + "}")
            ));

            item = context.reloadEntity(item);

            Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);

            assertThat(item.getResourcePolicies(), hasSize(2));
            assertThat(item.getResourcePolicies(), containsInAnyOrder(
                matches(Constants.READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null, null),
                matches(Constants.READ, group, TYPE_INHERITED)
            ));
        } finally {
            configurationService.setProperty("core.authorization.installitem.inheritance-read.append-mode", false);
        }
    }

    @Test
    public void performBulkAccessWithReplaceModeOnItemsWithMultipleBundlesTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group adminGroup = groupService.findByName(context, Group.ADMIN);

        Community parentCommunity = CommunityBuilder.createCommunity(context)
                                                    .withName("parent community")
                                                    .build();

        Community subCommunity = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                                 .withName("sub community one")
                                                 .build();

        Collection collection = CollectionBuilder.createCollection(context, subCommunity)
                                                 .withName("collection one")
                                                 .build();

        Item itemOne = ItemBuilder.createItem(context, collection).build();
        Item itemTwo = ItemBuilder.createItem(context, collection).build();
        ItemBuilder.createItem(context, collection).build();

        Bundle bundleOne = BundleBuilder.createBundle(context, itemOne)
                                        .withName("ORIGINAL")
                                        .build();

        Bundle bundleTwo = BundleBuilder.createBundle(context, itemTwo)
                                        .withName("ORIGINAL")
                                        .build();

        BundleBuilder.createBundle(context, itemTwo)
                     .withName("ORIGINAL")
                     .build();

        BundleBuilder.createBundle(context, itemOne)
                     .withName("TEXT")
                     .build();

        Bitstream bitstreamOne;
        Bitstream bitstreamTwo;
        String bitstreamContent = "Dummy content";
        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamOne =
                BitstreamBuilder.createBitstream(context, bundleOne, is)
                                .withName("bistream of bundle one")
                                .build();
        }

        try (InputStream is = IOUtils.toInputStream(bitstreamContent, CharEncoding.UTF_8)) {
            bitstreamTwo =
                BitstreamBuilder.createBitstream(context, bundleTwo, is)
                                .withName("bitstream of bundle two")
                                .build();
        }

        context.restoreAuthSystemState();

        String jsonOne = "{\n" +
            "  \"bitstream\": {\n" +
            "    \"constraints\": {\n" +
            "      \"uuid\": []\n" +
            "    },\n" +
            "    \"mode\": \"replace\",\n" +
            "    \"accessConditions\": [\n" +
            "      {\n" +
            "        \"name\": \"administrator\",\n" +
            "        \"startDate\": null,\n" +
            "        \"endDate\": null\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}";

        buildJsonFile(jsonOne);

        String[] args = new String[] {
            "bulk-access-control",
            "-u", subCommunity.getID().toString(),
            "-f", tempFilePath,
            "-e", admin.getEmail()
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getInfoMessages(), hasSize(2));

        assertThat(testDSpaceRunnableHandler.getInfoMessages(), containsInAnyOrder(
            containsString("Replacing Bitstream {" + bitstreamOne.getID() +
                "} policy to access conditions:{administrator}"),
            containsString("Replacing Bitstream {" + bitstreamTwo.getID() +
                "} policy to access conditions:{administrator}")
        ));

        bitstreamOne = context.reloadEntity(bitstreamOne);
        bitstreamTwo = context.reloadEntity(bitstreamTwo);

        assertThat(bitstreamOne.getResourcePolicies(), hasSize(1));
        assertThat(bitstreamOne.getResourcePolicies(), hasItem(
            matches(READ, adminGroup, "administrator", TYPE_CUSTOM)
        ));

        assertThat(bitstreamTwo.getResourcePolicies(), hasSize(1));
        assertThat(bitstreamTwo.getResourcePolicies(), hasItem(
            matches(READ, adminGroup, "administrator", TYPE_CUSTOM)
        ));
    }

    @Test
    public void performBulkAccessWithHelpParamTest() throws Exception {

        String[] args = new String[] {"bulk-access-control", "-h"};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
    }

    private List<Item> findItems(String query) throws SearchServiceException {

        DiscoverQuery discoverQuery = new DiscoverQuery();
        discoverQuery.setDSpaceObjectFilter(IndexableItem.TYPE);
        discoverQuery.setQuery(query);

        return searchService.search(context, discoverQuery)
                            .getIndexableObjects()
                            .stream()
                            .map(indexableObject ->
                                ((IndexableItem) indexableObject).getIndexedObject())
                            .collect(Collectors.toList());
    }

    private List<Bitstream> findAllBitstreams(Item item) {
        return item.getBundles(CONTENT_BUNDLE_NAME)
                   .stream()
                   .flatMap(bundle -> bundle.getBitstreams().stream())
                   .collect(Collectors.toList());
    }

    private void buildJsonFile(String json) throws IOException {
        File file = new File(tempDir + "/bulk-access.json");
        Path path = Paths.get(file.getAbsolutePath());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }
}
