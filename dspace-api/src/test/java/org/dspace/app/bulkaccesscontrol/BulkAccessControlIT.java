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
import static org.dspace.core.Constants.READ;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.validation.constraints.AssertTrue;

import org.apache.commons.io.file.PathUtils;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.matcher.ResourcePolicyMatcher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.eperson.Group;
import org.dspace.eperson.GroupTest;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.GroupService;
import org.dspace.matcher.DateMatcher;
import org.dspace.util.MultiFormatDateParser;
import org.dspace.utils.DSpace;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Basic integration testing for the Bulk Access conditions Feature{@link BulkAccessControl}.
 *
 * @author Mohamed Eskander (mohamed.eskander at 4science.it)
 */
public class BulkAccessControlIT extends AbstractIntegrationTestWithDatabase {
    private Path tempDir;
    private String tempFilePath;
    private Collection collection;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private GroupService groupService = EPersonServiceFactory.getInstance().getGroupService();

    @Before
    @Override
    public void setUp() throws Exception {

        super.setUp();

        context.turnOffAuthorisationSystem();

        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();

        collection = CollectionBuilder.createCollection(context, parentCommunity)
                                      .withName("Collection")
                                      .withEntityType("Publication")
                                      .build();

        context.restoreAuthSystemState();

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

        context.restoreAuthSystemState();
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
            "            \"endDate\": \"2024-06-24T23:59:59.999+0000\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(jsonOne);

        String[] args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        TestDSpaceRunnableHandler testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());

        String jsonTwo = "{ \"item\": {\n" +
            "      \"mode\": \"add\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"lease\",\n" +
            "            \"startDate\": \"2024-06-24T23:59:59.999+0000\"\n" +
            "          }\n" +
            "      ]\n" +
            "   }}\n";

        buildJsonFile(jsonTwo);

        args = new String[] {"bulk-access-control", "-u", item.getID().toString(), "-f", tempFilePath};

        testDSpaceRunnableHandler = new TestDSpaceRunnableHandler();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), testDSpaceRunnableHandler, kernelImpl);

        assertThat(testDSpaceRunnableHandler.getErrorMessages(), hasSize(1));
        assertThat(testDSpaceRunnableHandler.getWarningMessages(), empty());
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
                                              .withName("sub community two")
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

        ItemBuilder.createItem(context, collectionOne).build();

        ItemBuilder.createItem(context, collectionTwo).build();

        Item itemThree = ItemBuilder.createItem(context, collectionThree).withTitle("item three title").build();

        Item itemFour = ItemBuilder.createItem(context, collectionThree).withTitle("item four title").build();

        context.restoreAuthSystemState();

        String jsonOne = "{ \"item\": {\n" +
            "      \"mode\": \"replace\",\n" +
            "      \"accessConditions\": [\n" +
            "          {\n" +
            "            \"name\": \"embargo\",\n" +
            "            \"startDate\": \"2024-06-24T00:00:00.000Z\"\n" +
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

        Iterator<Item> itemIteratorOne = itemService.findByCollection(context, collectionOne);
        Iterator<Item> itemIteratorTwo = itemService.findByCollection(context, collectionTwo);
        itemThree = context.reloadEntity(itemThree);
        itemFour = context.reloadEntity(itemFour);

        Group anonymousGroup = groupService.findByName(context, Group.ANONYMOUS);


//        matchItemsResourcePolicies(itemIteratorOne, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null);
//        matchItemsResourcePolicies(itemIteratorTwo, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null);
//        matchItemResourcePolicies(itemThree, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24", null);

        assertThat(itemThree.getResourcePolicies(), hasSize(2));
        assertThat(itemThree.getResourcePolicies(), containsInAnyOrder(
            matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED),
            matches(READ, anonymousGroup, "embargo", TYPE_CUSTOM, "2024-06-24T00:00:00.000Z", null, null)
        ));

        // just a note here is working fine
        assertThat(itemThree.getResourcePolicies(), hasItem(
            matches(READ, anonymousGroup, "embargo", TYPE_CUSTOM,
                itemThree.getResourcePolicies().get(0).getStartDate(), null, null)
        ));

        assertThat(itemFour.getResourcePolicies().size(), is(1));
        assertThat(itemFour.getResourcePolicies(), hasItem(
            matches(Constants.READ, anonymousGroup, ResourcePolicy.TYPE_INHERITED)
        ));




    }

    private void matchItemsResourcePolicies(
        Iterator<Item> itemIterator, Group group, String rpName, String rpType, String startDate, String endDate) {
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();
            matchItemResourcePolicies(item, group, rpName, rpType, startDate, endDate);
        }
    }

    private void matchItemResourcePolicies(
        Item item, Group group, String rpName, String rpType, String startDate, String endDate) {

        assertThat(item.getResourcePolicies(), hasItem(
            matches(READ, group, rpName, rpType, startDate, endDate, null)));
    }

    private void buildJsonFile(String json) throws IOException {
        File file = new File(tempDir + "/bulk-access.json");
        Path path = Paths.get(file.getAbsolutePath());
        Files.writeString(path, json, StandardCharsets.UTF_8);
    }
}
