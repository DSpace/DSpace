/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.solr.common.SolrDocument;
import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.content.authority.ItemSimpleAuthorityMetadataGenerator;
import org.dspace.discovery.indexobject.ItemIndexFactoryImpl;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.FileSystemXmlApplicationContext;

/**
 * Verifies that the authority metadata generators are wired with their relationship
 * generator delegates by the real Spring configuration and that the delegate is used
 * to produce the expected choice extras.
 *
 * <p>The authority generators are declared in the REST Spring configuration, which is
 * not part of the kernel context used by the DSpace API integration tests. This test
 * therefore starts a child context from the real REST configuration file, using the
 * kernel context as its parent so that the referenced relationship metadata generator
 * beans are resolved exactly as they are at runtime.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class ItemSimpleAuthorityMetadataGeneratorConfigurationIT extends AbstractIntegrationTestWithDatabase {

    private static final String AUTHOR_AFFILIATION_KEY_ID = "oairecerif_author_affiliation";
    private static final String EDITOR_AFFILIATION_KEY_ID = "oairecerif_editor_affiliation";
    private static final String ORCID_KEY_ID = "person_identifier_orcid";

    private static final String AUTHOR_AFFILIATION_SOURCE_FIELD = "oairecerif.person.affiliation";
    private static final String EDITOR_AFFILIATION_SOURCE_FIELD = "person.affiliation.name";
    private static final String ORCID_SOURCE_FIELD = "person.identifier.orcid";

    private static final String STORE_SEPARATOR = ItemIndexFactoryImpl.STORE_SEPARATOR;

    /**
     * Verify that the real REST configuration starts and that every authority metadata
     * generator bean it declares has a delegate configured.
     */
    @Test
    public void testAllAuthorityGeneratorsHaveDelegate() {
        try (FileSystemXmlApplicationContext context = loadRealConfiguration()) {
            Map<String, ItemSimpleAuthorityMetadataGenerator> generators = generatorsByKeyId(context);

            assertFalse("Expected at least one ItemSimpleAuthorityMetadataGenerator bean",
                generators.isEmpty());

            for (ItemSimpleAuthorityMetadataGenerator generator : generators.values()) {
                assertNotNull("Expected a delegate for the authority generator with keyId " + generator.getKeyId(),
                    generator.getDelegate());
                assertNotNull("Expected the delegate of the authority generator with keyId " + generator.getKeyId()
                        + " to declare a source field",
                    generator.getDelegate().getSourceField());
            }
        }
    }

    /**
     * Verify that the configured generators resolve the expected relationship metadata
     * generator beans.
     */
    @Test
    public void testExpectedDelegatesAreWired() {
        ServiceManager serviceManager = new DSpace().getServiceManager();

        Map<String, RelationshipMetadataGenerator> enhancerGenerators =
            serviceManager.getServicesWithNamesByType(RelationshipMetadataGenerator.class);
        assertNotNull("Expected the 'authorOairecerifAffiliationMetadataGenerator' bean to be configured",
            enhancerGenerators.get("authorOairecerifAffiliationMetadataGenerator"));
        assertNotNull("Expected the 'authorOrcidMetadataGenerator' bean to be configured",
            enhancerGenerators.get("authorOrcidMetadataGenerator"));
        assertNotNull("Expected the 'authorAffiliationMetadataGenerator' bean to be configured",
            enhancerGenerators.get("authorAffiliationMetadataGenerator"));

        try (FileSystemXmlApplicationContext context = loadRealConfiguration()) {
            Map<String, ItemSimpleAuthorityMetadataGenerator> generatorsByKeyId = generatorsByKeyId(context);

            assertDelegateSourceField(generatorsByKeyId, AUTHOR_AFFILIATION_KEY_ID, AUTHOR_AFFILIATION_SOURCE_FIELD);
            assertDelegateSourceField(generatorsByKeyId, EDITOR_AFFILIATION_KEY_ID, EDITOR_AFFILIATION_SOURCE_FIELD);
            assertDelegateSourceField(generatorsByKeyId, ORCID_KEY_ID, ORCID_SOURCE_FIELD);
        }
    }

    /**
     * Verify the extras produced for the author affiliation generator.
     */
    @Test
    public void testAuthorAffiliationExtras() {
        try (FileSystemXmlApplicationContext context = loadRealConfiguration()) {
            ItemSimpleAuthorityMetadataGenerator generator = generatorsByKeyId(context).get(AUTHOR_AFFILIATION_KEY_ID);
            assertNotNull("Expected the '" + AUTHOR_AFFILIATION_KEY_ID + "' authority generator to be configured",
                generator);

            SolrDocument document = new SolrDocument();
            document.addField(AUTHOR_AFFILIATION_SOURCE_FIELD + "_stored",
                stored("OrgUnit_1", "OrgUnit_1", "org-unit-uuid", "en"));

            Map<String, String> extras = generator.build("AuthorAuthority", document);

            assertEquals("OrgUnit_1::org-unit-uuid", extras.get(AUTHOR_AFFILIATION_KEY_ID));
            assertEquals("OrgUnit_1::org-unit-uuid", extras.get("data-" + AUTHOR_AFFILIATION_KEY_ID));
        }
    }

    /**
     * Verify the extras produced for the ORCID generator.
     */
    @Test
    public void testOrcidExtras() {
        try (FileSystemXmlApplicationContext context = loadRealConfiguration()) {
            ItemSimpleAuthorityMetadataGenerator generator = generatorsByKeyId(context).get(ORCID_KEY_ID);
            assertNotNull("Expected the '" + ORCID_KEY_ID + "' authority generator to be configured", generator);

            SolrDocument document = new SolrDocument();
            document.addField(ORCID_SOURCE_FIELD + "_stored",
                stored("0000-0001-2345-6789", "0000-0001-2345-6789", "null", "en"));

            Map<String, String> extras = generator.build("AuthorAuthority", document);

            assertEquals("0000-0001-2345-6789", extras.get(ORCID_KEY_ID));
            assertEquals("0000-0001-2345-6789", extras.get("data-" + ORCID_KEY_ID));
        }
    }

    /**
     * The REST Spring configuration is not loaded by the kernel used in the DSpace API
     * integration tests. Load the real file in a child context so that its beans can
     * reference the relationship metadata generators declared by the kernel context.
     */
    private FileSystemXmlApplicationContext loadRealConfiguration() {
        String dspaceDir = new DSpace().getConfigurationService().getProperty("dspace.dir");
        File configuration = new File(dspaceDir, "config/spring/rest/cris-authority-metadatagenerator.xml");

        FileSystemXmlApplicationContext context = new FileSystemXmlApplicationContext();
        context.setParent(new DSpace().getServiceManager().getApplicationContext());
        context.setConfigLocation(configuration.toURI().toString());
        context.refresh();
        return context;
    }

    private Map<String, ItemSimpleAuthorityMetadataGenerator> generatorsByKeyId(ApplicationContext context) {
        Map<String, ItemSimpleAuthorityMetadataGenerator> generatorsByKeyId = new HashMap<>();
        for (ItemSimpleAuthorityMetadataGenerator generator :
            context.getBeansOfType(ItemSimpleAuthorityMetadataGenerator.class).values()) {
            generatorsByKeyId.put(generator.getKeyId(), generator);
        }
        return generatorsByKeyId;
    }

    private void assertDelegateSourceField(Map<String, ItemSimpleAuthorityMetadataGenerator> generatorsByKeyId,
                                           String keyId, String expectedSourceField) {
        ItemSimpleAuthorityMetadataGenerator generator = generatorsByKeyId.get(keyId);
        assertNotNull("Expected the '" + keyId + "' authority generator to be configured", generator);
        assertNotNull("Expected the '" + keyId + "' authority generator to have a delegate", generator.getDelegate());
        assertEquals(expectedSourceField, generator.getDelegate().getSourceField());
    }

    private String stored(String value, String label, String authority, String language) {
        return String.join(STORE_SEPARATOR, value, label, "null", authority, language);
    }
}
