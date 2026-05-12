/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.dspace.authority.service.AuthorityValueService.GENERATE;
import static org.dspace.authority.service.AuthorityValueService.SPLIT;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.exception.MetadataSourceException;
import org.dspace.importer.external.openaire.service.OpenAireProjectImportMetadataSourceServiceImpl;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Mykhaylo Boychuk (4science.it)
 */
public class OpenAIREProjectAuthority extends ItemAuthority {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrcidAuthority.class);

    private ServiceManager serviceManager = new DSpace().getServiceManager();

    private ConfigurationService configurationService = DSpaceServicesFactory.getInstance().getConfigurationService();

    private List<OpenAIREExtraMetadataGenerator> extraMetadataGenerators = serviceManager
        .getServicesByType(OpenAIREExtraMetadataGenerator.class);

    private OpenAireProjectImportMetadataSourceServiceImpl openAIREProjectService = serviceManager
        .getServiceByName("OpenAIREService", OpenAireProjectImportMetadataSourceServiceImpl.class);


    @Override
    public Choices getMatches(String text, int start, int limit, String locale) {

        Choices itemChoices = super.getMatches(text, start, limit, locale);

        int openAIRESearchStart = start > itemChoices.total ? start - itemChoices.total : 0;
        int openAIRESearchLimit = limit > itemChoices.values.length ? limit - itemChoices.values.length : 0;

        try {

            Choices openAIREChoices = openAIREProjectSearch(text, openAIRESearchStart, openAIRESearchLimit);
            int total = itemChoices.total + openAIREChoices.total;

            Choice[] choices = addAll(itemChoices.values, openAIREChoices.values);
            return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);

        } catch (Exception ex) {
            LOGGER.error("An error occurs performing projects search on OpenAIRE", ex);
            return itemChoices;
        }
    }

    private Choices openAIREProjectSearch(String text, int start, int limit) {

        List<ImportRecord> records = importOpenAIREProjects(text, start, limit);

        if (CollectionUtils.isEmpty(records)) {
            return new Choices(Choices.CF_UNSET);
        }

        int total = records.size();

        Choice[] choices = records.stream()
            .map(this::convertToChoice)
            .toArray(Choice[]::new);

        return new Choices(choices, start, total, calculateConfidence(choices), total > (start + limit), 0);
    }

    private List<ImportRecord> importOpenAIREProjects(String text, int start, int limit) {
        try {
            return (List<ImportRecord>) openAIREProjectService.getRecords(text, start, limit);
        } catch (MetadataSourceException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Choice convertToChoice(ImportRecord record) {
        String value = getMetadataValue(record, "dc", "title", null);
        String code = getMetadataValue(record, "oairecerif", "funding", "identifier");
        String authority = getAuthorityPrefix() + code;
        String label = StringUtils.isNotBlank(code) ? value + "(" + code + ")" : value;
        return new Choice(authority, value, label, getOpenAireExtra(code), getSource());
    }

    private String getMetadataValue(ImportRecord record, String schema, String element, String qualifier) {
        return record.getValueList().stream()
            .filter(metadatum -> StringUtils.equals(metadatum.getSchema(), schema))
            .filter(metadatum -> StringUtils.equals(metadatum.getElement(), element))
            .filter(metadatum -> StringUtils.equals(metadatum.getQualifier(), qualifier))
            .map(metadatum -> metadatum.getValue())
            .findFirst()
            .orElse("");
    }

    private String getAuthorityPrefix() {
        return configurationService.getProperty("openaire-project.authority.prefix",
            GENERATE + "OPENAIRE-PROJECT-ID" + SPLIT);
    }

    private Map<String, String> getOpenAireExtra(String value) {
        Map<String, String> extras = new HashMap<String, String>();

        for (OpenAIREExtraMetadataGenerator extraMetadataGenerator : extraMetadataGenerators) {
            extras.putAll(extraMetadataGenerator.build(value));
        }

        return extras;
    }
}