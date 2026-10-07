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
import org.dspace.app.openpolicyfinder.OpenPolicyFinderService;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderPublisherResponse;
import org.dspace.utils.DSpace;

/**
 * Publisher name authority based on the Jisc Open Policy Finder API (formerly SHERPA/RoMEO v2)
 *
 * @author Larry Stone
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @version $Revision $
 * @see OpenPolicyFinderJournalTitle
 */
public class OpenPolicyFinderPublisherAuthority implements ChoiceAuthority {
    private String pluginInstanceName;

    public OpenPolicyFinderPublisherAuthority() {
        super();
    }

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0) {
            return new Choices(true);
        }
        OpenPolicyFinderService openPolicyFinderService =
            new DSpace().getSingletonService(OpenPolicyFinderService.class);
        OpenPolicyFinderPublisherResponse opfResponse =
            openPolicyFinderService.performPublisherRequest("publisher", "name",
                "contains word", text, 0, 0);
        Choices result;
        if (CollectionUtils.isNotEmpty(opfResponse.getPublishers())) {
            List<Choice> list = opfResponse
                    .getPublishers().stream()
                        .skip(start)
                        .limit(limit)
                        .map(publisher ->
                            new Choice(publisher.getIdentifier(),
                                    publisher.getName(), publisher.getName()))
                    .collect(Collectors.toList());
            int total = opfResponse.getPublishers().size();
            result = new Choices(list.toArray(new Choice[list.size()]), start, total, Choices.CF_ACCEPTED,
                    total > (start + limit));
        } else {
            result = new Choices(false);
        }
        return result;
    }

    @Override
    public Choices getBestMatch(String text, String locale) {
        // punt if there is no query text
        if (text == null || text.trim().length() == 0) {
            return new Choices(true);
        }
        int limit = 10;
        OpenPolicyFinderService openPolicyFinderService =
            new DSpace().getSingletonService(OpenPolicyFinderService.class);
        OpenPolicyFinderPublisherResponse opfResponse =
            openPolicyFinderService.performPublisherRequest("publisher", "name",
                "equals", text, 0, limit);
        Choices result;
        if (CollectionUtils.isNotEmpty(opfResponse.getPublishers())) {
            List<Choice> list = opfResponse
                    .getPublishers().stream()
                    .map(publisher ->
                            new Choice(publisher.getIdentifier(),
                                    publisher.getName(), publisher.getName()))
                    .collect(Collectors.toList());
            int total = opfResponse.getPublishers().size();

            int confidence;
            if (list.isEmpty()) {
                confidence = Choices.CF_NOTFOUND;
            } else if (list.size() == 1) {
                confidence = Choices.CF_UNCERTAIN;
            } else {
                confidence = Choices.CF_AMBIGUOUS;
            }
            result = new Choices(list.toArray(new Choice[list.size()]), 0, total, confidence,
                    total > limit);
        } else {
            result = new Choices(false);
        }
        return result;
    }

    @Override
    public String getLabel(String key, String locale) {
        OpenPolicyFinderService openPolicyFinderService =
            new DSpace().getSingletonService(OpenPolicyFinderService.class);
        OpenPolicyFinderPublisherResponse opfResponse =
            openPolicyFinderService.performPublisherRequest("publisher", "id",
                "equals", key, 0, 1);
        if (CollectionUtils.isNotEmpty(opfResponse.getPublishers())) {
            return opfResponse.getPublishers().get(0).getName();
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