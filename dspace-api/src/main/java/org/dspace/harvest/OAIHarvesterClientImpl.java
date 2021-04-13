/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import static org.dspace.util.ExceptionMessageUtils.getRootMessage;

import java.util.List;
import java.util.Map;

import ORG.oclc.oai.harvester2.verb.GetRecord;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.apache.commons.lang.StringUtils;
import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.dspace.util.ThrowingSupplier;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link OAIHarvesterClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OAIHarvesterClientImpl implements OAIHarvesterClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(OAIHarvesterClientImpl.class);

    @Override
    public OAIHarvesterResponseDTO listRecords(String baseURL, String from, String until, String set,
        String metadataPrefix) {
        try {
            return harvest(() -> new ListRecords(baseURL, from, until, set, metadataPrefix));
        } catch (Exception ex) {
            Map<String, String> parameters = Map.of("verb", "ListRecords", "from", from,
                "until", until, "set", set, "metadataPrefix", metadataPrefix);
            throw new HarvestingException(formatMessage(baseURL, parameters, ex), ex);
        }
    }

    @Override
    public OAIHarvesterResponseDTO listRecords(String baseURL, String resumptionToken) {
        try {
            return harvest(() -> new ListRecords(baseURL, resumptionToken));
        } catch (Exception ex) {
            Map<String, String> parameters = Map.of("verb", "ListRecords", "resumptionToken", resumptionToken);
            throw new HarvestingException(formatMessage(baseURL, parameters, ex), ex);
        }
    }

    @Override
    public OAIHarvesterResponseDTO listMetadataFormats(String baseURL) {
        try {
            return harvest(() -> new ListMetadataFormats(baseURL));
        } catch (Exception ex) {
            Map<String, String> parameters = Map.of("verb", "ListMetadataFormats");
            throw new HarvestingException(formatMessage(baseURL, parameters, ex), ex);
        }
    }

    @Override
    public OAIHarvesterResponseDTO identify(String baseURL) {
        try {
            return harvest(() -> new Identify(baseURL));
        } catch (Exception ex) {
            Map<String, String> parameters = Map.of("verb", "Identify");
            throw new HarvestingException(formatMessage(baseURL, parameters, ex), ex);
        }
    }

    @Override
    public OAIHarvesterResponseDTO getRecord(String baseURL, String identifier, String metadataPrefix) {
        try {
            return harvest(() -> new GetRecord(baseURL, identifier, metadataPrefix));
        } catch (Exception ex) {
            Map<String, String> parameters = Map.of("verb", "GetRecord", "identifier", identifier,
                "metadataPrefix", metadataPrefix);
            throw new HarvestingException(formatMessage(baseURL, parameters, ex), ex);
        }
    }

    @SuppressWarnings("unchecked")
    public String resolveNamespaceToPrefix(String baseUrl, String namespace) {

        // Query the OAI server for the metadata
        OAIHarvesterResponseDTO responseDTO = listMetadataFormats(baseUrl);
        Document response = responseDTO.getDocument();
        List<Element> mdFormats = response.getRootElement().getChild("ListMetadataFormats", OAI_NS)
            .getChildren("metadataFormat", OAI_NS);

        return mdFormats.stream()
            .filter(mdFormat -> namespace.equals(mdFormat.getChildText("metadataNamespace", OAI_NS)))
            .map(mdFormat -> mdFormat.getChildText("metadataPrefix", OAI_NS))
            .findFirst()
            .orElse(null);
    }

    private OAIHarvesterResponseDTO harvest(ThrowingSupplier<? extends HarvesterVerb, Exception> supplier)
        throws Exception {
        HarvesterVerb harvesterVerb = supplier.get();
        LOGGER.info("HTTP Request: " + harvesterVerb.getRequestURL());
        return OAIHarvesterResponseDTO.fromHarvesterVerb(harvesterVerb);
    }

    private String formatMessage(String baseURL, Map<String,String> parameters, Exception ex) {
        String message = "There was a problem calling " + baseURL;
        if (parameters.isEmpty()) {
            return appendRootExceptionMessage(message, ex);
        }

        message = message + "?";

        for (String parameterName : parameters.keySet()) {
            String parameterValue = parameters.get(parameterName);
            if (StringUtils.isNotBlank(parameterValue)) {
                message = message + parameterName + "=" + parameterValue + "&";
            }
        }

        return appendRootExceptionMessage(message.substring(0, message.length() - 1), ex);
    }

    private String appendRootExceptionMessage(String message, Exception ex) {
        return message + ": " + getRootMessage(ex);
    }

}
