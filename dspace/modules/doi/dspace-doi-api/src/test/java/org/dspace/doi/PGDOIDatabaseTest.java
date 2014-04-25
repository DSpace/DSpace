/*
 */
package org.dspace.doi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

/**
 * Test cases for DOI Database in Postgres.  All DOIs created use
 * PGDOIDatabase.INTERNAL_TESTING_PREFIX for prefix
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PGDOIDatabaseTest {
    private static final PGDOIDatabase PG_DOI_DATABASE = PGDOIDatabase.getInstance();
    private static final int RANDOM_INT = (int) (Math.random() * 10000);
    private static final String RANDOM_SUFFIX_ORIGINAL = String.format("test-suffix-%d", RANDOM_INT);
    private static final String RANDOM_SUFFIX_MODIFIED = String.format("test-suffix-%d-modified", RANDOM_INT);
    private static final String BASE_URL_STRING = "http://test-suffix.doi.org";
    private static final String DOI_URL_STRING_1 = BASE_URL_STRING + "/1/" + RANDOM_SUFFIX_ORIGINAL;
    private static final String DOI_URL_STRING_2 = BASE_URL_STRING + "/2/" + RANDOM_SUFFIX_ORIGINAL;
    private static final String DUMP_FILE_PREFIX = "PGDOIDatabaseTest";
    private static final String DUMP_FILE_SUFFIX = "txt";
    private static final int NUM_CONCURRENT_TIMERS = 10;
    private File testOutputFile;
    private int numberOfDOIsToCreateConcurrently = 10000;

    @BeforeClass
    public static void setupBeforeClass() {
        // delete DOIs created by this class
        int removed = PG_DOI_DATABASE.removeTestDOIs();
        System.out.println("Removed " + removed + " test DOIs before running tests");
    }

    @AfterClass
    public static void teardownAfterClass() {
        // delete DOIs created by this class
        int removed = PG_DOI_DATABASE.removeTestDOIs();
        System.out.println("Removed " + removed + " test DOIs after running tests");
        PG_DOI_DATABASE.close();
    }

    @Test
    public void testSet() {
        // Verify a DOI can be set
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_ORIGINAL, DOI_URL_STRING_1);
        DOI setDOI = PG_DOI_DATABASE.set(aDOI);
        Assert.assertEquals(aDOI, setDOI);

        // Verify the DOI we set can be retrieved
        DOI getDOI = PG_DOI_DATABASE.getByDOI(aDOI.toString());
        Assert.assertEquals(aDOI, getDOI);

        //Verify set also works to change the target of the DOI
        // change the target URL of the DOI
        DOI otherDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_ORIGINAL, DOI_URL_STRING_2);
        // Update the DOI
        boolean put = PG_DOI_DATABASE.put(otherDOI);
        getDOI = PG_DOI_DATABASE.getByDOI(aDOI.toString());
        // The DOI internal identifiers should not be equal
        // even though they have the same prefix/suffix
        Assert.assertNotSame(aDOI.getInternalIdentifier(), getDOI.getInternalIdentifier());
        Assert.assertEquals(otherDOI, getDOI);
    }

    @Test
    public void testPutContainsRemove() {
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED,DOI_URL_STRING_1);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI));
        // make sure put was successful
        Assert.assertTrue(PG_DOI_DATABASE.contains(aDOI));
        Assert.assertTrue(PG_DOI_DATABASE.remove(aDOI));
    }

    @Test
    public void testGetByDOI() {
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED,DOI_URL_STRING_1);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI));
        // doi:10.5061/dryad.xxxxx
        String doiKey = "doi:" + PGDOIDatabase.INTERNAL_TESTING_PREFIX + '/' + RANDOM_SUFFIX_MODIFIED;
        DOI byKey = PG_DOI_DATABASE.getByDOI(doiKey);
        Assert.assertEquals(byKey,aDOI);
    }

    @Test
    public void testGetByURL() {
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED,DOI_URL_STRING_1);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI));
        // doi:10.5061/dryad.xxxxx
        Set<DOI> DOIsbyURL = PG_DOI_DATABASE.getByURL(DOI_URL_STRING_1);
        Assert.assertTrue(DOIsbyURL.contains(aDOI));
    }

    @Test
    public void testGetALL() {
        DOI aDOI1 = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_ORIGINAL,DOI_URL_STRING_1);
        DOI aDOI2 = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED,DOI_URL_STRING_2);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI1));
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI2));
        Set<DOI> allDOIs = PG_DOI_DATABASE.getALL();
        Assert.assertFalse(allDOIs.isEmpty());
        Assert.assertTrue(allDOIs.contains(aDOI1));
        Assert.assertTrue(allDOIs.contains(aDOI2));
        Assert.assertTrue(allDOIs.size() >= 2);
    }

    @Test
    public void testSize() {
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED,DOI_URL_STRING_1);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI));
        int size = PG_DOI_DATABASE.size();
        Assert.assertTrue(size > 0);
    }

    // Returns true if testOutputFile contains a DOI with
    // prefix INTERNAL_TESTING_PREFIX and suffix RANDOM_SUFFIX_MODIFIED
    // Shared by testDump and testDumpTo
    private boolean verifyDOIInFile() {
        // Make sure our put doi is in the file.
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(testOutputFile);
        } catch (FileNotFoundException ex) {
            Assert.fail("Unable to open temp file for reading: " + ex.toString());
        }

        BufferedReader reader = new BufferedReader(fileReader);
        boolean foundDoiInFile = false;
        try {
            while(reader.ready()) {
                String line = reader.readLine();
                if(
                        line.contains(PGDOIDatabase.INTERNAL_TESTING_PREFIX) &&
                        line.contains(RANDOM_SUFFIX_MODIFIED)
                        ) {
                    // verify line contains prefix and suffix components of our DOI
                    // dump() uses getTargetURL, which differs from our internal
                    // url, so we don't test that.
                    foundDoiInFile = true;
                    }
            }
        } catch (IOException ex) {
            Assert.fail("Unable to read lines from temp file: " + ex.toString());
        }
        return foundDoiInFile;
    }

    @Before
    public void createTempDumpFile() {
        try {
            testOutputFile = File.createTempFile(DUMP_FILE_PREFIX, DUMP_FILE_SUFFIX);
        } catch (IOException ex) {
            Assert.fail("Unable to create temp file: " + ex.toString());
        }
    }

    @Test
    public void testDump() {
        // put a DOI
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED, DOI_URL_STRING_1);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI));

        // Dump the database to a file output stream
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(testOutputFile);
        } catch (FileNotFoundException ex) {
            Assert.fail("Unable to open temp file for writing: " + ex.toString());
        }
        Assert.assertNotNull(fos);
        try {
            PG_DOI_DATABASE.dump(fos);
            fos.close();
        } catch (IOException ex) {
            Assert.fail("Unable to dump doi database: " + ex.toString());
        }

        boolean foundDoiInDumpfile = verifyDOIInFile();
        Assert.assertTrue("DOI Not found in dump file", foundDoiInDumpfile);
    }

    @After
    public void removeTempDumpFile() {
        testOutputFile.delete();
    }

    @Before
    public void createTempDumpToFile() {
        try {
            testOutputFile = File.createTempFile(DUMP_FILE_PREFIX, DUMP_FILE_SUFFIX);
        } catch (IOException ex) {
            Assert.fail("Unable to create temp file: " + ex.toString());
        }
    }

    // Very similar to dump, but uses a FileWriter instead of an output stream
    @Test
    public void testDumpTo() {
        // put a DOI
        DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RANDOM_SUFFIX_MODIFIED, DOI_URL_STRING_1);
        Assert.assertTrue(PG_DOI_DATABASE.put(aDOI));

        // Dump the database to a file writer
        FileWriter writer = null;
        try {
            writer = new FileWriter(testOutputFile);
        } catch (IOException ex) {
            Assert.fail("Unable to open temp file for writing: " + ex.toString());
        }
        Assert.assertNotNull(writer);
        try {
            PG_DOI_DATABASE.dumpTo(writer);
            writer.close();
        } catch (IOException ex) {
            Assert.fail("Unable to dumpTo doi database: " + ex.toString());
        }

        boolean foundDoiInDumpToFile = verifyDOIInFile();
        Assert.assertTrue("DOI Not found in dump file", foundDoiInDumpToFile);    }

    @After
    public void removeTempDumpToFile() {
        testOutputFile.delete();
    }

    private synchronized void madeDOI() {
        numberOfDOIsToCreateConcurrently--;
    }
    private synchronized int getNumberOfDoisLeftToCreate() {
        return numberOfDOIsToCreateConcurrently;
    }

    private void putGetRemove(DOI aDOI, String url) {
        PG_DOI_DATABASE.put(aDOI);
        Set<DOI> DOIsbyURL = PG_DOI_DATABASE.getByURL(url);
        PG_DOI_DATABASE.remove(aDOI);
    }

    class UpdateTask extends TimerTask {

        @Override
        public void run() {
            // Guarantee collisions
            int randomInt = (int) (Math.random() * 100);
            String RandomSuffix = String.format("test-suffix-%d", randomInt);
            String url = BASE_URL_STRING + String.format("/%d", randomInt);
            DOI aDOI = new DOI(PGDOIDatabase.INTERNAL_TESTING_PREFIX, RandomSuffix, url);
            putGetRemove(aDOI, url);
            madeDOI();
        }
    }

    @Test
    public void testConcurrency() {
        Timer[] timers = new Timer[NUM_CONCURRENT_TIMERS];
        for(int i=0;i<NUM_CONCURRENT_TIMERS;i++) {
            timers[i] = new Timer("PGDOIConcurrencyTest-" + i);
        }
        // Keep track of how many are left to create.  Test fails if this number
        // doesn't keep going down.
        int last = getNumberOfDoisLeftToCreate();
        // Round-robin the tasks to the timers
        for(int i=0;i<last;i++) {
            int timerId = i % NUM_CONCURRENT_TIMERS;
            timers[timerId].schedule(new UpdateTask(), 0l);
        }
        // This operation continues on main thread while timer threads
        // do work.
        while(getNumberOfDoisLeftToCreate() > 0) {
            try {
                // Periodically check how many are left before finishing test
                Thread.sleep(1000l);
                int left = getNumberOfDoisLeftToCreate();
                if(last == left) {
                    Assert.fail("DB is locked up with " + left + " DOIs left");
                }
                last = getNumberOfDoisLeftToCreate();
                System.out.println("Waiting with " + last + " left");
            } catch (InterruptedException ex) {
                Assert.fail("Interrupted during concurrency");
            }
        }
    }
}
