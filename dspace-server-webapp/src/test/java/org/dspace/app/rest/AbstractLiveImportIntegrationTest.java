/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
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
            assertTrue(sameMetadatum(list.get(i), list2.get(i)),
                       "'" + list.get(i).toString() + "' should be equal to '" + list2.get(i).toString() + "'");
        }
    }

    private boolean sameMetadatum(MetadatumDTO metadatum, MetadatumDTO metadatum2) {
        if (Strings.CS.equals(metadatum.getSchema(), metadatum2.getSchema()) &&
            Strings.CS.equals(metadatum.getElement(), metadatum2.getElement()) &&
            Strings.CS.equals(metadatum.getQualifier(), metadatum2.getQualifier()) &&
            Strings.CS.equals(metadatum.getValue(), metadatum2.getValue())) {
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
        BasicHttpEntity basicHttpEntity = new BasicHttpEntity(
            IOUtils.toInputStream(xmlExample, "UTF-8"),
            ContentType.APPLICATION_XML,
            true  // chunked
        );

        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        when(response.getCode()).thenReturn(statusCode);
        when(response.getReasonPhrase()).thenReturn(reason);
        when(response.getEntity()).thenReturn(basicHttpEntity);
        return response;
    }

}