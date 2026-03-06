/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.text.WordUtils.capitalizeFully;
import static org.dspace.authority.service.AuthorityValueService.REFERENCE;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.orcid.client.OrcidClient;
import org.dspace.orcid.client.OrcidConfiguration;
import org.dspace.orcid.factory.OrcidServiceFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedResult;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extension of {@link ItemAuthority} that also includes profiles from the orcid
 * register in the choices.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidAuthority extends ItemAuthority {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidAuthority.class);

    private static final String IS_LATIN_REGEX = "\\p{IsLatin}+";

    public static final String DEFAULT_ORCID_KEY = "person_identifier_orcid";

    public static final String DEFAULT_INSTITUTION_KEY = "oairecerif_author_affiliation";

    public static final String ORCID_REGEX = "\\d{4}-\\d{4}-\\d{4}-\\d{4}";

    public static final Pattern ORCID_PATTERN = Pattern.compile(ORCID_REGEX);

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private static String accessToken;

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {
        Choices itemChoices = super.getMatches(text, start, limit, locale);

        int orcidSearchStart = start > itemChoices.total ? start - itemChoices.total : 0;
        int orcidSearchLimit = limit > itemChoices.values.length ? limit - itemChoices.values.length : 0;

        try {

            Choices orcidChoices = performOrcidSearch(text, orcidSearchStart, orcidSearchLimit);
            int total = itemChoices.total + orcidChoices.total;

            Choice[] choices = addAll(itemChoices.values, orcidChoices.values);
            return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);

        } catch (Exception ex) {
            LOGGER.error("An error occurs performing profiles search on ORCID registry", ex);
            return itemChoices;
        }

    }

    private Choices performOrcidSearch(String text, int start, int rows) {

        String query = formatQuery(text);
        ExpandedSearch searchResult = expandedSearch(query, start, rows);

        List<ExpandedResult> searchResults = searchResult.getResults();
        int total = searchResult.getNumFound() != null ? searchResult.getNumFound().intValue() : searchResults.size();

        Choice[] choices = searchResults.stream()
            .map(this::convertToChoice)
            .toArray(Choice[]::new);

        return new Choices(choices, start, total, calculateConfidence(choices), total > (start + rows), 0);

    }

    private String formatQuery(String text) {
        text = text.trim();
        Matcher matcher = ORCID_PATTERN.matcher(text);
        if (matcher.matches()) {
            return format("(orcid:%s)", text);
        }
        return Arrays.stream(replaceCommaWithSpace(text).split(" "))
            .map(name -> format("(given-names:%s+OR+family-name:%s+OR+other-names:%s)", name, name, name))
            .collect(Collectors.joining("+AND+"));
    }

    private String replaceCommaWithSpace(String text) {
        return StringUtils.normalizeSpace(text.replaceAll(",", " "));
    }

    private ExpandedSearch expandedSearch(String query, int start, int rows) {
        if (getOrcidConfiguration().isApiConfigured()) {
            return getOrcidClient().expandedSearch(getAccessToken(), query, start, rows);
        } else {
            return getOrcidClient().expandedSearch(query, start, rows);
        }
    }

    private Choice convertToChoice(ExpandedResult result) {
        String title = getTitle(result);
        String authority = composeAuthorityValue(result.getOrcidId());
        Map<String, String> extras = composeExtras(result);
        return new Choice(authority, title, title, extras, getSource());
    }

    private String getTitle(ExpandedResult result) {
        String givenName = result.getGivenNames();
        String familyName = result.getFamilyNames();

        String capitalizedFamilyName = capitalizeFully(familyName);
        String capitalizedGivenName = capitalizeFully(givenName);
        String title = capitalizedFamilyName + ", " + capitalizedGivenName;
        if (!givenName.matches(IS_LATIN_REGEX) || !familyName.matches(IS_LATIN_REGEX)) {
            title = isNotBlank(familyName) ? capitalizeFully(familyName) : "";
            title += isNotBlank(givenName) ? " " + capitalizeFully(givenName) : "";
        }
        return title.trim();
    }

    private String composeAuthorityValue(String orcid) {
        String prefix = configurationService.getProperty("orcid.authority.prefix", REFERENCE + "ORCID" + SPLIT);
        return prefix.endsWith(SPLIT) ? prefix + orcid : prefix + SPLIT + orcid;
    }

    private Map<String, String> composeExtras(ExpandedResult result) {
        Map<String, String> extras = new HashMap<>();
        String orcidIdKey = getOrcidIdKey();
        String orcidId = result.getOrcidId();
        if (StringUtils.isNotBlank(orcidId)) {
            if (useOrcidIDAsData()) {
                extras.put("data-" + orcidIdKey, orcidId);
            }
            if (useOrcidIDForDisplaying()) {
                extras.put(orcidIdKey, orcidId);
            }
        }
        String institutionKey = getInstitutionKey();
        String[] institutionNames = result.getInstitutionNames();

        if (ArrayUtils.isNotEmpty(institutionNames) && useInstitutionAsData()) {
            extras.put("data-" + institutionKey, String.join(", ", institutionNames));
        }
        if (ArrayUtils.isNotEmpty(institutionNames) && useInstitutionForDisplaying()) {
            extras.put(institutionKey, String.join(", ", institutionNames));
        }

        return extras;
    }

    private String getAccessToken() {
        if (accessToken == null) {
            accessToken = getOrcidClient().getReadPublicAccessToken().getAccessToken();
        }
        return accessToken;
    }

    @Override
    public String getLinkedEntityType() {
        return configurationService.getProperty("researcher-profile.type", "Person");
    }

    private OrcidClient getOrcidClient() {
        return OrcidServiceFactory.getInstance().getOrcidClient();
    }

    private OrcidConfiguration getOrcidConfiguration() {
        return OrcidServiceFactory.getInstance().getOrcidConfiguration();
    }

    public static void setAccessToken(String accessToken) {
        OrcidAuthority.accessToken = accessToken;
    }

    public String getOrcidIdKey() {
        return configurationService.getProperty("cris.OrcidAuthority." + getPluginInstanceName() + ".orcid-id.key",
                configurationService.getProperty("cris.OrcidAuthority.orcid-id.key", DEFAULT_ORCID_KEY));
    }

    public String getInstitutionKey() {
        return configurationService.getProperty("cris.OrcidAuthority." + getPluginInstanceName() + ".institution.key",
                configurationService.getProperty("cris.OrcidAuthority.institution.key", DEFAULT_INSTITUTION_KEY));
    }

    public boolean useInstitutionAsData() {
        return configurationService.getBooleanProperty(
                "cris.OrcidAuthority." + getPluginInstanceName() + ".institution.as-data",
                configurationService.getBooleanProperty("cris.OrcidAuthority.institution.as-data", true));
    }

    public boolean useInstitutionForDisplaying() {
        return configurationService.getBooleanProperty(
                "cris.OrcidAuthority." + getPluginInstanceName() + ".institution.display",
                configurationService.getBooleanProperty("cris.OrcidAuthority.institution.display", true));
    }

    public boolean useOrcidIDAsData() {
        return configurationService.getBooleanProperty(
                "cris.OrcidAuthority." + getPluginInstanceName() + ".orcid-id.as-data",
                configurationService.getBooleanProperty("cris.OrcidAuthority.orcid-id.as-data", true));
    }

    public boolean useOrcidIDForDisplaying() {
        return configurationService.getBooleanProperty(
                "cris.OrcidAuthority." + getPluginInstanceName() + ".orcid-id.display",
                configurationService.getBooleanProperty(
                        "cris.OrcidAuthority." + getPluginInstanceName() + ".orcid-id.display", true));

    }

}
