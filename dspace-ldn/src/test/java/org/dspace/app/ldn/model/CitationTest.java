/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.ldn.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 *
 */
public class CitationTest {

    @Test
    public void testCitation() {
        Citation citation = new Citation();

        citation.setIetfCiteAs("Test");
        assertEquals("Test", citation.getIetfCiteAs());

        Url url = new Url();
        url.setId("4af4d9d5-c5c4-464a-b310-f0124c191928");
        citation.setUrl(url);
        assertEquals(url, citation.getUrl());
    }

}
