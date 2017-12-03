/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
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
import org.junit.Test;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

/**
 * Integration test to test the /api/discover/browses endpoint
 * (Class has to start or end with IT to be picked up by the failsafe plugin)
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
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

                //Our default Discovery config has 4 browse indexes so we expect this to be reflected in the page object
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
    public void findBrowseBySubjectEntries() throws Exception{
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
                .withSubject("AnotherTest").withSubject("TestingForMore").withSubject("ExtraEntry")
                .build();

        //** WHEN **
        //An anonymous user browses this endpoint to find which subjects are currently in the repository
        getClient().perform(get("/api/discover/browses/subject/entries"))

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
                .andExpect(jsonPath("$._embedded.browseEntryResources", contains(BrowseEntryResourceMatcher.matchBrowseEntry("AnotherTest", 1),
                        BrowseEntryResourceMatcher.matchBrowseEntry( "ExtraEntry", 3),
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
                .andExpect(jsonPath("$._embedded.browseEntryResources", contains(BrowseEntryResourceMatcher.matchBrowseEntry("TestingForMore", 2),
                        BrowseEntryResourceMatcher.matchBrowseEntry( "ExtraEntry", 3),
                        BrowseEntryResourceMatcher.matchBrowseEntry("AnotherTest", 1)
                        )));
    }

    @Test
    public void findBrowseBySubjectItems() throws Exception{
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

        //2. Two public items with the same subject and another public item that contains that same subject, but also another one
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
                .andExpect(jsonPath("$._embedded.items", contains(ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1, "zPublic item more", "2017-10-17")
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
                .andExpect(jsonPath("$._embedded.items", contains(ItemMatcher.matchItemWithTitleAndDateIssued(publicItem2, "Public item 2", "2016-02-13"),
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
                .makePrivate()
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
                                "Public item 2", "2016-02-13"),
                                ItemMatcher.matchItemWithTitleAndDateIssued(publicItem1,
                                        "Public item 1", "2017-10-17"),
                                ItemMatcher.matchItemWithTitleAndDateIssued(embargoedItem,
                                        "An embargoed publication", "2017-08-10"))))

                //The private item must not be present
                .andExpect(jsonPath("$._embedded.items[*].metadata[?(@.key=='dc.title')].value",
                        not(hasItem("This is a private item"))))

                //The internal item must not be present
                .andExpect(jsonPath("$._embedded.items[*].metadata[?(@.key=='dc.title')].value",
                        not(hasItem("Internal publication"))))
        ;

        //** CLEANUP **
        GroupBuilder.cleaner().delete(internalGroup);
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
}