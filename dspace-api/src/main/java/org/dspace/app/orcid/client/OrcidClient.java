/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.client;

import java.util.Optional;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.orcid.jaxb.model.v3.release.record.Funding;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.record.Work;

/**
 * Interface for classes that allow to contact ORCID.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidClient {

    /**
     * Exchange the authorization code for an ORCID iD and 3-legged access token.
     * The authorization code expires upon use.
     *
     * @param  code                 the authorization code
     * @return                      the ORCID token
     * @throws OrcidClientException if some error occurs during the exchange
     */
    OrcidTokenResponseDTO getAccessToken(String code);

    /**
     * Retrieves a summary of the ORCID person related to the given orcid.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id of the record to retrieve
     * @return                      the Person
     * @throws OrcidClientException if some error occurs during the search
     */
    Person getPerson(String accessToken, String orcid);

    /**
     * Retrieves a summary of the ORCID record related to the given orcid.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id of the record to retrieve
     * @return                      the Record
     * @throws OrcidClientException if some error occurs during the search
     */
    Record getRecord(String accessToken, String orcid);

    /**
     * Retrieves a summary of the work with the given putCode related to the given
     * orcid.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id of the record to retrieve
     * @return                      the Work, if any
     * @throws OrcidClientException if some error occurs during the search
     */
    Optional<Work> getWork(String accessToken, String orcid, String putCode);

    /**
     * Retrieves a summary of the funding with the given putCode related to the
     * given orcid.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id of the record to retrieve
     * @return                      the Funding, if any
     * @throws OrcidClientException if some error occurs during the search
     */
    Optional<Funding> getFunding(String accessToken, String orcid, String putCode);
}
