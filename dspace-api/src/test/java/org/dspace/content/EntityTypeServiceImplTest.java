/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
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
import org.mockito.runners.MockitoJUnitRunner;

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
        when(entityTypeDAO.findByEntityType(context, "TestType")).thenReturn(entityType);
        assertEquals("TestFindByEntityType 0", entityType, entityTypeService.findByEntityType(context, "TestType"));
    }


    @Test
    public void testFindAll() throws Exception {
        List<EntityType> entityTypeList = new ArrayList<>();
        when(entityTypeDAO.findAll(context, EntityType.class)).thenReturn(entityTypeList);
        assertEquals("TestFindAll 0", entityTypeList, entityTypeService.findAll(context));
    }


    @Test
    public void testCreate() throws Exception {
        when(authorizeService.isAdmin(context)).thenReturn(true);
        EntityType entityType = new EntityType();
        entityType.setLabel("Test");
        when(entityTypeDAO.create(any(), any())).thenReturn(entityType);
        assertEquals("TestCreate 0", entityType.getLabel(), entityTypeService.create(context, "Test").getLabel());
        assertEquals("TestCreate 1", entityType, entityTypeService.create(context));
    }

    @Test
    public void testFind() throws Exception {
        when(entityTypeDAO.findByID(context, EntityType.class, 0)).thenReturn(entityType);
        assertEquals("TestFind 0", entityType, entityTypeService.find(context, 0));
    }

    @Test
    public void testUpdate() throws Exception {
        EntityType entityTypeTest = mock(EntityType.class);
        List<EntityType> entityTypeList = new ArrayList<>();
        entityTypeList.add(entityType);
        when(authorizeService.isAdmin(context)).thenReturn(true);
        entityTypeService.update(context, entityTypeTest);
        entityTypeService.update(context, entityTypeList);
        Mockito.verify(entityTypeDAO,times(1)).save(context, entityType);
    }

    @Test
    public void testDelete() throws Exception {
        when(authorizeService.isAdmin(context)).thenReturn(true);
        entityTypeService.delete(context, entityType);
        Mockito.verify(entityTypeDAO,times(1)).delete(context, entityType);
    }

    public EntityType makeEntityType() {
        return new EntityType();
    }


}
