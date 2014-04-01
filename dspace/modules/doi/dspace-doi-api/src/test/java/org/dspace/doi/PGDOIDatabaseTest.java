/*
 */
package org.dspace.doi;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
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
 * PGDOIDatabase.internalTestingPrefix for prefix
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PGDOIDatabaseTest {
    private static PGDOIDatabase myPGDOIDatabase;
    private static String myRandomSuffix, myRandomSuffixModified;
    private static String url1, url2;
    private static String myBaseURL = "http://test-suffix.doi.org";
    private static String myDumpFilePrefix = "PGDOIDatabaseTest";
    private static String myDumpFileSuffix = "txt";
    private File testOutputFile;
    private int numberOfDOIsToCreateConcurrently = 10000;
    private int numberOfTimers = 10;

    @BeforeClass
    public static void setupBeforeClass() {
        myPGDOIDatabase = PGDOIDatabase.getInstance();
        int randomInt = (int) (Math.random() * 10000);
        myRandomSuffix = String.format("test-suffix-%d", randomInt);
        myRandomSuffixModified = String.format("test-suffix-%d-modified", randomInt);
        url1 = myBaseURL + "/1/" + myRandomSuffix;
        url2 = myBaseURL + "/2/" + myRandomSuffix;
        // delete DOIs created by this class
        int removed = myPGDOIDatabase.removeTestDOIs();
        System.out.println("Removed " + removed + " test DOIs before running tests");
    }

    @AfterClass
    public static void teardownAfterClass() {
        // delete DOIs created by this class
        int removed = myPGDOIDatabase.removeTestDOIs();
        System.out.println("Removed " + removed + " test DOIs after running tests");
        myPGDOIDatabase.close();
    }

    @Test
    public void testSet() {
        // Verify a DOI can be set
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffix, url1);
        DOI setDOI = myPGDOIDatabase.set(aDOI);
        Assert.assertEquals(aDOI, setDOI);

        // Verify the DOI we set can be retrieved
        DOI getDOI = myPGDOIDatabase.getByDOI(aDOI.toString());
        Assert.assertEquals(aDOI, getDOI);

        //Verify set also works to change the target of the DOI
        // change the target URL of the DOI
        DOI otherDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffix, url2);
        // Update the DOI
        boolean put = myPGDOIDatabase.put(otherDOI);
        getDOI = myPGDOIDatabase.getByDOI(aDOI.toString());
        // The DOI internal identifiers should not be equal
        // even though they have the same prefix/suffix
        Assert.assertNotSame(aDOI.getInternalIdentifier(), getDOI.getInternalIdentifier());
        Assert.assertEquals(otherDOI, getDOI);
    }

    @Test
    public void testPutContainsRemove() {
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffixModified,url1);
        Assert.assertTrue(myPGDOIDatabase.put(aDOI));
        // make sure put was successful
        Assert.assertTrue(myPGDOIDatabase.contains(aDOI));
        Assert.assertTrue(myPGDOIDatabase.remove(aDOI));
    }

    @Test
    public void testGetByDOI() {
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffixModified,url1);
        Assert.assertTrue(myPGDOIDatabase.put(aDOI));
        // doi:10.5061/dryad.xxxxx
        String doiKey = "doi:" + PGDOIDatabase.internalTestingPrefix + '/' + myRandomSuffixModified;
        DOI byKey = myPGDOIDatabase.getByDOI(doiKey);
        Assert.assertEquals(byKey,aDOI);
    }

    @Test
    public void testGetByURL() {
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffixModified,url1);
        Assert.assertTrue(myPGDOIDatabase.put(aDOI));
        // doi:10.5061/dryad.xxxxx
        Set<DOI> DOIsbyURL = myPGDOIDatabase.getByURL(url1);
        Assert.assertTrue(DOIsbyURL.contains(aDOI));
    }

    @Test
    public void testGetALL() {
        DOI aDOI1 = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffix,url1);
        DOI aDOI2 = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffixModified,url2);
        Assert.assertTrue(myPGDOIDatabase.put(aDOI1));
        Assert.assertTrue(myPGDOIDatabase.put(aDOI2));
        Set<DOI> allDOIs = myPGDOIDatabase.getALL();
        Assert.assertFalse(allDOIs.isEmpty());
        Assert.assertTrue(allDOIs.contains(aDOI1));
        Assert.assertTrue(allDOIs.contains(aDOI2));
        Assert.assertTrue(allDOIs.size() >= 2);
    }

    @Test
    public void testSize() {
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffixModified,url1);
        Assert.assertTrue(myPGDOIDatabase.put(aDOI));
        int size = myPGDOIDatabase.size();
        Assert.assertTrue(size > 0);
    }

    @Before
    public void createTempDumpFile() {
        try {
            testOutputFile = File.createTempFile(myDumpFilePrefix, myDumpFileSuffix);
        } catch (IOException ex) {
            Assert.fail("Unable to create temp file: " + ex.toString());
        }
    }

    @Test
    public void testDump() {
        // put a DOI
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffixModified, url1);
        Assert.assertTrue(myPGDOIDatabase.put(aDOI));

        // Dump the database to a file output stream
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(testOutputFile);
        } catch (FileNotFoundException ex) {
            Assert.fail("Unable to open temp file for writing: " + ex.toString());
        }
        Assert.assertNotNull(fos);
        try {
            myPGDOIDatabase.dump(fos);
            fos.close();
        } catch (IOException ex) {
            Assert.fail("Unable to dump doi database: " + ex.toString());
        }

        // Make sure our put doi is in the file.
        FileReader fileReader = null;
        try {
            fileReader = new FileReader(testOutputFile);
        } catch (FileNotFoundException ex) {
            Assert.fail("Unable to open temp file for reading: " + ex.toString());
        }

        BufferedReader reader = new BufferedReader(fileReader);
        boolean foundDoiInDumpfile = false;
        try {
            while(reader.ready()) {
                String line = reader.readLine();
                if(
                        line.contains(PGDOIDatabase.internalTestingPrefix) &&
                        line.contains(myRandomSuffixModified) &&
                        line.contains(url1)
                        ) {
                    // line contains all three components of our DOI
                    foundDoiInDumpfile = true;
                    }
            }
        } catch (IOException ex) {
            Assert.fail("Unable to read lines from temp file: " + ex.toString());
        }
        Assert.assertTrue("DOI Not found in dump file", foundDoiInDumpfile);

    }

    @After
    public void removeTempDumpFile() {
        testOutputFile.delete();
    }

    public void testDumpTo() {
        Assert.fail("not implemented");
    }

    private synchronized void madeDOI() {
        numberOfDOIsToCreateConcurrently--;
    }
    private synchronized int getNumberOfDoisLeftToCreate() {
        return numberOfDOIsToCreateConcurrently;
    }

    private void putGetRemove(DOI aDOI, String url) {
        myPGDOIDatabase.put(aDOI);
        Set<DOI> DOIsbyURL = myPGDOIDatabase.getByURL(url);
        myPGDOIDatabase.remove(aDOI);
    }

    class UpdateTask extends TimerTask {

        @Override
        public void run() {
            // Guarantee collisions
            int randomInt = (int) (Math.random() * 100);
            String RandomSuffix = String.format("test-suffix-%d", randomInt);
            String url = myBaseURL + String.format("/%d", randomInt);
            DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, RandomSuffix, url);
            putGetRemove(aDOI, url);
            madeDOI();
        }
    }

    @Test
    public void testConcurrency() {
        Timer[] timers = new Timer[numberOfTimers];
        for(int i=0;i<numberOfTimers;i++) {
            timers[i] = new Timer("PGDOIConcurrencyTest-" + i);
        }
        // Keep track of how many are left to create.  Test fails if this number
        // doesn't keep going down.
        int last = getNumberOfDoisLeftToCreate();
        // Round-robin the tasks to the timers
        for(int i=0;i<last;i++) {
            int timerId = i % numberOfTimers;
            timers[timerId].schedule(new UpdateTask(), 0l);
        }
        while(getNumberOfDoisLeftToCreate() > 0) {
            try {
                Thread.sleep(5000l);
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
