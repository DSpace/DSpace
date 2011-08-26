package org.swordapp.server;

public interface CollectionDepositManager
{
    DepositReceipt createNew(String collectionURI, Deposit deposit, AuthCredentials auth, SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException;
}
