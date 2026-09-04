/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.dspace.AbstractUnitTest;
import org.dspace.checker.dao.ChecksumHistoryDAO;
import org.dspace.content.Bitstream;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class ChecksumHistoryServiceImplTest extends AbstractUnitTest {

    @InjectMocks
    // subject
    private ChecksumHistoryServiceImpl checksumHistoryService;

    @Mock
    private ChecksumHistoryDAO checksumHistoryDAO;

    @Mock
    private Bitstream bitstreamMock;

    @Before
    public void setUp()  {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testFind() throws Exception {
        // given
        Long id = 123L;
        ChecksumHistory expectedHistory = new ChecksumHistory();

        when(checksumHistoryDAO.findByID(context, id)).thenReturn(expectedHistory);

        // when
        ChecksumHistory result = checksumHistoryService.find(context, id);

        // then
        assertEquals(expectedHistory, result);
        verify(checksumHistoryDAO).findByID(context, id);
    }

    @Test
    public void testFindAll() throws Exception {
        // given
        int pageSize = 10;
        int offset = 0;
        List<ChecksumHistory> expectedList = Arrays.asList(new ChecksumHistory(), new ChecksumHistory());

        when(checksumHistoryDAO.findAll(context, ChecksumHistory.class, pageSize, offset)).thenReturn(expectedList);

        // when
        List<ChecksumHistory> result = checksumHistoryService.findAll(context, pageSize, offset);

        // then
        assertEquals(expectedList, result);
        verify(checksumHistoryDAO).findAll(context, ChecksumHistory.class, pageSize, offset);
    }

    @Test
    public void testCountTotal() throws Exception {
        // given
        int expectedCount = 42;
        when(checksumHistoryDAO.countTotal(context)).thenReturn(expectedCount);

        // when
        int result = checksumHistoryService.countTotal(context);

        // then
        assertEquals(expectedCount, result);
        verify(checksumHistoryDAO).countTotal(context);
    }

    @Test
    public void testFindByBitstream() throws Exception {
        // given
        int pageSize = 5;
        int offset = 10;
        List<ChecksumHistory> expectedList = List.of(new ChecksumHistory());

        when(checksumHistoryDAO.findByBitstream(context, bitstreamMock, pageSize, offset)).thenReturn(expectedList);

        // when
        List<ChecksumHistory> result = checksumHistoryService.findByBitstream(context, bitstreamMock, pageSize, offset);

        // then
        assertEquals(expectedList, result);
        verify(checksumHistoryDAO).findByBitstream(context, bitstreamMock, pageSize, offset);
    }

    @Test
    public void testCountByBitstream() throws Exception {
        // given
        int expectedCount = 5;
        when(checksumHistoryDAO.countByBitstream(context, bitstreamMock)).thenReturn(expectedCount);

        // when
        int result = checksumHistoryService.countByBitstream(context, bitstreamMock);

        // then
        assertEquals(expectedCount, result);
        verify(checksumHistoryDAO).countByBitstream(context, bitstreamMock);
    }

    @Test
    public void testFindByResultCode() throws Exception {
        // given
        int pageSize = 20;
        int offset = 0;
        ChecksumResultCode code = ChecksumResultCode.CHECKSUM_MATCH;
        List<ChecksumHistory> expectedList = List.of(new ChecksumHistory());

        when(checksumHistoryDAO.findByResultCode(context, code, pageSize, offset)).thenReturn(expectedList);

        // when
        List<ChecksumHistory> result = checksumHistoryService.findByResultCode(context, code, pageSize, offset);

        // then
        assertEquals(expectedList, result);
        verify(checksumHistoryDAO).findByResultCode(context, code, pageSize, offset);
    }

    @Test
    public void testCountByResultCode() throws Exception {
        // given
        ChecksumResultCode code = ChecksumResultCode.CHECKSUM_NO_MATCH;
        int expectedCount = 3;

        when(checksumHistoryDAO.countByResultCode(context, code)).thenReturn(expectedCount);

        // when
        int result = checksumHistoryService.countByResultCode(context, code);

        // then
        assertEquals(expectedCount, result);
        verify(checksumHistoryDAO).countByResultCode(context, code);
    }
}
