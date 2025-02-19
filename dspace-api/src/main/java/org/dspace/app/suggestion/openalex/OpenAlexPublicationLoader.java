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
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 **/
public class OpenAlexPublicationLoader extends PublicationLoader {

    @Override
    public List<String> searchMetadataValues(Item researcher) {
        List<String> names = getNames();

        // First, check for "dc.identifier.other" and build the filter if present
        List<String> authorIds = names.stream()
                                      .filter("dc.identifier.other"::equals)
                                      .map(name -> itemService.getMetadata(researcher, name))
                                      .filter(Objects::nonNull)
                                      .collect(Collectors.toList());

        if (!authorIds.isEmpty()) {
            return Collections.singletonList("filter=authorships.author.id:" + StringUtils.join(authorIds, "|"));
        }

        // Otherwise, collect all available metadata values
        return names.stream()
                    .map(name -> itemService.getMetadata(researcher, name))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
    }

}
