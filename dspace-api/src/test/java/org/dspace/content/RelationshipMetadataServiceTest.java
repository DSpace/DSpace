/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.mockito.Mockito.when;

import java.util.LinkedList;

import org.dspace.content.service.RelationshipService;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelationshipMetadataServiceTest {

    @InjectMocks
    private RelationshipMetadataServiceImpl relationshipMetadataService;

    @Mock
    private RelationshipService relationshipService;

    @Mock
    private Item item;
    private Relationship firstRelationship;
    private Relationship secondRelationship;
    private LinkedList<Relationship> list;
    private Context context;

    @Before
    public void init() {
        firstRelationship = new Relationship();
        secondRelationship = new Relationship();
        list = new LinkedList<>();
        list.add(firstRelationship);
        list.add(secondRelationship);
    }

    @Test
    @Ignore
    public void testGetRelationshipMetadata() throws Exception {
        when(item.getMetadata()).thenReturn(new LinkedList<>());
        when(relationshipService.findByItem(context, item)).thenReturn(list);


        String t = "";
        relationshipMetadataService.getRelationshipMetadata(item, true);


    }
}
