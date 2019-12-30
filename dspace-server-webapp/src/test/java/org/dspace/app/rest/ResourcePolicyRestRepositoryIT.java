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
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.ResoucePolicyMatcher;
import org.dspace.app.rest.model.ResourcePolicyRest;
import org.dspace.app.rest.model.patch.Operation;
import org.dspace.app.rest.model.patch.ReplaceOperation;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.authorize.service.ResourcePolicyService;
import org.dspace.content.Collection;
import org.dspace.content.Community;
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
 *
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
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
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
    public void findOneUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context).withDspaceObject(community)
                .withAction(Constants.READ).withPolicyType(ResourcePolicy.TYPE_CUSTOM).withUser(eperson).build();

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

        String authToken = getAuthToken(eperson1.getEmail(),"qwerty01");

        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
                .param("uuid",eperson1.getID().toString()))
        .andExpect(status().isOk())
        .andExpect(content().contentType(contentType))
        .andExpect(jsonPath("$._embedded.resourcepolicies", Matchers.contains(
                 ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfEPerson1))))
        .andExpect(jsonPath("$._embedded.resourcepolicies",
                Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfEPerson2)))))
        .andExpect(jsonPath("$._links.self.href", Matchers.containsString("api/authz/resourcepolicies/search/eperson")))
        .andExpect(jsonPath("$.page.totalElements", is(1)));
    }


    @Test
    public void findResoucesPolicyByEpersonUuidAndResourceUuidTest() throws Exception {
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
                .andExpect(jsonPath("$._embedded.resourcepolicies",Matchers.containsInAnyOrder(
                        ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfCommunity),
                        ResoucePolicyMatcher.matchResourcePolicy(secondResourcePolicyOfCommunity)
                        )))
                .andExpect(jsonPath("$._embedded.resourcepolicies",
                        Matchers.not(is(ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyOfCollection)))))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("api/authz/resourcepolicies/search/eperson")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));
    }

    /**
     * this method return 422, but must return 400
     * FIX BUG
     * @throws Exception
     */
    @Test
    public void findResoucesPolicyEPersonWithoutParametersBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context).withEmail("myemail@mail.com").withPassword("qwerty01")
                .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void findResoucesPolicyByEPersonUuidUnAuthenticatedTest() throws Exception {
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
    public void findEPersonNotFoundTest() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
        .param("uuid", UUID.randomUUID().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void findResoucesPolicyByEPersonUuidForbiddenTest() throws Exception {
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
                .findByTypeGroupAction(context,community, EPersonServiceFactory.getInstance()
                .getGroupService()
                .findByName(context, Group.ANONYMOUS),Constants.READ);

      context.restoreAuthSystemState();

      String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
      getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
                .param("uuid", community.getID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.resourcepolicies",Matchers.containsInAnyOrder(
                        ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson1),
                        ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson2),
                        ResoucePolicyMatcher.matchResourcePolicy(resourcePolicyAnonymous)
                        )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("api/authz/resourcepolicies/search/resource")))
                .andExpect(jsonPath("$.page.totalElements", is(3)));
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

        Collection collection = CollectionBuilder.createCollection(context,community).withName("My collection").build();


        ResourcePolicy firstResourcePolicyOfEPerson1 = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community)
                .withAction(Constants.ADMIN)
                .withUser(eperson1).build();

        ResourcePolicy firstResourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(collection)
                .withAction(Constants.ADD)
                .withUser(eperson2).build();

      context.restoreAuthSystemState();

      String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
      getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
                .param("uuid", community.getID().toString())
                .param("action", "ADMIN"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.resourcepolicies",Matchers.contains(
                        ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson1)
                        )))
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("api/authz/resourcepolicies/search/resource")))
                .andExpect(jsonPath("$.page.totalElements", is(1)));

      String authToken2 = getAuthToken(eperson2.getEmail(), "qwerty02");
      getClient(authToken2).perform(get("/api/authz/resourcepolicies/search/resource")
                .param("uuid", collection.getID().toString())
                .param("action", "ADD"))
                .andExpect(status().isForbidden());
  }

  /**
   * this method return 422, but must return 400
   * FIX BUG
   * @throws Exception
   */
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
              .andExpect(status().isUnprocessableEntity());
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
  public void findResourceNotFoundTest() throws Exception {

      String authToken = getAuthToken(admin.getEmail(), password);
      getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
      .param("uuid", UUID.randomUUID().toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements", is(0)));
  }


  @Test
  public void findResoucesPoliciesByResourceUuidForbiddenTest() throws Exception {
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
    public void findResourcePolicyByGroupUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Group group1 = GroupBuilder.createGroup(context).withName("My group").build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .withGroupMembership(group1)
                .build();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("My community")
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
                .andExpect(jsonPath("$._links.self.href",
                        Matchers.containsString("api/authz/resourcepolicies/search/group")))
                .andExpect(jsonPath("$.page.totalElements", is(3)));
    }

    @Test
    public void findResourcePolicyByGroupUuidAndResourceUuidTest() throws Exception {
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

    /**
     * this method return 422, but must return 400
     * FIX BUG
     * @throws Exception
     */
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
                .andExpect(status().isUnprocessableEntity());
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
    public void findResoucesPolicyByGroupUuidForbiddenTest() throws Exception {
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

        Community community2 = CommunityBuilder.createCommunity(context).withName("My 2 community").build();

        ResourcePolicy resourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community).withAction(Constants.WRITE)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withGroup(group1).build();

        ResourcePolicy secondresourcePolicyOfGroup1 = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community2).withAction(Constants.ADD)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withGroup(group1).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson2.getEmail(), "qwerty02");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group")
                .param("uuid", group1.getID().toString())
                .param("resource", community2.getID().toString()))
                .andExpect(status().isForbidden());
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
                    .andExpect(jsonPath("$",Matchers.allOf(
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
    public void deliteOneUnAuthenticatedTest() throws Exception {
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
    public void deliteOneForbiddenTest() throws Exception {
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

    }

    @Test
    public void deliteOneNotFoundTest() throws Exception {
        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(delete("/api/authz/resourcepolicies/" + Integer.MAX_VALUE))
                 .andExpect(status().isNotFound());
    }

    @Test
    public void patchReplaceStartDataTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("My Community")
                .build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .build();


        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date data = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
                .withAction(Constants.READ)
                .withDspaceObject(community)
                .withUser(eperson1)
                .withStartDate(data)
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

        String authToken = getAuthToken(admin.getEmail(),password);
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isOk());
    }

    @Test
    public void patchReplaceStartDataUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("My Community")
                .build();

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2010);
        calendar.set(Calendar.MONTH, 5);
        calendar.set(Calendar.DATE, 15);

        Date data = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
                .withAction(Constants.WRITE)
                .withDspaceObject(community)
                .withStartDate(data)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .build();

        context.restoreAuthSystemState();

        Calendar calendar2 = Calendar.getInstance();
        SimpleDateFormat formatDate = new SimpleDateFormat("yyyy-MM-dd");

        calendar2.set(Calendar.YEAR, 2021);
        calendar2.set(Calendar.MONTH, 2);
        calendar2.set(Calendar.DATE, 21);

        Date newData = calendar2.getTime();

        List<Operation> ops = new ArrayList<Operation>();
        ReplaceOperation replaceOperation = new ReplaceOperation("/startDate", formatDate.format(newData));
        ops.add(replaceOperation);
        String patchBody = getPatchContent(ops);

        getClient().perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
                   .content(patchBody)
                   .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                   .andExpect(status().isUnauthorized());
    }

    @Test
    public void patchReplaceStartDataForbiddenTest() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
                .withName("My Community")
                .build();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("eperson1@mail.com")
                .withPassword("qwerty01")
                .build();


        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.YEAR, 2019);
        calendar.set(Calendar.MONTH, 9);
        calendar.set(Calendar.DATE, 31);

        Date data = calendar.getTime();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
                .withAction(Constants.READ)
                .withDspaceObject(community)
                .withUser(eperson1)
                .withStartDate(data)
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

        String authToken = getAuthToken(eperson1.getEmail(),"qwerty01");
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + resourcePolicy.getID())
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isForbidden());
    }

    @Test
    public void patchReplaceStartDataNotFoundTest() throws Exception {

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

        String authToken = getAuthToken(admin.getEmail(),password);
        getClient(authToken).perform(patch("/api/authz/resourcepolicies/" + Integer.MAX_VALUE)
                            .content(patchBody)
                            .contentType(MediaType.APPLICATION_JSON_PATCH_JSON))
                            .andExpect(status().isNotFound());
    }
}
