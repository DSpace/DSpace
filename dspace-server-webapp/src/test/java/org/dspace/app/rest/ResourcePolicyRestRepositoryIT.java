/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.is;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.EPersonBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ResourcePolicyBuilder;
import org.dspace.app.rest.matcher.ResoucePolicyMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.ResourcePolicy;
import org.dspace.authorize.service.AuthorizeService;
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

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context).withDspaceObject(community)
                .withAction(Constants.READ).withPolicyType(ResourcePolicy.TYPE_CUSTOM).withUser(admin).build();

        context.restoreAuthSystemState();

        String authToken = getAuthToken(admin.getEmail(), password);
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
    public void searchOneResoucesPolicyByEpersonUuidTest() throws Exception {
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
                .withDspaceObject(community).withAction(Constants.ADD).withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withUser(eperson1).build();

        ResourcePolicy resourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community2).withAction(Constants.REMOVE)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM).withUser(eperson2).build();

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
    public void searchResoucesPolicyByEpersonUuidAndResourceUuidTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context).withEmail("myemail@mail.com").withPassword("qwerty01")
                .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();
        Collection collection = CollectionBuilder.createCollection(context, community).withName("My collection")
                .build();

        ResourcePolicy resourcePolicyOfCommunity = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community).withAction(Constants.READ).withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withUser(eperson1).build();

        ResourcePolicy secondResourcePolicyOfCommunity = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community).withAction(Constants.REMOVE).withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withUser(eperson1).build();

        ResourcePolicy resourcePolicyOfCollection = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(collection).withAction(Constants.REMOVE)
                .withPolicyType(ResourcePolicy.TYPE_SUBMISSION).withUser(eperson1).build();

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
    public void searchResoucesPolicyEPersonWithoutParametersBadRequestTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context).withEmail("myemail@mail.com").withPassword("qwerty01")
                .build();
        context.restoreAuthSystemState();

        String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void searchResoucesPolicyByEPersonUuidUnAuthenticatedTest() throws Exception {
        context.turnOffAuthorisationSystem();

        EPerson eperson1 = EPersonBuilder.createEPerson(context)
                .withEmail("myemail@mail.com")
                .withPassword("qwerty01")
                .build();

        Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

        ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(community)
                .withAction(Constants.READ)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withUser(eperson1).build();

        context.restoreAuthSystemState();

        getClient().perform(get("/api/authz/resourcepolicies/search/eperson")
                .param("uuid", eperson1.getID().toString())
                .param("resource", community.getID().toString()))
                .andExpect(status().isUnauthorized());

    }

    @Test
    public void searchEPersonNotFoundTest() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/eperson")
        .param("uuid", UUID.randomUUID().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void searchResoucesPolicyByEPersonUuidForbiddenTest() throws Exception {
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
  public void searchResourcePoliciesOfOneResourceWithoutActionTest() throws Exception {
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
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
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
  public void searchResourcePoliciesOfOneResourceIsOwnerTest() throws Exception {
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
                .withDspaceObject(collection)
                .withAction(Constants.ADMIN)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withUser(eperson1).build();

        ResourcePolicy firstResourcePolicyOfEPerson2 = ResourcePolicyBuilder.createResourcePolicy(context)
                .withDspaceObject(collection)
                .withAction(Constants.ADD)
                .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
                .withUser(eperson2).build();

      context.restoreAuthSystemState();

      String authToken = getAuthToken(eperson1.getEmail(), "qwerty01");
      getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
                .param("uuid", collection.getID().toString())
                .param("action", "ADD"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(contentType))
                .andExpect(jsonPath("$._embedded.resourcepolicies",Matchers.contains(
                        ResoucePolicyMatcher.matchResourcePolicy(firstResourcePolicyOfEPerson2)
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
  public void searchResoucesPoliciesOfResourceWithoutParametersBadRequestTest() throws Exception {
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
  public void searchResoucesPoliciesByResourceUuidUnAuthenticatedTest() throws Exception {
      context.turnOffAuthorisationSystem();

      EPerson eperson1 = EPersonBuilder.createEPerson(context)
              .withEmail("myemail@mail.com")
              .withPassword("qwerty01")
              .build();

      Community community = CommunityBuilder.createCommunity(context).withName("My community").build();

      ResourcePolicy resourcePolicy = ResourcePolicyBuilder.createResourcePolicy(context)
              .withDspaceObject(community)
              .withAction(Constants.READ)
              .withPolicyType(ResourcePolicy.TYPE_CUSTOM)
              .withUser(eperson1).build();

      context.restoreAuthSystemState();

      getClient().perform(get("/api/authz/resourcepolicies/search/resource")
              .param("uuid", community.getID().toString()))
              .andExpect(status().isUnauthorized());

  }

  @Test
  public void searchResourceNotFoundTest() throws Exception {

      String authToken = getAuthToken(admin.getEmail(), password);
      getClient(authToken).perform(get("/api/authz/resourcepolicies/search/resource")
      .param("uuid", UUID.randomUUID().toString()))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.page.totalElements", is(0)));
  }


  @Test
  public void searchResoucesPoliciesByResourceUuidForbiddenTest() throws Exception {
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
              .param("uuid", eperson1.getID().toString())
              .param("resource", community2.getID().toString()))
              .andExpect(status().isForbidden());
  }


    @Test
    public void searchResourcePolicyByGroupUuidTest() throws Exception {
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
    public void searchResourcePolicyByGroupUuidAndResourceUuidTest() throws Exception {
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
    public void searchResoucesPoliciesByGroupWithoutParametersBadRequestTest() throws Exception {
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
    public void searchResoucesPoliciesByGroupUuidUnAuthenticatedTest() throws Exception {
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
    public void searchGroupNotFoundTest() throws Exception {

        String authToken = getAuthToken(admin.getEmail(), password);
        getClient(authToken).perform(get("/api/authz/resourcepolicies/search/group")
        .param("uuid", UUID.randomUUID().toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    public void searchResoucesPolicyByGroupUuidForbiddenTest() throws Exception {
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
}
