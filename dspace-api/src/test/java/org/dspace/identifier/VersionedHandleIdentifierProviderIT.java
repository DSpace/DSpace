/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.identifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.CollectionBuilder;
import org.dspace.builder.CommunityBuilder;
import org.dspace.builder.ItemBuilder;
import org.dspace.builder.VersionBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.kernel.ServiceManager;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

public class VersionedHandleIdentifierProviderIT extends AbstractIntegrationTestWithDatabase {

    private List<IdentifierProvider> originalProviders;

    private ServiceManager serviceManager;
    private IdentifierServiceImpl identifierService;
    private List<Object> registeredBeans = new ArrayList<>();

    private String firstHandle;

    private Collection collection;
    private Item itemV1;
    private Item itemV2;
    private Item itemV3;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        serviceManager = DSpaceServicesFactory.getInstance().getServiceManager();
        identifierService = serviceManager.getServicesByType(IdentifierServiceImpl.class).get(0);
        originalProviders = (List<IdentifierProvider>) ReflectionTestUtils.getField(identifierService, "providers");
        context.turnOffAuthorisationSystem();

        identifierService.setProviders(List.of());
        parentCommunity = CommunityBuilder.createCommunity(context)
                .withName("Parent Community")
                .build();
        collection = CollectionBuilder.createCollection(context, parentCommunity)
                .withName("Collection")
                .build();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        super.destroy();
        // restore providers
        identifierService.setProviders(originalProviders);
        // clean beans
        unregisterBeans(registeredBeans);
    }

    private void unregisterBeans(List<Object> registeredBeans) {
        AutowireCapableBeanFactory factory =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getApplicationContext()
                                 .getAutowireCapableBeanFactory();
        Iterator<Object> iterator = registeredBeans.iterator();
        while (iterator.hasNext()) {
            Object registeredBean = iterator.next();
            factory.destroyBean(registeredBean);
            iterator.remove();
            registeredBeans.remove(registeredBean);
        }
    }

    private <T> T registerBean(Class<T> type)
        throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        AutowireCapableBeanFactory factory =
            DSpaceServicesFactory.getInstance()
                                 .getServiceManager()
                                 .getApplicationContext()
                                 .getAutowireCapableBeanFactory();
        // Define our special bean for testing the target class.
        T bean = type.getDeclaredConstructor()
                     .newInstance();

        registeredBeans.add(bean);

        factory.autowireBean(bean);
        return bean;
    }

    private void registerSingleProvider(IdentifierProvider provider) {
        identifierService.setProviders(List.of(provider));
    }

    private <T> T getOrProvide(Class<T> type)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        List<T> servicesByType = serviceManager.getServicesByType(type);
        if (servicesByType == null || servicesByType.isEmpty()) {
            servicesByType = List.of(registerBean(type));
        }
        return servicesByType.get(0);
    }


    private void createVersions() throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();

        itemV1 = ItemBuilder.createItem(context, collection)
                .withTitle("First version")
                .build();
        firstHandle = itemV1.getHandle();
        itemV2 = VersionBuilder.createVersion(context, itemV1, "Second version").build().getItem();
        itemV3 = VersionBuilder.createVersion(context, itemV1, "Third version").build().getItem();

        context.restoreAuthSystemState();
    }

    @Test
    public void testDefaultVersionedHandleProvider() throws Exception {
        registerSingleProvider(
            getOrProvide(VersionedHandleIdentifierProvider.class)
        );
        createVersions();

        // Confirm the original item only has its original handle
        assertEquals(firstHandle, itemV1.getHandle());
        assertEquals(1, itemV1.getHandles().size());
        // Confirm the second item has the correct version handle
        assertEquals(firstHandle + ".2", itemV2.getHandle());
        assertEquals(1, itemV2.getHandles().size());
        // Confirm the last item has the correct version handle
        assertEquals(firstHandle + ".3", itemV3.getHandle());
        assertEquals(1, itemV3.getHandles().size());
    }

    @Test
    public void testCanonicalVersionedHandleProvider() throws Exception {
        registerSingleProvider(
            getOrProvide(VersionedHandleIdentifierProviderWithCanonicalHandles.class)
        );
        createVersions();

        // Confirm the original item only has a version handle
        assertEquals(firstHandle + ".1", itemV1.getHandle());
        assertEquals(1, itemV1.getHandles().size());
        // Confirm the second item has the correct version handle
        assertEquals(firstHandle + ".2", itemV2.getHandle());
        assertEquals(1, itemV2.getHandles().size());
        // Confirm the last item has both the correct version handle and the original handle
        assertEquals(firstHandle, itemV3.getHandle());
        assertEquals(2, itemV3.getHandles().size());
        containsHandle(itemV3, firstHandle + ".3");
    }

    private void containsHandle(Item item, String handle) {
        assertTrue(item.getHandles().stream().anyMatch(h -> handle.equals(h.getHandle())));
    }
}
