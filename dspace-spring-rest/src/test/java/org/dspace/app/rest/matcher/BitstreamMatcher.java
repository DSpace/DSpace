/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.matcher;

import org.dspace.content.Bitstream;
import org.hamcrest.Matcher;

import java.util.UUID;

import static com.jayway.jsonpath.matchers.JsonPathMatchers.hasJsonPath;
import static org.hamcrest.Matchers.*;

public class BitstreamMatcher {

    public static Matcher<? super Object> matchBitstreamEntry(Bitstream bitstream) {
        return allOf(
                //Check core metadata (the JSON Path expression evaluates to a collection so we have to use contains)
                hasJsonPath("$.uuid", is(bitstream.getID().toString())),
                hasJsonPath("$.name", is(bitstream.getName())),
                hasJsonPath("$.bundleName", is("ORIGINAL")),
                hasJsonPath("$.metadata", containsInAnyOrder(
                        BitstreamMetadataMatcher.matchTitle(bitstream.getName()),
                        BitstreamMetadataMatcher.matchDescription(bitstream.getDescription())
                )),
                hasJsonPath("$.sizeBytes", is((int) bitstream.getSize())),
                hasJsonPath("$.checkSum", matchChecksum()),
                hasJsonPath("$._embedded.format", matchFormat()),
                //Check links
                matchBitstreamLinks(bitstream.getID())
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
                matchBitstreamLinks(uuid)
        );
    }

    private static Matcher<? super Object> matchBitstreamLinks(UUID uuid) {
        return allOf(
                hasJsonPath("$._links.format.href", containsString("/api/core/bitstreams/" + uuid + "/format")),
                hasJsonPath("$._links.self.href", containsString("/api/core/bitstreams/"+uuid)),
                hasJsonPath("$._links.content.href", containsString("/api/core/bitstreams/"+uuid+"/content"))
        );
    }

    private static Matcher<? super Object> matchChecksum(){
        return allOf(
                hasJsonPath("$.checkSumAlgorithm", not(empty())),
                hasJsonPath("$.value", not(empty()))
        );
    }

    private static Matcher<? super Object> matchFormat(){
        return allOf(
                hasJsonPath("$.mimetype", not(empty())),
                hasJsonPath("$.type", is("bitstreamformat")),
                hasJsonPath("$._links.self.href", not(empty()))
        );
    }

}
