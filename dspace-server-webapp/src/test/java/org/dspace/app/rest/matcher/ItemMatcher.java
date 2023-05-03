/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.dspace.app.rest.matcher.HalMatcher.matchEmbeds;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadata;
import static org.dspace.app.rest.matcher.MetadataMatcher.matchMetadataDoesNotExist;
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.UUID;

import org.dspace.content.Item;
import org.hamcrest.Matcher;

/**
 * Utility class to construct a Matcher for an item
 *
 * @author Tom Desair (tom dot desair at atmire dot com)
 * @author Raf Ponsaerts (raf dot ponsaerts at atmire dot com)
 */
public class ItemMatcher {

    private ItemMatcher() { }

    public static Matcher<? super Object> matchItemWithTitleAndDateIssued(Item item, String title, String dateIssued) {
        return allOf(
            //Check item properties
            matchItemProperties(item),

            //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
            hasJsonPath("$.metadata", allOf(
                    matchMetadata("dc.title", title),
                    matchMetadata("dc.date.issued", dateIssued))),

            //Check links
            matchLinks(item.getID())
        );
    }

    public static Matcher<? super Object> matchItemWithTitleAndApproximateDateIssued(Item item, String title,
                                                                                     String approximateDateIssued) {
        return allOf(
                //Check item properties
                matchItemProperties(item),

                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.metadata", allOf(
                        matchMetadata("dc.title", title),
                        matchMetadata("local.approximateDate.issued", approximateDateIssued),
                        matchMetadata("dc.date.issued", approximateDateIssued))),

                //Check links
                matchLinks(item.getID())
        );
    }

    /**
     * The item has the metadata `local.submission.note`
     */
    public static Matcher<? super Object> matchItemWithLocalNote(Item item, String localNote) {
        return allOf(
                //Check item properties
                matchItemProperties(item),

                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.metadata", allOf(
                        matchMetadata("local.submission.note", localNote))),

                //Check links
                matchLinks(item.getID())
        );
    }

    /**
     * The item doesn't have the metadata `local.submission.note`
     */
    public static Matcher<? super Object> notMatchItemWithLocalNote(Item item) {
        return allOf(
                //Check item properties
                matchItemProperties(item),

                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.metadata", allOf(
                        matchMetadataDoesNotExist("local.submission.note"))),

                //Check links
                matchLinks(item.getID())
        );
    }

    /**
     * The item doesn't have the metadata `dc.description.provenance`
     */
    public static Matcher<? super Object> matchItemWithDescriptionProvenance(Item item, String provenance) {
        return allOf(
                //Check item properties
                matchItemProperties(item),

                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.metadata", allOf(
                        matchMetadata("dc.description.provenance", provenance))),

                //Check links
                matchLinks(item.getID())
        );
    }

    /**
     * The item doesn't have the metadata `dc.description.provenance`
     */
    public static Matcher<? super Object> notMatchItemWithDescriptionProvenance(Item item) {
        return allOf(
                //Check item properties
                matchItemProperties(item),

                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.metadata", allOf(
                        matchMetadataDoesNotExist("dc.description.provenance"))),

                //Check links
                matchLinks(item.getID())
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "bundles[]",
                "mappedCollections[]",
                "owningCollection",
                "version",
                "relationships[]",
                "templateItemOf",
                "thumbnail"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/items/" + uuid,
                "bundles",
                "mappedCollections",
                "owningCollection",
                "relationships",
                "self",
                "version",
                "templateItemOf",
                "thumbnail"
        );
    }

    public static Matcher<? super Object> matchItemProperties(Item item) {
        return allOf(
            hasJsonPath("$.uuid", is(item.getID().toString())),
            hasJsonPath("$.name", is(item.getName())),
            hasJsonPath("$.handle", is(item.getHandle())),
            hasJsonPath("$.inArchive", is(item.isArchived())),
            hasJsonPath("$.discoverable", is(item.isDiscoverable())),
            hasJsonPath("$.withdrawn", is(item.isWithdrawn())),
            hasJsonPath("$.lastModified", is(notNullValue())),
            hasJsonPath("$.type", is("item"))
        );
    }

}
