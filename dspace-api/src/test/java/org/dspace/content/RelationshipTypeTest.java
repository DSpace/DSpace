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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.dspace.content.dao.RelationshipTypeDAO;
import org.dspace.core.Context;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RelationshipTypeTest {
    @InjectMocks
    private RelationshipTypeServiceImpl relationshipTypeService;

    @Mock
    private RelationshipTypeDAO relationshipTypeDAO;

    private RelationshipType firstRelationshipType;
    private RelationshipType secondRelationshipType;

    private Context context;

    @Before
    public void init() {
        // Default state of firstRelationshipType
        firstRelationshipType = mock(RelationshipType.class);
        firstRelationshipType.setId(1);
        firstRelationshipType.setLeftType(mock(EntityType.class));
        firstRelationshipType.setRightType(mock(EntityType.class));
        firstRelationshipType.setLeftwardType("isAuthorOfPublication");
        firstRelationshipType.setRightwardType("isPublicationOfAuthor");
        firstRelationshipType.setLeftMinCardinality(0);
        firstRelationshipType.setLeftMaxCardinality(null);
        firstRelationshipType.setRightMinCardinality(0);
        firstRelationshipType.setRightMinCardinality(null);

        // Default state of secondRelationshipType
        secondRelationshipType = mock(RelationshipType.class);
        secondRelationshipType.setId(new Random().nextInt());
        secondRelationshipType.setLeftType(mock(EntityType.class));
        secondRelationshipType.setRightType(mock(EntityType.class));
        secondRelationshipType.setLeftwardType("isProjectOfPerson");
        secondRelationshipType.setRightwardType("isPersonOfProject");
        secondRelationshipType.setLeftMinCardinality(0);
        secondRelationshipType.setLeftMaxCardinality(null);
        secondRelationshipType.setRightMinCardinality(0);
        secondRelationshipType.setRightMinCardinality(null);
    }


    @Test
    public void testRelationshipTypeFind() throws Exception {
        // Mock DAO to return our firstRelationshipType
        when(relationshipTypeDAO.findByID(any(), any(), any(Integer.class))).thenReturn(firstRelationshipType);

        // Declare objects utilized for this test
        RelationshipType found = relationshipTypeService.find(context, 1);

        // Pass expected and actual RelationshipTypes into comparator method
        checkRelationshipTypeValues(found, firstRelationshipType);
    }

    @Test
    public void testRelationshipTypeFindByTypesAndLabels() throws Exception {
        // Mock DAO to return our firstRelationshipType
        when(relationshipTypeDAO.findbyTypesAndTypeName(any(), any(), any(), any(), any()))
                .thenReturn(firstRelationshipType);

        // Declare objects utilized for this test
        RelationshipType found = relationshipTypeService.findbyTypesAndTypeName(context, mock(EntityType.class),
                mock(EntityType.class),
                "mock", "mock");

        // Pass expected and actual RelationshipTypes into comparator method
        checkRelationshipTypeValues(found, firstRelationshipType);
    }

    @Test
    public void testRelationshipTypeFindAll() throws Exception {
        // Declare objects utilized for this test
        List<RelationshipType> mockedList = new ArrayList<>();
        mockedList.add(firstRelationshipType);
        mockedList.add(secondRelationshipType);

        // Mock DAO to return our mockedList
        when(relationshipTypeDAO.findAll(context, RelationshipType.class, -1, -1)).thenReturn(mockedList);

        // Invoke findAll()
        List<RelationshipType> foundRelationshipTypes = relationshipTypeService.findAll(context);

        // Assert that our foundRelationshipTypes should not be null and contain two RelationshipTypes
        assertThat(foundRelationshipTypes, notNullValue());
        assertThat(foundRelationshipTypes.size(), equalTo(2));
    }

    @Test
    public void testRelationshipTypeFindByLeftOrRightwardType() throws Exception {
        // Declare objects utilized for this test
        List<RelationshipType> mockedList = new ArrayList<>();
        mockedList.add(firstRelationshipType);

        // Mock DAO to return our mockedList
        when(relationshipTypeDAO.findByLeftwardOrRightwardTypeName(context, "mock", -1, -1)).thenReturn(mockedList);

        // Invoke findByLeftwardOrRightwardTypeName()
        List<RelationshipType> found = relationshipTypeService.findByLeftwardOrRightwardTypeName(context, "mock");

        // Assert that our expected list contains our expected RelationshipType and nothing more
        assertThat(found, notNullValue());
        assertThat(found.size(), equalTo(1));
        checkRelationshipTypeValues(found.get(0), firstRelationshipType);
    }

    @Test
    public void testRelationshipTypefindByEntityType() throws Exception {
        // Declare objects utilized for this test
        List<RelationshipType> mockedList = new ArrayList<>();
        mockedList.add(firstRelationshipType);

        // Mock DAO to return our mockedList
        when(relationshipTypeDAO.findByEntityType(any(), any(), any(), any())).thenReturn(mockedList);

        // Invoke findByEntityType()
        List<RelationshipType> found = relationshipTypeService
                .findByEntityType(context, mock(EntityType.class), -1, -1);

        // Assert that our expected list contains our expected RelationshipType and nothing more
        assertThat(found, notNullValue());
        assertThat(found.size(), equalTo(1));
        checkRelationshipTypeValues(found.get(0), firstRelationshipType);
    }

    /**
     * Helper method that compares RelationshipTypes
     * @param found   The reported RelationshipType
     * @param original The original RelationshipType
     */
    private void checkRelationshipTypeValues(RelationshipType found, RelationshipType original) {
        assertThat(found, notNullValue());
        assertThat(found.getLeftwardType(), equalTo(original.getLeftwardType()));
        assertThat(found.getRightwardType(), equalTo(original.getRightwardType()));
        assertThat(found.getLeftType(), equalTo(original.getLeftType()));
        assertThat(found.getRightType(), equalTo(original.getRightType()));
        assertThat(found.getLeftMinCardinality(), equalTo(original.getLeftMinCardinality()));
        assertThat(found.getLeftMaxCardinality(), equalTo(original.getLeftMaxCardinality()));
        assertThat(found.getRightMinCardinality(), equalTo(original.getRightMinCardinality()));
        assertThat(found.getRightMaxCardinality(), equalTo(original.getRightMaxCardinality()));
    }
}
