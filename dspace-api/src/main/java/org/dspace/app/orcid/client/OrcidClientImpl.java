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
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.dspace.app.orcid.model.OrcidTokenResponseDTO;
import org.dspace.authenticate.OrcidClientException;
import org.dspace.util.ThrowingSupplier;
import org.orcid.jaxb.model.v3.release.record.Record;

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

        HttpClient client = HttpClientBuilder.create().build();

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("code", code));
        params.add(new BasicNameValuePair("grant_type", "authorization_code"));
        params.add(new BasicNameValuePair("redirect_uri", orcidConfiguration.getRedirectUri()));
        params.add(new BasicNameValuePair("client_id", orcidConfiguration.getClientId()));
        params.add(new BasicNameValuePair("client_secret", orcidConfiguration.getClientSecret()));

        return executeAndReturns(() -> {

            HttpUriRequest httpUriRequest = RequestBuilder.post(orcidConfiguration.getTokenEndpointUrl())
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .addHeader("Accept", "application/json")
                .setEntity(new UrlEncodedFormEntity(params, "UTF-8"))
                .build();

            HttpResponse response = client.execute(httpUriRequest);

            if (isNotSuccessfull(response)) {
                throw new OrcidClientException(formatErrorMessage(response));
            }

            return objectMapper.readValue(response.getEntity().getContent(), OrcidTokenResponseDTO.class);

        });

    }

    @Override
    public Record getRecord(String accessToken, String orcid) {

        HttpClient client = HttpClientBuilder.create().build();

        HttpUriRequest httpUriRequest = get(orcidConfiguration.getApiUrl() + "/" + orcid + "/record")
            .addHeader("Content-Type", "application/x-www-form-urlencoded")
            .addHeader("Authorization", "Bearer " + accessToken)
            .build();

        return executeAndReturns(() -> {

            HttpResponse response = client.execute(httpUriRequest);

            if (isNotSuccessfull(response)) {
                throw new OrcidClientException(formatErrorMessage(response));
            }

            System.out.println(IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset()));

            return null;

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

    private String formatErrorMessage(HttpResponse response) {
        try {
            return IOUtils.toString(response.getEntity().getContent(), Charset.defaultCharset());
        } catch (UnsupportedOperationException | IOException e) {
            return "Generic error";
        }
    }

    private boolean isNotSuccessfull(HttpResponse response) {
        return response.getStatusLine().getStatusCode() != HttpStatus.SC_OK;
    }

}
