/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.importer.external.crossref;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;

import org.junit.jupiter.api.Test;

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
        assertEquals("1957-01-27", metadataValues[0], "[yyyy, MM, dd] should parse");
        assertEquals("1957-01", metadataValues[1], "[yyyy, MM] should parse");
        assertEquals("1957", metadataValues[2], "[yyyy] should parse");
    }
}
