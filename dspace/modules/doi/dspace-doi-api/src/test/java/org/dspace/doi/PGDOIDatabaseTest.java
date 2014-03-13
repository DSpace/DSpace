/*
 */
package org.dspace.doi;

import org.dspace.core.ConfigurationManager;
import org.dspace.storage.rdbms.DatabaseManager;
import org.dspace.utils.DSpace;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class PGDOIDatabaseTest {
    private static PGDOIDatabase myPGDOIDatabase;
    private static String myRandomSuffix;
    @BeforeClass
    public static void setupBeforeClass() {
        myPGDOIDatabase = PGDOIDatabase.getInstance();
        int randomInt = (int) (Math.random() * 10000);
        myRandomSuffix = String.format("test-suffix-%d", randomInt);
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
        String url1 = "http://test-suffix.doi.org/1/" + myRandomSuffix;
        String url2 = "http://test-suffix.doi.org/2/" + myRandomSuffix;

        // Verify a DOI can be set
        DOI aDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffix, url1);
        DOI setDOI = myPGDOIDatabase.set(aDOI);
        assert aDOI.equals(setDOI);

        // Verify the DOI we set can be retrieved
        DOI getDOI = myPGDOIDatabase.getByDOI(aDOI.toString());
        assert aDOI.equals(getDOI);

        //Verify set also works to change the target of the DOI
        // change the target URL of the DOI
        DOI otherDOI = new DOI(PGDOIDatabase.internalTestingPrefix, myRandomSuffix, url2);
        // Update the DOI
        boolean put = myPGDOIDatabase.put(otherDOI);
        getDOI = myPGDOIDatabase.getByDOI(aDOI.toString());
        // The DOI internal identifiers should not be equal
        // even though they have the same prefix/suffix
        assert aDOI.getInternalIdentifier().equals(getDOI.getInternalIdentifier()) == false;
        assert otherDOI.equals(getDOI);
    }

    @Test
    public void testContains() {

    }

    public void testPut() {
        // Put updates a DOI.


    }

    public void testRemove() {

    }

    public void testGetByDOI() {

    }

    public void testGetByURL() {

    }

    public void testGetALL() {

    }

    public void testSize() {

    }

}
