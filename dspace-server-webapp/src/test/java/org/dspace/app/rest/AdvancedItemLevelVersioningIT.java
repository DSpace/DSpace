/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.SQLException;

import org.dspace.app.rest.authorization.AuthorizationFeatureService;
import org.dspace.app.rest.converter.VersionConverter;
import org.dspace.app.rest.matcher.VersionMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.service.InstallItemService;
import org.dspace.content.service.WorkspaceItemService;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.versioning.Version;
import org.dspace.versioning.service.VersioningService;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration Test to discover it.
 *
 * Before to run only tests:
 * mvn clean install
 *
 * To run single IT tests:
 * mvn test -DskipUnitTests=false -Dtest=[full.package.testClassName]#[testMethodName] -DfailIfNoTests=false
 *
 * ResultActions methods
 * https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/test/web/servlet/ResultActions.html
 */
public class AdvancedItemLevelVersioningIT extends AbstractControllerIntegrationTest {

    @Autowired
    private VersioningService versioningService;

    @Autowired
    private VersionConverter versionConverter;

    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private InstallItemService installItemService;

    @Autowired
    private WorkspaceItemService workspaceItemService;

    @Autowired
    private AuthorizationFeatureService authorizationFeatureService;

    Community community;
    Collection collection;

    Item item;
    Version version1st;
    Version version2nd;

    String baseUrl;

    @Before
    public void setup() throws SQLException, AuthorizeException {
        // disable file upload mandatory
        configurationService.setProperty("webui.submit.upload.required", false);

        context.turnOffAuthorisationSystem();

        community = CommunityBuilder.createCommunity(context)
                    .withName("Community")
                    .build();
        collection =
            CollectionBuilder.createCollection(context, community)
                    .withName("Collection")
                    .build();

        context.restoreAuthSystemState();

        baseUrl = configurationService.getProperty("dspace.ui.url") + "/handle/";
    }

