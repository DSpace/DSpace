/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.client;

import java.util.List;
import java.util.Optional;

import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.orcid.jaxb.model.v3.release.record.Person;
import org.orcid.jaxb.model.v3.release.record.Record;
import org.orcid.jaxb.model.v3.release.record.WorkBulk;
import org.orcid.jaxb.model.v3.release.record.summary.Works;
import org.orcid.jaxb.model.v3.release.search.expanded.ExpandedSearch;

/**
 * Interface for classes that allow to contact ORCID.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public interface OrcidClient {

    /**
     * Retrieves an /read-public access token using a client-credentials OAuth flow,
     * or 2-step OAuth.
     *
     * @param  code                 the authorization code
     * @return                      the ORCID token
     * @throws OrcidClientException if some error occurs during the exchange
     */
    OrcidTokenResponseDTO getReadPublicAccessToken();

    /**
     * Retrieves a /webhook access token using a client-credentials OAuth flow, or
     * 2-step OAuth. A single token can be used to register webhooks for multiple
     * records.
     *
     * @param  code                 the authorization code
     * @return                      the ORCID token
     * @throws OrcidClientException if some error occurs during the exchange
     */
    OrcidTokenResponseDTO getWebhookAccessToken();

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
     * Retrieves all the works related to the given orcid.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id related to the works
     * @return                      the Works
     * @throws OrcidClientException if some error occurs during the search
     */
    Works getWorks(String accessToken, String orcid);

    /**
     * Retrieves all the works related to the given orcid.
     *
     * @param  orcid                the orcid id related to the works
     * @return                      the Works
     * @throws OrcidClientException if some error occurs during the search
     */
    Works getWorks(String orcid);

    /**
     * Retrieves all the works with the given putCodes related to the given orcid
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id
     * @param  putCodes             the putCodes of the works to retrieve
     * @return                      the Works
     * @throws OrcidClientException if some error occurs during the search
     */
    WorkBulk getWorkBulk(String accessToken, String orcid, List<String> putCodes);

    /**
     * Retrieves all the works with the given putCodes related to the given orcid
     *
     * @param  orcid                the orcid id
     * @param  putCodes             the putCodes of the works to retrieve
     * @return                      the Works
     * @throws OrcidClientException if some error occurs during the search
     */
    WorkBulk getWorkBulk(String orcid, List<String> putCodes);

    /**
     * Retrieves an object from ORCID with the given putCode related to the given
     * orcid.
     *
     * @param  accessToken              the access token
     * @param  orcid                    the orcid id
     * @param  putCode                  the object's put code
     * @param  clazz                    the object's class
     * @return                          the Object, if any
     * @throws OrcidClientException     if some error occurs during the search
     * @throws IllegalArgumentException if the given object class is not an valid
     *                                  ORCID object
     */
    <T> Optional<T> getObject(String accessToken, String orcid, String putCode, Class<T> clazz);

    /**
     * Retrieves an object from ORCID with the given putCode related to the given
     * orcid using the public API.
     *
     * @param  orcid                    the orcid id
     * @param  putCode                  the object's put code
     * @param  clazz                    the object's class
     * @return                          the Object, if any
     * @throws OrcidClientException     if some error occurs during the search
     * @throws IllegalArgumentException if the given object class is not an valid
     *                                  ORCID object
     */
    <T> Optional<T> getObject(String orcid, String putCode, Class<T> clazz);

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

    /**
     * Register a webhook against the user’s ORCID record with the given orcid id.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id
     * @param  url                  the webhook url
     * @return                      the orcid response if no error occurs
     * @throws OrcidClientException if some error occurs during the search
     */
    OrcidResponse registerWebhook(String accessToken, String orcid, String url);

    /**
     * Unregister the webhook related to the user’s ORCID record with the given
     * orcid id.
     *
     * @param  accessToken          the access token
     * @param  orcid                the orcid id
     * @param  url                  the webhook url
     * @return                      the orcid response if no error occurs
     * @throws OrcidClientException if some error occurs during the search
     */
    OrcidResponse unregisterWebhook(String accessToken, String orcid, String url);

    /**
     * Perform an expanded search with the given query and pagination using the api
     * endpoint.
     *
     * @param  accessToken the access token
     * @param  query       the query
     * @param  start       the start index
     * @param  rows        the number of rows to retrieve
     * @return             the expanded search result
     */
    ExpandedSearch expandedSearch(String accessToken, String query, int start, int rows);

    /**
     * Perform an expanded search with the given query and pagination using the
     * public endpoint.
     *
     * @param  accessToken the access token
     * @param  query       the query
     * @param  start       the start index
     * @param  rows        the number of rows to retrieve
     * @return             the expanded search result
     */
    ExpandedSearch expandedSearch(String query, int start, int rows);

}
