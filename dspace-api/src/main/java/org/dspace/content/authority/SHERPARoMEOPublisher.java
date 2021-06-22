/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.dspace.app.sherpa.SHERPAService;
import org.dspace.app.sherpa.v2.SHERPAPublisherResponse;
import org.dspace.utils.DSpace;

/**
 * Publisher name authority based on SHERPA/RoMEO v2
 *
 * @author Larry Stone
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @version $Revision $
 * @see SHERPARoMEOProtocol
 */
public class SHERPARoMEOPublisher implements ChoiceAuthority {
    private String pluginInstanceName;

    public SHERPARoMEOPublisher() {
        super();
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0) {
            return new Choices(true);
        }
        SHERPAService sherpaService = new DSpace().getSingletonService(SHERPAService.class);
        SHERPAPublisherResponse sherpaResponse = sherpaService.performPublisherRequest("publisher", "name",
                "contains word", text, 0, 0);
        Choices result;
        if (CollectionUtils.isNotEmpty(sherpaResponse.getPublishers())) {
            List<Choice> list = sherpaResponse
                    .getPublishers().stream()
                        .skip(start)
                        .limit(limit)
                        .map(sherpaPublisher ->
                            new Choice(sherpaPublisher.getIdentifier(),
                                    sherpaPublisher.getName(), sherpaPublisher.getName()))
                    .collect(Collectors.toList());
            int total = sherpaResponse.getPublishers().size();
            result = new Choices(list.toArray(new Choice[list.size()]), start, total, Choices.CF_ACCEPTED,
                    total > (start + limit));
        } else {
            result = new Choices(false);
        }
        return result;
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        return getMatches(text, 0, 1, locale);
    }

    @Override
    public String getLabel(String key, String locale) {
        SHERPAService sherpaService = new DSpace().getSingletonService(SHERPAService.class);
        SHERPAPublisherResponse sherpaResponse = sherpaService.performPublisherRequest("publisher", "id",
                "equals", key, 0, 1);
        if (CollectionUtils.isNotEmpty(sherpaResponse.getPublishers())) {
            return sherpaResponse.getPublishers().get(0).getName();
        } else {
            return null;
        }
    }

    @Override
    public void setPluginInstanceName(String name) {
        this.pluginInstanceName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return pluginInstanceName;
    }
}