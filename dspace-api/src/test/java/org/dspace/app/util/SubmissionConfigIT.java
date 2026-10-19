/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import static org.junit.Assert.assertEquals;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.submit.factory.SubmissionServiceFactory;
import org.dspace.submit.service.SubmissionConfigService;
import org.junit.Test;

/**
 * Integration Tests for parsing and utilities on submission config forms / readers
 *
 * @author Toni Prieto
 */
public class SubmissionConfigIT extends AbstractIntegrationTestWithDatabase {

    @Test
    public void testSubmissionMapByCommunityHandleSubmissionConfig()
        throws SubmissionConfigReaderException {

        context.turnOffAuthorisationSystem();
        // Sep up a structure with one top community and two subcommunities with collections
        Community topcom = CommunityBuilder.createCommunity(context, "123456789/topcommunity-test")
            .withName("Parent Community")
            .build();
        Community subcom1 = CommunityBuilder.createSubCommunity(context, topcom, "123456789/subcommunity-test")
            .withName("Subcommunity 1")
            .build();
        Community subcom2 = CommunityBuilder.createSubCommunity(context, topcom, "123456789/not-mapped3")
            .withName("Subcommunity 2")
            .build();
        // col1 should use the form item submission form mapped for subcom1
        Collection col1 = CollectionBuilder.createCollection(context, subcom1, "123456789/not-mapped1")
            .withName("Collection 1")
            .build();
        // col2 should use the item submission form mapped for the top community
        Collection col2 = CollectionBuilder.createCollection(context, subcom2, "123456789/not-mapped2")
            .withName("Collection 2")
            .build();
        // col3 should use the item submission form directly mapped for this collection
        Collection col3 = CollectionBuilder.createCollection(context, subcom1, "123456789/collection-test")
            .withName("Collection 3")
            .withEntityType("CustomEntityType")
            .build();
        // col4 should use the item submission form mapped for the entity type CustomEntityType
        Collection col4 = CollectionBuilder.createCollection(context, subcom1, "123456789/not-mapped4")
            .withName("Collection 4")
            .withEntityType("CustomEntityType")
            .build();
        context.restoreAuthSystemState();

        SubmissionConfigService submissionConfigService = SubmissionServiceFactory.getInstance()
            .getSubmissionConfigService();

        // for col1, it should return the item submission form defined for their parent subcom1
        SubmissionConfig submissionConfig1 = submissionConfigService.getSubmissionConfigByCollection(col1);
        assertEquals("subcommunitytest", submissionConfig1.getSubmissionName());

        // for col2, it should return the item submission form defined for topcom
        SubmissionConfig submissionConfig2 = submissionConfigService.getSubmissionConfigByCollection(col2);
        assertEquals("topcommunitytest", submissionConfig2.getSubmissionName());

        // for col3, it should return the item submission form defined directly for the collection
        SubmissionConfig submissionConfig3 = submissionConfigService.getSubmissionConfigByCollection(col3);
        assertEquals("collectiontest", submissionConfig3.getSubmissionName());

        // for col4, it should return the item submission form defined for the entitytype CustomEntityType
        SubmissionConfig submissionConfig4 = submissionConfigService.getSubmissionConfigByCollection(col4);
        assertEquals("entitytypetest", submissionConfig4.getSubmissionName());
    }

    @Test
    public void testSubmissionDefinitionMetadataOverride()
        throws SubmissionConfigReaderException {

        context.turnOffAuthorisationSystem();

        // Set up a community structure using a new community without existing mapping
        Community testCom = CommunityBuilder.createCommunity(context)
            .withName("Test Community")
            .build();

        // Test Case 1: Collection with dspace.submission.definition metadata overrides handle mapping
        // Handle "123456789/collection-test" is mapped to "collectiontest" in XML (line 33),
        // but dspace.submission.definition="modeC" metadata should take precedence
        Collection colWithMetadataOverridesHandle = CollectionBuilder.createCollection(
                context, testCom, "123456789/collection-test")
            .withName("Collection with Metadata Override Handle")
            .withSubmissionDefinition("modeC")
            .build();

        // Test Case 2: Collection with dspace.submission.definition metadata overrides entity type mapping
        // This collection has entity type "CustomEntityType" (mapped to "entitytypetest" in XML line 34),
        // but dspace.submission.definition="funding" should take precedence
        Collection colWithMetadataOverridesEntityType = CollectionBuilder.createCollection(
                context, testCom)
            .withName("Collection with Metadata Override Entity Type")
            .withEntityType("CustomEntityType")
            .withSubmissionDefinition("funding")
            .build();

        // Test Case 3: Collection with dspace.submission.definition metadata overrides community mapping
        // Re-use existing community with mapping: "123456789/subcommunity-test" -> "subcommunitytest" (line 32)
        Community subcomWithMapping = CommunityBuilder.createSubCommunity(
                context, testCom, "123456789/subcommunity-test")
            .withName("Subcommunity with Mapping")
            .build();
        Collection colWithMetadataOverridesCommunity = CollectionBuilder.createCollection(
                context, subcomWithMapping)
            .withName("Collection with Metadata Override Community")
            .withSubmissionDefinition("orgunit")
            .build();

        // Test Case 4: Collection without metadata falls back to entity type mapping
        // Priority: metadata -> handle -> entity type -> community -> default
        // This collection has no metadata, no handle mapping, but has entity type
        Collection colWithoutMetadata = CollectionBuilder.createCollection(
                context, testCom)
            .withName("Collection without Metadata")
            .withEntityType("CustomEntityType")
            .build();

        context.restoreAuthSystemState();

        SubmissionConfigService submissionConfigService = SubmissionServiceFactory.getInstance()
            .getSubmissionConfigService();

        // Test Case 1: Metadata should override handle mapping
        SubmissionConfig config1 = submissionConfigService.getSubmissionConfigByCollection(
            colWithMetadataOverridesHandle);
        assertEquals("Metadata should override handle mapping",
                     "modeC", config1.getSubmissionName());

        // Test Case 2: Metadata should override entity type mapping
        SubmissionConfig config2 = submissionConfigService.getSubmissionConfigByCollection(
            colWithMetadataOverridesEntityType);
        assertEquals("Metadata should override entity type mapping",
                     "funding", config2.getSubmissionName());

        // Test Case 3: Metadata should override community mapping
        SubmissionConfig config3 = submissionConfigService.getSubmissionConfigByCollection(
            colWithMetadataOverridesCommunity);
        assertEquals("Metadata should override community mapping",
                     "orgunit", config3.getSubmissionName());

        // Test Case 4: Without metadata, should fall back to entity type mapping
        SubmissionConfig config4 = submissionConfigService.getSubmissionConfigByCollection(colWithoutMetadata);
        assertEquals("Without metadata, should use entity type mapping",
                     "entitytypetest", config4.getSubmissionName());
    }

