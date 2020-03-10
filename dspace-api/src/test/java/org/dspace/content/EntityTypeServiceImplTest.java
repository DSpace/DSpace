/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.dao.EntityTypeDAO;
import org.dspace.core.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EntityTypeServiceImplTest   {

    @InjectMocks
    private EntityTypeServiceImpl entityTypeService;


    @Mock
    private EntityType entityType;

    @Mock
    private EntityTypeDAO entityTypeDAO;

    @Mock
    private Context context;

    @Mock
    private AuthorizeService authorizeService;


    @Test
    public void testFindByEntityType() throws Exception {
        // Mock DAO to return our mocked EntityType
        when(entityTypeDAO.findByEntityType(context, "TestType")).thenReturn(entityType);

        // The EntityType reported from our TestType parameter should match our mocked EntityType
        assertEquals("TestFindByEntityType 0", entityType, entityTypeService.findByEntityType(context, "TestType"));
    }


    @Test
    public void testFindAll() throws Exception {
        // Declare objects utilized in unit test
        List<EntityType> entityTypeList = new ArrayList<>();

        // The EntityType(s) reported from our mocked state should match our entityTypeList
        assertEquals("TestFindAll 0", entityTypeList, entityTypeService.findAll(context));
    }


    @Test
    public void testCreate() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Declare objects utilized in unit test
        EntityType entityType = new EntityType();
        entityType.setLabel("Test");

        // Mock DAO to return our defined EntityType
        when(entityTypeDAO.create(any(), any())).thenReturn(entityType);

        // The newly created EntityType's label should match our mocked EntityType's label
        assertEquals("TestCreate 0", entityType.getLabel(), entityTypeService.create(context, "Test").getLabel());
        // The newly created EntityType should match our mocked EntityType
        assertEquals("TestCreate 1", entityType, entityTypeService.create(context));
    }

    @Test
    public void testFind() throws Exception {
        // Mock DAO to return our mocked EntityType
        when(entityTypeDAO.findByID(context, EntityType.class, 0)).thenReturn(entityType);

        // The reported EntityType should match our mocked entityType
        assertEquals("TestFind 0", entityType, entityTypeService.find(context, 0));
    }

    @Test
    public void testUpdate() throws Exception {
        // Declare objects utilized in unit test
        List<EntityType> entityTypeList = new ArrayList<>();
        entityTypeList.add(entityType);

        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Invoke both impls of method update()
        entityTypeService.update(context, entityType);
        entityTypeService.update(context, entityTypeList);

        // Verify entityTypeDAO.save was invoked twice to confirm proper invocation of both impls of update()
        Mockito.verify(entityTypeDAO,times(2)).save(context, entityType);
    }

    @Test
    public void testDelete() throws Exception {
        // Mock admin state
        when(authorizeService.isAdmin(context)).thenReturn(true);

        // Invoke method delete()
        entityTypeService.delete(context, entityType);

        // Verify entityTypeDAO.delete() ran once to confirm proper invocation of delete()
        Mockito.verify(entityTypeDAO,times(1)).delete(context, entityType);
    }

    /**
     * Helper method that reutrns new EntityType
     * @return new EntityType
     */
    public EntityType makeEntityType() {
        return new EntityType();
    }


}
