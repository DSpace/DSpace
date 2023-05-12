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
import static org.dspace.core.Constants.CONTENT_BUNDLE_NAME;
import static org.dspace.core.Constants.READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.codec.CharEncoding;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.file.PathUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.BitstreamBuilder;
import org.dspace.builder.BundleBuilder;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Basic integration testing for the Bulk Access conditions Feature{@link BulkAccessControl}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessControlIT extends AbstractIntegrationTestWithDatabase {
    private Path tempDir;
    private String tempFilePath;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();
    private SearchService searchService = SearchUtils.getSearchService();

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
    public void performBulkAccessWithoutRequiredParamTest() throws Exception {

        buildJsonFile("");

        String[] args = new String[] {"bulk-access-control", "-f", tempFilePath};

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

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

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

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("wrong value for item mode<wrong>")
        ));

        json = "{ \"item\": {\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
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

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("wrong value for bitstream mode<wrong>")
        ));

        json = "{ \"bitstream\": {\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"openaccess\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(json);

        args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
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

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("wrong access condition <wrongAccess>")
        ));
    }

    @Test
    public void performBulkAccessWithInvalidAccessConditionDateTest() throws Exception {
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

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasItem(
            containsString("invalid access condition, The access condition embargo requires a start date.")
        ));

        String jsonTwo = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"lease\",\n" +
            "            \"startDate\": \"2024-06-24T00:00:00Z\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(jsonTwo);

        args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
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
                "-f", tempFilePath};

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
                "-f", tempFilePath};

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
                "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());

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
            new String[] {"bulk-access-control", "-u", parentCommunity.getID().toString(), "-f", tempFilePath};

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
            "-f", tempFilePath
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

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
            "-f", tempFilePath
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

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

            List<Bitstream> bitstreams = findAllBitstreams(item);

            for (Bitstream bitstream : bitstreams) {
                assertThat(bitstream.getResourcePolicies(), hasSize(1));
                assertThat(bitstream.getResourcePolicies(), hasItem(
                    matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
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
            "-f", tempFilePath
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

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
            "-f", tempFilePath
        };

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), empty());
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

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

            List<Bitstream> bitstreams = findAllBitstreams(item);

            for (Bitstream bitstream : bitstreams) {
                assertThat(bitstream.getResourcePolicies(), hasSize(2));
                assertThat(bitstream.getResourcePolicies(), containsInAnyOrder(
                    matches(READ, anonymousGroup, "openaccess", TYPE_CUSTOM),
                    matches(Constants.READ, anonymousGroup, "lease", TYPE_CUSTOM, null, "2023-06-24", null)
                ));
            }
        }
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
