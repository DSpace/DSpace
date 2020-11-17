/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.util.List;

import ORG.oclc.oai.harvester2.verb.GetRecord;
import ORG.oclc.oai.harvester2.verb.HarvesterVerb;
import ORG.oclc.oai.harvester2.verb.Identify;
import ORG.oclc.oai.harvester2.verb.ListMetadataFormats;
import ORG.oclc.oai.harvester2.verb.ListRecords;
import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.dspace.harvest.service.OAIHarvesterClient;
import org.dspace.util.ThrowingSupplier;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
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

    private DOMBuilder domBuilder = new DOMBuilder();

    @Override
    public OAIHarvesterResponseDTO listRecords(String baseURL, String from, String until, String set,
        String metadataPrefix) {
        return harvest(() -> new ListRecords(baseURL, from, until, set, metadataPrefix));
    }

    @Override
    public OAIHarvesterResponseDTO listRecords(String baseURL, String resumptionToken) {
        return harvest(() -> new ListRecords(baseURL, resumptionToken));
    }

    @Override
    public OAIHarvesterResponseDTO listMetadataFormats(String baseURL) {
        return harvest(() -> new ListMetadataFormats(baseURL));
    }

    @Override
    public OAIHarvesterResponseDTO identify(String baseURL) {
        return harvest(() -> new Identify(baseURL));
    }

    @Override
    public OAIHarvesterResponseDTO getRecord(String baseURL, String identifier, String metadataPrefix) {
        return harvest(() -> new GetRecord(baseURL, identifier, metadataPrefix));
    }

    @SuppressWarnings("unchecked")
    public String resolveNamespaceToPrefix(String baseUrl, String namespace) {

        // Query the OAI server for the metadata
        OAIHarvesterResponseDTO responseDTO = listMetadataFormats(baseUrl);
        Document response = domBuilder.build(responseDTO.getDocument());
        List<Element> mdFormats = response.getRootElement().getChild("ListMetadataFormats", OAI_NS)
            .getChildren("metadataFormat", OAI_NS);

        return mdFormats.stream()
            .filter(mdFormat -> namespace.equals(mdFormat.getChildText("metadataNamespace", OAI_NS)))
            .map(mdFormat -> mdFormat.getChildText("metadataPrefix", OAI_NS))
            .findFirst()
            .orElse(null);
    }

    private OAIHarvesterResponseDTO harvest(ThrowingSupplier<? extends HarvesterVerb> supplier) {
        try {
            HarvesterVerb harvesterVerb = supplier.get();
            LOGGER.info("HTTP Request: " + harvesterVerb.getRequestURL());
            return OAIHarvesterResponseDTO.fromHarvesterVerb(harvesterVerb);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
