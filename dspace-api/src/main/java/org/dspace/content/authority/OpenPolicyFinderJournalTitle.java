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
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.dspace.utils.DSpace;

/**
 * Journal-name authority based on the Jisc Open Policy Finder API (formerly SHERPA/RoMEO v2)
 *
 * @author Larry Stone
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 * @version $Revision $
 * @see OpenPolicyFinderJournalTitle
 */
public class OpenPolicyFinderJournalTitle implements ChoiceAuthority {
    private String pluginInstanceName;

    public OpenPolicyFinderJournalTitle() {
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
        OpenPolicyFinderResponse opfResponse = openPolicyFinderService.performRequest("publication", "title",
                "contains word", text, 0, 0);
        Choices result;
        if (CollectionUtils.isNotEmpty(opfResponse.getJournals())) {
            List<Choice> list = opfResponse
                    .getJournals().stream()
                        .skip(start)
                        .limit(limit)
                        .map(journal -> new Choice(journal.getIssns().get(0),
                            journal.getTitles().get(0), journal.getTitles().get(0)))
                    .collect(Collectors.toList());
            int total = opfResponse.getJournals().size();
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
        OpenPolicyFinderResponse opfResponse = openPolicyFinderService.performRequest("publication", "title",
            "equals", text, 0, limit);
        Choices result;
        if (CollectionUtils.isNotEmpty(opfResponse.getJournals())) {
            List<Choice> list = opfResponse
                .getJournals().stream()
                .map(journal -> new Choice(journal.getIssns().get(0),
                    journal.getTitles().get(0), journal.getTitles().get(0)))
                .collect(Collectors.toList());
            int total = opfResponse.getJournals().size();

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
        OpenPolicyFinderResponse opfResponse = openPolicyFinderService.performRequest("publication", "issn",
                "equals", key, 0, 1);
        if (CollectionUtils.isNotEmpty(opfResponse.getJournals())) {
            return opfResponse.getJournals().get(0).getTitles().get(0);
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