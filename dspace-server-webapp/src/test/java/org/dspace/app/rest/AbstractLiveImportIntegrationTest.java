/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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

    protected void matchRecords(ArrayList<ImportRecord> recordsImported, ArrayList<ImportRecord> records2match) {
        assertEquals(records2match.size(), recordsImported.size());
        for (int i = 0; i < recordsImported.size(); i++) {
            ImportRecord imported = recordsImported.get(i);
            ImportRecord toMatch = records2match.get(i);
            checkMetadataValue(imported.getValueList(), toMatch.getValueList());
        }
    }

    private void checkMetadataValue(List<MetadatumDTO> list, List<MetadatumDTO> list2) {
        assertEquals(list.size(), list2.size());
        for (int i = 0; i < list.size(); i++) {
            assertTrue(sameMetadatum(list.get(i), list2.get(i)));
        }
    }

    private boolean sameMetadatum(MetadatumDTO metadatum, MetadatumDTO metadatum2) {
        if (StringUtils.equals(metadatum.getSchema(), metadatum2.getSchema()) &&
            StringUtils.equals(metadatum.getElement(), metadatum2.getElement()) &&
            StringUtils.equals(metadatum.getQualifier(), metadatum2.getQualifier()) &&
            StringUtils.equals(metadatum.getValue(), metadatum2.getValue())) {
            return true;
        }
        return false;
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