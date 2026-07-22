/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.validator.routines.ISSNValidator;
import org.dspace.app.openpolicyfinder.OpenPolicyFinderService;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderJournal;
import org.dspace.app.openpolicyfinder.v2.OpenPolicyFinderResponse;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link ChoiceAuthority} that searches for Journals
 * using the Open Policy Finder (OPF) API.
 * <p>
 * This authority combines local item lookups (when enabled) with remote
 * OPF results to provide a unified journal choice list. Pagination is
 * handled by prioritizing local items first, then filling remaining slots
 * with OPF results.
 * </p>
 * <p>
 * Configuration properties:
 * <ul>
 *   <li>{@code cris.<pluginName>.local-item-choices-enabled} - enable/disable local item lookup</li>
 *   <li>{@code opf.authority.prefix} - prefix for composed authority values (default: REFERENCE + "ISSN" + SPLIT)</li>
 * </ul>
 * </p>
 *
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 * @author Luca Giamminonni (luca.giamminonni at 4science.com)
 */
public class OpenPolicyFinderAuthority extends ItemAuthority {

    private static final String TYPE = "publication";
    private static final String ISSN_FIELD = "issn";
    private static final String TITLE_FIELD = "title";
    private static final String PREDICATE_EQUALS = "equals";
    private static final String PREDICATE_CONTAINS_WORD = "contains word";

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private DSpace dspace = new DSpace();
    private OpenPolicyFinderService opfService = dspace.getSingletonService(OpenPolicyFinderService.class);
    private List<OpenPolicyFinderExtraMetadataGenerator> generators =
        dspace.getServiceManager()
              .getServicesByType(OpenPolicyFinderExtraMetadataGenerator.class);

