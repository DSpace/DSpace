/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.orcid.client;

import org.dspace.orcid.exception.OrcidClientException;
import org.dspace.orcid.model.OrcidTokenResponseDTO;
import org.orcid.jaxb.model.v3.release.record.Person;

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
     * Push the given object to ORCID.
     *
     * @param  accessToken              the access token
     * @param  orcid                    the orcid id
     * @param  object                   the orcid object to push
     * @return                          the orcid response if no error occurs
     * @throws OrcidClientException     if some error occurs during the push
     * @throws IllegalArgumentException if the given object is not an valid ORCID
     *                                  object
     */
    OrcidResponse push(String accessToken, String orcid, Object object);

    /**
     * Update the object with the given putCode.
     *
     * @param  accessToken              the access token
     * @param  orcid                    the orcid id
     * @param  object                   the orcid object to push
     * @param  putCode                  the put code of the resource to delete
     * @return                          the orcid response if no error occurs
     * @throws OrcidClientException     if some error occurs during the push
     * @throws IllegalArgumentException if the given object is not an valid ORCID
     *                                  object
     */
    OrcidResponse update(String accessToken, String orcid, Object object, String putCode);

    /**
     * Delete the ORCID object with the given putCode on the given path.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id
     * @param  putCode              the put code of the resource to delete
     * @param  path                 the path of the resource to delete
     * @return                      the orcid response if no error occurs
     * @throws OrcidClientException if some error occurs during the search
     */
    OrcidResponse deleteByPutCode(String accessToken, String orcid, String putCode, String path);

}
