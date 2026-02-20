/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.solr.common.SolrInputDocument;
import org.dspace.content.Item;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;

/**
 * Implementation of {@link SolrServiceIndexPlugin} that creates an index for
 * the best match using some more strict configuration.
 *
 * @author Stefano Maffei at 4science.it
 *
 */
public class SolrServiceStrictBestMatchIndexingPlugin extends SolrServiceBestMatchIndexingPlugin {

    private static ConfigurationService configurationService = new DSpace().getConfigurationService();

    public static final String BEST_MATCH_INDEX = "bestmatchstrict_s";

    private static final String EXCLUDE_PUNCTUATION_CONFIG = "solr-service.strict-best-match.exclude.punctuation";

    private static final String EXCLUDE_DASH_CONFIG = "solr-service.strict-best-match.exclude.dash";

    private static final String EXCLUDE_LETTER_CASE_CONFIG = "solr-service.strict-best-match.exclude.letter-case";

    private static final String EXCLUDE_NUMBERS_CONFIG = "solr-service.strict-best-match.exclude.numbers";

    private static final String NORMALIZE_WHITESPACES = "solr-service.strict-best-match.exclude.normalize-whitespaces";

    private final static String NUMERIC_CHARS_REGEX = "[0-9]+";

    private final static String DASH_PATTERN = "‐";

    @Override
    protected void addIndexValueForPersonItem(Item item, SolrInputDocument document) {

        String firstName = getMetadataValue(item, FIRSTNAME_FIELD);
        String lastName = getMetadataValue(item, LASTNAME_FIELD);
        Collection<String> fullnames = getMetadataValues(item, FULLNAME_FIELDS);

        Set<String> bestMatchIndexValues = new HashSet<String>();

        bestMatchIndexValues.addAll(getPossibleBestMatchValues(firstName, lastName, fullnames));

        bestMatchIndexValues.forEach(variant -> addIndexValue(document, variant));
    }

    public Set<String> getPossibleBestMatchValues(String firstName, String lastName,
        Collection<String> fullnames) {
        Set<String> nameSet = generateBaseNameSet(firstName, lastName, fullnames);
        return getPossibleBestMatchValues(nameSet);
    }

    public Set<String> getPossibleBestMatchValues(Collection<String> fullnames) {
        return fullnames
                .stream()
                .filter(Objects::nonNull)
                // add all possible matches to the solr index
                .map(SolrServiceStrictBestMatchIndexingPlugin::cleanNameWithStrictPolicies)
                .collect(Collectors.toSet());
    }

    public static String cleanNameWithStrictPolicies(String name) {
        if (name == null) {
            return null;
        }

        if (configurationService.getBooleanProperty(EXCLUDE_LETTER_CASE_CONFIG, true)) {
            name = name.toLowerCase();
        }

        if (configurationService.getBooleanProperty(EXCLUDE_PUNCTUATION_CONFIG, true)) {
            name = name.replaceAll(PUNCT_CHARS_REGEX, " ");
        }

        if (configurationService.getBooleanProperty(EXCLUDE_DASH_CONFIG, true)) {
            name = name.replace(DASH_PATTERN, " ");
        }

        if (configurationService.getBooleanProperty(EXCLUDE_NUMBERS_CONFIG, true)) {
            name = name.replaceAll(NUMERIC_CHARS_REGEX, "");
        }

        if (configurationService.getBooleanProperty(NORMALIZE_WHITESPACES, true)) {
            name = StringUtils.normalizeSpace(name);
        }

        return name;
    }

    private static Set<String> generateBaseNameSet(String firstName, String lastName, Collection<String> fullnames) {
        Set<String> baseNameSet = new HashSet<String>();
        if (StringUtils.isNoneBlank(firstName, lastName)) {
            baseNameSet.add(firstName + " " + lastName);
            baseNameSet.add(lastName + " " + firstName);
        }
        baseNameSet.addAll(fullnames);
        return baseNameSet;
    }

    @Override
    protected void addIndexValue(SolrInputDocument document, String value) {
        document.addField(BEST_MATCH_INDEX, value);
    }
}