    /**
     * {@inheritDoc}
     * <p>
     * Retrieves the label for a given authority key by performing a lookup
     * against both local items and the OPF API.
     * </p>
     */
    @Override
    public String getLabel(String key, String locale) {
        Choices choices = getMatches(key, 0, 1, locale);
        return choices.values.length == 1 ? choices.values[0].label : EMPTY;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns combined choices from local items (if enabled) and the Open Policy
     * Finder API. Local items are returned first; OPF results fill remaining page slots.
     * </p>
     */
    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Choices itemChoices = getLocalItemChoices(text, start, limit, locale);

        int opfSearchStart = start > itemChoices.total ? start - itemChoices.total : 0;
        int opfSearchLimit = limit > itemChoices.values.length ? limit - itemChoices.values.length : 0;

        Choices choicesFromOpenPolicyFinder = getOpenPolicyFinderChoices(text, opfSearchStart, opfSearchLimit);
        int total = itemChoices.total + choicesFromOpenPolicyFinder.total;

        Choice[] choices = addAll(itemChoices.values, choicesFromOpenPolicyFinder.values);

        return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);
    }

    /**
     * Retrieve choices from local DSpace items matching the given text.
     *
     * @param text   the search text
     * @param start  the offset for pagination
     * @param limit  the maximum number of results
     * @param locale the locale for the search
     * @return the matching local item choices, or empty choices if disabled
     */
    private Choices getLocalItemChoices(String text, int start, int limit, String locale) {
        if (isLocalItemChoicesEnabled()) {
            return super.getMatches(text, start, limit, locale);
        }
        return new Choices(Choices.CF_UNSET);
    }

    /**
     * Retrieve choices from the Open Policy Finder API.
     *
     * @param text  the search text (ISSN or title fragment)
     * @param start the offset for pagination
     * @param limit the maximum number of results
     * @return the matching OPF choices
     */
    private Choices getOpenPolicyFinderChoices(String text, int start, int limit) {
        boolean isIssn = ISSNValidator.getInstance().isValid(text);
        String field = isIssn ? ISSN_FIELD : TITLE_FIELD;
        String predicate = isIssn ? PREDICATE_EQUALS : PREDICATE_CONTAINS_WORD;

        List<OpenPolicyFinderJournal> journals = getJournalsFromOpenPolicyFinder(field, predicate, text, start, limit);

        Choice[] results = journals.stream()
                                   .map(journal -> convertToChoice(journal))
                                   .toArray(Choice[]::new);

        // From OpenPolicyFinder we don't get the total number of results for a specific search,
        // so the pagination count may be incorrect
        int total = opfService.performCountRequest(TYPE, field, predicate, text);

        if (total <= 0) {
            total = results.length;
        }
        return new Choices(results, start, total, calculateConfidence(results), total > (start + limit), 0);
    }

    /**
     * Query the Open Policy Finder API for journals matching the given criteria.
     *
     * @param field     the field to search (e.g. "issn" or "title")
     * @param predicate the predicate (e.g. "equals" or "contains word")
     * @param text      the search value
     * @param start     the offset for pagination
     * @param limit     the maximum number of results
     * @return list of matching journals, or empty list if none found
     */
    private List<OpenPolicyFinderJournal> getJournalsFromOpenPolicyFinder(String field, String predicate, String text,
                                                                          int start, int limit) {
        if (limit <= 0) {
            return List.of();
        }

        OpenPolicyFinderResponse opfResponse = opfService.performRequest(TYPE, field, predicate, text, start, limit);
        if (opfResponse == null || CollectionUtils.isEmpty(opfResponse.getJournals())) {
            return List.of();
        }
        return opfResponse.getJournals();
    }

    /**
     * Convert an {@link OpenPolicyFinderJournal} to a {@link Choice} for the authority framework.
     *
     * @param journal the OPF journal to convert
     * @return the corresponding choice
     */
    private Choice convertToChoice(OpenPolicyFinderJournal journal) {
        String authority = composeAuthorityValue(journal);
        Map<String, String> extras = getOpenPolicyFinderExtra(journal);
        String title = journal.getTitles().get(0);
        return new Choice(authority, title, title, extras, getSource());
    }

    /**
     * Build extra metadata map for the given journal using registered generators.
     *
     * @param journal the OPF journal
     * @return map of extra metadata key-value pairs
     */
    private Map<String, String> getOpenPolicyFinderExtra(OpenPolicyFinderJournal journal) {
        Map<String, String> extras = new HashMap<>();
        if (CollectionUtils.isNotEmpty(generators)) {
            for (OpenPolicyFinderExtraMetadataGenerator generator : generators) {
                extras.putAll(generator.build(journal));
            }
        }
        return extras;
    }

    /**
     * Compose the authority value string for the given journal, using the
     * configured prefix and the journal's first ISSN.
     *
     * @param journal the OPF journal
     * @return the composed authority value, or empty string if no ISSN available
     */
    private String composeAuthorityValue(OpenPolicyFinderJournal journal) {
        if (CollectionUtils.isEmpty(journal.getIssns())) {
            return "";
        }

        String issn = journal.getIssns().get(0);

        String prefix = configurationService.getProperty("opf.authority.prefix", REFERENCE + "ISSN" + SPLIT);
        return prefix.endsWith(SPLIT) ? prefix + issn : prefix + SPLIT + issn;
    }

    @Override
    public String[] getLinkedEntityTypes() {
        String[] result = super.getLinkedEntityTypes();
        if (result.length == 0) {
            return new String[] {"Journal"};
        }
        return result;
    }

    @Override
    public String getPrimaryLinkedEntityType() {
        String result = super.getPrimaryLinkedEntityType();
        if (isEmpty(result)) {
            return "Journal";
        }
        return result;
    }

    /**
     * Check whether local item choices lookup is enabled for this authority instance.
     *
     * @return {@code true} if local item choices are enabled, {@code false} otherwise
     */
    private boolean isLocalItemChoicesEnabled() {
        return configurationService
            .getBooleanProperty("cris." + getPluginInstanceName() + ".local-item-choices-enabled");
    }

}