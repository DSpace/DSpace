/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.enhancer.generator;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.content.authority.ItemAuthorityExtraMetadataGenerator;
import org.dspace.content.enhancer.generator.impl.SourceFieldMetadataGenerator;
import org.dspace.kernel.ServiceManager;
import org.dspace.utils.DSpace;
import org.junit.Test;

/**
 * Checks that an authority autocomplete request for a metadata field does not return extra values produced
 * by the relationship metadata generators.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipMetadataGeneratorAuthorityLeakIT extends AbstractIntegrationTestWithDatabase {

    private static final String ORCID_GENERATOR_BEAN_NAME = "authorOrcidMetadataGenerator";

    /**
     * Verify that no enhancer {@link RelationshipMetadataGenerator} bean, and specifically not the
     * configured {@code authorOrcidMetadataGenerator}, is returned by the authority generator discovery.
     */
    @Test
    public void testEnhancerGeneratorsAreNotDiscoveredAsAuthorityGenerators() {
        ServiceManager serviceManager = new DSpace().getServiceManager();

        Map<String, RelationshipMetadataGenerator> enhancerGenerators =
            serviceManager.getServicesWithNamesByType(RelationshipMetadataGenerator.class);

        assertFalse("Expected at least one RelationshipMetadataGenerator bean in the application context",
            enhancerGenerators.isEmpty());

        RelationshipMetadataGenerator orcidGenerator = enhancerGenerators.get(ORCID_GENERATOR_BEAN_NAME);
        assertNotNull("Expected the '" + ORCID_GENERATOR_BEAN_NAME + "' bean to be a RelationshipMetadataGenerator",
            orcidGenerator);
        assertTrue("Expected the '" + ORCID_GENERATOR_BEAN_NAME + "' bean to be a SourceFieldMetadataGenerator",
            orcidGenerator instanceof SourceFieldMetadataGenerator);
        assertSame("Expected the '" + ORCID_GENERATOR_BEAN_NAME + "' bean to be resolved by name",
            orcidGenerator,
            serviceManager.getServiceByName(ORCID_GENERATOR_BEAN_NAME, RelationshipMetadataGenerator.class));

        List<ItemAuthorityExtraMetadataGenerator> authorityGenerators =
            serviceManager.getServicesByType(ItemAuthorityExtraMetadataGenerator.class);

        assertNotNull("Expected the authority generator discovery to return a non-null list", authorityGenerators);
        for (ItemAuthorityExtraMetadataGenerator authorityGenerator : authorityGenerators) {
            assertFalse("Relationship enhancer generator bean '" + ORCID_GENERATOR_BEAN_NAME
                    + "' must not be discovered as an ItemAuthorityExtraMetadataGenerator",
                authorityGenerator instanceof RelationshipMetadataGenerator);
        }
        assertFalse("Relationship enhancer generator bean '" + ORCID_GENERATOR_BEAN_NAME
                + "' must not appear in the authority autocomplete discovery set",
            authorityGenerators.contains(orcidGenerator));

        Map<String, ItemAuthorityExtraMetadataGenerator> authorityGeneratorsByName =
            serviceManager.getServicesWithNamesByType(ItemAuthorityExtraMetadataGenerator.class);
        for (String enhancerGeneratorBeanName : enhancerGenerators.keySet()) {
            assertFalse("Relationship enhancer generator bean '" + enhancerGeneratorBeanName
                    + "' must not be registered under the authority extra metadata generator type",
                authorityGeneratorsByName.containsKey(enhancerGeneratorBeanName));
        }
    }

}
