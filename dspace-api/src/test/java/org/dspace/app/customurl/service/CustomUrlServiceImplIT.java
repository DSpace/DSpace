/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.customurl.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.customurl.CustomUrlService;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchServiceException;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Comprehensive unit tests for {@link CustomUrlServiceImpl}.
 * Tests the core business logic methods with edge cases and error scenarios.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class CustomUrlServiceImplIT extends AbstractIntegrationTestWithDatabase {

    private final CustomUrlService customUrlService = new DSpace().getSingletonService(CustomUrlService.class);
    private final IndexingService indexingService = new DSpace().getSingletonService(IndexingService.class);
    private Collection collection;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        context.turnOffAuthorisationSystem();
        collection = CollectionBuilder.createCollection(context,
                                                        CommunityBuilder.createCommunity(context).build()).build();
        context.restoreAuthSystemState();
    }

    @Test
    public void testFindItemByCustomUrl() throws SQLException, SearchServiceException {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        String url = "test-publication";
        customUrlService.replaceCustomUrl(context, item, url);
        reindexItem(item);

        // Test Success
        Optional<Item> found = customUrlService.findItemByCustomUrl(context, url);
        assertTrue(found.isPresent());
        assertEquals(item.getID(), found.get().getID());

        // Test Non-Existent and Blank (Consolidated)
        assertFalse(customUrlService.findItemByCustomUrl(context, "non-existent").isPresent());
        assertFalse(customUrlService.findItemByCustomUrl(context, "").isPresent());
        assertFalse(customUrlService.findItemByCustomUrl(context, null).isPresent());
    }

    @Test(expected = IllegalStateException.class)
    public void testFindItemByCustomUrl_ThrowsExceptionOnDuplicates() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item1 = ItemBuilder.createItem(context, collection).build();
        Item item2 = ItemBuilder.createItem(context, collection).build();
        customUrlService.replaceCustomUrl(context, item1, "duplicate");
        customUrlService.replaceCustomUrl(context, item2, "duplicate");
        context.restoreAuthSystemState();
        reindexItem(item1);
        reindexItem(item2);

        customUrlService.findItemByCustomUrl(context, "duplicate");
    }

    @Test
    public void testGenerateUniqueCustomUrl_MixedScriptAndSpecialCharacters() {
        // Test mixed scripts and symbols (Preserves Latin and Numbers, strips others)
        String mixed = customUrlService.generateUniqueCustomUrl(context, "Study 2024: Étude 数据研究");
        assertEquals("study-2024-etude", mixed);

        String math = customUrlService.generateUniqueCustomUrl(context, "Math ∑∫∞ Analysis");
        assertEquals("math-analysis", math);

        String emoji = customUrlService.generateUniqueCustomUrl(context, "Climate Change 🌍🌡️");
        assertEquals("climate-change", emoji);
    }

    @Test
    public void testGenerateUniqueCustomUrl_UnicodeNormalization() {
        // Test accented character decomposition
        assertEquals("cafe-au-lait", customUrlService.generateUniqueCustomUrl(context, "Café au Lait"));
        assertEquals("manana-es-otro-dia", customUrlService.generateUniqueCustomUrl(context, "Mañana es otro día"));
        assertEquals("naive-approach", customUrlService.generateUniqueCustomUrl(context, "Naïve Approach"));
        assertEquals("resume-with-accents", customUrlService.generateUniqueCustomUrl(context, "Résumé with Accénts"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateUniqueCustomUrl_ThrowsExceptionOnNull() {
        customUrlService.generateUniqueCustomUrl(context, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateUniqueCustomUrl_ThrowsExceptionOnBlank() {
        customUrlService.generateUniqueCustomUrl(context, "   ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateUniqueCustomUrl_ThrowsExceptionOnPureNonLatin() {
        // This string contains ONLY characters that are stripped by [^a-z0-9]
        // Resulting slug is empty -> Should throw exception
        customUrlService.generateUniqueCustomUrl(context, "测试文章标题");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateUniqueCustomUrl_ThrowsExceptionOnPureSymbols() {
        // Resulting slug is empty -> Should throw exception
        customUrlService.generateUniqueCustomUrl(context, "∑∫∞±≤≥");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateUniqueCustomUrl_ThrowsExceptionOnPureEmoji() {
        customUrlService.generateUniqueCustomUrl(context, "🌍🌡️📊");
    }

    @Test(expected = IllegalStateException.class)
    public void testGenerateUniqueCustomUrl_ThrowsExceptionOnOverflow() throws Exception {
        context.turnOffAuthorisationSystem();

        String baseUrl = "overflow-test";

        // 1. Create the BASE item (forces the logic to look for suffixes)
        Item itemBase = ItemBuilder.createItem(context, collection)
                                   .build();
        customUrlService.replaceCustomUrl(context, itemBase, baseUrl);

        // 2. Create the MAX SUFFIX item (the highest possible valid URL)
        // URL will be: overflow-test-2147483647
        Item itemMax = ItemBuilder.createItem(context, collection)
                                  .build();
        customUrlService.replaceCustomUrl(context, itemMax, baseUrl + "-" + Integer.MAX_VALUE);

        context.restoreAuthSystemState();

        // 3. Sync Solr so the service can "see" these URLs
        reindexItem(itemBase);
        reindexItem(itemMax);

        // 4. Attempt generation
        // This calls findLatest -> gets MAX_VALUE -> tries to add +1 -> Throws Exception
        customUrlService.generateUniqueCustomUrl(context, baseUrl);
    }

    @Test
    public void testGenerateUniqueUrl_SuffixLogic() throws Exception {
        context.turnOffAuthorisationSystem();
        String base = "suffix-test-" + System.currentTimeMillis();
        Item item1 = ItemBuilder.createItem(context, collection).build();
        Item item2 = ItemBuilder.createItem(context, collection).build();

        // Occupy base and a non-sequential suffix (-3)
        customUrlService.replaceCustomUrl(context, item1, base);
        customUrlService.addOldCustomUrl(context, item2, base + "-3"); // Works with old URLs too
        context.restoreAuthSystemState();
        reindexItem(item1);
        reindexItem(item2);

        // Should return base-1 if base-1 is free, OR increment from highest?
        // Current logic: findLatest finds base-3, returns base-4.
        assertEquals(base + "-4", customUrlService.generateUniqueCustomUrl(context, base));
    }

    @Test
    public void testFindLatestCustomUrlByPattern() throws Exception {
        context.turnOffAuthorisationSystem();
        String base = "pattern.test"; // Test dot handling
        Item item = ItemBuilder.createItem(context, collection).build();
        customUrlService.replaceCustomUrl(context, item, base + "-10");
        context.restoreAuthSystemState();
        reindexItem(item);

        Optional<String> latest = customUrlService.findLatestCustomUrlByPattern(context, base);
        assertTrue(latest.isPresent());
        assertEquals(base + "-10", latest.get());

        // Blank/Null checks
        assertFalse(customUrlService.findLatestCustomUrlByPattern(context, "").isPresent());
    }

    @Test
    public void testDeleteAnyOldCustomUrlEqualsTo() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        customUrlService.addOldCustomUrl(context, item, "duplicate");
        customUrlService.addOldCustomUrl(context, item, "different");
        customUrlService.addOldCustomUrl(context, item, "duplicate");

        customUrlService.deleteAnyOldCustomUrlEqualsTo(context, item, "duplicate");

        List<String> remaining = customUrlService.getOldCustomUrls(item);
        assertEquals(1, remaining.size());
        assertEquals("different", remaining.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeleteOldCustomUrlByIndex_OutOfBounds() {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        customUrlService.addOldCustomUrl(context, item, "old-1");
        context.restoreAuthSystemState();

        customUrlService.deleteOldCustomUrlByIndex(context, item, 5);
    }

    @Test
    public void testUnicodeSearchAndPatternMatching() throws Exception {
        context.turnOffAuthorisationSystem();
        String base = customUrlService.generateUniqueCustomUrl(context, "Café Research 研究"); // "cafe-research"

        Item item1 = ItemBuilder.createItem(context, collection).build();
        Item item2 = ItemBuilder.createItem(context, collection).build();

        customUrlService.replaceCustomUrl(context, item1, base);
        customUrlService.replaceCustomUrl(context, item2, base + "-1");

        context.restoreAuthSystemState();
        reindexItem(item1);
        reindexItem(item2);

        // Verify retrieval
        assertTrue(customUrlService.findItemByCustomUrl(context, base).isPresent());

        // Verify pattern matching handles the normalized base
        Optional<String> latest = customUrlService.findLatestCustomUrlByPattern(context, base);
        assertEquals(base + "-1", latest.get());
    }


    private void reindexItem(Item item) throws SQLException, SearchServiceException {
        context.commit();
        indexingService.indexContent(context, new IndexableItem(item), true);
        indexingService.commit();
    }
}