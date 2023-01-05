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

import org.dspace.builder.EntityTypeBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.RelationshipBuilder;
import org.dspace.builder.RelationshipTypeBuilder;
import org.junit.Test;

/**
 * This class carries out the same test cases as {@link RelationshipMetadataServiceIT} with a few modifications.
 */
public class LeftTiltedRelationshipMetadataServiceIT extends RelationshipMetadataServiceIT {

    /**
     * Similar to the parent implementation, but set the tilted property of
     * {@link #isAuthorOfPublicationRelationshipType}.
     */
    @Override
    protected void initPublicationAuthor() throws Exception {
        context.turnOffAuthorisationSystem();

        EntityType publicationEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType authorEntityType = EntityTypeBuilder.createEntityTypeBuilder(context, "Author").build();
        leftItem = ItemBuilder.createItem(context, col).build();
        rightItem = ItemBuilder.createItem(context, col2)
                .withPersonIdentifierLastName("familyName")
                .withPersonIdentifierFirstName("firstName").build();
        isAuthorOfPublicationRelationshipType =
                RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publicationEntityType, authorEntityType,
                        "isAuthorOfPublication", "isPublicationOfAuthor",
                        null, null, null, null).build();

        isAuthorOfPublicationRelationshipType.setTilted(RelationshipType.Tilted.LEFT);
        relationshipTypeService.update(context, isAuthorOfPublicationRelationshipType);

        relationship =
                RelationshipBuilder.createRelationshipBuilder(context, leftItem, rightItem,
                        isAuthorOfPublicationRelationshipType).build();

        context.restoreAuthSystemState();
    }

    @Test
    @Override
    public void testGetAuthorRelationshipMetadata() throws Exception {
        initPublicationAuthor();
        //leftItem is the publication
        //verify the dc.contributor.author virtual metadata
        List<MetadataValue> authorList = itemService.getMetadata(leftItem, "dc", "contributor", "author", Item.ANY);
        assertThat(authorList.size(), equalTo(1));
        assertThat(authorList.get(0).getValue(), equalTo("familyName, firstName"));

        //verify the relation.isAuthorOfPublication virtual metadata
        List<MetadataValue> leftRelationshipMetadataList = itemService
            .getMetadata(leftItem, MetadataSchemaEnum.RELATION.getName(), "isAuthorOfPublication", null, Item.ANY);
        assertThat(leftRelationshipMetadataList.size(), equalTo(1));
        assertThat(leftRelationshipMetadataList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));

        //request the virtual metadata of the publication only
        List<RelationshipMetadataValue> leftList = relationshipMetadataService
            .getRelationshipMetadata(leftItem, true);
        assertThat(leftList.size(), equalTo(3));

        assertThat(leftList.get(0).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(leftList.get(0).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(leftList.get(0).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(leftList.get(0).getMetadataField().getQualifier(), equalTo("latestForDiscovery"));
        assertThat(leftList.get(0).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(leftList.get(1).getValue(), equalTo("familyName, firstName"));
        assertThat(leftList.get(1).getMetadataField().getMetadataSchema().getName(), equalTo("dc"));
        assertThat(leftList.get(1).getMetadataField().getElement(), equalTo("contributor"));
        assertThat(leftList.get(1).getMetadataField().getQualifier(), equalTo("author"));
        assertThat(leftList.get(1).getAuthority(), equalTo("virtual::" + relationship.getID()));

        assertThat(leftList.get(2).getValue(), equalTo(String.valueOf(rightItem.getID())));
        assertThat(leftList.get(2).getMetadataField().getMetadataSchema().getName(),
            equalTo(MetadataSchemaEnum.RELATION.getName()));
        assertThat(leftList.get(2).getMetadataField().getElement(), equalTo("isAuthorOfPublication"));
        assertThat(leftList.get(2).getMetadataField().getQualifier(), nullValue());
        assertThat(leftList.get(2).getAuthority(), equalTo("virtual::" + relationship.getID()));

        // rightItem is the author
        List<MetadataValue> rightRelationshipMetadataList = itemService
            .getMetadata(rightItem, MetadataSchemaEnum.RELATION.getName(), "isPublicationOfAuthor", null, Item.ANY);
        assertThat(rightRelationshipMetadataList.size(), equalTo(0));

        //request the virtual metadata of the publication
        List<RelationshipMetadataValue> rightList = relationshipMetadataService
            .getRelationshipMetadata(rightItem, true);
        assertThat(rightList.size(), equalTo(0));
    }

}
