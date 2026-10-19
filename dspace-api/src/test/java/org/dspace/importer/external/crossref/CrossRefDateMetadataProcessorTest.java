/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import static org.junit.Assert.assertEquals;

import java.util.Collection;

import org.junit.Test;

/**
 *
 * @author mwood
 */
public class CrossRefDateMetadataProcessorTest {
    /**
     * Test of processMetadata method, of class CrossRefDateMetadataProcessor.
     */
    @Test
    public void testProcessMetadata() {
        CrossRefDateMetadataProcessor unit = new CrossRefDateMetadataProcessor();
        unit.setPathToArray("/dates");
        Collection metadata = unit.processMetadata("{\"dates\": ["
                + "[1957, 1, 27],"
                + "[1957, 1],"
                + "[1957]"
                + "]}");
        String[] metadataValues = (String[]) metadata.toArray(new String[3]);
        assertEquals("[yyyy, MM, dd] should parse", "1957-01-27", metadataValues[0]);
        assertEquals("[yyyy, MM] should parse", "1957-01", metadataValues[1]);
        assertEquals("[yyyy] should parse", "1957", metadataValues[2]);
    }
}
