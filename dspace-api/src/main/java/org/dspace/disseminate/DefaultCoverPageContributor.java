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
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

public class DefaultCoverPageContributor implements CoverPageContributor {
    private ConfigurationService configurationService;

    public DefaultCoverPageContributor() {
        this.configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();
    }

    @Override
    public Map<String, String> processCoverPageParams(Item item, Map<String, String> parameters) {
        // Set page size parameter, from configuration
        parameters.put("page_format", configurationService.getProperty("citation-page.page_format", "LETTER"));

        // Title : Subtitle
        var dcTitle = parameters.get("dc_title");
        var dcTitleSubtitle = parameters.get("dc_title_alternative");

        if (StringUtils.isBlank(dcTitleSubtitle)) {
            parameters.put("metadata_title", dcTitle);
        } else {
            parameters.put("metadata_title", String.format("%s: %s", dcTitle, dcTitleSubtitle));
        }
        // join authors to list
        var authors = Stream.concat(
            getParams(item, "dc_contributor_author").stream(),
            getParams(item, "dc_creator").stream()
        ).collect(Collectors.toList());

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
