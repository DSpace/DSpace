/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static com.jayway.jsonpath.JsonPath.read;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.ResoucePolicyMatcher;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.AddOperation;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.RemoveOperation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Constants;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test class for the resourcepolicies endpoint
 *
 * @author Mykhaylo Boychuk - 4Science
 */
public class ResourcePolicyRestRepositoryIT extends AbstractControllerIntegrationTest {

    @Autowired
    AuthorizeService authorizeService;

    @Autowired
    ResourcePolicyService resourcePolicyService;

    @Test
    public void findAllTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withDspaceObject(community).withAction(Constants.READ)
            .withUser(admin).build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    public void findAllUnAuthenticatedTest() throws Exception {

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicyBuilder.createResourcePolicy(context).withDspaceObject(community).withAction(Constants.READ)
            .withUser(admin).build();
        context.restoreAuthSystemState();

        getClient().perform(get("/api/authz/resourcepolicies")).andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.READ)
            .withUser(eperson1)
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk()).andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$", is(
                ResoucePolicyMatcher.matchResourcePolicy(resourcePolicy)
            )))
            .andExpect(jsonPath("$._links.self.href", Matchers
                .containsString("/api/authz/resourcepolicies/" + resourcePolicy.getID())));
    }

    @Test
    public void findOneResourcePolicyOfAnonymousGroupTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group groupAnonymous = EPersonServiceFactory.getInstance().getGroupService().findByName(context,
            Group.ANONYMOUS);

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.READ)
            .withGroup(groupAnonymous)
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk());

        getClient().perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findOneUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context).withDspaceObject(community)
            .withAction(Constants.READ).withUser(eperson).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void findOneNotFoundTest() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + UUID.randomUUID().toString()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void findOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("My collection").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collection)
            .withAction(Constants.WRITE)
            .withUser(eperson1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson2.getEmail(), "qwerty02");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isForbidden());

        String authToken1 = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken1).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void findOneAccessGrantToAdminTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.WRITE)
            .withUser(eperson1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(
                ResoucePolicyMatcher.matchResourcePolicy(resourcePolicy))));
    }

    @Test
    public void findOneAccessGrantToSameUserTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context).withName("My group").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .withGroupMembership(group1)
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("My collection").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collection)
            .withAction(Constants.ADD)
            .withGroup(group1)
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(ResoucePolicyMatcher.matchResourcePolicy(resourcePolicy))));

        String authTokenEperson2 = getAuthToken(eperson2.getEmail(), "qwerty02");
        getClient(authTokenEperson2).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findOneResoucesPolicyByEpersonUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("myemail@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("Xemail@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Community community2 = CommunityBuilder.createCommunity(context).withName("My community_2").build();

        ResourcePolicy resourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADD)
            .withUser(eperson1).build();

        ResourcePolicy resourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2)
            .withAction(Constants.REMOVE)
            .withUser(eperson2).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");

        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
            .param("uuid", eperson1.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies", Matchers.contains(
                ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfEPerson1))))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfEPerson2)))))
            .andExpect(jsonPath("$._links.self.href", Matchers.containsString(
                "api/authz/resourcepolicies/search/eperson")))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }


    @Test
    public void findResoucesPoliciesByEpersonUuidAndResourceUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context).withEmail("myemail@mail.com").withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).withName("My collection")
            .build();

        ResourcePolicy resourcePolicyOfCommunity = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.READ)
            .withUser(eperson1).build();

        ResourcePolicy secondResourcePolicyOfCommunity = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.REMOVE)
            .withUser(eperson1).build();

        ResourcePolicy resourcePolicyOfCollection = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collection)
            .withAction(Constants.REMOVE)
            .withUser(eperson1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");

        getClient(authToken)
            .perform(get("/api/authz/resourcepolicies/search/eperson")
                .param("uuid", eperson1.getID().toString())
                .param("resource", community.getID().toString()))
            .andExpect(status().isOk()).andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies", Matchers.containsInAnyOrder(
                ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfCommunity),
                ResoucePolicyMatcher.matchResourcePolicy(secondResourcePolicyOfCommunity)
            )))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfCollection)))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/eperson")))
            .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findResoucesPoliciesEPersonWithoutParametersBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context).withEmail("myemail@mail.com").withPassword("qwerty01")
            .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void findResoucesPoliciesByEPersonUuidUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("myemail@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.READ)
            .withUser(eperson1).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/authz/resourcepolicies/search/eperson")
            .param("uuid", eperson1.getID().toString())
            .param("resource", community.getID().toString()))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void findResourcesPoliciesByEPersonNotFoundTest() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
            .param("uuid", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findResourcesPoliciesByEPersonUuidForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        Community community2 = CommunityBuilder.createCommunity(context).withName("My 2 community").build();

        ResourcePolicy resourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community).withAction(Constants.WRITE)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson1).build();

        ResourcePolicy resourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2).withAction(Constants.ADD)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson2).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
            .param("uuid", eperson2.getID().toString())
            .param("resource", community2.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findResourcePoliciesOfOneResourceWithoutActionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        Community community2 = CommunityBuilder.createCommunity(context).withName("My second community").build();

        ResourcePolicy firstResourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADMIN)
            .withUser(eperson1).build();

        ResourcePolicy firstResourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2)
            .withAction(Constants.ADD)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson2).build();

        ResourcePolicy resourcePolicyAnonymous = authorizeService
            .findByTypeGroupAction(context, community, EPersonServiceFactory.getInstance()
                .getGroupService()
                .findByName(context, Group.ANONYMOUS), Constants.READ);

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies", Matchers.containsInAnyOrder(
                ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson1),
                ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyAnonymous)
            )))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson2)))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/resource")))
            .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    @Test
    public void findResourcePoliciesOfOneResourceWithActionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        Community community2 = CommunityBuilder.createCommunity(context).withName("My 2 community").build();

        ResourcePolicy firstResourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADMIN)
            .withUser(eperson1).build();

        ResourcePolicy firstResourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADD)
            .withUser(eperson2).build();

        ResourcePolicy secondResourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2)
            .withAction(Constants.ADD)
            .withUser(eperson2).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString())
            .param("action", "ADD"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies", Matchers.contains(
                ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson2)
            )))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(secondResourcePolicyOfEPerson2)))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/resource")))
            .andExpect(jsonPath("$.page.totalElements", is(1)));

        String authToken2 = getAuthToken(eperson2.getEmail(), "qwerty02");
        getClient(authToken2).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString())
            .param("action", "ADD"))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findResourcePoliciesOfOneResourcePaginationTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();


        ResourcePolicy firstResourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADMIN)
            .withUser(eperson1).build();

        ResourcePolicy firstResourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADD)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson2).build();

        ResourcePolicy resourcePolicyAnonymous = authorizeService
            .findByTypeGroupAction(context, community, EPersonServiceFactory.getInstance()
                .getGroupService()
                .findByName(context, Group.ANONYMOUS), Constants.READ);


        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString())
            .param("page", "0")
            .param("size", "2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.containsInAnyOrder(
                    hasJsonPath("$.type", is("resourcepolicy")),
                    hasJsonPath("$.type", is("resourcepolicy"))
                )))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/resource")))
            .andExpect(jsonPath("$.page.totalElements", is(3)))
            .andExpect(jsonPath("$.page.size", is(2)));
    }

  @Test
  public void findResoucesPoliciesOfResourceWithoutParametersBadRequestTest() throws Exception {
      context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void findResoucesPoliciesByResourceUuidUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("myemail@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.READ)
            .withUser(eperson1).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString()))
            .andExpect(status().isUnauthorized());

    }

    @Test
    public void findResourcePoliciesNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }


    @Test
    public void findResourcesPoliciesByResourceUuidForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        Community community2 = CommunityBuilder.createCommunity(context).withName("My 2 community").build();

        ResourcePolicy resourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.REMOVE)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson1).build();

        ResourcePolicy resourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2)
            .withAction(Constants.ADD)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson2).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
            .param("uuid", eperson2.getID().toString())
            .param("resource", community2.getID().toString()))
            .andExpect(status().isForbidden());
    }


    @Test
    public void findResourcePoliciesByGroupUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context).withName("My group").build();

        Group group2 = GroupBuilder.createGroup(context).withName("My 2 group").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .withGroupMembership(group1)
            .withGroupMembership(group2)
            .build();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("My community")
            .build();

        Community community2 = CommunityBuilder.createCommunity(context)
            .withName("My 2 community")
            .build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("My collection")
            .build();

        ResourcePolicy firstResourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADD)
            .withGroup(group1).build();

        ResourcePolicy secondResourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.READ)
            .withGroup(group1).build();

        ResourcePolicy collectionResourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collection)
            .withAction(Constants.WRITE)
            .withGroup(group1).build();

        ResourcePolicy firstResourcePolicyOfGroup2 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2)
            .withAction(Constants.ADD)
            .withGroup(group2).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken)
            .perform(get("/api/authz/resourcepolicies/search/group")
                .param("uuid", group1.getID().toString()))
            .andExpect(status().isOk()).andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.containsInAnyOrder(
                    ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfGroup1),
                    ResoucePolicyMatcher.matchResourcePolicy(secondResourcePolicyOfGroup1),
                    ResoucePolicyMatcher.matchResourcePolicy(collectionResourcePolicyOfGroup1))))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfGroup2)))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/group")))
            .andExpect(jsonPath("$.page.totalElements", is(3)));

        String authToken2 = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken2)
            .perform(get("/api/authz/resourcepolicies/search/group")
                .param("uuid", group2.getID().toString()))
            .andExpect(status().isOk()).andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.contains(
                    ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfGroup2))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/group")))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findResourcePoliciesByGroupUuidAndResourceUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context).withName("My group").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("myemail@mail.com")
            .withPassword("qwerty01")
            .withGroupMembership(group1)
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Community community2 = CommunityBuilder.createCommunity(context).withName("My second community").build();

        ResourcePolicy firstResourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADD)
            .withGroup(group1).build();

        ResourcePolicy secondResourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community2)
            .withAction(Constants.WRITE)
            .withGroup(group1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group")
            .param("uuid", group1.getID().toString())
            .param("resource", community.getID().toString()))
            .andExpect(status().isOk()).andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.contains(ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfGroup1))))
            .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(secondResourcePolicyOfGroup1)))))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/group")))
            .andExpect(jsonPath("$.page.totalElements", is(1)));
    }

    @Test
    public void findResoucesPoliciesByGroupWithoutParametersBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group"))
            .andExpect(status().isBadRequest());
    }

    @Test
    public void findResoucesPoliciesByGroupUuidUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context).withName("My group").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@gmail.com")
            .withPassword("qwerty01")
            .withGroupMembership(group1)
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy firstResourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.ADD)
            .withGroup(group1).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/authz/resourcepolicies/search/group")
            .param("uuid", group1.getID().toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void findGroupNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group")
            .param("uuid", UUID.randomUUID().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findResourcesPoliciesByGroupUuidForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context).withName("My group").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .withGroupMembership(group1)
            .build();

        EPerson eperson2 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson2@mail.com")
            .withPassword("qwerty02")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community).withAction(Constants.WRITE)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withGroup(group1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson2.getEmail(), "qwerty02");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group")
            .param("uuid", group1.getID().toString())
            .param("resource", community.getID().toString()))
            .andExpect(status().isForbidden());
    }

    @Test
    public void findResourcesPoliciesByGroupAnonymousTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group groupAnonymous = EPersonServiceFactory.getInstance().getGroupService().findByName(context,
            Group.ANONYMOUS);

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community).withAction(Constants.WRITE)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withGroup(groupAnonymous).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group")
            .param("uuid", groupAnonymous.getID().toString()))
            .andExpect(status().isOk());

        getClient().perform(get("/api/authz/resourcepolicies/search/group")
            .param("uuid", groupAnonymous.getID().toString()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    public void createWithEPersonTest() throws Exception {
        context.turnOffAuthorisationSystem();

        AtomicReference<Integer> idRef = new AtomicReference<Integer>();
        try {
            Community community = CommunityBuilder.createCommunity(context)
                .withName("My commynity")
                .build();

            EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .build();

            context.restoreAuthSystemState();

            ObjectMapper mapper = new ObjectMapper();
            ResourcePolicyRest resourcePolicyRest = new ResourcePolicyRest();

            resourcePolicyRest.setPolicyType(ResourcePolicy.TYPE_SUBMISSION);
            resourcePolicyRest.setAction(Constants.actionText[Constants.READ]);

            String authToken = getAuthToken(admin.getEmail(), password);
            getClient(authToken)
                .perform(post("/api/authz/resourcepolicies")
                    .content(mapper.writeValueAsBytes(resourcePolicyRest))
                    .param("resource", community.getID().toString())
                    .param("eperson", eperson1.getID().toString())
                    .contentType(contentType))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$", Matchers.allOf(
                    hasJsonPath("$.name", is(resourcePolicyRest.getName())),
                    hasJsonPath("$.description", is(resourcePolicyRest.getDescription())),
                    hasJsonPath("$.policyType", is(resourcePolicyRest.getPolicyType())),
                    hasJsonPath("$.action", is(resourcePolicyRest.getAction())),
                    hasJsonPath("$.startDate", is(resourcePolicyRest.getStartDate())),
                    hasJsonPath("$.endDate", is(resourcePolicyRest.getEndDate())),
                    hasJsonPath("$.type", is(resourcePolicyRest.getType())))))
                .andDo(result -> idRef.set(read(result.getResponse().getContentAsString(), "$.id")));

            String authToken1 = getAuthToken(eperson1.getEmail(), "qwerty01");
            getClient(authToken1).perform(get("/api/authz/resourcepolicies/" + idRef.get()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._links.self.href",
                    Matchers.containsString("/api/authz/resourcepolicies/" + idRef.get())));
        } finally {
            ResourcePolicyBuilder.delete(idRef.get());
        }
    }

    @Test
    public void createOneUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("My commynity")
            .build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        ResourcePolicyRest resourcePolicyRest = new ResourcePolicyRest();

        resourcePolicyRest.setPolicyType(ResourcePolicy.TYPE_SUBMISSION);
        resourcePolicyRest.setAction(Constants.actionText[Constants.ADMIN]);

        getClient().perform(post("/api/authz/resourcepolicies")
            .content(mapper.writeValueAsBytes(resourcePolicyRest))
            .param("resource", community.getID().toString())
            .param("eperson", eperson1.getID().toString())
            .contentType(contentType))
            .andExpect(status().isUnauthorized());

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString())
            .param("action", "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/resource")))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void createOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context)
            .withName("My commynity")
            .build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        context.restoreAuthSystemState();

        ObjectMapper mapper = new ObjectMapper();
        ResourcePolicyRest resourcePolicyRest = new ResourcePolicyRest();

        resourcePolicyRest.setPolicyType(ResourcePolicy.TYPE_SUBMISSION);
        resourcePolicyRest.setAction(Constants.actionText[Constants.ADMIN]);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(post("/api/authz/resourcepolicies")
            .content(mapper.writeValueAsBytes(resourcePolicyRest))
            .param("resource", community.getID().toString())
            .param("eperson", eperson1.getID().toString())
            .contentType(contentType))
            .andExpect(status().isForbidden());

        String authToken2 = getAuthToken(admin.getEmail(), password);
        getClient(authToken2).perform(get("/api/authz/resourcepolicies/search/resource")
            .param("uuid", community.getID().toString())
            .param("action", "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(contentType))
            .andExpect(jsonPath("$._links.self.href",
                Matchers.containsString("api/authz/resourcepolicies/search/resource")))
            .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void deleteOne() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("My community")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withUser(eperson1)
            .withAction(Constants.ADMIN)
            .build();

        context.restoreAuthSystemState();

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(delete("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().is(204));

        // Verify 404 after delete
        getClient(token).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isNotFound());
    }

    @Test
    public void deleteOneUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(community)
            .withAction(Constants.DELETE)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .withUser(eperson1)
            .build();

        context.restoreAuthSystemState();

        getClient().perform(delete("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isUnauthorized());

        String token = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(token).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk());
    }

    @Test
    public void deleteOneForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Collection collection = CollectionBuilder.createCollection(context, community)
            .withName("My collection").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withDspaceObject(collection)
            .withAction(Constants.ADD)
            .withUser(eperson1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(delete("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isForbidden());

        String token = getAuthToken(admin.getEmail(), password);
        getClient(token).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(ResoucePolicyMatcher.matchResourcePolicy(resourcePolicy))))
            .andExpect(jsonPath("$._links.self.href", Matchers
                .containsString("/api/authz/resourcepolicies/" + resourcePolicy.getID())));
    }

    @Test
    public void deleteOneNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(delete("/api/authz/resourcepolicies/" + Integer.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    public void patchReplaceStartDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date data = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withStartDate(data)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        Calendar newCalendar = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        newCalendar.set(Calendar.YEAR, 2020);
        newCalendar.set(Calendar.MONTH, 0);
        newCalendar.set(Calendar.DATE, 1);

        Date newDate = newCalendar.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/startDate", formatDate.format(newDate));
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(newDate))),
                hasJsonPath("$.endDate", is(resourcePolicy.getEndDate())))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(newDate))))));
    }

    @Test
    public void patchAddStartDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        Calendar newCalendar = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        newCalendar.set(Calendar.YEAR, 2019);
        newCalendar.set(Calendar.MONTH, 9);
        newCalendar.set(Calendar.DATE, 31);

        Date newDate = newCalendar.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        AddOperation addOperation = new AddOperation("/startDate", formatDate.format(newDate));
        ops.add(addOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(newDate))),
                hasJsonPath("$.endDate", is(resourcePolicy.getEndDate())))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(newDate))))));
    }

    @Test
    public void patchRemoveStartDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date data = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withStartDate(data)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation removeOperation = new RemoveOperation("/startDate");
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", nullValue()),
                hasJsonPath("$.endDate", is(resourcePolicy.getEndDate())))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", nullValue()))));
    }

    @Test
    public void patchReplaceStartDateBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date date = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withStartDate(date)
            .withDescription("my description")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String wrongStartDate = "";
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/startDate", wrongStartDate);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(date))),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())))));
    }

    @Test
    public void patchReplaceStartDataUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collection).build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 5);
        calendar.set(Calendar.DATE, 15);

        Date date = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.WRITE)
            .withDspaceObject(item)
            .withStartDate(date)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        Calendar calendar2 = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        calendar2.set(Calendar.YEAR, 2021);
        calendar2.set(Calendar.MONTH, 2);
        calendar2.set(Calendar.DATE, 21);

        Date newDate = calendar2.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/startDate", formatDate.format(newDate));
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient().perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnauthorized());

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(date))),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())))));
    }

    @Test
    public void patchReplaceStartDataForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date date = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(item)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withStartDate(date)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        Calendar calendar2 = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        calendar2.set(Calendar.YEAR, 2020);
        calendar2.set(Calendar.MONTH, 0);
        calendar2.set(Calendar.DATE, 1);

        Date newData = calendar2.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/startDate", formatDate.format(newData));
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isForbidden());

        String authToken2 = getAuthToken(admin.getEmail(), password);
        getClient(authToken2).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(date))),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())))));
    }

    @Test
    public void patchReplaceStartDateNotFoundTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        context.restoreAuthSystemState();

        Calendar calendar2 = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        calendar2.set(Calendar.YEAR, 2020);
        calendar2.set(Calendar.MONTH, 0);
        calendar2.set(Calendar.DATE, 1);

        Date newData = calendar2.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/startDate", formatDate.format(newData));
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + Integer.MAX_VALUE)
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void patchReplaceEndDateBeforeStartDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendarStartDate = Calendar.getInstance();

        calendarStartDate.set(Calendar.YEAR, 2019);
        calendarStartDate.set(Calendar.MONTH, 10);
        calendarStartDate.set(Calendar.DATE, 21);

        Date startDate = calendarStartDate.getTime();

        Calendar calendarEndDate = Calendar.getInstance();

        calendarEndDate.set(Calendar.YEAR, 2020);
        calendarEndDate.set(Calendar.MONTH, 10);
        calendarEndDate.set(Calendar.DATE, 21);

        Date endDate = calendarEndDate.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withUser(eperson1)
            .withStartDate(startDate)
            .withEndDate(endDate)
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        Calendar newEndDateCalendar = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        newEndDateCalendar.set(Calendar.YEAR, 2018);
        newEndDateCalendar.set(Calendar.MONTH, 10);
        newEndDateCalendar.set(Calendar.DATE, 21);

        Date newEndDate = newEndDateCalendar.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/endDate", formatDate.format(newEndDate));
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.startDate", is(formatDate.format(startDate))),
                hasJsonPath("$.endDate", is(formatDate.format(endDate))))));
    }

    @Test
    public void patchReplaceDescriptionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withDescription("my description")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String newDescription = "New Description";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/description", newDescription);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", is(newDescription)),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.description", is(newDescription)))));
    }

    @Test
    public void patchAddDescriptionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(item)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String description = "My Description";
        List<Operation> ops = new ArrayList<Operation>();
        AddOperation replaceOperation = new AddOperation("/description", description);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", is(description)),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.description", is(description)))));
    }

    @Test
    public void patchRemoveDescriptionTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(item)
            .withDescription("my description")
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation replaceOperation = new RemoveOperation("/description");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.description", nullValue()),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.description", nullValue()))));
    }

    @Test
    public void patchReplaceDescriptionUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collection).build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.WRITE)
            .withDspaceObject(item)
            .withDescription("My Description")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String newDescription = "New Description";

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/description", newDescription);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient().perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isUnauthorized());

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())))));
    }

    @Test
    public void patchReplaceDescriptionForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community).build();

        Item item = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(item)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withDescription("My Description")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String newDescription = "New Description";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/description", newDescription);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isForbidden());
    }

    @Test
    public void patchRemoveDescriptionNotFoundTest() throws Exception {
        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation removeOperation = new RemoveOperation("/description");
        ops.add(removeOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + Integer.MAX_VALUE)
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    public void patchReplaceDescriptionBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withDescription("my description")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String newDescription = "";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/description", newDescription);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.description", is(resourcePolicy.getRpDescription())))));
    }

    @Test
    public void patchReplaceNameTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item myItem = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(myItem)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withName("My name")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String newName = "New Name";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", newName);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(newName)),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(newName)))));
    }

    @Test
    public void patchReplaceNameBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item myItem = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(myItem)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withName("My name")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String errorName = "";
        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/name", errorName);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(resourcePolicy.getRpName())))));
    }

    @Test
    public void patchAddNameTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item myItem = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(myItem)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String name = "My Name";
        List<Operation> ops = new ArrayList<Operation>();
        AddOperation replaceOperation = new AddOperation("/name", name);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(name)))));
    }

    @Test
    public void patchAddNameBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item myItem = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(myItem)
            .withName("My name")
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        String name = "Add Name";
        List<Operation> ops = new ArrayList<Operation>();
        AddOperation replaceOperation = new AddOperation("/name", name);
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(resourcePolicy.getRpName())))));
    }

    @Test
    public void patchRemoveNameTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item myItem = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(myItem)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withName("My Name")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation replaceOperation = new RemoveOperation("/name");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", nullValue()))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", nullValue()))));
    }

    @Test
    public void patchRemoveNameForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .build();

        Item myItem = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(myItem)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withName("My Name")
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();
        RemoveOperation replaceOperation = new RemoveOperation("/name");
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isForbidden());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(resourcePolicy.getRpName())))));
    }

    @Test
    public void patchSuccessfulMultipleOperationsTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendarStartDate = Calendar.getInstance();

        calendarStartDate.set(Calendar.YEAR, 2017);
        calendarStartDate.set(Calendar.MONTH, 0);
        calendarStartDate.set(Calendar.DATE, 1);

        Date startDate = calendarStartDate.getTime();

        Calendar calendarEndDate = Calendar.getInstance();

        calendarEndDate.set(Calendar.YEAR, 2022);
        calendarEndDate.set(Calendar.MONTH, 11);
        calendarEndDate.set(Calendar.DATE, 31);

        Date endDate = calendarEndDate.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withStartDate(startDate)
            .withEndDate(endDate)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();

        String addName = "My Name";
        AddOperation addNameOperation = new AddOperation("/name", addName);
        ops.add(addNameOperation);

        String addDescription = "My Description";
        AddOperation addDescriptionOperation = new AddOperation("/description", addDescription);
        ops.add(addDescriptionOperation);

        String newName = "New Name";
        ReplaceOperation replaceNameOperation = new ReplaceOperation("/name", newName);
        ops.add(replaceNameOperation);

        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendarNewStartDate = Calendar.getInstance();
        calendarNewStartDate.set(Calendar.YEAR, 2018);
        calendarNewStartDate.set(Calendar.MONTH, 1);
        calendarNewStartDate.set(Calendar.DATE, 1);

        Date newStartDate = calendarNewStartDate.getTime();
        ReplaceOperation replaceStartDateOperation = new ReplaceOperation("/startDate",
            formatDate.format(newStartDate));
        ops.add(replaceStartDateOperation);

        RemoveOperation removeEndDateOperation = new RemoveOperation("/endDate");
        ops.add(removeEndDateOperation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.name", is(newName)),
                hasJsonPath("$.description", is(addDescription)),
                hasJsonPath("$.startDate", is(formatDate.format(newStartDate))),
                hasJsonPath("$.endDate", nullValue()),
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])))));

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(newName)),
                hasJsonPath("$.startDate", is(formatDate.format(newStartDate))),
                hasJsonPath("$.endDate", nullValue()),
                hasJsonPath("$.description", is(addDescription)))));
    }

    @Test
    public void patchWithMultipleOperationsFailTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
            .withEmail("eperson1@mail.com")
            .withPassword("qwerty01")
            .build();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection collection = CollectionBuilder.createCollection(context, community)
            .withAdminGroup(eperson1)
            .build();

        Item publicItem1 = ItemBuilder.createItem(context, collection)
            .withTitle("Public item")
            .build();

        Calendar calendarEndDate = Calendar.getInstance();

        calendarEndDate.set(Calendar.YEAR, 2022);
        calendarEndDate.set(Calendar.MONTH, 11);
        calendarEndDate.set(Calendar.DATE, 31);

        Date endDate = calendarEndDate.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
            .withAction(Constants.READ)
            .withDspaceObject(publicItem1)
            .withName("My Name")
            .withEndDate(endDate)
            .withGroup(EPersonServiceFactory.getInstance().getGroupService().findByName(context, Group.ANONYMOUS))
            .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
            .build();

        context.restoreAuthSystemState();

        List<Operation> ops = new ArrayList<Operation>();

        String newName = "New Name";
        ReplaceOperation replaceNameOperation = new ReplaceOperation("/name", newName);
        ops.add(replaceNameOperation);

        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");
        Calendar calendarNewStartDate = Calendar.getInstance();
        calendarNewStartDate.set(Calendar.YEAR, 2018);
        calendarNewStartDate.set(Calendar.MONTH, 1);
        calendarNewStartDate.set(Calendar.DATE, 1);

        Date newStartDate = calendarNewStartDate.getTime();
        ReplaceOperation replaceStartDateOperation = new ReplaceOperation("/startDate",
            formatDate.format(newStartDate));
        ops.add(replaceStartDateOperation);

        RemoveOperation removeEndDateOperation = new RemoveOperation("/endDate");
        ops.add(removeEndDateOperation);

        String patchBody = getPatchContent(ops);

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
            .content(patchBody)
            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
            .andExpect(status().isBadRequest());

        getClient(authToken).perform(get("/api/authz/resourcepolicies/" + resourcePolicy.getID()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", Matchers.allOf(
                hasJsonPath("$.action", is(Constants.actionText[resourcePolicy.getAction()])),
                hasJsonPath("$.name", is(resourcePolicy.getRpName())),
                hasJsonPath("$.startDate", nullValue()),
                hasJsonPath("$.endDate", is(formatDate.format(endDate))),
                hasJsonPath("$.description", nullValue()))));
    }
}
