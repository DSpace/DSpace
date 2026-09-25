/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.authority;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.authority.factory.ContentAuthorityServiceFactory;
import org.dspace.content.authority.service.AuthorityBackedRelationshipService;
import org.dspace.content.authority.service.ChoiceAuthorityService;
import org.dspace.content.authority.service.MetadataAuthorityService;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.dspace.discovery.DiscoverQuery;
import org.dspace.discovery.DiscoverResult;
import org.dspace.discovery.IndexableObject;
import org.dspace.discovery.IndexingService;
import org.dspace.discovery.SearchService;
import org.dspace.discovery.SearchUtils;
import org.dspace.discovery.indexobject.IndexableItem;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Once a resolved internal reference has minted its relationship row and elided the raw authority column,
 * every reader that consumes {@link MetadataValue#getAuthority()} must still see the related item's UUID
 * even though the column ({@link MetadataValue#getRawAuthority()}) is physically null — the getter
 * reconstitutes the UUID from the relationship's {@code right_id}.
 *
 * The four tests cover representative points on that read surface:
 * <ul>
 *   <li>the derived getter itself, stable across an evict-and-reload straight from the database
 *       (proving the null column was persisted, not just cached);</li>
 *   <li>the CSV export round-trip, a reader that funnels through {@code getAuthority()};</li>
 *   <li>Solr {@code author_authority} indexing, which feeds the person&rarr;publications listing;</li>
 *   <li>delete-time authority cleanup, which resolves the elided UUID to detach references before
 *       the deleted item's relationship rows are torn down.</li>
 * </ul>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ElidedAuthorityReadSurfaceIT extends AbstractIntegrationTestWithDatabase {

    private AuthorityBackedRelationshipService authorityBackedRelationshipService;
    private ItemService itemService;
    private SearchService searchService;
    private RelationshipService relationshipService;

    private final ConfigurationService configurationService =
        DSpaceServicesFactory.getInstance().getConfigurationService();
    private final MetadataAuthorityService metadataAuthorityService =
        ContentAuthorityServiceFactory.getInstance().getMetadataAuthorityService();
    private final ChoiceAuthorityService choiceAuthorityService =
        ContentAuthorityServiceFactory.getInstance().getChoiceAuthorityService();
    private final PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();
    private final IndexingService indexingService = DSpaceServicesFactory.getInstance().getServiceManager()
        .getServiceByName(IndexingService.class.getName(), IndexingService.class);

    private Collection collection;
    private Item owner;
    private Item target;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();

        authorityBackedRelationshipService = new DSpace().getServiceManager()
            .getServiceByName(AuthorityBackedRelationshipServiceImpl.class.getCanonicalName(),
                AuthorityBackedRelationshipService.class);
        itemService = ContentServiceFactory.getInstance().getItemService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        searchService = SearchUtils.getSearchService();

        // author_authority is only indexed when dc.contributor.author is authority controlled.
        configurationService.setProperty("authority.controlled.dc.contributor.author", "true");
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context)
            .withName("community")
            .build();

        collection = CollectionBuilder.createCollection(context, community)
            .withName("collection")
            .build();

        owner = ItemBuilder.createItem(context, collection)
            .withTitle("owner publication")
            .withAuthor("Smith, John")
            .build();

        target = ItemBuilder.createItem(context, collection)
            .withTitle("target person")
            .build();

        context.restoreAuthSystemState();
    }

    @Override
    public void destroy() throws Exception {
        configurationService.setProperty("authority.controlled.dc.contributor.author", null);
        metadataAuthorityService.clearCache();
        super.destroy();
    }

    @Test
    public void testCsvExportSurfacesTheElidedUuidThroughThePublicGetter() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
        authorityBackedRelationshipService.markRelationshipForResolvedAuthority(context, owner, authorValue, target);
        itemService.update(context, owner);
        context.commit();

        // Evict and reload from the DB: the raw column is persisted null, yet getAuthority()
        // reconstitutes the target UUID from the relationship's right_id.
        context.uncacheEntity(owner);
        owner = context.reloadEntity(owner);
        MetadataValue reloaded = itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
        assertThat(reloaded.getRawAuthority(), nullValue());
        assertThat(reloaded.getAuthority(), equalTo(target.getID().toString()));

        // The CSV export (a reader that funnels through getAuthority()) still emits the UUID.
        DSpaceCSV csv = new DSpaceCSV(false);
        csv.addItem(owner);
        String exported = csv.toString();

        context.restoreAuthSystemState();

        assertThat(exported, containsString(target.getID().toString()));
    }

    @Test
    public void testDerivedGetterIsStableAcrossAReloadFromTheDatabase() throws Exception {
        context.turnOffAuthorisationSystem();

        MetadataValue authorValue = itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY).get(0);
        authorityBackedRelationshipService.markRelationshipForResolvedAuthority(context, owner, authorValue, target);
        itemService.update(context, owner);
        context.commit();

        // Evict and reload straight from the DB: proves the null column was persisted, not just
        // cached, and that getAuthority() still derives the UUID after a cold load.
        context.uncacheEntity(owner);
        owner = context.reloadEntity(owner);

        List<MetadataValue> authors =
            itemService.getMetadata(owner, "dc", "contributor", "author", Item.ANY);
        assertThat(authors, hasSize(1));
        assertThat(authors.get(0).getRawAuthority(), nullValue());
        assertThat(authors.get(0).getAuthority(), equalTo(target.getID().toString()));

        context.restoreAuthSystemState();
    }

    @Test
    public void testSolrIndexingSurfacesTheElidedUuidAsAuthorAuthority() throws Exception {
        context.turnOffAuthorisationSystem();

        // A resolved internal reference: the author names the target by UUID with accepted
        // confidence, so the update-loop reconcile mints the row and elides the raw column.
        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("indexed publication")
            .withAuthor("Smith, John", target.getID().toString(), Choices.CF_ACCEPTED)
            .build();
        context.commit();
        indexingService.commit();

        MetadataValue authorValue =
            itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
        assertThat(authorValue.getRawAuthority(), nullValue());
        assertThat(authorValue.getAuthority(), equalTo(target.getID().toString()));

        context.restoreAuthSystemState();

        // The discovery index factory populates author_authority from getAuthority(); with the raw
        // column elided the derived UUID must still drive the person->publications listing.
        DiscoverQuery query = new DiscoverQuery();
        query.addFilterQueries("search.resourcetype:" + IndexableItem.TYPE);
        query.addFilterQueries("author_authority:" + target.getID().toString());
        DiscoverResult result = searchService.search(context, query);

        List<String> foundIds = result.getIndexableObjects().stream()
            .map(IndexableObject::getID)
            .map(Object::toString)
            .collect(Collectors.toList());
        assertThat(foundIds, hasItem(publication.getID().toString()));
    }

    @Test
    public void testDeleteTimeAuthorityCleanupResolvesTheElidedUuid() throws Exception {
        // Item-deletion authority cleanup reads MetadataValue.getAuthority(). For an elided
        // internal reference the raw column is null and the UUID is reconstituted from the
        // relationship endpoint, so the cleanup must run before the deleted item's relationship
        // rows are deleted.
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority",
            new String[] { "org.dspace.content.authority.ItemAuthority = AuthorAuthority" });
        configurationService.setProperty("choices.plugin.dc.contributor.author", "AuthorAuthority");
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", "Person");
        configurationService.setProperty("item-deletion.authority-cleanup.enabled", true);
        configurationService.setProperty(
            "item-deletion.authority-cleanup.mode.dc.contributor.author", "business-identifier");
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();

        context.turnOffAuthorisationSystem();

        Item person = ItemBuilder.createItem(context, collection)
            .withTitle("Doe, Jane")
            .withEntityType("Person")
            .withOrcidIdentifier("0000-0002-1825-0097")
            .build();

        // An accepted internal reference: the update-loop reconcile mints the row and elides the
        // raw authority column, so the publication points at the person only through the row.
        Item publication = ItemBuilder.createItem(context, collection)
            .withTitle("cleanup publication")
            .withAuthor("Doe, Jane", person.getID().toString(), Choices.CF_ACCEPTED)
            .build();
        context.commit();
        indexingService.commit();

        MetadataValue authorValue =
            itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);
        assertThat(authorValue.getRawAuthority(), nullValue());
        assertThat(authorValue.getAuthority(), equalTo(person.getID().toString()));

        UUID personId = person.getID();
        context.uncacheEntity(publication);
        context.uncacheEntity(person);
        person = itemService.find(context, personId);
        itemService.delete(context, person);
        context.commit();

        context.restoreAuthSystemState();

        context.uncacheEntity(publication);
        publication = context.reloadEntity(publication);
        MetadataValue reloaded =
            itemService.getMetadata(publication, "dc", "contributor", "author", Item.ANY).get(0);

        // cleanup resolved the elided UUID and rewrote the authority to the business reference token
        assertThat(reloaded.getAuthority(), equalTo("will be referenced::ORCID::0000-0002-1825-0097"));
        assertThat(reloaded.getConfidence(), equalTo(Choices.CF_UNSET));
        // the internal relationship was torn down; no dangling row
        assertThat(relationshipService.findByItem(context, publication, -1, -1, true), hasSize(0));

        // reset the cleanup + plugin configuration so it cannot bleed into sibling tests
        configurationService.setProperty("item-deletion.authority-cleanup.enabled", null);
        configurationService.setProperty("item-deletion.authority-cleanup.mode.dc.contributor.author", null);
        configurationService.setProperty(
            "plugin.named.org.dspace.content.authority.ChoiceAuthority", null);
        configurationService.setProperty("choices.plugin.dc.contributor.author", null);
        configurationService.setProperty("cris.ItemAuthority.AuthorAuthority.entityType", null);
        pluginService.clearNamedPluginClasses();
        choiceAuthorityService.clearCache();
        metadataAuthorityService.clearCache();
    }
}
