/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.core;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * A failed business transaction must not be silently replaced or reported as committed.
 */
public class HibernateTransactionFailureTest {

    @Test
    public void sessionAccessMustNotRestartRollbackOnlyTransaction() {
        Transaction transaction = mock(Transaction.class);
        Session session = mock(Session.class);
        HibernateDBConnection connection = rollbackOnlyConnection(session, transaction);

        assertThrows(SQLException.class, connection::getSession);
        verify(transaction, never()).rollback();
        verify(session, never()).beginTransaction();
    }

    @Test
    public void commitMustReportRollbackOnlyTransaction() {
        Transaction transaction = mock(Transaction.class);
        Session session = mock(Session.class);
        HibernateDBConnection connection = rollbackOnlyConnection(session, transaction);

        assertThrows(SQLException.class, connection::commit);
        verify(transaction, never()).commit();
        verify(transaction, never()).rollback();
    }

    private HibernateDBConnection rollbackOnlyConnection(Session session, Transaction transaction) {
        SessionFactory factory = mock(SessionFactory.class);
        when(factory.getCurrentSession()).thenReturn(session);
        when(session.getTransaction()).thenReturn(transaction);
        when(transaction.isActive()).thenReturn(true);
        when(transaction.getStatus()).thenReturn(TransactionStatus.MARKED_ROLLBACK);
        doAnswer(invocation -> {
            when(transaction.getStatus()).thenReturn(TransactionStatus.ACTIVE);
            return transaction;
        }).when(session).beginTransaction();
        HibernateDBConnection connection = new HibernateDBConnection();
        ReflectionTestUtils.setField(connection, "sessionFactory", factory);
        return connection;
    }
}
