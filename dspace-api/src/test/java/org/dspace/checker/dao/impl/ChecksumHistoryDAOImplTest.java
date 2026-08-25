/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.Query;
import org.dspace.AbstractUnitTest;
import org.dspace.checker.ChecksumHistory;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.CoreHelpers;
import org.dspace.core.HibernateDBConnection;
import org.hibernate.query.MutationQuery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author mwood
 */
public class ChecksumHistoryDAOImplTest
    extends AbstractUnitTest {
    public ChecksumHistoryDAOImplTest() {
    }

    @BeforeClass
    public static void setUpClass()
        throws SQLException, ClassNotFoundException {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown()
        throws SQLException {
    }

    /**
     * Test of deleteByDateAndCode method, of class ChecksumHistoryDAOImpl.
     */
    @Test
    public void testDeleteByDateAndCode()
        throws Exception {
        Instant retentionDate = Instant.now();
        ChecksumResultCode resultCode = ChecksumResultCode.CHECKSUM_MATCH;

        // Create two older rows
        HibernateDBConnection dbc = (HibernateDBConnection) CoreHelpers.getDBConnection(context);
        Query qry = dbc.getSession().createNativeQuery(
            "INSERT INTO checksum_history"
                + "(check_id, process_end_date, result, bitstream_id)"
                + " VALUES (:id, :date, :result, :bitstream)");

        BitstreamService bss = ContentServiceFactory.getInstance().getBitstreamService();
        InputStream is = new ByteArrayInputStream(new byte[0]);
        Bitstream bs = bss.create(context, is);
        context.turnOffAuthorisationSystem();
        bss.update(context, bs);
        context.restoreAuthSystemState();

        // Add a past date row with matching result code
        Instant matchDate = retentionDate.minus(1, ChronoUnit.DAYS);
        int matchId = 0;
        qry.setParameter("id", matchId);
        qry.setParameter("date", matchDate);
        qry.setParameter("result", ChecksumResultCode.CHECKSUM_MATCH.name());
        qry.setParameter("bitstream", bs.getID()); // FIXME identifier not being set???
        qry.executeUpdate();

        // Add a past date row with a nonmatching result code
        Instant noMatchDate = retentionDate.minus(2, ChronoUnit.DAYS);
        int noMatchId = 1;
        qry.setParameter("id", noMatchId);
        qry.setParameter("date", noMatchDate);
        qry.setParameter("result", ChecksumResultCode.CHECKSUM_NO_MATCH.name());
        qry.setParameter("bitstream", bs.getID()); // FIXME identifier not being set???
        qry.executeUpdate();

        // Add a future date row with a matching result code
        Instant futureDate = retentionDate.plus(3, ChronoUnit.DAYS);
        int futureMatchId = 2;
        qry.setParameter("id", futureMatchId);
        qry.setParameter("date", futureDate);
        qry.setParameter("result", ChecksumResultCode.CHECKSUM_MATCH.name());
        qry.setParameter("bitstream", bs.getID()); // FIXME identifier not being set???
        qry.executeUpdate();

        // Test!
        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();
        int expResult = 1;
        int result = instance.deleteByDateAndCode(context, retentionDate,
                                                  resultCode);
        assertEquals(expResult, result);

        // See if matching old row is gone.
        qry = dbc.getSession().createQuery(
            "SELECT COUNT(*) FROM ChecksumHistory WHERE id = :id");
        long count;

        qry.setParameter("id", matchId);
        count = (Long) qry.getSingleResult();
        assertEquals("Should find no row at matchDate", 0, count);

        // See if nonmatching old row is still present.
        qry.setParameter("id", noMatchId);
        count = (Long) qry.getSingleResult();
        assertEquals("Should find one row at noMatchDate", 1, count);

        // See if future date row is still present.
        qry.setParameter("id", futureMatchId);
        count = (Long) qry.getSingleResult();
        assertEquals("Should find one row at futureDate", 1, count);
    }

    /**
     * Test of deleteByBitstream method, of class ChecksumHistoryDAOImpl.
     */
/*
    @Test
    public void testDeleteByBitstream()
            throws Exception
    {
        System.out.println("deleteByBitstream");
        Bitstream bitstream = null;
        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();
        instance.deleteByBitstream(context, bitstream);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
*/

    @Test
    public void testCountByBitstream() throws Exception {
        // given
        Bitstream bs1 = createDummyBitstream();
        Bitstream bs2 = createDummyBitstream();
        MutationQuery qry = createHistoryInsertQuery();
        Instant now = Instant.now();

        insertHistoryRecord(qry, 100, bs1.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 101, bs1.getID(), now.plusSeconds(10), ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 102, bs2.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);

        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();

        // when
        int countBs1 = instance.countByBitstream(context, bs1);
        int countBs2 = instance.countByBitstream(context, bs2);

        // then
        assertEquals("should find 2 records for bs1", 2, countBs1);
        assertEquals("should find 1 record for bs2", 1, countBs2);
    }

    @Test
    public void testFindByBitstream() throws Exception {
        // given
        Bitstream bs1 = createDummyBitstream();
        Bitstream bs2 = createDummyBitstream();
        MutationQuery qry = createHistoryInsertQuery();
        Instant now = Instant.now();

        insertHistoryRecord(qry, 100, bs1.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 101, bs1.getID(), now.plusSeconds(10), ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 102, bs1.getID(), now.plusSeconds(20), ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 103, bs1.getID(), now.plusSeconds(30), ChecksumResultCode.CHECKSUM_MATCH);

        insertHistoryRecord(qry, 104, bs2.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);

        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();

        // when
        List<ChecksumHistory> bs1Page1 = instance.findByBitstream(context, bs1, 2, 0);
        List<ChecksumHistory> bs1Page2 = instance.findByBitstream(context, bs1, 2, 2);
        List<ChecksumHistory> bs1Page3 = instance.findByBitstream(context, bs1, 2, 4);
        List<ChecksumHistory> bs2List = instance.findByBitstream(context, bs2, 2, 0);

        // then
        assertEquals("Should find 2 records on page 1 for bs1", 2, bs1Page1.size());
        assertEquals("First element should be the newest (103)", Long.valueOf(103), bs1Page1.get(0).getID());
        assertEquals("Second element should be (102)", Long.valueOf(102), bs1Page1.get(1).getID());

        assertEquals("Should find 2 records on page 2 for bs1", 2, bs1Page2.size());
        assertEquals("First element on page 2 should be (101)", Long.valueOf(101), bs1Page2.get(0).getID());
        assertEquals("Second element on page 2 should be the oldest (100)", Long.valueOf(100), bs1Page2.get(1).getID());

        assertEquals("Page 3 should be empty as there are only 4 records", 0, bs1Page3.size());

        assertEquals("Should find 1 record for bs2", 1, bs2List.size());
        assertEquals("Record for bs2 should have ID 104", Long.valueOf(104), bs2List.get(0).getID());
    }

    @Test
    public void testCountByResultCode() throws Exception {
        // given
        Bitstream bs1 = createDummyBitstream();
        Bitstream bs2 = createDummyBitstream();
        MutationQuery qry = createHistoryInsertQuery();
        Instant now = Instant.now();

        insertHistoryRecord(qry, 100, bs1.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 101, bs1.getID(), now.plusSeconds(10), ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 102, bs2.getID(), now, ChecksumResultCode.CHECKSUM_ALGORITHM_INVALID);

        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();

        // when
        int countMatched = instance.countByResultCode(context, ChecksumResultCode.CHECKSUM_MATCH);
        int countAlgorithmInvalid = instance.countByResultCode(context, ChecksumResultCode.CHECKSUM_ALGORITHM_INVALID);

        // then
        assertEquals("should find 2 records for ChecksumResultCode.CHECKSUM_MATCH",
                2, countMatched);
        assertEquals("should find 1 record for ChecksumResultCode.CHECKSUM_ALGORITHM_INVALID",
                1, countAlgorithmInvalid);
    }

    @Test
    public void testFindByResultCode() throws Exception {
        // given
        Bitstream bs1 = createDummyBitstream();
        Bitstream bs2 = createDummyBitstream();
        MutationQuery qry = createHistoryInsertQuery();
        Instant now = Instant.now();


        insertHistoryRecord(qry, 200, bs1.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 201, bs2.getID(), now.plusSeconds(10), ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 202, bs1.getID(), now.plusSeconds(20), ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 203, bs2.getID(), now.plusSeconds(30), ChecksumResultCode.CHECKSUM_MATCH);

        insertHistoryRecord(qry, 204, bs1.getID(), now.plusSeconds(40), ChecksumResultCode.CHECKSUM_NO_MATCH);

        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();

        // when
        List<ChecksumHistory> matchPage1 = instance.findByResultCode(context,
                ChecksumResultCode.CHECKSUM_MATCH, 2, 0);
        List<ChecksumHistory> matchPage2 = instance.findByResultCode(context,
                ChecksumResultCode.CHECKSUM_MATCH, 2, 2);
        List<ChecksumHistory> matchPage3 = instance.findByResultCode(context,
                ChecksumResultCode.CHECKSUM_MATCH, 2, 4);

        List<ChecksumHistory> noMatchList = instance.findByResultCode(context,
                ChecksumResultCode.CHECKSUM_NO_MATCH, 2, 0);

        // then
        assertEquals("Should find 2 MATCH records on page 1",
                2, matchPage1.size());
        assertEquals("First element should be newest MATCH (203)",
                Long.valueOf(203), matchPage1.get(0).getID());
        assertEquals("Second element should be (202)",
                Long.valueOf(202), matchPage1.get(1).getID());

        assertEquals("Should find 2 MATCH records on page 2",
                2, matchPage2.size());
        assertEquals("First element on page 2 should be (201)",
                Long.valueOf(201), matchPage2.get(0).getID());
        assertEquals("Second element on page 2 should be oldest MATCH (200)",
                Long.valueOf(200), matchPage2.get(1).getID());

        assertEquals("Page 3 should be empty",
                0, matchPage3.size());

        assertEquals("Should find 1 NO_MATCH record",
                1, noMatchList.size());
        assertEquals("Record for NO_MATCH should have ID 204",
                Long.valueOf(204), noMatchList.getFirst().getID());
    }

    @Test
    public void testFindByID() throws Exception {
        // given
        Bitstream bs = createDummyBitstream();
        MutationQuery qry = createHistoryInsertQuery();
        Instant now = Instant.now();

        insertHistoryRecord(qry, 300, bs.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 301, bs.getID(), now, ChecksumResultCode.CHECKSUM_NO_MATCH);

        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();

        // when
        ChecksumHistory found = instance.findByID(context, 300L);
        ChecksumHistory notFound = instance.findByID(context, 999L);

        // then
        assertNotNull("Should find record with ID 300", found);
        assertEquals("Found record should have ID 300", Long.valueOf(300), found.getID());
        assertEquals("Found record should have correct Bitstream", bs.getID(), found.getBitstream().getID());
        assertEquals("Found record should have correct ResultCode",
                ChecksumResultCode.CHECKSUM_MATCH, found.getResult().getResultCode());

        assertNull("Should return null for non-existent ID 999", notFound);
    }

    @Test
    public void testCountTotal() throws Exception {
        // given
        ChecksumHistoryDAOImpl instance = new ChecksumHistoryDAOImpl();

        int initialCount = instance.countTotal(context);

        Bitstream bs = createDummyBitstream();
        MutationQuery qry = createHistoryInsertQuery();
        Instant now = Instant.now();

        insertHistoryRecord(qry, 400, bs.getID(), now, ChecksumResultCode.CHECKSUM_MATCH);
        insertHistoryRecord(qry, 401, bs.getID(), now, ChecksumResultCode.CHECKSUM_NO_MATCH);
        insertHistoryRecord(qry, 402, bs.getID(), now, ChecksumResultCode.CHECKSUM_ALGORITHM_INVALID);

        // when
        int newCount = instance.countTotal(context);

        // then
        assertEquals("Total count should increase by exactly 3", initialCount + 3, newCount);
    }

    private Bitstream createDummyBitstream() throws Exception {
        BitstreamService bss = ContentServiceFactory.getInstance().getBitstreamService();
        context.turnOffAuthorisationSystem();
        Bitstream bs = bss.create(context, new ByteArrayInputStream(new byte[0]));
        bss.update(context, bs);
        context.restoreAuthSystemState();
        return bs;
    }

    private MutationQuery createHistoryInsertQuery() throws SQLException {
        HibernateDBConnection dbc = (HibernateDBConnection) CoreHelpers.getDBConnection(context);
        return dbc.getSession().createNativeMutationQuery("""
            INSERT INTO checksum_history
            (check_id, bitstream_id, process_start_date, process_end_date,
             checksum_expected, checksum_calculated, result)
            VALUES (:id, :bitstream, :startDate, :endDate, :expected, :calculated, :result)
            """);
    }

    private void insertHistoryRecord(MutationQuery qry,
                                     int id,
                                     UUID bitstreamId,
                                     Instant startDate,
                                     ChecksumResultCode checksumResultCode) {
        qry.setParameter("id", id);
        qry.setParameter("bitstream", bitstreamId);
        qry.setParameter("startDate", startDate);
        qry.setParameter("endDate", startDate.plusSeconds(1));
        qry.setParameter("expected", "abcde12345");
        qry.setParameter("calculated", "abcde12345");
        qry.setParameter("result", checksumResultCode.name());
        qry.executeUpdate();
    }
}
