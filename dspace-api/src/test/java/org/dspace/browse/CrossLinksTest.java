/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.browse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.dspace.AbstractDSpaceTest;
import org.dspace.services.ConfigurationService;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Test class for {@link CrossLinks}
 */
public class CrossLinksTest extends AbstractDSpaceTest {
    protected ConfigurationService configurationService;


    @Before
    public void setUp() {
      configurationService = new DSpace().getConfigurationService();
    }

    @Test
    public void testFindLinkType_Null() throws Exception {
        CrossLinks crossLinks = new CrossLinks();
        assertNull(crossLinks.findLinkType(null));
    }

    @Test
    public void testFindLinkType_NoMatch() throws Exception {
        CrossLinks crossLinks = new CrossLinks();
        String metadataField = "foo.bar.baz.does.not.exist";
        assertNull(crossLinks.findLinkType(metadataField));
    }

    @Test
    public void testFindLinkType_WildcardMatch() throws Exception {
        configurationService.setProperty("webui.browse.link.1", "author:dc.contributor.*");
        CrossLinks crossLinks = new CrossLinks();

        String metadataField = "dc.contributor.author";
        assertEquals("author",crossLinks.findLinkType(metadataField));
    }

    @Test
    public void testFindLinkType_SingleExactMatch_Author() throws Exception {
        configurationService.setProperty("webui.browse.link.1", "author:dc.contributor.author");
        CrossLinks crossLinks = new CrossLinks();

        assertEquals("type",crossLinks.findLinkType("dc.genre"));
        assertEquals("author",crossLinks.findLinkType("dc.contributor.author"));
    }

    @Test
    public void testFindLinkType_SingleExactMatch_Type() throws Exception {
        configurationService.setProperty("webui.browse.link.1", "type:dc.genre");
        CrossLinks crossLinks = new CrossLinks();

        assertEquals("type",crossLinks.findLinkType("dc.genre"));
    }

    @Test
    public void testFindLinkType_MultipleExactMatches_DifferentIndexes() throws Exception {
        configurationService.setProperty("webui.browse.link.1", "author:dc.contributor.author");
        configurationService.setProperty("webui.browse.link.2", "type:dc.genre");
        CrossLinks crossLinks = new CrossLinks();

        assertEquals("author",crossLinks.findLinkType("dc.contributor.author"));
        assertEquals("type",crossLinks.findLinkType("dc.genre"));
    }

    @Test
    public void testFindLinkType_MultipleWildcardMatches_DifferentIndexes() throws Exception {
        configurationService.setProperty("webui.browse.link.1", "author:dc.contributor.*");
        configurationService.setProperty("webui.browse.link.2", "subject:dc.subject.*");
        CrossLinks crossLinks = new CrossLinks();

        assertEquals("author",crossLinks.findLinkType("dc.contributor.author"));
        assertEquals("subject",crossLinks.findLinkType("dc.subject.lcsh"));
    }

    @Test
    public void testFindLinkType_MultiplExactAndWildcardMatches_DifferentIndexes() throws Exception {
        configurationService.setProperty("webui.browse.link.1", "author:dc.contributor.*");
        configurationService.setProperty("webui.browse.link.2", "subject:dc.subject.*");
        configurationService.setProperty("webui.browse.link.3", "type:dc.genre");
        configurationService.setProperty("webui.browse.link.4", "dateissued:dc.date.issued");
        CrossLinks crossLinks = new CrossLinks();

        assertEquals("author",crossLinks.findLinkType("dc.contributor.author"));
        assertEquals("subject",crossLinks.findLinkType("dc.subject.lcsh"));
        assertEquals("type",crossLinks.findLinkType("dc.genre"));
        assertEquals("dateissued",crossLinks.findLinkType("dc.date.issued"));
    }
}
