/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A mock for the HarvestedCollectionService
 * @author Jelle Pelgrims (jelle.pelgrims at atmire.com)
 */
public class MockHarvestedCollectionServiceImpl extends HarvestedCollectionServiceImpl {

    @Override
    public List<String> verifyOAIharvester(String oaiSource,
                                           String oaiSetId, String metaPrefix, boolean testORE) {

        if (metaPrefix.equals("dc")) {
            return new ArrayList<>();
        } else {
            return Arrays.asList("(Mock error) Incorrect metadataConfigID");
        }
    }
}