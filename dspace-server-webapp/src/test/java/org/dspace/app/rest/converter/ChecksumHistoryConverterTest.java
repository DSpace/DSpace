/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.converter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Date;

import org.dspace.AbstractUnitTest;
import org.dspace.app.rest.model.ChecksumHistoryRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResult;
import org.dspace.checker.ChecksumResultCode;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class ChecksumHistoryConverterTest extends AbstractUnitTest {

    @InjectMocks
    // subject
    private ChecksumHistoryConverter converter;

    @Mock
    private Projection projection;

    @Mock
    private ChecksumHistory checksumHistoryMock;

    @Mock
    private ChecksumResult checksumResultMock;

    @Test
    public void testConvert() {
        // given
        Long domainId = 123L;
        Integer expectedId = 123;
        String expectedChecksum = "abc123expected";
        String calculatedChecksum = "abc123calculated";
        Instant startDate = Instant.now().minusSeconds(60);
        Instant endDate = Instant.now();
        ChecksumResultCode resultCode = ChecksumResultCode.CHECKSUM_MATCH;

        when(checksumHistoryMock.getID()).thenReturn(domainId);
        when(checksumHistoryMock.getChecksumExpected()).thenReturn(expectedChecksum);
        when(checksumHistoryMock.getChecksumCalculated()).thenReturn(calculatedChecksum);
        when(checksumHistoryMock.getProcessStartDate()).thenReturn(startDate);
        when(checksumHistoryMock.getProcessEndDate()).thenReturn(endDate);

        when(checksumResultMock.getResultCode()).thenReturn(resultCode);
        when(checksumHistoryMock.getResult()).thenReturn(checksumResultMock);

        // when
        ChecksumHistoryRest rest = converter.convert(checksumHistoryMock, projection);

        // then
        assertNotNull(rest);
        assertEquals("ID should match",
                expectedId, rest.getId());
        assertEquals("Expected checksum should match",
                expectedChecksum, rest.getChecksumExpected());
        assertEquals("Calculated checksum should match",
                calculatedChecksum, rest.getChecksumCalculated());
        assertEquals("Start date should be properly converted",
                Date.from(startDate), rest.getProcessStartDate());
        assertEquals("End date should be properly converted",
                Date.from(endDate), rest.getProcessEndDate());
        assertEquals("Result code should be mapped to String",
                "CHECKSUM_MATCH", rest.getResultCodeValue());
        assertEquals("Projection should be assigned",
                projection, rest.getProjection());
    }

    @Test
    public void testConvertWithNulls() {
        // given
        Long domainId = 456L;
        Integer expectedId = 456;

        when(checksumHistoryMock.getID()).thenReturn(domainId);
        when(checksumHistoryMock.getProcessStartDate()).thenReturn(null);
        when(checksumHistoryMock.getProcessEndDate()).thenReturn(null);
        when(checksumHistoryMock.getResult()).thenReturn(null);

        // when
        ChecksumHistoryRest rest = converter.convert(checksumHistoryMock, projection);

        // then
        assertNotNull(rest);
        assertEquals(expectedId, rest.getId());
        assertNull("Process start date should be null", rest.getProcessStartDate());
        assertNull("Process end date should be null", rest.getProcessEndDate());
        assertNull("Result code value should be null", rest.getResultCodeValue());
    }

    @Test
    public void testGetModelClass() {
        // when
        Class<ChecksumHistory> modelClass = converter.getModelClass();

        // then
        assertEquals("Should return ChecksumHistory class", ChecksumHistory.class, modelClass);
    }
}
