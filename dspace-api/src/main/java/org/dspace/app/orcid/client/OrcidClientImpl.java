/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.orcid.client;

import static org.apache.http.client.methods.RequestBuilder.get;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.app.orcid.exception.OrcidClientException;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.util.ThrowingSupplier;
import org.orcid.jaxb.model.v3.release.record.Person;

/**
 * Implementation of {@link OrcidClient}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class OrcidClientImpl implements OrcidClient {

    private final OrcidConfiguration orcidConfiguration;

    private final ObjectMapper objectMapper;

    public OrcidClientImpl(OrcidConfiguration orcidConfiguration) {
        this.orcidConfiguration = orcidConfiguration;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public OrcidTokenResponseDTO getAccessToken(String code) {

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("client_id", orcidConfiguration.getClientId()));
        params.add(new BasicNameValuePair("client_secret", orcidConfiguration.getClientSecret()));

        HttpUriRequest httpUriRequest = RequestBuilder.post(orcidConfiguration.getTokenEndpointUrl())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Accept", "application/json")
            .setEntity(new UrlEncodedFormEntity(params, Charset.defaultCharset()))
            .build();

        return executeAndParseJson(httpUriRequest, OrcidTokenResponseDTO.class);

    }

    @Override
    public Person getPerson(String accessToken, String orcid) {
        HttpUriRequest httpUriRequest = buildGetUriRequest(accessToken, "/" + orcid + "/person");
        return executeAndUnmarshall(httpUriRequest, false, Person.class);
    }

    private HttpUriRequest buildGetUriRequest(String accessToken, String relativePath) {
        return get(orcidConfiguration.getApiUrl() + relativePath.trim())
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();
    }

    private <T> T executeAndParseJson(HttpUriRequest httpUriRequest, Class<T> clazz) {

        HttpClient client = HttpClientBuilder.create().build();

        return executeAndReturns(() -> {

            HttpResponse response = client.execute(httpUriRequest);

            if (isNotSuccessfull(response)) {
                throw new OrcidClientException(getStatusCode(response), formatErrorMessage(response));
            }

            return objectMapper.readValue(response.getEntity().getContent(), clazz);

        });

    }

    private <T> T executeAndUnmarshall(HttpUriRequest httpUriRequest, boolean handleNotFoundAsNull, Class<T> clazz) {

        HttpClient client = HttpClientBuilder.create().build();

        return executeAndReturns(() -> {

            HttpResponse response = client.execute(httpUriRequest);

            if (handleNotFoundAsNull && isNotFound(response)) {
                return null;
            }

            if (isNotSuccessfull(response)) {
                throw new OrcidClientException(getStatusCode(response), formatErrorMessage(response));
            }

            return unmarshall(response.getEntity(), clazz);

        });
    }

    private <T> T executeAndReturns(ThrowingSupplier<T, Exception> supplier) {
        try {
            return supplier.get();
        } catch (OrcidClientException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new OrcidClientException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T unmarshall(HttpEntity entity, Class<T> clazz) throws Exception {
        InputStream content = entity.getContent();
        JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        return (T) unmarshaller.unmarshal(content);
    }

    private String formatErrorMessage(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            return "Generic error";
        }
    }

    private boolean isNotSuccessfull(HttpResponse response) {
        int statusCode = getStatusCode(response);
        return statusCode < 200 || statusCode > 299;
    }

    private boolean isNotFound(HttpResponse response) {
        return getStatusCode(response) == HttpStatus.SC_NOT_FOUND;
    }

    private int getStatusCode(HttpResponse response) {
        return response.getStatusLine().getStatusCode();
    }

}
