/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.crosswalk;

import static org.dspace.app.matcher.MetadataValueMatcher.with;
import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.core.CrisConstants;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.core.service.PluginService;
import org.jdom.Document;
import org.jdom.input.SAXBuilder;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link CERIFIngestionCrosswalk}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CERIFIngestionCrosswalkIT extends AbstractIntegrationTestWithDatabase {

    private static final String CROSSWALK_DIR_PATH = "./target/testing/dspace/assetstore/crosswalk/";

    private static final String OAI_PMH_DIR_PATH = "./target/testing/dspace/assetstore/oai-pmh/cerif/";

    private static final String METADATA_PLACEHOLDER = CrisConstants.PLACEHOLDER_PARENT_METADATA_VALUE;

    private Community community;

    private Collection collection;

    private CERIFIngestionCrosswalk crosswalk;

    private SAXBuilder builder = new SAXBuilder();

    private PluginService pluginService = CoreServiceFactory.getInstance().getPluginService();

    @Before
    public void setup() throws Exception {

        crosswalk = (CERIFIngestionCrosswalk) pluginService.getNamedPlugin(IngestionCrosswalk.class, "cerif");
        assertThat("A CERIF ingestion crosswalk should be configured", crosswalk, notNullValue());
        crosswalk.setIdPrefix("repository-id::");

        context.turnOffAuthorisationSystem();
        community = createCommunity(context).build();
        collection = createCollection(context, community).withAdminGroup(eperson).build();
        context.restoreAuthSystemState();

    }

    @Test
    @SuppressWarnings("unchecked")
    public void testPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        Document document = readDocument(OAI_PMH_DIR_PATH, "sample-publication.xml");
        crosswalk.ingest(context, item, document.getRootElement(), true);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasItems(with("dc.type", "http://purl.org/coar/resource_type/c_5794", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.title", "Metadata and Semantics Research", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.title.alternative",
            "6th Research Conference, MTSR 2012, Cádiz, Spain, November 28-30, 2012. Proceedings", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.date.issued", "2020-03-30", null, null, 0, -1)));
        assertThat(values, hasItems(with("oaire.citation.startPage", "10", null, null, 0, -1)));
        assertThat(values, hasItems(with("oaire.citation.endPage", "20", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.identifier.doi", "10.1007/978-3-642-35233-1", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.publisher", "Springer, Berlin, Heidelberg", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.subject", "cultural heritage", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.subject", "digital libraries", null, null, 1, -1)));
        assertThat(values, hasItems(with("dc.subject", "learning objects", null, null, 2, -1)));
        assertThat(values, hasItems(with("dc.subject", "linked open data", null, null, 3, -1)));
        assertThat(values, hasItems(with("dc.subject", "scholarly publications", null, null, 4, -1)));

        assertThat(values, hasItems(with("dc.relation.project", "2nd-Generation Open Access Infrastructure", null,
            "will be generated::repository-id::e9ed438e-c7f7-4a18-95e5-3f635ea65fee", 0, 500)));

        assertThat(values, hasItems(with("dc.relation.conference",
            "6th Research Conference on Metadata and Semantics Research", null, null, 0, -1)));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testExportPublicationIngest() throws Exception {
        context.turnOffAuthorisationSystem();
        Item item = ItemBuilder.createItem(context, collection).build();
        context.restoreAuthSystemState();

        Document document = readDocument(CROSSWALK_DIR_PATH, "publication.xml");
        crosswalk.ingest(context, item, document.getRootElement(), true);

        List<MetadataValue> values = item.getMetadata();
        assertThat(values, hasItems(with("dc.type", "http://purl.org/coar/resource_type/c_efa0", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.language.iso", "en", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.title", "Test Publication", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.title.alternative", "Alternative publication title", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.relation.publication", "Published in publication", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.relation.doi", "doi:10.3972/test", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.publisher", "Publication publisher", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.date.issued", "2020-01-01", null, null, 0, -1)));
        assertThat(values, hasItems(with("oaire.citation.volume", "V.01", null, null, 0, -1)));
        assertThat(values, hasItems(with("oaire.citation.issue", "Issue", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.identifier.doi", "doi:111.111/publication", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.identifier.isbn", "978-3-16-148410-0", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.identifier.issn", "2049-3630", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.identifier.isi", "111-222-333", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.identifier.scopus", "99999999", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.subject", "test", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.subject", "export", null, null, 1, -1)));

        assertThat(values, hasItems(with("dc.contributor.author", "John Smith", null, null, 0, -1)));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", METADATA_PLACEHOLDER, null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.contributor.author", "Walter White", null,
            "will be generated::repository-id::6c36b2b0-b2cf-41a5-8241-11d0ea56ed97", 1, 500)));
        assertThat(values, hasItems(with("oairecerif.author.affiliation", "Company", null, null, 1, -1)));

        assertThat(values, hasItems(with("dc.contributor.editor", "Editor", null,
            "will be generated::repository-id::25887329-a648-46f9-a2ac-99319b8e9766", 0, 500)));
        assertThat(values, hasItems(with("oairecerif.editor.affiliation", "Editor Affiliation", null, null, 0, -1)));

        assertThat(values, hasItems(with("dc.relation.project", "Test Project", null,
            "will be generated::repository-id::mock-id", 0, 500)));

        assertThat(values, hasItems(with("dc.relation.funding", "Another Test Funding", null,
            "will be generated::repository-id::mock-id", 0, 500)));

        assertThat(values, hasItems(with("dc.relation.conference", "The best Conference", null, null, 0, -1)));
        assertThat(values, hasItems(with("dc.relation.dataset", "DataSet", null, null, 0, -1)));
    }

    private Document readDocument(String dir, String name) throws Exception {
        try (InputStream inputStream = new FileInputStream(new File(dir, name))) {
            return builder.build(inputStream);
        }
    }
}
