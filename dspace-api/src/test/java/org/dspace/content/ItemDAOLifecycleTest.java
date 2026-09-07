/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import jakarta.persistence.PersistenceException;
import org.dspace.AbstractUnitTest;
import org.dspace.content.dao.ItemDAO;
import org.dspace.utils.DSpace;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for predefined identities and deletion across transaction boundaries.
 */
public class ItemDAOLifecycleTest extends AbstractUnitTest {

    private final ItemDAO dao = new DSpace().getServiceManager().getServiceByName(null, ItemDAO.class);

    /**
     * Preserve a requested UUID when it is unused.
     * @throws Exception if persistence fails
     */
    @Test
    public void createsUnusedPredefinedIdentity() throws Exception {
        UUID id = UUID.randomUUID();
        Item item = dao.create(context, new Item(id));
        context.flush();
        assertEquals(id, item.getID());
        assertNotNull(dao.findByID(context, Item.class, id));
    }

    /**
     * Reject a UUID already held by another item instead of assigning a different UUID.
     * @throws Exception if setup fails
     */
    @Test
    public void rejectsExistingItemIdentity() throws Exception {
        UUID id = UUID.randomUUID();
        dao.create(context, new Item(id));
        context.flush();
        assertThrows(PersistenceException.class, () -> {
            dao.create(context, new Item(id));
            context.flush();
        });
    }

    /**
     * Reject a UUID already held by another DSpace object type.
     * @throws Exception if setup fails
     */
    @Test
    public void rejectsIdentityOfAnotherObjectType() throws Exception {
        UUID id = context.getCurrentUser().getID();
        context.flush();
        assertThrows(PersistenceException.class, () -> {
            dao.create(context, new Item(id));
            context.flush();
        });
    }

    /**
     * Make a replacement visible when a deleted UUID is explicitly reused in the same transaction.
     * @throws Exception if persistence fails
     */
    @Test
    public void recreatesDeletedIdentityInSameTransaction() throws Exception {
        UUID id = UUID.randomUUID();
        Item original = dao.create(context, new Item(id));
        context.flush();
        dao.delete(context, original);
        context.flush();
        assertNull(dao.findByID(context, Item.class, id));

        Item replacement = dao.create(context, new Item(id));
        context.flush();
        assertEquals(id, replacement.getID());
        assertSame(replacement, dao.findByID(context, Item.class, id));
    }

    /**
     * Clear deletion bookkeeping when a rollback restores the database row.
     * @throws Exception if persistence fails
     */
    @Test
    public void rollbackMakesDeletedItemVisibleAgain() throws Exception {
        UUID id = UUID.randomUUID();
        dao.create(context, new Item(id));
        context.commit();
        try {
            Item item = dao.findByID(context, Item.class, id);
            dao.delete(context, item);
            context.flush();
            assertNull(dao.findByID(context, Item.class, id));
            context.rollback();
            assertNotNull(dao.findByID(context, Item.class, id));
        } finally {
            context.rollback();
            // Allow cleanup even when the bookkeeping regression is present.
            context.clearDeletedEntityIds();
            Item remaining = dao.findByID(context, Item.class, id);
            if (remaining != null) {
                dao.delete(context, remaining);
            }
            context.commit();
        }
    }
}
