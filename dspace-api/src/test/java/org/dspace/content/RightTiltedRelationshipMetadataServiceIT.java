/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.junit.Test;

/**
 * This class carries out the same test cases as {@link RelationshipMetadataServiceIT} with a few modifications.
 */
public class RightTiltedRelationshipMetadataServiceIT extends RelationshipMetadataServiceIT {

    /**
     * Similar to the parent implementation, but set the tilted property of isIssueOfVolume.
     */
    @Override
    protected void initJournalVolumeIssue() throws Exception {
        context.turnOffAuthorisationSystem();

        Community community = CommunityBuilder.createCommunity(context).build();

        Collection col = CollectionBuilder.createCollection(context, community)
                               .withEntityType("JournalIssue")
                               .build();
        Collection col2 = CollectionBuilder.createCollection(context, community)
                                .withEntityType("JournalVolume")
                                .build();

        EntityType journalIssueEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();
        EntityType publicationVolumeEntityType =
            EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        leftItem = ItemBuilder.createItem(context, col)
            .withPublicationIssueNumber("2").build();
        rightItem = ItemBuilder.createItem(context, col2)
            .withPublicationVolumeNumber("30").build();
        RelationshipType isIssueOfVolume =
            RelationshipTypeBuilder
                .createRelationshipTypeBuilder(context, journalIssueEntityType, publicationVolumeEntityType,
                    "isJournalVolumeOfIssue", "isIssueOfJournalVolume",
                    null, null, null, null).build();

        isIssueOfVolume.setTilted(RelationshipType.Tilted.RIGHT);
        relationshipTypeService.update(context, isIssueOfVolume);

        relationship =
            RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem, isIssueOfVolume).build();
        context.restoreAuthSystemState();
    }

    @Test
    @Override
    public void testGetJournalRelationshipMetadata() throws Exception {
        initJournalVolumeIssue();

        //leftItem is the journal issue item
        //verify the publicationvolume.volumeNumber virtual metadata
        List<MetadataValue> volumeList =
            itemService.getMetadata(leftItem, "publicationvolume", "volumeNumber", null, Item.ANY);
        assertThat(volumeList.size(), equalTo(0));

        //rightItem is the journal volume item
        //verify the publicationissue.issueNumber virtual metadata
        List<MetadataValue> issueList =
            itemService.getMetadata(rightItem, "publicationissue", "issueNumber", null, Item.ANY);
        assertThat(issueList.size(), equalTo(1));
        assertThat(issueList.get(0).getValue(), equalTo("2"));

        //request the virtual metadata of the journal issue
        List<RelationshipMetadataValue> issueRelList =
            relationshipMetadataService.getRelationshipMetadata(leftItem, true);
        assertThat(issueRelList.size(), equalTo(0));

        //request the virtual metadata of the journal volume
        List<RelationshipMetadataValue> volumeRelList =
            relationshipMetadataService.getRelationshipMetadata(rightItem, true);
        assertThat(volumeRelList.size(), equalTo(3));

        assertThat(volumeRelList.get(0).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(volumeRelList.get(0).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(volumeRelList.get(0).getMetadataField().getElement(), equalTo("isIssueOfJournalVolume"));
        assertThat(volumeRelList.get(0).getMetadataField().getQualifier(), equalTo("latestForDiscovery"));
        assertThat(volumeRelList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(volumeRelList.get(1).getValue(), equalTo("2"));
        assertThat(volumeRelList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("publicationissue"));
        assertThat(volumeRelList.get(1).getMetadataField().getElement(), equalTo("issueNumber"));
        assertThat(volumeRelList.get(1).getMetadataField().getQualifier(), equalTo(null));
        assertThat(volumeRelList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(volumeRelList.get(2).getValue(), equalTo(String.valueOf(leftItem.getID())));
        assertThat(volumeRelList.get(2).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(volumeRelList.get(2).getMetadataField().getElement(), equalTo("isIssueOfJournalVolume"));
        assertThat(volumeRelList.get(2).getMetadataField().getQualifier(), nullValue());
        assertThat(volumeRelList.get(2).getAuthority(), equalTo("virtual::" + relationship.getID()));
    }

}
