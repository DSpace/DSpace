/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model.hateoas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.ArrayList;
import java.util.Map;

import org.junit.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public class EmbeddedPageHeaderTest {

    @Test
    public void testGetPageInfoOnePage() throws Exception {
        // Initialize a dummy page request for page 1
        PageRequest pageRequest = PageRequest.of(1, 20);
        // Initialize with a total of 10 elements
        Page page = new PageImpl<>(new ArrayList<>(), pageRequest, 20);
        // Initialize our EmbeddedPageHeader for testing below
        EmbeddedPageHeader embeddedPageHeader = new EmbeddedPageHeader("http://mydspace/", page, true);

        Map<String,Long> pageInfo = embeddedPageHeader.getPageInfo();
        // Parsed page info should match what we assigned above.
        assertEquals(1, pageInfo.get("number").intValue());
        assertEquals(20, pageInfo.get("size").intValue());
        assertEquals(1, pageInfo.get("totalPages").intValue());
        assertEquals(20, pageInfo.get("totalElements").intValue());
    }

    @Test
    public void testGetPageInfoTotalUnknown() throws Exception {
        // Initialize a dummy page request for page 1, zero page size (should result in one page)
        PageRequest pageRequest = PageRequest.of(1, 20);
        // Initialize with a total of 10 elements
        Page page = new PageImpl<>(new ArrayList<>(), pageRequest, 20);
        // Initialize our EmbeddedPageHeader for testing below, specifying total elements known = false
        EmbeddedPageHeader embeddedPageHeader = new EmbeddedPageHeader("http://mydspace/", page, false);

        Map<String,Long> pageInfo = embeddedPageHeader.getPageInfo();
        // Parsed page info should match what we assigned above.
        assertEquals(1, pageInfo.get("number").intValue());
        assertEquals(20, pageInfo.get("size").intValue());
        // There should not be a totalPages or totalElements included (as total elements was unknown)
        assertFalse(pageInfo.containsKey("totalPages"));
        assertFalse(pageInfo.containsKey("totalElements"));
    }

    @Test
    public void testGetPageInfoMultiplePages() throws Exception {
        // Initialize a dummy page request for page 1, with 10 elements returned
        PageRequest pageRequest = PageRequest.of(1, 10);
        // Initialize with a total of 50 elements (will result in 5 pages of 10 elements each)
        Page page = new PageImpl<>(new ArrayList<>(), pageRequest, 50);
        // Initialize our EmbeddedPageHeader for testing below
        EmbeddedPageHeader embeddedPageHeader = new EmbeddedPageHeader("http://mydspace/", page, true);

        Map<String,Long> pageInfo = embeddedPageHeader.getPageInfo();
        // Parsed page info should match what we assigned above.
        assertEquals(1, pageInfo.get("number").intValue());
        assertEquals(10, pageInfo.get("size").intValue());
        assertEquals(5, pageInfo.get("totalPages").intValue());
        assertEquals(50, pageInfo.get("totalElements").intValue());
    }

    @Test
    public void testGetLinksOnFirstPage() throws Exception {
        // First page is the default, so the URL should include no params
        String dspaceURL = "http://mydspace/server";

        // Initialize a dummy page request for page 0 (which is the first page), with 10 elements returned
        PageRequest pageRequest = PageRequest.of(0, 10);
        // Initialize with a total of 50 elements (will result in 5 pages of 10 elements each)
        Page page = new PageImpl<>(new ArrayList<>(), pageRequest, 50);
        // Initialize our EmbeddedPageHeader for testing below
        EmbeddedPageHeader embeddedPageHeader = new EmbeddedPageHeader(dspaceURL, page, true);

        Map<String,Object> links = embeddedPageHeader.getLinks();
        // "self" should be same as URL
        assertEquals(dspaceURL + "?size=10", ((EmbeddedPageHeader.Href) links.get("self")).getHref());
        // "first" should not exist, as we are on the first page.
        assertFalse(links.containsKey("first"));
        // "prev" should not exist, as we are on the first page.
        assertFalse(links.containsKey("prev"));
        // "next" should be page 1 (which is second page)
        assertEquals(dspaceURL + "?page=1&size=10", ((EmbeddedPageHeader.Href) links.get("next")).getHref());
        // "last" should be page 4 (which is fifth page)
        assertEquals(dspaceURL + "?page=4&size=10", ((EmbeddedPageHeader.Href) links.get("last")).getHref());
    }

    @Test
    public void testGetLinksOnSecondPage() throws Exception {
        String dspaceBaseURL = "http://mydspace/server";
        // On second page, URL will include page/size query params
        String dspaceURL = dspaceBaseURL + "?page=1&size=10";

        // Initialize a page request matching the URL above:
        // page = 1 (second page), 10 elements per page
        PageRequest pageRequest = PageRequest.of(1, 10);
        // Initialize with a total of 50 elements (will result in 5 pages of 10 elements each)
        Page page = new PageImpl<>(new ArrayList<>(), pageRequest, 50);
        // Initialize our EmbeddedPageHeader for testing below
        EmbeddedPageHeader embeddedPageHeader = new EmbeddedPageHeader(dspaceURL, page, true);

        Map<String,Object> links = embeddedPageHeader.getLinks();
        // "self" should be same as URL
        assertEquals(dspaceURL, ((EmbeddedPageHeader.Href) links.get("self")).getHref());
        // "first" should be page 0 (which is first page)
        assertEquals(dspaceBaseURL + "?page=0&size=10", ((EmbeddedPageHeader.Href) links.get("first")).getHref());
        // "prev" should be page 0 (which is first page)
        assertEquals(dspaceBaseURL + "?page=0&size=10", ((EmbeddedPageHeader.Href) links.get("prev")).getHref());
        // "next" should be page 2
        assertEquals(dspaceBaseURL + "?page=2&size=10", ((EmbeddedPageHeader.Href) links.get("next")).getHref());
        // "last" should be page 4 (which is fifth page)
        assertEquals(dspaceBaseURL + "?page=4&size=10", ((EmbeddedPageHeader.Href) links.get("last")).getHref());
    }

    @Test
    public void testGetLinksOnLastPage() throws Exception {
        String dspaceBaseURL = "http://mydspace/server";
        // On last page, URL will include page/size query params
        String dspaceURL = dspaceBaseURL + "?page=4&size=10";

        // Initialize a page request matching the URL above:
        // page = 4 (fifth page), 10 elements per page
        PageRequest pageRequest = PageRequest.of(4, 10);
        // Initialize with a total of 50 elements (will result in 5 pages of 10 elements each)
        Page page = new PageImpl<>(new ArrayList<>(), pageRequest, 50);
        // Initialize our EmbeddedPageHeader for testing below
        EmbeddedPageHeader embeddedPageHeader = new EmbeddedPageHeader(dspaceURL, page, true);

        Map<String,Object> links = embeddedPageHeader.getLinks();
        // "self" should be same as current URL
        assertEquals(dspaceURL, ((EmbeddedPageHeader.Href) links.get("self")).getHref());
        // "first" should be page 0 (which is first page)
        assertEquals(dspaceBaseURL + "?page=0&size=10", ((EmbeddedPageHeader.Href) links.get("first")).getHref());
        // "prev" should be page 3 (which is fourth page)
        assertEquals(dspaceBaseURL + "?page=3&size=10", ((EmbeddedPageHeader.Href) links.get("prev")).getHref());
        // "next" should not exist, as we are on the last page.
        assertFalse(links.containsKey("next"));
        // "last" should not exist, as we are on the last page.
        assertFalse(links.containsKey("last"));
    }
}
