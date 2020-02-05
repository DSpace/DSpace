/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.dspace.app.rest.builder.CollectionBuilder;
import org.dspace.app.rest.builder.CommunityBuilder;
import org.dspace.app.rest.builder.GroupBuilder;
import org.dspace.app.rest.builder.ItemBuilder;
import org.dspace.app.rest.matcher.BrowseEntryResourceMatcher;
import org.dspace.app.rest.matcher.BrowseIndexMatcher;
import org.dspace.app.rest.matcher.ItemMatcher;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.eperson.Group;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test to test the /api/discover/browses endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 *
 * @author Frederic Van Reet (frederic dot vanreet at atmire dot com)
 * @author Tom Desair (tom dot desair at atmire dot com)
 */
public class BrowsesResourceControllerIT extends AbstractControllerIntegrationTest {

    @Test
    public void findAll() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browses"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //Our default Discovery config has 4 browse indexes so we expect this to be reflected in the page
                   // object
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(4)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //The array of browse index should have a size 4
                   .andExpect(jsonPath("$._embedded.browses", hasSize(4)))

                   //Check that all (and only) the default browse indexes are present
                   .andExpect(jsonPath("$._embedded.browses", containsInAnyOrder(
                       BrowseIndexMatcher.dateIssuedBrowseIndex("asc"),
                       BrowseIndexMatcher.contributorBrowseIndex("asc"),
                       BrowseIndexMatcher.titleBrowseIndex("asc"),
                       BrowseIndexMatcher.subjectBrowseIndex("asc")
                   )))
        ;
    }

    @Test
    public void findBrowseByTitle() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browses/title"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //Check that the JSON root matches the expected browse index
                   .andExpect(jsonPath("$", BrowseIndexMatcher.titleBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseByDateIssued() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browses/dateissued"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //Check that the JSON root matches the expected browse index
                   .andExpect(jsonPath("$", BrowseIndexMatcher.dateIssuedBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseByContributor() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browses/author"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //Check that the JSON root matches the expected browse index
                   .andExpect(jsonPath("$", BrowseIndexMatcher.contributorBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseBySubject() throws Exception {
        //When we call the root endpoint
        getClient().perform(get("/api/discover/browses/subject"))
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //Check that the JSON root matches the expected browse index
                   .andExpect(jsonPath("$", BrowseIndexMatcher.subjectBrowseIndex("asc")))
        ;
    }

    @Test
    public void findBrowseBySubjectEntries() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Three public items that are readable by Anonymous with different subjects
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("TestingForMore").withSubject("ExtraEntry")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest").withSubject("TestingForMore")
                                      .withSubject("ExtraEntry")
                                      .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find which subjects are currently in the repository
        getClient().perform(get("/api/discover/browses/subject/entries")
                    .param("projection", "full"))

                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())

                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   //Check that there are indeed 3 different subjects
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   //Check the embedded resources and that they're sorted alphabetically
                   //Check that the subject matches as expected
                   //Verify that they're sorted alphabetically
                   .andExpect(jsonPath("$._embedded.entries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("AnotherTest", 1),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("ExtraEntry", 3),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("TestingForMore", 2)
                                       )));

        getClient().perform(get("/api/discover/browses/subject/entries")
                                .param("sort", "value,desc"))

                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())
                   .andDo(MockMvcResultHandlers.print())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   //Check that there are indeed 3 different subjects
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   //Check the embedded resources and that they're sorted alphabetically
                   //Check that the subject matches as expected
                   //Verify that they're sorted alphabetically
                   .andExpect(jsonPath("$._embedded.entries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("TestingForMore", 2),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("ExtraEntry", 3),
                                                BrowseEntryResourceMatcher.matchBrowseEntry("AnotherTest", 1)
                                       )));
    }

    @Test
    public void findBrowseBySubjectItems() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Two public items with the same subject and another public item that contains that same subject, but also
        // another one
        //   All of the items are readable by an Anonymous user
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("zPublic item more")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("ExtraEntry").withSubject("AnotherTest")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest")
                                      .build();

        Item publicItem3 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 3")
                                      .withIssueDate("2016-02-14")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("AnotherTest")
                                      .build();

        //** WHEN **
        //An anonymous user browses the items that correspond with the ExtraEntry subject query
        getClient().perform(get("/api/discover/browses/subject/items")
                                .param("filterValue", "ExtraEntry"))
                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   //We expect there to be only one element, the one that we've added with the requested subject
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   //Verify that the title of the public and embargoed items are present and sorted descending
                   .andExpect(jsonPath("$._embedded.items", contains(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "zPublic item more", "2017-10-17")
                   )));

        //** WHEN **
        //An anonymous user browses the items that correspond with the AnotherTest subject query
        getClient().perform(get("/api/discover/browses/subject/items")
                                .param("filterValue", "AnotherTest"))
                   //** THEN **
                   //The status has to be 200
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))
                   //We expect there to be only three elements, the ones that we've added with the requested subject
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   .andExpect(jsonPath("$.page.size", is(20)))
                   //Verify that the title of the public and embargoed items are present and sorted descending
                   .andExpect(jsonPath("$._embedded.items", contains(
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem3, "Public item 3", "2016-02-14"),
                       ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "zPublic item more", "2017-10-17")
                   )));

    }

    @Test
    public void findBrowseByTitleItems() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. Two public items that are readable by Anonymous
        Item publicItem1 = ItemBuilder.createItem(context, col1)
                                      .withTitle("Public item 1")
                                      .withIssueDate("2017-10-17")
                                      .withAuthor("Smith, Donald").withAuthor("Doe, John")
                                      .withSubject("Java").withSubject("Unit Testing")
                                      .build();

        Item publicItem2 = ItemBuilder.createItem(context, col2)
                                      .withTitle("Public item 2")
                                      .withIssueDate("2016-02-13")
                                      .withAuthor("Smith, Maria").withAuthor("Doe, Jane")
                                      .withSubject("Angular").withSubject("Unit Testing")
                                      .build();

        //3. An item that has been made private
        Item privateItem = ItemBuilder.createItem(context, col1)
                                      .withTitle("This is a private item")
                                      .withIssueDate("2015-03-12")
                                      .withAuthor("Duck, Donald")
                                      .withSubject("Cartoons").withSubject("Ducks")
                                      .makeUnDiscoverable()
                                      .build();

        //4. An item with an item-level embargo
        Item embargoedItem = ItemBuilder.createItem(context, col2)
                                        .withTitle("An embargoed publication")
                                        .withIssueDate("2017-08-10")
                                        .withAuthor("Mouse, Mickey")
                                        .withSubject("Cartoons").withSubject("Mice")
                                        .withEmbargoPeriod("12 months")
                                        .build();

        //5. An item that is only readable for an internal groups
        Group internalGroup = GroupBuilder.createGroup(context)
                                          .withName("Internal Group")
                                          .build();

        Item internalItem = ItemBuilder.createItem(context, col2)
                                       .withTitle("Internal publication")
                                       .withIssueDate("2016-09-19")
                                       .withAuthor("Doe, John")
                                       .withSubject("Unknown")
                                       .withReaderGroup(internalGroup)
                                       .build();

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses the items in the Browse by item endpoint
        //sorted descending by tile
        getClient().perform(get("/api/discover/browses/title/items")
                                .param("sort", "title,desc"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect only the two public items and the embargoed item to be present
                   .andExpect(jsonPath("$.page.size", is(20)))
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   .andExpect(jsonPath("$.page.totalPages", is(1)))
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //Verify that the title of the public and embargoed items are present and sorted descending
                   .andExpect(jsonPath("$._embedded.items",
                                       contains(ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2,
                                                                                            "Public item 2",
                                                                                            "2016-02-13"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                                                                                            "Public item 1",
                                                                                            "2017-10-17"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(embargoedItem,
                                                                                            "An embargoed publication",
                                                                                            "2017-08-10"))))

                   //The private and internal items must not be present
                   .andExpect(jsonPath("$._embedded.items[*].metadata", Matchers.allOf(
                           not(matchMetadata("dc.title", "This is a private item")),
                           not(matchMetadata("dc.title", "Internal publication")))));
    }

    @Test
    /**
     * This test was introduced to reproduce the bug DS-4269 Pagination links must be consistent also when there is not
     * explicit pagination parameters in the request (i.e. defaults apply)
     * 
     * @throws Exception
     */
    public void browsePaginationWithoutExplicitParams() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community and one collection.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Collection col1 = CollectionBuilder.createCollection(context, parentCommunity).withName("Collection 1").build();

        //2. Twenty-one public items that are readable by Anonymous
        for (int i = 0; i <= 20; i++) {
            ItemBuilder.createItem(context, col1)
                  .withTitle("Public item " + String.format("%02d", i))
                  .withIssueDate("2017-10-17")
                  .withAuthor("Test, Author" + String.format("%02d", i))
                  .withSubject("Java").withSubject("Unit Testing")
                  .build();
        }

        context.restoreAuthSystemState();

        //** WHEN **
        //An anonymous user browses the items in the Browse by item endpoint
        getClient().perform(get("/api/discover/browses/title/items")
               .param("projection", "full"))
               //** THEN **
               //The status has to be 200 OK
               .andExpect(status().isOk())
               //We expect the content type to be "application/hal+json;charset=UTF-8"
               .andExpect(content().contentType(contentType))
               //We expect 21 public items
               .andExpect(jsonPath("$.page.size", is(20)))
               .andExpect(jsonPath("$.page.totalElements", is(21)))
               .andExpect(jsonPath("$.page.totalPages", is(2)))
               .andExpect(jsonPath("$.page.number", is(0)))
               // embedded items are already checked by other test, we focus on links here
               .andExpect(jsonPath("$._links.next.href", Matchers.containsString("/api/discover/browses/title/items?")))
               .andExpect(jsonPath("$._links.last.href", Matchers.containsString("/api/discover/browses/title/items?")))
               .andExpect(jsonPath("$._links.self.href", Matchers.endsWith("/api/discover/browses/title/items")));

        //** WHEN **
        //An anonymous user browses the items in the Browse by item endpoint
        getClient().perform(get("/api/discover/browses/author/entries")
               .param("projection", "full"))
               //** THEN **
               //The status has to be 200 OK
               .andExpect(status().isOk())
               //We expect the content type to be "application/hal+json;charset=UTF-8"
               .andExpect(content().contentType(contentType))
               //We expect 21 public items
               .andExpect(jsonPath("$.page.size", is(20)))
               .andExpect(jsonPath("$.page.totalElements", is(21)))
               .andExpect(jsonPath("$.page.totalPages", is(2)))
               .andExpect(jsonPath("$.page.number", is(0)))
               // embedded items are already checked by other test, we focus on links here
                .andExpect(jsonPath("$._links.next.href",
                        Matchers.containsString("/api/discover/browses/author/entries?")))
                .andExpect(jsonPath("$._links.last.href",
                        Matchers.containsString("/api/discover/browses/author/entries?")))
                .andExpect(jsonPath("$._links.self.href", Matchers.endsWith("/api/discover/browses/author/entries")));
    }

    @Test
    public void testPaginationBrowseByDateIssuedItems() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. 7 public items that are readable by Anonymous
        Item item1 = ItemBuilder.createItem(context, col1)
                                .withTitle("Item 1")
                                .withIssueDate("2017-10-17")
                                .build();

        Item item2 = ItemBuilder.createItem(context, col2)
                                .withTitle("Item 2")
                                .withIssueDate("2016-02-13")
                                .build();

        Item item3 = ItemBuilder.createItem(context, col1)
                                .withTitle("Item 3")
                                .withIssueDate("2016-02-12")
                                .build();

        Item item4 = ItemBuilder.createItem(context, col2)
                                .withTitle("Item 4")
                                .withIssueDate("2016-02-11")
                                .build();

        Item item5 = ItemBuilder.createItem(context, col1)
                                .withTitle("Item 5")
                                .withIssueDate("2016-02-10")
                                .build();

        Item item6 = ItemBuilder.createItem(context, col2)
                                .withTitle("Item 6")
                                .withIssueDate("2016-01-13")
                                .build();

        Item item7 = ItemBuilder.createItem(context, col1)
                                .withTitle("Item 7")
                                .withIssueDate("2016-01-12")
                                .build();

        //** WHEN **
        //An anonymous user browses the items in the Browse by date issued endpoint
        //sorted ascending by tile with a page size of 5
        getClient().perform(get("/api/discover/browses/dateissued/items")
                                .param("sort", "title,asc")
                                .param("size", "5"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect only the first five items to be present
                   .andExpect(jsonPath("$.page.size", is(5)))
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //Verify that the title and date of the items match and that they are sorted ascending
                   .andExpect(jsonPath("$._embedded.items",
                                       contains(ItemMatcher.matchItemWithTitleAndDateIssued(item1,
                                                                                            "Item 1", "2017-10-17"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item2,
                                                                                            "Item 2", "2016-02-13"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item3,
                                                                                            "Item 3", "2016-02-12"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item4,
                                                                                            "Item 4", "2016-02-11"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item5,
                                                                                            "Item 5", "2016-02-10")
                                       )));

        //The next page gives us the last two items
        getClient().perform(get("/api/discover/browses/dateissued/items")
                                .param("sort", "title,asc")
                                .param("size", "5")
                                .param("page", "1"))

                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect only the first five items to be present
                   .andExpect(jsonPath("$.page.size", is(5)))
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   .andExpect(jsonPath("$.page.totalPages", is(2)))
                   .andExpect(jsonPath("$.page.number", is(1)))

                   //Verify that the title and date of the items match and that they are sorted ascending
                   .andExpect(jsonPath("$._embedded.items",
                                       contains(ItemMatcher.matchItemWithTitleAndDateIssued(item6,
                                                                                            "Item 6", "2016-01-13"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item7,
                                                                                            "Item 7", "2016-01-12")
                                       )));
    }

    @Test
    public void testBrowseByEntriesStartsWith() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. 7 public items that are readable by Anonymous
        Item item1 = ItemBuilder.createItem(context, col1)
                                .withTitle("Alan Turing")
                                .withAuthor("Turing, Alan Mathison")
                                .withIssueDate("1912-06-23")
                                .withSubject("Computing")
                                .build();

        Item item2 = ItemBuilder.createItem(context, col1)
                                .withTitle("Blade Runner")
                                .withAuthor("Scott, Ridley")
                                .withIssueDate("1982-06-25")
                                .withSubject("Science Fiction")
                                .build();

        Item item3 = ItemBuilder.createItem(context, col1)
                                .withTitle("Python")
                                .withAuthor("Van Rossum, Guido")
                                .withIssueDate("1990")
                                .withSubject("Computing")
                                .build();

        Item item4 = ItemBuilder.createItem(context, col2)
                                .withTitle("Java")
                                .withAuthor("Gosling, James")
                                .withIssueDate("1995-05-23")
                                .withSubject("Computing")
                                .build();

        Item item5 = ItemBuilder.createItem(context, col2)
                                .withTitle("Zeta Reticuli")
                                .withAuthor("Universe")
                                .withIssueDate("2018-01-01")
                                .withSubject("Astronomy")
                                .build();

        Item item6 = ItemBuilder.createItem(context, col2)
                                .withTitle("Moon")
                                .withAuthor("Universe")
                                .withIssueDate("2018-01-02")
                                .withSubject("Astronomy")
                                .build();

        Item item7 = ItemBuilder.createItem(context, col2)
                                .withTitle("T-800")
                                .withAuthor("Cameron, James")
                                .withIssueDate("2029")
                                .withSubject("Science Fiction")
                                .build();

         // ---- BROWSES BY ENTRIES ----

        //** WHEN **
        //An anonymous user browses the entries in the Browse by Author endpoint
        //with startsWith set to U
        getClient().perform(get("/api/discover/browses/author/entries?startsWith=U")
                                .param("size", "2")
                   .param("projection", "full"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect only the "Universe" entry to be present
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   //As entry browsing works as a filter, we expect to be on page 0
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //Verify that the index filters to the "Universe" entries and Counts 2 Items.
                   .andExpect(jsonPath("$._embedded.entries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("Universe", 2)
                                       )))
                   //Verify startsWith parameter is included in the links
                    .andExpect(jsonPath("$._links.self.href", containsString("?startsWith=U")));

        //** WHEN **
        //An anonymous user browses the entries in the Browse by Author endpoint
        //with startsWith set to T and scope set to Col 1
        getClient().perform(get("/api/discover/browses/author/entries?startsWith=T")
                                .param("scope", col1.getID().toString()))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect only the entry "Turing, Alan Mathison" to be present
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   //As entry browsing works as a filter, we expect to be on page 0
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //Verify that the index filters to the "Turing, Alan'" items.
                   .andExpect(jsonPath("$._embedded.entries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("Turing, Alan Mathison", 1)
                                       )))
                   //Verify that the startsWith paramater is included in the links
                    .andExpect(jsonPath("$._links.self.href", containsString("?startsWith=T")));

        //** WHEN **
        //An anonymous user browses the entries in the Browse by Subject endpoint
        //with startsWith set to C
        getClient().perform(get("/api/discover/browses/subject/entries?startsWith=C"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect only the entry "Computing" to be present
                   .andExpect(jsonPath("$.page.totalElements", is(1)))
                   //As entry browsing works as a filter, we expect to be on page 0
                   .andExpect(jsonPath("$.page.number", is(0)))

                   //Verify that the index filters to the "Computing'" items.
                   .andExpect(jsonPath("$._embedded.entries",
                                       contains(BrowseEntryResourceMatcher.matchBrowseEntry("Computing", 3)
                                       )))
                   //Verify that the startsWith paramater is included in the links
                    .andExpect(jsonPath("$._links.self.href", containsString("?startsWith=C")));

    };

    @Test
    public void testBrowseByItemsStartsWith() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. 7 public items that are readable by Anonymous
        Item item1 = ItemBuilder.createItem(context, col1)
                                .withTitle("Alan Turing")
                                .withAuthor("Turing, Alan Mathison")
                                .withIssueDate("1912-06-23")
                                .withSubject("Computing")
                                .build();

        Item item2 = ItemBuilder.createItem(context, col1)
                                .withTitle("Blade Runner")
                                .withAuthor("Scott, Ridley")
                                .withIssueDate("1982-06-25")
                                .withSubject("Science Fiction")
                                .build();

        Item item3 = ItemBuilder.createItem(context, col1)
                                .withTitle("Python")
                                .withAuthor("Van Rossum, Guido")
                                .withIssueDate("1990")
                                .withSubject("Computing")
                                .build();

        Item item4 = ItemBuilder.createItem(context, col2)
                                .withTitle("Java")
                                .withAuthor("Gosling, James")
                                .withIssueDate("1995-05-23")
                                .withSubject("Computing")
                                .build();

        Item item5 = ItemBuilder.createItem(context, col2)
                                .withTitle("Zeta Reticuli")
                                .withAuthor("Universe")
                                .withIssueDate("2018-01-01")
                                .withSubject("Astronomy")
                                .build();

        Item item6 = ItemBuilder.createItem(context, col2)
                                .withTitle("Moon")
                                .withAuthor("Universe")
                                .withIssueDate("2018-01-02")
                                .withSubject("Astronomy")
                                .build();

        Item item7 = ItemBuilder.createItem(context, col2)
                                .withTitle("T-800")
                                .withAuthor("Cameron, James")
                                .withIssueDate("2029")
                                .withSubject("Science Fiction")
                                .build();
        // ---- BROWSES BY ITEM ----

        //** WHEN **
        //An anonymous user browses the items in the Browse by date issued endpoint
        //with startsWith set to 1990
        getClient().perform(get("/api/discover/browses/dateissued/items?startsWith=1990")
                                .param("size", "2")
                                .param("projection", "full"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect the totalElements to be the 7 items present in the repository
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   //We expect to jump to page 1 of the index
                   .andExpect(jsonPath("$.page.number", is(1)))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$._links.first.href", containsString("startsWith=1990")))

                   //Verify that the index jumps to the "Python" item.
                   .andExpect(jsonPath("$._embedded.items",
                                       contains(ItemMatcher.matchItemWithTitleAndDateIssued(item3,
                                                                                            "Python", "1990"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item4,
                                                                                            "Java", "1995-05-23")
                                       )));
        //** WHEN **
        //An anonymous user browses the items in the Browse by Title endpoint
        //with startsWith set to T
        getClient().perform(get("/api/discover/browses/title/items?startsWith=T")
                            .param("size", "2")
                            .param("projection", "full"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect the totalElements to be the 7 items present in the repository
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   //We expect to jump to page 2 in the index
                   .andExpect(jsonPath("$.page.number", is(2)))
                   .andExpect(jsonPath("$._links.self.href", containsString("startsWith=T")))

                   //Verify that the index jumps to the "T-800" item.
                   .andExpect(jsonPath("$._embedded.items",
                                       contains(ItemMatcher.matchItemWithTitleAndDateIssued(item7,
                                                                                            "T-800", "2029"),
                                               ItemMatcher.matchItemWithTitleAndDateIssued(item5,
                                                                                            "Zeta Reticuli",
                                                                                            "2018-01-01")
                                       )));

        //** WHEN **
        //An anonymous user browses the items in the Browse by Title endpoint
        //with startsWith set to Blade and scope set to Col 1
        getClient().perform(get("/api/discover/browses/title/items?startsWith=Blade")
                                .param("scope", col1.getID().toString())
                                .param("size", "2")
                                .param("projection", "full"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect the totalElements to be the 3 items present in the collection
                   .andExpect(jsonPath("$.page.totalElements", is(3)))
                   //As this is is a small collection, we expect to go-to page 0
                   .andExpect(jsonPath("$.page.number", is(0)))
                   .andExpect(jsonPath("$._links.self.href", containsString("startsWith=Blade")))

                   //Verify that the index jumps to the "Blade Runner" item.
                   .andExpect(jsonPath("$._embedded.items",
                           contains(ItemMatcher.matchItemWithTitleAndDateIssued(item2,
                                                                                            "Blade Runner",
                                                                                            "1982-06-25"),
                                               ItemMatcher.matchItemWithTitleAndDateIssued(item3,
                                                                                            "Python", "1990")
                                       )));
    }

    @Test
    public void testBrowseByStartsWithAndPage() throws Exception {
        context.turnOffAuthorisationSystem();

        //** GIVEN **
        //1. A community-collection structure with one parent community with sub-community and two collections.
        parentCommunity = CommunityBuilder.createCommunity(context)
                                          .withName("Parent Community")
                                          .build();
        Community child1 = CommunityBuilder.createSubCommunity(context, parentCommunity)
                                           .withName("Sub Community")
                                           .build();
        Collection col1 = CollectionBuilder.createCollection(context, child1).withName("Collection 1").build();
        Collection col2 = CollectionBuilder.createCollection(context, child1).withName("Collection 2").build();

        //2. 7 public items that are readable by Anonymous
        Item item1 = ItemBuilder.createItem(context, col1)
                                .withTitle("Alan Turing")
                                .withAuthor("Turing, Alan Mathison")
                                .withIssueDate("1912-06-23")
                                .withSubject("Computing")
                                .build();

        Item item2 = ItemBuilder.createItem(context, col1)
                                .withTitle("Blade Runner")
                                .withAuthor("Scott, Ridley")
                                .withIssueDate("1982-06-25")
                                .withSubject("Science Fiction")
                                .build();

        Item item3 = ItemBuilder.createItem(context, col2)
                                .withTitle("Java")
                                .withAuthor("Gosling, James")
                                .withIssueDate("1995-05-23")
                                .withSubject("Computing")
                                .build();

        Item item4 = ItemBuilder.createItem(context, col2)
                                .withTitle("Moon")
                                .withAuthor("Universe")
                                .withIssueDate("2018-01-02")
                                .withSubject("Astronomy")
                                .build();

        Item item5 = ItemBuilder.createItem(context, col1)
                                .withTitle("Python")
                                .withAuthor("Van Rossum, Guido")
                                .withIssueDate("1990")
                                .withSubject("Computing")
                                .build();

        Item item6 = ItemBuilder.createItem(context, col2)
                                .withTitle("T-800")
                                .withAuthor("Cameron, James")
                                .withIssueDate("2029")
                                .withSubject("Science Fiction")
                                .build();

        Item item7 = ItemBuilder.createItem(context, col2)
                                .withTitle("Zeta Reticuli")
                                .withAuthor("Universe")
                                .withIssueDate("2018-01-01")
                                .withSubject("Astronomy")
                                .build();

        // ---- BROWSES BY ITEM ----

        //** WHEN **
        //An anonymous user browses the items in the Browse by date issued endpoint
        //with startsWith set to 1990 and Page to 3
        getClient().perform(get("/api/discover/browses/dateissued/items?startsWith=1990")
                                .param("size", "2").param("page", "2"))

                   //** THEN **
                   //The status has to be 200 OK
                   .andExpect(status().isOk())
                   //We expect the content type to be "application/hal+json;charset=UTF-8"
                   .andExpect(content().contentType(contentType))

                   //We expect the totalElements to be the 7 items present in the repository
                   .andExpect(jsonPath("$.page.totalElements", is(7)))
                   //We expect to jump to page 1 of the index
                   .andExpect(jsonPath("$.page.number", is(2)))
                   .andExpect(jsonPath("$.page.size", is(2)))
                   .andExpect(jsonPath("$._links.self.href", containsString("startsWith=1990")))

                   //Verify that the index jumps to the "Zeta Reticuli" item.
                   .andExpect(jsonPath("$._embedded.items",
                                       contains(ItemMatcher.matchItemWithTitleAndDateIssued(item7,
                                                                                        "Zeta Reticuli", "2018-01-01"),
                                                ItemMatcher.matchItemWithTitleAndDateIssued(item4,
                                                                                        "Moon", "2018-01-02")
                                       )));
    }

}