    @Test
    public void testSubmissionDefinitionMetadataInvalidValue()
        throws SubmissionConfigReaderException {

        context.turnOffAuthorisationSystem();

        Community testCom = CommunityBuilder.createCommunity(context)
            .withName("Invalid Metadata Test Community")
            .build();

        // Test Case 1: Collection with invalid dspace.submission.definition metadata value
        // and handle mapping -> should log error and fall back to handle mapping
        // Handle "123456789/collection-test" -> "collectiontest" (XML line 33)
        Collection colWithInvalidMetadataHasHandle = CollectionBuilder.createCollection(
                context, testCom, "123456789/collection-test")
            .withName("Collection with Invalid Metadata and Handle")
            .withSubmissionDefinition("non-existent-submission-config")
            .build();

        // Test Case 2: Collection with invalid metadata and entity type (no handle mapping)
        // Should log error, skip invalid metadata, and fall back to entity type mapping
        Collection colWithInvalidMetadataHasEntityType = CollectionBuilder.createCollection(
                context, testCom)
            .withName("Collection with Invalid Metadata and Entity Type")
            .withEntityType("CustomEntityType")
            .withSubmissionDefinition("invalid-config-name")
            .build();

        context.restoreAuthSystemState();

        SubmissionConfigService submissionConfigService = SubmissionServiceFactory.getInstance()
            .getSubmissionConfigService();

        // Test Case 1: Should fall back to handle mapping when metadata value is invalid
        SubmissionConfig config1 = submissionConfigService.getSubmissionConfigByCollection(
            colWithInvalidMetadataHasHandle);
        assertEquals("Invalid metadata should fall back to handle mapping",
                     "collectiontest", config1.getSubmissionName());

        // Test Case 2: Should fall back to entity type mapping when metadata is invalid and no handle mapping
        SubmissionConfig config2 = submissionConfigService.getSubmissionConfigByCollection(
            colWithInvalidMetadataHasEntityType);
        assertEquals("Invalid metadata should fall back to entity type mapping",
                     "entitytypetest", config2.getSubmissionName());
    }

    @Test
    public void testSubmissionDefinitionMetadataPriorityOrder()
        throws SubmissionConfigReaderException {

        context.turnOffAuthorisationSystem();

        // Create community hierarchy
        Community topcom = CommunityBuilder.createCommunity(context, "123456789/topcommunity-test")
            .withName("Top Community with Mapping")
            .build();
        Community subcom = CommunityBuilder.createSubCommunity(context, topcom, "123456789/subcommunity-test")
            .withName("Subcommunity with Mapping")
            .build();

        // Collection that has ALL possible mappings to test priority:
        // 1. dspace.submission.definition metadata
        // 2. handle mapping in XML
        // 3. entity type mapping
        // 4. community mapping
        Collection colWithAllMappings = CollectionBuilder.createCollection(
                context, subcom, "123456789/collection-test")
            .withName("Collection with All Mappings")
            .withEntityType("CustomEntityType")
            .withSubmissionDefinition("patent")
            .build();

        context.restoreAuthSystemState();

        SubmissionConfigService submissionConfigService = SubmissionServiceFactory.getInstance()
            .getSubmissionConfigService();

        // Should use the metadata value "patent" despite having:
        // - handle mapping: "123456789/collection-test" -> "collectiontest"
        // - entity type: "CustomEntityType" -> "entitytypetest"
        // - subcommunity: "123456789/subcommunity-test" -> "subcommunitytest"
        SubmissionConfig config = submissionConfigService.getSubmissionConfigByCollection(colWithAllMappings);
        assertEquals("dspace.submission.definition metadata should have highest priority",
                     "patent", config.getSubmissionName());
    }
}
