/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.suggestion.openalex;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.dspace.app.suggestion.loader.PublicationLoader;
import org.dspace.content.Item;


/**
 * Implementation of {@link PublicationLoader} that retrieves metadata values
 * from an OpenAlex external source.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexPublicationLoader extends PublicationLoader {

    /**
     * Searches for metadata values related to a given researcher item.
     * It first checks for "dc.identifier" metadata and builds the filter accordingly.
     * If not found, it collects available metadata values to be used in the search query.
     *
     * @param researcher The researcher item from which metadata values are extracted.
     * @return A list of search query parameters for OpenAlex.
     */
    @Override
    public List<String> searchMetadataValues(Item researcher) {
        List<String> names = getNames();

        // First, check for "dc.identifier.openalex" and build the filter if present
        List<String> authorIds = names.stream()
                                      .filter("dc.identifier.openalex"::equals)
                                      .map(name -> itemService.getMetadata(researcher, name))
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.toList());

        if (!authorIds.isEmpty()) {
            return Collections.singletonList(StringUtils.join(authorIds, "|"));
        }

        // Otherwise, collect all available metadata values
        return names.stream()
                    .map(name -> itemService.getMetadata(researcher, name))
                    .filter(Objects::nonNull)
                    .map(i -> "search_by_author=" + i)
                    .collect(Collectors.toList());
    }

}
