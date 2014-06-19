/*
 */
package org.datadryad.journalstatistics.extractor;

import java.io.File;
import java.io.FileInputStream;
import org.datadryad.api.DryadDataFile;
import org.datadryad.test.ContextUnitTest;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author Dan Leehr <dan.leehr@nescent.org>
 */
public class DataFileTotalSizeTest extends ContextUnitTest {
    private File file1, file2, file3;

    @Before
    public void setUp() {
        super.setUp();
        try {
            file1 = new File(DataFileTotalSizeTest.class.getClassLoader().getResource("Logo-one-tone-dark.jpg").toURI());
            file2 = new File(DataFileTotalSizeTest.class.getClassLoader().getResource("world.js").toURI());
            file3 = new File(DataFileTotalSizeTest.class.getClassLoader().getResource("world.js").toURI());
        } catch (Exception ex) {
            fail("Exception setting up files for total size test " + ex);
        }
    }
    /**
     * Test of extract method, of class DataFileTotalSize.
     */
    @Test
    public void testCountDataFileTotalSize() throws Exception {
        System.out.println("countDataFileTotalSize");
        String journalName1 = "Test Journal 1";
        String journalName2 = "Test Journal 2";
        DataFileTotalSize instance = new DataFileTotalSize(context);

        // We will add some files to both journals and verify that only the
        // size for one journal is counted

        Long initialSizeForJournalName1 = instance.extract(journalName1);
        Long initialSizeForJournalName2 = instance.extract(journalName2);

        Long expectedSizeIncreaseForJournalName1 = file1.length() + file2.length();
        Long expectedSizeIncreaseForJournalName2 = file3.length();
        assertTrue("file1 and file2 have no size",expectedSizeIncreaseForJournalName1 > 0);
        assertTrue("file3 has no size", expectedSizeIncreaseForJournalName2 > 0);
        assertTrue("file1+file2 size should not equal file3 size", expectedSizeIncreaseForJournalName1 != expectedSizeIncreaseForJournalName2);

        // Create a file and associate it with journal 1

        DryadDataFile dataFile1 = DryadDataFile.create(context);
        dataFile1.setPublicationName(journalName1);
        dataFile1.addBitstream(new FileInputStream(file1));
        dataFile1.addBitstream(new FileInputStream(file2));

        // Create a second file, associate with journal2 and add file3

        DryadDataFile dataFile2 = DryadDataFile.create(context);
        dataFile2.setPublicationName(journalName2);
        dataFile2.addBitstream(new FileInputStream(file3));
        
        Long totalSizeForJournalName1 = instance.extract(journalName1);
        Long sizeIncrease1 = totalSizeForJournalName1 - initialSizeForJournalName1;
        assertEquals("journal name 1 size increase mismatch", expectedSizeIncreaseForJournalName1, sizeIncrease1);

        Long totalSizeForJournalName2 = instance.extract(journalName2);
        Long sizeIncrease2 = totalSizeForJournalName2 - initialSizeForJournalName2;
        assertEquals("journal name 2 size increase mismatch", expectedSizeIncreaseForJournalName2, sizeIncrease2);
    }
}
