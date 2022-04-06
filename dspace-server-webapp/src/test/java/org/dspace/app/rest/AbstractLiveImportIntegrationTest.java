/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.Collection;

import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.tools.ant.filters.StringInputStream;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.importer.external.datamodel.ImportRecord;
import org.dspace.importer.external.metadatamapping.MetadatumDTO;

/**
 * @author Mykhaylo Boychuk (mykhaylo.boychuk at 4science.com)
 */
public class AbstractLiveImportIntegrationTest extends AbstractControllerIntegrationTest {

    protected boolean matchRecords(Collection<ImportRecord> recordsImported, Collection<ImportRecord> records2match) {
        ImportRecord  firstImported = recordsImported.iterator().next();
        ImportRecord  secondImported = recordsImported.iterator().next();
        ImportRecord  first2match = recordsImported.iterator().next();
        ImportRecord  second2match = recordsImported.iterator().next();
        boolean checkFirstRecord = first2match.getValueList().containsAll(firstImported.getValueList());
        boolean checkSecondRecord = second2match.getValueList().containsAll(secondImported.getValueList());
        return checkFirstRecord && checkSecondRecord;
    }

    protected MetadatumDTO createMetadatumDTO(String schema, String element, String qualifier, String value) {
        MetadatumDTO metadatumDTO = new MetadatumDTO();
        metadatumDTO.setSchema(schema);
        metadatumDTO.setElement(element);
        metadatumDTO.setQualifier(qualifier);
        metadatumDTO.setValue(value);
        return metadatumDTO;
    }

    protected CloseableHttpResponse mockResponse(String xmlExample, int statusCode, String reason)
            throws UnsupportedEncodingException {
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity();
        basicHttpEntity.setChunked(true);
        basicHttpEntity.setContent(new StringInputStream(xmlExample));

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getStatusLine()).thenReturn(statusLine(statusCode, reason));
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

    protected StatusLine statusLine(int statusCode, String reason) {
        return new StatusLine() {
            @Override
            public ProtocolVersion getProtocolVersion() {
                return new ProtocolVersion("http", 1, 1);
            }

            @Override
            public int getStatusCode() {
                return statusCode;
            }

            @Override
            public String getReasonPhrase() {
                return reason;
            }
        };
    }

}