/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.swordapp.server;

public interface CollectionDepositManager
{
    DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth, SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException;
}
