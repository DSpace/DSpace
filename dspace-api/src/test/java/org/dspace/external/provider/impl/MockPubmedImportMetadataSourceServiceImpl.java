/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dspace.importer.external.pubmed.service.PubmedImportMetadataSourceServiceImpl;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.util.FileCopyUtils;

/**
 * we override the init method to mock the rest call to pubmed the following
 * mock definitions will allow to answer to efetch or esearch requests using the
 * test resource files (pubmed-esearch.fcgi.xml or pubmed-efetch.fcgi.xml)
 * 
 * @author Andrea Bollini (andrea.bollini at 4science.it)
 *
 */
public class MockPubmedImportMetadataSourceServiceImpl extends PubmedImportMetadataSourceServiceImpl {

    @Override
    public void init() throws Exception {
        pubmedWebTarget = Mockito.mock(WebTarget.class);
        ArgumentCaptor<String> valueCapture = ArgumentCaptor.forClass(String.class);
        when(pubmedWebTarget.queryParam(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenAnswer(new Answer<WebTarget>() {
                    @Override
                    public WebTarget answer(InvocationOnMock invocation) throws Throwable {
                        return pubmedWebTarget;
                    }
                });
        when(pubmedWebTarget.path(valueCapture.capture())).thenAnswer(new Answer<WebTarget>() {
            @Override
            public WebTarget answer(InvocationOnMock invocation) throws Throwable {
                return pubmedWebTarget;
            }
        });
        when(pubmedWebTarget.request(ArgumentMatchers.any(MediaType.class)))
                .thenAnswer(new Answer<Invocation.Builder>() {
                    @Override
                    public Invocation.Builder answer(InvocationOnMock invocation) throws Throwable {
                        Invocation.Builder builder = Mockito.mock(Invocation.Builder.class);
                        when(builder.get()).thenAnswer(new Answer<Response>() {
                            @Override
                            public Response answer(InvocationOnMock invocation) throws Throwable {
                                Response response = Mockito.mock(Response.class);
                                when(response.readEntity(ArgumentMatchers.eq(String.class))).then(new Answer<String>() {
                                    @Override
                                    public String answer(InvocationOnMock invocation) throws Throwable {
                                        String resourceName = "pubmed-" + valueCapture.getValue() + ".xml";
                                        InputStream resource = getClass().getResourceAsStream(resourceName);
                                        try (Reader reader = new InputStreamReader(resource, StandardCharsets.UTF_8)) {
                                            return FileCopyUtils.copyToString(reader);
                                        } catch (IOException e) {
                                            throw new UncheckedIOException(e);
                                        }
                                    }
                                });
                                return response;
                            }
                        });
                        return builder;
                    };
                });
    }

}
