/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.disseminate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.tika.utils.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;

public class DefaultCoverPageContributor implements CoverPageContributor {
    @Override
    public Map<String, String> processCoverPageParams(Item item, Map<String, String> parameters) {
        // Title : Subtitle
        var dcTitle = parameters.get("dc_title");
        var dcTitleSubtitle = parameters.get("dc_title_subtitle");

        if (StringUtils.isBlank(dcTitleSubtitle)) {
            parameters.put("metadata_title", dcTitle);
        } else {
            parameters.put("metadata_title", "%s: %s".formatted(dcTitle, dcTitleSubtitle));
        }

        // join authors to list
        var authors = getParams(item, "dc_contributor_author");
        parameters.put("metadata_author", join(authors, "; ", 5, " ..."));

        // join editors to list
        var editors = getParams(item, "dc_contributor_editor");
        parameters.put("metadata_editor", join(editors, "; ", 5, " ..."));

        return parameters;
    }

    protected List<String> getParams(Item item, String metaDataField) {
        return item.getMetadata().stream()
                .filter(meta -> meta.getMetadataField().toString().equals(metaDataField))
                .map(MetadataValue::getValue).collect(Collectors.toList());
    }

    protected static String join(List<String> items, String separator, int limit, String limitMessage) {
        var sb = new StringBuilder();

        if (items.size() > limit) {
            sb.append(String.join(separator, items.subList(0, limit)));
            sb.append(limitMessage);
        } else {
            sb.append(String.join(separator, items));
        }

        return sb.toString();
    }

}
