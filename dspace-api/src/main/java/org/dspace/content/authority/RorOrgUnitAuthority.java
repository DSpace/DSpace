/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.content.authority;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.content.authority.factory.ItemAuthorityServiceFactory;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;
import org.dspace.importer.external.ror.service.RorImportMetadataSourceService;
import org.dspace.importer.external.ror.service.RorServicesFactory;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * Authority provider for organizational units that extends {@link ItemAuthority}
 * with a fallback to the ROR (Research Organization Registry) API.
 *
 * <p>When a Solr-based search returns no results, this authority queries the ROR API
 * to find matching organizational units. The results are converted to {@link Choice}
 * objects with configurable extra metadata (e.g., ROR ID, type, acronym, country)
 * that can be toggled for display and data purposes via configuration properties.</p>
 *
 * <p>Configuration properties follow the pattern:
 * {@code cris.RorOrgUnitAuthority.[pluginInstance.]<extraType>.display} and
 * {@code cris.RorOrgUnitAuthority.[pluginInstance.]<extraType>.as-data}.</p>
 *
 * @author Mykhaylo Boychuk (4science.it)
 */
public class RorOrgUnitAuthority extends ItemAuthority {

    private static final Logger log = LogManager.getLogger(RorOrgUnitAuthority.class);

    private final RorImportMetadataSourceService rorImportMetadataSource =
        RorServicesFactory.getInstance().getRorImportMetadataSourceService();

    private final ItemAuthorityServiceFactory itemAuthorityServiceFactory =
        dspace.getServiceManager().getServiceByName("itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);
    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private String authorityName;

    /**
     * {@inheritDoc}
     * <p>Falls back to the ROR API when the Solr-based search returns no results.</p>
     */
    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {

        super.setPluginInstanceName(authorityName);
        Choices solrChoices = super.getMatches(text, start, limit, locale);

        int rorSearchStart = start > solrChoices.total ? start - solrChoices.total : 0;
        int rorSearchLimit = limit > solrChoices.values.length ? limit - solrChoices.values.length : 0;

        try {

            Choices rorChoices = getRORApiMatches(text, locale, rorSearchStart, rorSearchLimit);
            int total = solrChoices.total + rorChoices.total;

            Choice[] choices = ArrayUtils.addAll(solrChoices.values, rorChoices.values);
            return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);

        } catch (MetadataSourceException e) {
            log.error("An error occurred while querying the ROR API for text '{}'; "
                + "falling back to local results", text, e);
            return solrChoices;
        }
    }

    private Choices getRORApiMatches(String text, String locale, int start, int limit) throws MetadataSourceException {
        if (limit <= 0) {
            return new Choices(Choices.CF_UNSET);
        }

        Choice[] rorApiChoices = getChoiceFromRORQueryResults(
            rorImportMetadataSource.getRecords(text, start, limit), locale)
            .toArray(new Choice[0]);

        int total = rorImportMetadataSource.getRecordsCount(text);
        if (total <= 0) {
            total = rorApiChoices.length;
        }

        int confidenceValue = itemAuthorityServiceFactory.getInstance(authorityName)
                                                         .getConfidenceForChoices(rorApiChoices);

        return new Choices(rorApiChoices, start, total, confidenceValue,
                           total > (start + limit), 0);
    }

    private List<Choice> getChoiceFromRORQueryResults(Collection<ImportRecord> orgUnits, String locale) {
        return orgUnits
            .stream()
            .map(orgUnit ->
                new Choice(
                    composeAuthorityValue(getIdentifier(orgUnit)),
                    getName(orgUnit),
                    getName(orgUnit),
                    buildExtras(orgUnit, locale),
                    getSource()
                )
            )
            .collect(Collectors.toList());
    }

    private String getIdentifier(ImportRecord orgUnit) {
        return orgUnit.getValue("organization", "identifier", "ror").stream()
            .findFirst()
            .map(metadata -> metadata.getValue())
            .orElse(null);
    }

    private String getName(ImportRecord orgUnit) {
        return orgUnit.getValue("organization", "legalName", null).stream()
            .findFirst()
            .map(metadata -> metadata.getValue())
            .orElse(null);
    }

    private Map<String, String> buildExtras(ImportRecord orgUnit, String locale) {

        Map<String, String> extras = new LinkedHashMap<String, String>();

        addExtra(extras, getIdentifier(orgUnit), "id");

        orgUnit.getSingleValue("dc", "type", null)
            .ifPresent(type -> addExtra(extras, type, "type"));

        String acronym = orgUnit.getValue("oairecerif", "acronym", null).stream()
            .map(MetadatumDTO::getValue)
            .collect(Collectors.joining(", "));

        if (StringUtils.isNotBlank(acronym)) {
            addExtra(extras, acronym, "acronym");
        }

        orgUnit.getSingleValue("organization", "address", "addressCountry").ifPresent(country -> {
            String countryName = country;
            ChoiceAuthority countryAuthority = (ChoiceAuthority) pluginService.getNamedPlugin(
                    ChoiceAuthority.class, "common_iso_countries");
            if (countryAuthority != null) {
                String label = countryAuthority.getLabel(country, locale);
                if (StringUtils.isNotBlank(label) && !StringUtils.startsWith(label, DCInputAuthority.UNKNOWN_KEY)) {
                    countryName = label;
                }
            }

            addExtra(extras, countryName, "countryName");
            addExtra(extras, country, "country");
        });

        return extras;
    }

    private void addExtra(Map<String, String> extras, String value, String extraType) {

        String key = getKey(extraType);

        if (useAsData(extraType)) {
            extras.put("data-" + key, value);
        }
        if (useForDisplaying(extraType)) {
            extras.put(key, value);
        }

    }

    private boolean useForDisplaying(String extraType) {
        return configurationService.getBooleanProperty(
                "cris.RorOrgUnitAuthority." + getPluginInstanceName() + "." + extraType + ".display",
                configurationService.getBooleanProperty(
                        "cris.RorOrgUnitAuthority." + extraType + ".display", true));
    }

    private boolean useAsData(String extraType) {
        return configurationService.getBooleanProperty(
                "cris.RorOrgUnitAuthority." + getPluginInstanceName() + "." + extraType + ".as-data",
                configurationService.getBooleanProperty(
                        "cris.RorOrgUnitAuthority." + extraType + ".as-data", true));
    }

    private String getKey(String extraType) {
        return configurationService.getProperty(
                "cris.RorOrgUnitAuthority." + getPluginInstanceName() + "." + extraType + ".key", configurationService
                        .getProperty("cris.RorOrgUnitAuthority." + extraType + ".key", "ror_orgunit_" + extraType));
    }

    private String composeAuthorityValue(String rorId) {
        String prefix = configurationService.getProperty("ror.authority." + getPluginInstanceName() + "prefix",
                configurationService.getProperty("ror.authority.prefix", "will be referenced::ROR-ID::"));
        return prefix + rorId;
    }

    /** {@inheritDoc} */
    @Override
    public String[] getLinkedEntityTypes() {
        return configurationService.getArrayProperty("cris.ItemAuthority." + authorityName + ".entityType");
    }

    /** {@inheritDoc} */
    @Override
    public String getPrimaryLinkedEntityType() {
        String entityType = configurationService.getProperty(
            "cris.ItemAuthority." + authorityName + ".primaryEntityType");
        if (StringUtils.isNotBlank(entityType)) {
            return entityType;
        }

        // fallback strategy
        String[] entityTypes = getLinkedEntityTypes();
        if (entityTypes != null && entityTypes.length == 1) {
            return entityTypes[0];
        }

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    /** {@inheritDoc} */
    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
