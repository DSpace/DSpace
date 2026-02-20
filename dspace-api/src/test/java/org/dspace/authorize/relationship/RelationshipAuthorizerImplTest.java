/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize.relationship;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.dspace.content.EntityType;
import org.dspace.content.Item;
import org.dspace.content.RelationshipType;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link RelationshipAuthorizerImpl}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class RelationshipAuthorizerImplTest {

    @Mock
    private Item leftItemMock;

    @Mock
    private Item rightItemMock;

    @Mock
    private Context context;

    @Test
    public void testNullRelationshipType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authorizer.canHandleRelationship(context, null, leftItemMock, rightItemMock));

        assertThat(exception.getMessage(), is("The relationship type is required to handle a relationship"));

    }

    @Test
    public void testNullLeftItem() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authorizer.canHandleRelationship(context, relationshipType, null, rightItemMock));

        assertThat(exception.getMessage(), is("The left item is required to handle a relationship"));

    }

    @Test
    public void testNullRightItem() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> authorizer.canHandleRelationship(context, relationshipType, leftItemMock, null));

        assertThat(exception.getMessage(), is("The right item is required to handle a relationship"));

    }

    @Test
    public void testNotMatchingRelationshipTypeForDifferentLeftEntityType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("AnotherType1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForDifferentLeftwardType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "AnotherLeftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForDifferentRightEntityType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "AnotherType2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForDifferentRightwardType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "AnotherRightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForNullEntityType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType(null, "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForNullLeftwardType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", null, "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForDifferentNullEntityType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", null, "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testNotMatchingRelationshipTypeForNullRightwardType() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", null);

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithAndConditionBothTrue() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithAndConditionFirstTrue() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(false);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithAndConditionSecondTrue() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(false);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithAndConditionBothFalse() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(false);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(false);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithOrConditionBothTrue() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithOrConditionFirstTrue() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(false);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithOrConditionSecondTrue() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(false);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithOrConditionBothFalse() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(false);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(false);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(false));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithoutLeftEntityTypeConfigured() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithoutLeftwardTypeConfigured() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setRightEntityType("Type2");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", null, "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithoutRightEntityTypeConfigured() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightwardType("rightType");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithoutRightwardTypeConfigured() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);
        authorizer.setLeftEntityType("Type1");
        authorizer.setLeftwardType("leftType");
        authorizer.setRightEntityType("Type2");
        authorizer.setAndCondition(true);

        RelationshipType relationshipType = mockRelationshipType("Type1", "leftType", "Type2", "rightType");

        boolean result = authorizer.canHandleRelationship(context, relationshipType, leftItemMock, rightItemMock);
        assertThat(result, is(true));

        verify(leftItemAuthorizer).canHandleRelationshipOnItem(context, leftItemMock);
        verify(rightItemAuthorizer).canHandleRelationshipOnItem(context, rightItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    @Test
    public void testMatchingRelationshipTypeWithoutRelationshipTypeAttributesConfigured() {

        RelationshipItemAuthorizer leftItemAuthorizer = mockItemAuthorizer(true);
        RelationshipItemAuthorizer rightItemAuthorizer = mockItemAuthorizer(true);

        RelationshipAuthorizerImpl authorizer = new RelationshipAuthorizerImpl(leftItemAuthorizer, rightItemAuthorizer);

        RelationshipType type = mockRelationshipType("Type1", "leftType", "Type2", "rightType");
        assertThat(authorizer.canHandleRelationship(context, type, leftItemMock, rightItemMock), is(true));

        type = mockRelationshipType("Type1", null, "Type2", "rightType");
        assertThat(authorizer.canHandleRelationship(context, type, leftItemMock, rightItemMock), is(true));

        type = mockRelationshipType("Type1", null, "Type2", null);
        assertThat(authorizer.canHandleRelationship(context, type, leftItemMock, rightItemMock), is(true));

        type = mockRelationshipType("Type1", null, null, null);
        assertThat(authorizer.canHandleRelationship(context, type, leftItemMock, rightItemMock), is(true));

        type = mockRelationshipType(null, null, null, null);
        assertThat(authorizer.canHandleRelationship(context, type, leftItemMock, rightItemMock), is(true));

        verify(leftItemAuthorizer, times(5)).canHandleRelationshipOnItem(context, leftItemMock);
        verifyNoMoreInteractions(leftItemAuthorizer, rightItemAuthorizer);

    }

    private RelationshipType mockRelationshipType(String leftEntityType,
        String leftwardType, String rightEntityType, String rightwardType) {

        RelationshipType relationshipType = mock(RelationshipType.class);

        EntityType mockLeftEntityType = mockEntityType(leftEntityType);
        EntityType mockRightEntityType = mockEntityType(rightEntityType);

        lenient().when(relationshipType.getLeftwardType()).thenReturn(leftwardType);
        lenient().when(relationshipType.getRightwardType()).thenReturn(rightwardType);
        lenient().when(relationshipType.getLeftType()).thenReturn(mockLeftEntityType);
        lenient().when(relationshipType.getRightType()).thenReturn(mockRightEntityType);

        return relationshipType;

    }

    private EntityType mockEntityType(String entityType) {

        if (entityType == null) {
            return null;
        }

        EntityType entity = mock(EntityType.class);
        when(entity.getLabel()).thenReturn(entityType);

        return entity;
    }

    private RelationshipItemAuthorizer mockItemAuthorizer(boolean result) {
        RelationshipItemAuthorizer itemAuthorizer = mock(RelationshipItemAuthorizer.class);
        lenient().when(itemAuthorizer.canHandleRelationshipOnItem(any(), any())).thenReturn(result);
        return itemAuthorizer;
    }

}
