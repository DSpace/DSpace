/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.logging.log4j.Logger;
import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelationshipTypeTest {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(RelationshipTypeTest.class);

    @InjectMocks
    private RelationshipTypeServiceImpl relationshipTypeService;

    @Mock
    private RelationshipTypeDAO relationshipTypeDAO;

    private RelationshipType firstRelationshipType;
    private RelationshipType secondRelationshipType;

    private Context context;


    @Before
    public void init() {
        firstRelationshipType = mock(RelationshipType.class);
        firstRelationshipType.setId(new Random().nextInt());
        firstRelationshipType.setLeftType(mock(EntityType.class));
        firstRelationshipType.setRightType(mock(EntityType.class));
        firstRelationshipType.setLeftLabel("isAuthorOfPublication");
        firstRelationshipType.setRightLabel("isPublicationOfAuthor");
        firstRelationshipType.setLeftMinCardinality(0);
        firstRelationshipType.setLeftMaxCardinality(null);
        firstRelationshipType.setRightMinCardinality(0);
        firstRelationshipType.setRightMinCardinality(null);

        secondRelationshipType = mock(RelationshipType.class);
        secondRelationshipType.setId(new Random().nextInt());
        secondRelationshipType.setLeftType(mock(EntityType.class));
        secondRelationshipType.setRightType(mock(EntityType.class));
        secondRelationshipType.setLeftLabel("isProjectOfPerson");
        secondRelationshipType.setRightLabel("isPersonOfProject");
        secondRelationshipType.setLeftMinCardinality(0);
        secondRelationshipType.setLeftMaxCardinality(null);
        secondRelationshipType.setRightMinCardinality(0);
        secondRelationshipType.setRightMinCardinality(null);

    }


    @Test
    public void testRelationshipTypeFind() throws Exception {
        when(relationshipTypeDAO.findByID(any(), any(), any(Integer.class))).thenReturn(firstRelationshipType);
        RelationshipType found = relationshipTypeService.find(context, new Random().nextInt());
        checkRelationshipTypeValues(found, firstRelationshipType);
    }

    @Test
    public void testRelationshipTypeFindByTypesAndLabels() throws Exception {
        when(relationshipTypeDAO.findByTypesAndLabels(any(), any(), any(), any(), any()))
            .thenReturn(firstRelationshipType);
        RelationshipType found = relationshipTypeService.findbyTypesAndLabels(context, mock(EntityType.class),
                                                                              mock(EntityType.class),
                                                                              "mock", "mock");
        checkRelationshipTypeValues(found, firstRelationshipType);
    }

    @Test
    public void testRelationshipTypeFindAll() throws Exception {
        List<RelationshipType> mockedList = new LinkedList<>();
        mockedList.add(firstRelationshipType);
        mockedList.add(secondRelationshipType);
        when(relationshipTypeDAO.findAll(context, RelationshipType.class)).thenReturn(mockedList);
        List<RelationshipType> foundRelationshipTypes = relationshipTypeService.findAll(context);
        assertThat(foundRelationshipTypes, notNullValue());
        assertThat(foundRelationshipTypes.size(), equalTo(2));
    }

    @Test
    public void testRelationshipTypeFindByLeftOrRightLabel() throws Exception {
        List<RelationshipType> mockedList = new LinkedList<>();
        mockedList.add(firstRelationshipType);
        when(relationshipTypeDAO.findByLeftOrRightLabel(any(), any())).thenReturn(mockedList);
        List<RelationshipType> found = relationshipTypeService.findByLeftOrRightLabel(context, "mock");
        assertThat(found, notNullValue());
        assertThat(found.size(), equalTo(1));
        checkRelationshipTypeValues(found.get(0), firstRelationshipType);
    }

    @Test
    public void testRelationshipTypefindByEntityType() throws Exception {
        List<RelationshipType> mockedList = new LinkedList<>();
        mockedList.add(firstRelationshipType);
        when(relationshipTypeDAO.findByEntityType(any(), any())).thenReturn(mockedList);
        List<RelationshipType> found = relationshipTypeService.findByEntityType(context, mock(EntityType.class));
        assertThat(found, notNullValue());
        assertThat(found.size(), equalTo(1));
        checkRelationshipTypeValues(found.get(0), firstRelationshipType);
    }

    private void checkRelationshipTypeValues(RelationshipType found, RelationshipType original) {
        assertThat(found, notNullValue());
        assertThat(found.getLeftLabel(), equalTo(original.getLeftLabel()));
        assertThat(found.getRightLabel(), equalTo(original.getRightLabel()));
        assertThat(found.getLeftType(), equalTo(original.getLeftType()));
        assertThat(found.getRightType(), equalTo(original.getRightType()));
        assertThat(found.getLeftMinCardinality(), equalTo(original.getLeftMinCardinality()));
        assertThat(found.getLeftMaxCardinality(), equalTo(original.getLeftMaxCardinality()));
        assertThat(found.getRightMinCardinality(), equalTo(original.getRightMinCardinality()));
        assertThat(found.getRightMaxCardinality(), equalTo(original.getRightMaxCardinality()));
    }
}
