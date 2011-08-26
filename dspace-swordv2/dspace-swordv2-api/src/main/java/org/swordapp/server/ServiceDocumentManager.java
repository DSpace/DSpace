package org.swordapp.server;

public interface ServiceDocumentManager
{
    ServiceDocument getServiceDocument(String sdUri, AuthCredentials auth, SwordConfiguration config) throws SwordError, SwordServerException, SwordAuthException;
}