    /*
     * Test the item and its versions built with initialVersionSuffix = false
     */
    @Test
    public void itemWithoutVersionSuffixTest() throws Exception {
        System.out.println("itemWithoutVersionSuffixTest");

        configurationService.setProperty("dspace.initialVersionSuffix", false);

        context.turnOffAuthorisationSystem();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item without first .1 suffix")
                          .withIssueDate("2022-07-15")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        System.out.println("Item handle is: ");
        System.out.println(item.getHandle());

        version1st = VersionBuilder.createVersion(
            context, item, "First version of the item without .1 suffix")
                .build();
        version2nd = VersionBuilder.createVersion(
            context, item, "Second version of the item without .1 suffix")
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // The firstly created item should not have a ".1" version suffix
        getClient(token).perform(get("/api/core/items/" + item.getID() ))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.handle", not(endsWith(".1"))));

        getClient(token).perform(get("/api/versioning/versions/" + version1st.getID()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version1st))))
          .andExpect(jsonPath("$._links.versionhistory.href", Matchers.allOf(Matchers.containsString(
                              "api/versioning/versions/" + version1st.getID() + "/versionhistory"))))
          .andExpect(jsonPath("$._links.item.href", Matchers.allOf(Matchers.containsString(
                              "api/versioning/versions/" + version1st.getID() + "/item"))))
          .andExpect(jsonPath("$._links.self.href", Matchers.allOf(Matchers.containsString(
                              "api/versioning/versions/" + version1st.getID()))));

        getClient(token).perform(get("/api/versioning/versions/" + version2nd.getID()))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version2nd))))
          .andExpect(jsonPath("$._links.versionhistory.href", Matchers.allOf(Matchers.containsString(
                              "api/versioning/versions/" + version2nd.getID() + "/versionhistory"))))
          .andExpect(jsonPath("$._links.item.href", Matchers.allOf(Matchers.containsString(
                              "api/versioning/versions/" + version2nd.getID() + "/item"))))
          .andExpect(jsonPath("$._links.self.href", Matchers.allOf(Matchers.containsString(
                              "api/versioning/versions/" + version2nd.getID()))));
    }

    /*
     * Test the item and its versions built with initialVersionSuffix = true
     */
    @Test
    public void itemWithFirstVersionSuffixTest() throws Exception {
        System.out.println("itemWithVersionSuffixTest");

        configurationService.setProperty("dspace.initialVersionSuffix", true);

        context.turnOffAuthorisationSystem();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item with first .1 suffix")
                          .withIssueDate("2022-07-15")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        System.out.println("Item handle is: ");
        System.out.println(item.getHandle());

        version1st = VersionBuilder.createVersion(
            context, item, "First version of the item with .1 suffix")
                .build();
        version2nd = VersionBuilder.createVersion(
            context, item, "Second version of the item with .1 suffix")
                .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);

        // The firstly created item should have a ".1" suffix
        getClient(token).perform(get("/api/core/items/" + item.getID()))
                        .andExpect(status().isOk())
                        .andExpect(content().contentType(contentType))
                        .andExpect(jsonPath("$.handle", endsWith(".1")));

        getClient(token).perform(get("/api/versioning/versions/" + version1st.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version1st))))
            .andExpect(jsonPath("$._links.versionhistory.href", Matchers.allOf(Matchers.containsString(
                                "api/versioning/versions/" + version1st.getID() + "/versionhistory"))))
            .andExpect(jsonPath("$._links.item.href", Matchers.allOf(Matchers.containsString(
                                "api/versioning/versions/" + version1st.getID() + "/item"))))
            .andExpect(jsonPath("$._links.self.href", Matchers.allOf(Matchers.containsString(
                                "api/versioning/versions/" + version1st.getID()))));

        getClient(token).perform(get("/api/versioning/versions/" + version2nd.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.is(VersionMatcher.matchEntry(version2nd))))
            .andExpect(jsonPath("$._links.versionhistory.href", Matchers.allOf(Matchers.containsString(
                                "api/versioning/versions/" + version2nd.getID() + "/versionhistory"))))
            .andExpect(jsonPath("$._links.item.href", Matchers.allOf(Matchers.containsString(
                                "api/versioning/versions/" + version2nd.getID() + "/item"))))
            .andExpect(jsonPath("$._links.self.href", Matchers.allOf(Matchers.containsString(
                                "api/versioning/versions/" + version2nd.getID()))));
    }

    @Test
    public void resolveToURLWithFirstSuffixTest() throws Exception {
        System.out.println("resolveToURLTest");

        configurationService.setProperty("dspace.initialVersionSuffix", true);

        context.turnOffAuthorisationSystem();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item with first .1 suffix")
                          .withIssueDate("2022-07-15")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        version1st = VersionBuilder.createVersion(
            context, item, "First version of the item without .1 suffix")
                .build();

        System.out.println("hello from resolveToURLTest: " + item.getHandle());

        // get URL with ".1" suffix
        String resolvedHandleWithSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                              context, item.getHandle());
        // get URL without ".1" suffix
        String resolvedHandleWithoutSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                                context, item.getHandle().split("\\.")[0]);
        String resolvedVersionHandle = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                          context, version1st.getItem().getHandle());

        System.out.println("resolved handle with suffix: " + resolvedHandleWithSuffix);
        System.out.println("resolved handle without suffix: " + resolvedHandleWithoutSuffix);

        assertEquals(resolvedHandleWithSuffix, baseUrl + item.getHandle());
        assertEquals(resolvedHandleWithoutSuffix, baseUrl + item.getHandle().split("\\.")[0]);
        assertEquals(resolvedVersionHandle, baseUrl + version1st.getItem().getHandle());

        // check that the resolving of the handle is still working
        configurationService.setProperty("dspace.initialVersionSuffix", false);

        // get URL with ".1" suffix
        resolvedHandleWithSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                              context, item.getHandle());
        // get URL without ".1" suffix
        resolvedHandleWithoutSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                                context, item.getHandle().split("\\.")[0]);

        System.out.println("resolved handle with suffix: " + resolvedHandleWithSuffix);
        System.out.println("resolved handle without suffix: " + resolvedHandleWithoutSuffix);

        assertEquals(resolvedHandleWithSuffix, baseUrl + item.getHandle());
        assertEquals(resolvedHandleWithoutSuffix, resolvedHandleWithoutSuffix);
    }

    @Test
    public void resolveToURLWithoutFirstSuffixTest() throws Exception {
        System.out.println("resolveToURLTest");

        configurationService.setProperty("dspace.initialVersionSuffix", false);

        context.turnOffAuthorisationSystem();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item with first .1 suffix")
                          .withIssueDate("2022-07-15")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        System.out.println("hello from resolveToURLTest: " + item.getHandle());

        // get URL with ".1" suffix
        String resolvedHandleWithSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                              context, item.getHandle());
        // get URL without ".1" suffix
        String resolvedHandleWithoutSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                                context, item.getHandle().split("\\.")[0]);

        System.out.println("resolved handle with suffix: " + resolvedHandleWithSuffix);
        System.out.println("resolved handle without suffix: " + resolvedHandleWithoutSuffix);

        assertEquals(resolvedHandleWithSuffix, baseUrl + item.getHandle());
        assertEquals(resolvedHandleWithoutSuffix, baseUrl + item.getHandle().split("\\.")[0]);

        // check that the resolving of the handle is still working
        configurationService.setProperty("dspace.initialVersionSuffix", true);
        // get URL with ".1" suffix
        resolvedHandleWithSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                              context, item.getHandle());
        // get URL without ".1" suffix
        resolvedHandleWithoutSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToURL(
                                                context, item.getHandle().split("\\.")[0]);

        System.out.println("resolved handle with suffix: " + resolvedHandleWithSuffix);
        System.out.println("resolved handle without suffix: " + resolvedHandleWithoutSuffix);

        assertEquals(resolvedHandleWithSuffix, baseUrl + item.getHandle());
        assertEquals(resolvedHandleWithoutSuffix, baseUrl + item.getHandle().split("\\.")[0]);
    }

    @Test
    public void resolveUrlToHandleTest() throws Exception, SQLException {
        System.out.println("resolveUrlToHandleTest");

        configurationService.setProperty("dspace.initialVersionSuffix", true);

        context.turnOffAuthorisationSystem();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item with first .1 suffix")
                          .withIssueDate("2022-07-15")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        System.out.println("hello from resolveUrlToHandleTest: " + item.getHandle());

        // get URL with ".1" suffix
        String resolvedHandleWithSuffix = HandleServiceFactory.getInstance().getHandleService().resolveUrlToHandle(
                                              context, baseUrl + item.getHandle());
        // get URL without ".1" suffix
        String resolvedHandleWithoutSuffix = HandleServiceFactory.getInstance().getHandleService().resolveUrlToHandle(
                                                context, baseUrl + item.getHandle().split("\\.")[0]);

        System.out.println("resolved handle with suffix: " + resolvedHandleWithSuffix);
        System.out.println("resolved handle without suffix: " + resolvedHandleWithoutSuffix);

        assertEquals(resolvedHandleWithSuffix, item.getHandle());
        assertEquals(resolvedHandleWithoutSuffix, item.getHandle().split("\\.")[0]);
    }

    @Test
    public void resolveToObjectTest() throws Exception, SQLException {
        System.out.println("resolveToObjectTest");

        configurationService.setProperty("dspace.initialVersionSuffix", true);

        context.turnOffAuthorisationSystem();

        item = ItemBuilder.createItem(context, collection)
                          .withTitle("Public item with first .1 suffix")
                          .withIssueDate("2022-07-15")
                          .withAuthor("Smith, Donald").withAuthor("Doe, John")
                          .withSubject("ExtraEntry")
                          .build();

        System.out.println("hello from resolveToObjectTest: " + item.getHandle());

        // get dso with ".1" suffix
        DSpaceObject resolvedDsoWithSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToObject(
                                              context, item.getHandle(), true);
        // get dso without ".1" suffix
        DSpaceObject resolvedDsoWithoutSuffix = HandleServiceFactory.getInstance().getHandleService().resolveToObject(
                                                context, item.getHandle().split("\\.")[0], true);

        System.out.println("resolved dso with suffix: " + resolvedDsoWithSuffix.getHandle());
        System.out.println("resolved dso without suffix: " + resolvedDsoWithoutSuffix.getHandle());

        assertEquals(resolvedDsoWithSuffix.getHandle(), item.getHandle());
        assertEquals(resolvedDsoWithoutSuffix.getHandle(), item.getHandle());
    }
}
