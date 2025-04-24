/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.checker.dao.impl;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Query;
import org.dspace.AbstractUnitTest;
import org.dspace.checker.ChecksumResultCode;
import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.CoreHelpers;
import org.dspace.core.HibernateDBConnection;
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
}
