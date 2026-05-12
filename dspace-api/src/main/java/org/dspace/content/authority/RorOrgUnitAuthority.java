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

import org.apache.commons.lang3.StringUtils;
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

public class RorOrgUnitAuthority extends ItemAuthority {

    private final RorImportMetadataSourceService rorImportMetadataSource =
        RorServicesFactory.getInstance().getRorImportMetadataSourceService();

    private final ItemAuthorityServiceFactory itemAuthorityServiceFactory =
        dspace.getServiceManager().getServiceByName("itemAuthorityServiceFactory", ItemAuthorityServiceFactory.class);
    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    private String authorityName;

    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {

        super.setPluginInstanceName(authorityName);
        Choices solrChoices = super.getMatches(text, start, limit, locale);

        try {
            return solrChoices.values.length == 0 ? getRORApiMatches(text, locale, start, limit) : solrChoices;
        } catch (MetadataSourceException e) {
            throw new RuntimeException(e);
        }
    }

    private Choices getRORApiMatches(String text, String locale, int start, int limit) throws MetadataSourceException {
        Choice[] rorApiChoices = getChoiceFromRORQueryResults(rorImportMetadataSource.getRecords(text, 0, 0), locale)
            .toArray(new Choice[0]);

        int confidenceValue = itemAuthorityServiceFactory.getInstance(authorityName)
                                                         .getConfidenceForChoices(rorApiChoices);

        return new Choices(rorApiChoices, start, rorApiChoices.length, confidenceValue,
                           rorApiChoices.length > (start + limit), 0);
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
        return orgUnit.getValue("dc", "title", null).stream()
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

    @Override
    public String[] getLinkedEntityTypes() {
        return configurationService.getArrayProperty("cris.ItemAuthority." + authorityName + ".entityType");
    }

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

    @Override
    public void setPluginInstanceName(String name) {
        authorityName = name;
    }

    @Override
    public String getPluginInstanceName() {
        return authorityName;
    }
}
