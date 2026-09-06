/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.dspace.content.Item;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;

/**
 * Verify that a failed removal does not hide an entity from subsequent lookups.
 */
public class AbstractHibernateDAOTest {

    @Test
    public void failedRemoveMustNotMarkEntityDeleted() throws Exception {
        Session session = mock(Session.class);
        Context context = mock(Context.class);
        Item item = mock(Item.class);
        UUID id = UUID.randomUUID();
        when(item.getID()).thenReturn(id);
        doThrow(new IllegalStateException("Removal rejected")).when(session).remove(item);
        AbstractHibernateDAO<Item> dao = new AbstractHibernateDAO<>() {
            @Override
            protected Session getHibernateSession(Context ignored) {
                return session;
            }

            @Override
            protected boolean isEntityRemoved(Context ignored, Object entity) {
                return false;
            }
        };

        assertThrows(IllegalStateException.class, () -> dao.delete(context, item));
        verify(context, never()).markEntityDeleted(id);
    }
}
