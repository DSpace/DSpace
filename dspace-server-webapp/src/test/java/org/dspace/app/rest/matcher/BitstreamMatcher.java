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
import static org.dspace.app.rest.test.AbstractControllerIntegrationTest.REST_SERVER_URL;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.sql.SQLException;
import java.util.UUID;

import org.dspace.content.Bitstream;
import org.hamcrest.Matcher;

public class BitstreamMatcher {

    private BitstreamMatcher() { }

    public static Matcher<? super Object> matchBitstreamEntry(Bitstream bitstream) {
        return allOf(
            //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
            matchProperties(bitstream),
            //Make sure we have a checksum
            hasJsonPath("$.checkSum", matchChecksum()),
            //Make sure we have a valid format
            hasJsonPath("$._embedded.format", matchFormat()),
            //Check links
            matchLinks(bitstream.getID())
        );
    }

    public static Matcher<? super Object> matchBitstreamEntry(UUID uuid, long size, String name, String description) {
        return allOf(
                //Check ID and size
                matchProperties(uuid, size, name, description),
                //Make sure we have a checksum
                hasJsonPath("$.checkSum", matchChecksum()),
                //Make sure we have a valid format
                hasJsonPath("$._embedded.format", matchFormat()),
                //Check links
                matchLinks(uuid)
        );
    }

    public static Matcher<? super Object> matchBitstreamEntry(UUID uuid, long size) {
        return allOf(
            //Check ID and size
            hasJsonPath("$.uuid", is(uuid.toString())),
            hasJsonPath("$.sizeBytes", is((int) size)),
            //Make sure we have a checksum
            hasJsonPath("$.checkSum", matchChecksum()),
            //Make sure we have a valid format
            hasJsonPath("$._embedded.format", matchFormat()),
            //Check links
            matchLinks(uuid)
        );
    }

    private static Matcher<? super Object> matchChecksum() {
        return allOf(
            hasJsonPath("$.checkSumAlgorithm", not(empty())),
            hasJsonPath("$.value", not(empty()))
        );
    }

    private static Matcher<? super Object> matchFormat() {
        return allOf(
            hasJsonPath("$.mimetype", not(empty())),
            hasJsonPath("$.type", is("bitstreamformat")),
            hasJsonPath("$._links.self.href", not(empty()))
        );
    }

    /**
     * Gets a matcher for all expected embeds when the full projection is requested.
     */
    public static Matcher<? super Object> matchFullEmbeds() {
        return matchEmbeds(
                "bundle",
                "format"
        );
    }

    /**
     * Gets a matcher for all expected links.
     */
    public static Matcher<? super Object> matchLinks(UUID uuid) {
        return HalMatcher.matchLinks(REST_SERVER_URL + "core/bitstreams/" + uuid,
                "bundle",
                "content",
                "format",
                "self"
        );
    }

    private static Matcher<? super Object> matchProperties(Bitstream bitstream) {
        try {
            return allOf(
                    hasJsonPath("$.uuid", is(bitstream.getID().toString())),
                    hasJsonPath("$.name", is(bitstream.getName())),
                    hasJsonPath("$.bundleName", is(bitstream.getBundles().get(0).getName())),
                    hasJsonPath("$.metadata", allOf(
                            matchMetadata("dc.title", bitstream.getName()),
                            matchMetadata("dc.description", bitstream.getDescription())
                    )),
                    hasJsonPath("$.sizeBytes", is((int) bitstream.getSizeBytes())),
                    hasJsonPath("$.checkSum", matchChecksum())
            );
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static Matcher<? super Object> matchProperties(UUID uuid, long size, String name, String description) {
        return allOf(
                hasJsonPath("$.uuid", is(uuid.toString())),
                hasJsonPath("$.name", is(name)),
                hasJsonPath("$.metadata", allOf(
                        matchMetadata("dc.title", name),
                        matchMetadata("dc.description", description)
                )),
                hasJsonPath("$.sizeBytes", is((int) size)),
                hasJsonPath("$.checkSum", matchChecksum())
        );
    }
}
