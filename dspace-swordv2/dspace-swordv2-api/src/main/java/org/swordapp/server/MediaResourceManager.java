package org.swordapp.server;

import java.util.Map;

public interface MediaResourceManager
{
    MediaResource getMediaResourceRepresentation(String uri, Map<String, String> accept, AuthCredentials auth, SwordConfiguration config)
            throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt replaceMediaResource(String uri, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    void deleteMediaResource(String uri, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;

    DepositReceipt addResource(String uri, Deposit deposit, AuthCredentials auth, SwordConfiguration config)
        throws SwordError, SwordServerException, SwordAuthException;
}
