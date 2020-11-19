/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest.service;

import org.dspace.harvest.model.OAIHarvesterResponseDTO;
import org.jdom.Namespace;

/**
 * OAI-PMH client.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OAIHarvesterClient {

    Namespace OAI_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/");

    Namespace OAI_IDENTIFIER_NS = Namespace.getNamespace("http://www.openarchives.org/OAI/2.0/oai-identifier");

    /**
     * Harvest records from a repository.
     *
     * @param  baseURL        the repository base url
     * @param  from           specifies a lower bound for datestamp-based selective
     *                        harvesting
     * @param  until          specifies a upper bound for datestamp-based selective
     *                        harvesting
     * @param  set            specifies set criteria for selective harvesting
     * @param  metadataPrefix specifies the metadataPrefix of the format that should
     *                        be included in the metadata part of the returned
     *                        records
     * @return                the repository response
     */
    OAIHarvesterResponseDTO listRecords(String baseURL, String from, String until, String set, String metadataPrefix);

    /**
     * Harvest records from a repository using the flow control token.
     *
     * @param  baseURL         the repository base url
     * @param  resumptionToken the flow control token returned by a previous
     *                         ListRecords request that issued an incomplete list
     * @return                 the repository response
     */
    OAIHarvesterResponseDTO listRecords(String baseURL, String resumptionToken);

    /**
     * Retrieve the metadata formats available from a repository.
     *
     * @param  baseURL the repository base url
     * @return         the repository response
     */
    OAIHarvesterResponseDTO listMetadataFormats(String baseURL);

    /**
     * Retrieve information about a repository. Some of the information returned is
     * required as part of the OAI-PMH.
     *
     * @param  baseURL the repository base url
     * @return         the repository response
     */
    OAIHarvesterResponseDTO identify(String baseURL);

    /**
     * Retrieve an individual metadata record from a repository.
     *
     * @param  baseURL        the repository base url
     * @param  identifier     specifies the unique identifier of the item in the
     *                        repository must be disseminated.
     * @param  metadataPrefix the metadataPrefix of the format that should be
     *                        included in the metadata part of the returned record
     * @return                the repository response
     */
    OAIHarvesterResponseDTO getRecord(String baseURL, String identifier, String metadataPrefix);

    /**
     * Query the OAI-PMH server for its mapping of the supplied namespace and
     * metadata prefix. For example for a typical OAI-PMH server a query
     * "http://www.openarchives.org/OAI/2.0/oai_dc/" would return "oai_dc".
     *
     * @param  baseUrl   the address of the OAI-PMH provider
     * @param  namespace the namespace that we are trying to resolve to the
     *                   metadataPrefix
     * @return           metadataPrefix the OAI-PMH provider has assigned to the
     *                   supplied namespace
     */
    String resolveNamespaceToPrefix(String baseUrl, String namespace);
}
