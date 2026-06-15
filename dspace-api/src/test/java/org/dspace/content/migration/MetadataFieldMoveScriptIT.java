/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.launcher.ScriptLauncher;
import org.dspace.app.scripts.handler.impl.TestDSpaceRunnableHandler;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.builder.MetadataSchemaBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.content.service.MetadataSchemaService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration test for {@link MetadataFieldMoveScript}.
 *
 * <p>Source/target values are created and read back through short-lived helper contexts so the
 * shared test context never caches the metadata rows the script mutates on its own context.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class MetadataFieldMoveScriptIT extends AbstractIntegrationTestWithDatabase {

    private ItemService itemService;
    private MetadataFieldService metadataFieldService;
    private MetadataSchemaService metadataSchemaService;

    private Collection collection;

    @Before
    public void setup() throws Exception {
        itemService = ContentServiceFactory.getInstance().getItemService();
        metadataFieldService = ContentServiceFactory.getInstance().getMetadataFieldService();
        metadataSchemaService = ContentServiceFactory.getInstance().getMetadataSchemaService();

        context.turnOffAuthorisationSystem();
        Community community = CommunityBuilder.createCommunity(context).withName("Community").build();
        collection = CollectionBuilder.createCollection(context, community).withName("Collection").build();
        context.restoreAuthSystemState();
    }

    /**
     * Exact (non-regex) move: dc.relation -&gt; dc.relation.project. Values move to the target and the
     * source field is emptied; value and place are preserved (target was empty).
     */
    @Test
    public void testExactMove() throws Exception {
        UUID itemId = createItemWithFields("Exact Move", "mvexact.source", "mvexact.target");
        addValues(itemId, "mvexact", "source", null, "Resource A", "Resource B");

        TestDSpaceRunnableHandler handler = runMove("^mvexact\\.source$", "mvexact.target");
        assertThat(handler.getErrorMessages(), empty());

        assertThat(read(itemId, "mvexact.source"), empty());
        List<MetadataValue> moved = read(itemId, "mvexact.target");
        moved.sort(Comparator.comparingInt(MetadataValue::getPlace));
        assertThat(moved, hasSize(2));
        assertEquals("Resource A", moved.get(0).getValue());
        assertEquals(0, moved.get(0).getPlace());
        assertEquals("Resource B", moved.get(1).getValue());
        assertEquals(1, moved.get(1).getPlace());
    }

    /**
     * Regex move with a backreference: src.alpha -&gt; dst.alpha via {@code -s '^src\.(.+)$' -t 'dst.$1'}.
     * Custom schemas are used so the regex only matches the test field.
     */
    @Test
    public void testRegexMove() throws Exception {
        UUID itemId = createItemWithFields("Regex Move", "mvregsrc.alpha", "mvregdst.alpha");
        addValues(itemId, "mvregsrc", "alpha", null, "Regex Value");

        TestDSpaceRunnableHandler handler = runMove("^mvregsrc\\.(.+)$", "mvregdst.$1");
        assertThat(handler.getErrorMessages(), empty());

        assertThat(read(itemId, "mvregsrc.alpha"), empty());
        List<MetadataValue> moved = read(itemId, "mvregdst.alpha");
        assertThat(moved, hasSize(1));
        assertEquals("Regex Value", moved.get(0).getValue());
    }

    /**
     * Skip exact duplicates: a source value identical (value+authority+language) to an existing target
     * value is dropped, leaving a single value on the target.
     */
    @Test
    public void testSkipExactDuplicate() throws Exception {
        UUID itemId = createItemWithFields("Skip Duplicate", "mvdup.source", "mvdup.target");
        addValues(itemId, "mvdup", "target", null, "Same Value");
        addValues(itemId, "mvdup", "source", null, "Same Value");

        TestDSpaceRunnableHandler handler = runMove("^mvdup\\.source$", "mvdup.target");
        assertThat(handler.getErrorMessages(), empty());

        assertThat(read(itemId, "mvdup.source"), empty());
        List<MetadataValue> target = read(itemId, "mvdup.target");
        assertThat("Exact duplicate must not be added again", target, hasSize(1));
        assertEquals("Same Value", target.get(0).getValue());
    }

    /**
     * Append on a populated target: when the target already has values, moved values are appended after
     * the highest existing place rather than colliding with it.
     */
    @Test
    public void testAppendOnPopulatedTarget() throws Exception {
        UUID itemId = createItemWithFields("Append", "mvapp.source", "mvapp.target");
        addValues(itemId, "mvapp", "target", null, "Existing 0", "Existing 1");
        addValues(itemId, "mvapp", "source", null, "Moved");

        TestDSpaceRunnableHandler handler = runMove("^mvapp\\.source$", "mvapp.target");
        assertThat(handler.getErrorMessages(), empty());

        assertThat(read(itemId, "mvapp.source"), empty());
        List<MetadataValue> target = read(itemId, "mvapp.target");
        target.sort(Comparator.comparingInt(MetadataValue::getPlace));
        assertThat(target, hasSize(3));
        assertEquals("Existing 0", target.get(0).getValue());
        assertEquals("Existing 1", target.get(1).getValue());
        assertEquals("Moved", target.get(2).getValue());
        assertThat("Moved value must be appended after the existing maximum place",
            target.get(2).getPlace(), greaterThan(target.get(1).getPlace()));
    }

    /**
     * Missing target field: the run must abort with an error and make no changes.
     */
    @Test
    public void testMissingTargetFails() throws Exception {
        UUID itemId = createItemWithFields("Missing Target", "mvmiss.foo", null);
        addValues(itemId, "mvmiss", "foo", null, "Untouched");

        TestDSpaceRunnableHandler handler = runMove("^mvmiss\\.foo$", "ghostschema.bar");
        assertThat("A missing target must produce an error", handler.getErrorMessages(), not(empty()));

        List<MetadataValue> source = read(itemId, "mvmiss.foo");
        assertThat("No changes must be made when a target is missing", source, hasSize(1));
        assertEquals("Untouched", source.get(0).getValue());
    }

    /**
     * Collision: two distinct source fields resolving to the same target must abort with no changes.
     */
    @Test
    public void testDuplicateTargetCollisionFails() throws Exception {
        UUID itemId = createItemWithFields("Collision", "mvcoll.a", "mvcolltgt.merged");
        context.turnOffAuthorisationSystem();
        ensureField("mvcoll", "b", null);
        context.commit();
        context.restoreAuthSystemState();
        addValues(itemId, "mvcoll", "a", null, "Value A");
        addValues(itemId, "mvcoll", "b", null, "Value B");

        TestDSpaceRunnableHandler handler = runMove("^mvcoll\\.(?:a|b)$", "mvcolltgt.merged");
        assertThat("A target collision must produce an error", handler.getErrorMessages(), not(empty()));
        assertNotNull("A target collision must raise an exception", handler.getException());

        assertThat(read(itemId, "mvcoll.a"), hasSize(1));
        assertThat(read(itemId, "mvcoll.b"), hasSize(1));
        assertThat(read(itemId, "mvcolltgt.merged"), empty());
    }

    /**
     * Dry run: resolved pairs and counts are reported but nothing is committed.
     */
    @Test
    public void testDryRunMakesNoChanges() throws Exception {
        UUID itemId = createItemWithFields("Dry Run", "mvdry.source", "mvdry.target");
        addValues(itemId, "mvdry", "source", null, "Dry Value");

        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "metadata-field-move", "-s", "^mvdry\\.source$", "-t", "mvdry.target", "-n"
        };
        context.uncacheEntities();
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);
        assertThat(handler.getErrorMessages(), empty());

        boolean reportedPair = handler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("mvdry.source") && msg.contains("mvdry.target"));
        assertThat("Dry run should report the resolved field pair", reportedPair, is(true));
        boolean reportedDryRun = handler.getInfoMessages().stream()
            .anyMatch(msg -> msg.contains("DRY RUN"));
        assertThat("Dry run should announce that nothing is committed", reportedDryRun, is(true));

        assertThat(read(itemId, "mvdry.source"), hasSize(1));
        assertThat(read(itemId, "mvdry.target"), empty());
    }

    /**
     * Create an item and ensure the given source/target fields are registered, committing everything so
     * the script (and the helper contexts) see it.
     *
     * @param title       the item title
     * @param sourceField canonical source field name to register
     * @param targetField canonical target field name to register, or {@code null} to skip
     * @return the new item's id
     */
    private UUID createItemWithFields(String title, String sourceField, String targetField) throws Exception {
        context.turnOffAuthorisationSystem();
        ensureFieldFromCanonical(sourceField);
        if (targetField != null) {
            ensureFieldFromCanonical(targetField);
        }
        Item item = ItemBuilder.createItem(context, collection).withTitle(title).build();
        context.commit();
        context.restoreAuthSystemState();
        return item.getID();
    }

    private void ensureFieldFromCanonical(String canonical) throws Exception {
        String[] parts = canonical.split("\\.");
        String schema = parts[0];
        String element = parts[1];
        String qualifier = parts.length == 3 ? parts[2] : null;
        ensureSchema(schema);
        ensureField(schema, element, qualifier);
    }

    /**
     * Add stored metadata values to an item using a dedicated context so the shared test context does
     * not cache them.
     */
    private void addValues(UUID itemId, String schema, String element, String qualifier, String... values)
        throws Exception {
        try (Context c = new Context()) {
            c.turnOffAuthorisationSystem();
            Item item = itemService.find(c, itemId);
            for (String value : values) {
                itemService.addMetadata(c, item, schema, element, qualifier, null, value);
            }
            itemService.update(c, item);
            c.commit();
            // Evict the item and its values so the thread-bound Hibernate session does not keep a
            // managed reference to rows the script will later move/delete on its own context.
            for (MetadataValue mv : item.getMetadata()) {
                c.uncacheEntity(mv);
            }
            c.uncacheEntity(item);
            c.complete();
        }
    }

    /**
     * Read the stored values of a field for an item using a fresh context (sees committed DB state).
     */
    private List<MetadataValue> read(UUID itemId, String field) throws Exception {
        try (Context c = new Context()) {
            // Clear the thread-bound Hibernate session so a value the script deleted on another context
            // does not linger as a stale managed reference and break the read with a cascade flush.
            c.uncacheEntities();
            Item item = itemService.find(c, itemId);
            return itemService.getMetadataByMetadataString(item, field);
        }
    }

    private TestDSpaceRunnableHandler runMove(String sourceRegex, String targetTemplate) throws Exception {
        // Detach all setup entities from the thread-bound session so the script (sharing the session)
        // does not flush a managed item that still references rows it deletes.
        context.uncacheEntities();
        TestDSpaceRunnableHandler handler = new TestDSpaceRunnableHandler();
        String[] args = new String[] {
            "metadata-field-move", "-s", sourceRegex, "-t", targetTemplate
        };
        ScriptLauncher.handleScript(args, ScriptLauncher.getConfig(kernelImpl), handler, kernelImpl);
        return handler;
    }

    private void ensureSchema(String name) throws Exception {
        if (metadataSchemaService.find(context, name) == null) {
            MetadataSchemaBuilder.createMetadataSchema(context, name, "http://dspace.org/mvtest/" + name).build();
        }
    }

    private MetadataField ensureField(String schema, String element, String qualifier) throws Exception {
        String key = schema + "." + element + (qualifier != null ? "." + qualifier : "");
        MetadataField existing = metadataFieldService.findByString(context, key, '.');
        if (existing != null) {
            return existing;
        }
        MetadataSchema mdSchema = metadataSchemaService.find(context, schema);
        return MetadataFieldBuilder.createMetadataField(context, mdSchema, element, qualifier, "test field").build();
    }
}
